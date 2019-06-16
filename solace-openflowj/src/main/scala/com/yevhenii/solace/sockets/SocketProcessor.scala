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
import com.yevhenii.solace.table.MacTable
import io.netty.buffer.Unpooled
import org.projectfloodlight.openflow.protocol._
import org.projectfloodlight.openflow.types.{DatapathId, OFGroup, OFPort, TableId}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class SocketProcessor(
  connection: ActorRef,
  remote: InetSocketAddress,
  macTable: MacTable[String, Short, Future],
  metricReporter: MetricReporter
)(
  implicit val ec: ExecutionContext
) extends Actor with ActorLogging {

  import Tcp._
  import SocketProcessor._

  implicit var dpid: DatapathId = DatapathId.NONE
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

  val statsRequests = context.system.scheduler.schedule(StatsRequestFrequency, StatsRequestFrequency, self, AskStats)

  // sign death pact: this actor terminates when connection breaks
  context.watch(connection)

  override def preStart(): Unit = sayHi()

  // start out in optimistic write-through mode
  def receive: Receive = {
    case Received(data) =>
      log.info(s"received raw data, size: ${data.size}")
      totalTransferred += data.size
      processData(data)

    case AskStats =>
      log.info("asking for stats")
      val request = factory.buildAggregateStatsRequest()
        .setOutGroup(OFGroup.ANY)
        .setOutPort(OFPort.ANY)
        .setTableId(TableId.ALL)
        .build()
      write(request)

    case PeerClosed =>
      log.warning("peer closed")

    case unknown =>
      log.warning(s"unhandled message [$unknown]")
  }

  override def postStop(): Unit = {
    log.info(s"transferred $totalTransferred bytes from/to [$remote]")
    log.info(s"stopping switch...")
    statsRequests.cancel()
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
        e.printStackTrace()
      }
    }

    val msgs = readMessages(data)
    // TODO move to better place
    msgs.find(_.getType == OFType.FEATURES_REPLY).map(_.asInstanceOf[OFFeaturesReply]).foreach(msg => dpid = msg.getDatapathId)

    msgs.filterNot(_.getType == OFType.FEATURES_REPLY)
      .map(processor.processMessage)
      .foreach(processResponse)
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
    connection ! Write(bytestring)
  }
}

object SocketProcessor {
  val DefaultBufferSize: Int = 65536
  val StatsRequestFrequency = 10 seconds

  case class WriteMessage(message: OFMessage)
  case class WriteRawMessage(bytes: ByteString)
  case object AskStats

  def props(
    connection: ActorRef,
    remote: InetSocketAddress,
    macTable: MacTable[String, Short, Future],
    metricReporter: MetricReporter)(
    implicit ec: ExecutionContext
  ): Props = Props(new SocketProcessor(connection, remote, macTable, metricReporter))
}