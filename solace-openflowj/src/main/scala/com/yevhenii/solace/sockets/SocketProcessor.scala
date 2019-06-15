package com.yevhenii.solace.sockets

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import cats.data.Writer
import cats.instances.list._
import com.typesafe.config.ConfigFactory
import com.yevhenii.solace.metrics.MetricReporter
import com.yevhenii.solace.metrics.Metrics._
import com.yevhenii.solace.processing.{MessageProcessor, OFSwitch}
import com.yevhenii.solace.table.RedisMacTable
import io.netty.buffer.Unpooled
import org.projectfloodlight.openflow.protocol._

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class SocketProcessor(
  connection: ActorRef,
  remote: InetSocketAddress,
  macTable: RedisMacTable,
  metricReporter: MetricReporter
)(
  implicit val ec: ExecutionContext
) extends Actor with ActorLogging {

  import Tcp._
  import SocketProcessor._

  private var dpid = "0"
  private var totalTransferred = 0

  private val config = ConfigFactory.load()
  private val version = config.getInt("solace.of.protocol")

  private val factory = OFFactories.getFactory(
    OFVersion.values()
      .find(v => v.wireVersion == version)
      .get
  )
  private val processor = new MessageProcessor(macTable, factory)
  private val switch = context.actorOf(Props(classOf[OFSwitch], factory))

  // sign death pact: this actor terminates when connection breaks
  context.watch(connection)

//  override def preStart(): Unit = sayHi()

  // start out in optimistic write-through mode
  def receive: Receive = {
    case Received(data) =>
      log.info(s"received raw data, size: ${data.size}")
      totalTransferred += data.size
      processData(data)

    case PeerClosed =>
      log.warning("peer closed")

    case unknown =>
      log.warning(s"unhandled message [$unknown]")
  }

  override def postStop(): Unit = {
    log.info(s"transferred $totalTransferred bytes from/to [$remote]")
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

  def processData(data: ByteString): Unit = {
    def processResponse(respWriterFuture: Future[Writer[Metrics, List[OFMessage]]]): Unit = {
      respWriterFuture.map {
        respWriter =>
          val withLog = for {
            msgs <- respWriter
            _ <- Writer.tell[Metrics](List(SizeOF -> data.size))
            _ <- Writer.tell[Metrics](List(Sender -> s"${remote.getHostString}:${remote.getPort}"))
            _ <- Writer.tell[Metrics](List(DPID -> dpid))
          } yield msgs

          val (log, res) = withLog.run
          metricReporter.report(log)

          res.foreach(write)
      }.failed.foreach { e =>
        log.error("Error during processing message", e)
      }
    }

    readMessages(data)
      .map(processor.processMessage)
      .foreach(processResponse)
  }

//  def sayHi(): Unit = {
//    log.info(s"Got new connection from $remote")
//    write(factory.buildHello().build())
//    write(factory.featuresRequest())
//  }

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
}

object SocketProcessor {
  val DefaultBufferSize: Int = 65536

  case class WriteMessage(message: OFMessage)
  case class WriteRawMessage(bytes: ByteString)

  def props(
    connection: ActorRef,
    remote: InetSocketAddress,
    macTable: RedisMacTable,
    metricReporter: MetricReporter)(
    implicit ec: ExecutionContext
  ): Props = Props(new SocketProcessor(connection, remote, macTable, metricReporter))
}