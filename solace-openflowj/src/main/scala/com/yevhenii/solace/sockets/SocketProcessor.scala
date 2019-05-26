package com.yevhenii.solace.sockets

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import com.yevhenii.solace.processing.OFSwitch
import com.yevhenii.solace.processing.OFSwitch.PacketIn
import io.netty.buffer.Unpooled
import org.projectfloodlight.openflow.protocol._

import scala.annotation.tailrec
import scala.util.{Success, Try}

class SocketProcessor(connection: ActorRef, remote: InetSocketAddress)
  extends Actor with ActorLogging {

  import Tcp._
  import SocketProcessor._

  implicit val ec = context.dispatcher

  private var dpid = "0"
  private var transferred = 0

  private val config = ConfigFactory.load()
  private val version = config.getInt("solace.protocol")

  private val factory = OFFactories.getFactory(
    OFVersion.values()
      .find(v => v.wireVersion == version)
      .get
  )

  private val switch = context.actorOf(Props(classOf[OFSwitch], factory))

  // sign death pact: this actor terminates when connection breaks
  context.watch(connection)

  override def preStart(): Unit = sayHi()

  // start out in optimistic write-through mode
  def receive: Receive = {
    case Received(data) =>
      //      connection ! Write(data, Connected(remote, localAddress))
      log.info(s"received raw data, size: ${data.size}")
      transferred += data.size
      processData(data)

    case WriteMessage(msg) =>
      write(msg)

    case WriteRawMessage(msg) =>
      write(msg)

    case PeerClosed =>
      log.warning("peer closed")
  }

  override def postStop(): Unit = {
    log.info(s"transferred $transferred bytes from/to [$remote]")
    log.info(s"stopping switch...")
    context.stop(switch)
  }

  def readMessages(data: ByteString): List[OFMessage] = {
    val buffer = Unpooled.copiedBuffer(data.toByteBuffer)

    @tailrec def loop(acc: List[OFMessage]): List[OFMessage] = {
      val optMsg = Option { factory.getReader.readFrom(buffer) }
      optMsg match {
        case Some(msg) => loop(msg :: acc)
        case _ => acc
      }
    }

    loop(Nil)
  }

  def processMessage(message: OFMessage): Unit = {
    message.getType match {
      case OFType.PACKET_IN =>
        log.info(s"Got PACKET_IN from $remote")
        switch ! PacketIn(message.asInstanceOf[OFPacketIn])
      case OFType.HELLO =>
        log.info(s"Got HELLO from $remote")
      case OFType.ECHO_REQUEST =>
        log.info(s"Got ECHO_REQUEST from $remote")
        val reply = factory.buildEchoReply().setXid(message.getXid).build()
        write(reply)
      case t =>
        log.warning(s"Unhandled message $t from $remote")
    }
  }

  def processData(data: ByteString): Unit = {
    readMessages(data).foreach(processMessage)
  }

  def sayHi(): Unit = {
    log.info(s"Got new connection from $remote")
    write(factory.buildHello().build())
    write(factory.featuresRequest())
  }

  def write(message: OFMessage): Unit = {
    log.info(s"Writing to socket message ${message.getType}")
    val buffer = Unpooled.buffer(0, DefaultBufferSize)
    message.writeTo(buffer)
    val nioBuffer = buffer.nioBuffer()
    val bytestring = ByteString(nioBuffer)
    if (message.getType == OFType.FLOW_MOD) {
      println("debug is here")
    }
    connection ! Write(bytestring)
  }

  def write(message: ByteString): Unit = {
    log.info(s"Writing to socket raw message")
    connection ! Write(message)
  }
}

object SocketProcessor {
  val DefaultBufferSize: Int = 65536

  case class WriteMessage(message: OFMessage)
  case class WriteRawMessage(bytes: ByteString)
}