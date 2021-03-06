package com.yevhenii.solace.processing

import cats.data.Writer
import cats.instances.list._
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.solace.metrics.Metrics._
import com.yevhenii.solace.table.MacTable
import org.projectfloodlight.openflow.protocol._
import org.projectfloodlight.openflow.types.DatapathId

import scala.concurrent.{ExecutionContext, Future}

class MessageProcessor(
  val table: MacTable[String, Short, Future],
  val factory: OFFactory
) extends HelloMessageProcessor
  with EchoMessageProcessor
  with PacketInMessageProcessor
  with ErrorMessageProcessor
  with StatsReplyProcessor
  with LazyLogging {

  type Result = Writer[Metrics, List[OFMessage]]

  def processMessageInternal(msg: OFMessage)(implicit ec: ExecutionContext, dpid: DatapathId): Future[Result] = msg.getType match {
    case OFType.PACKET_IN =>
      processPacketIn(msg.asInstanceOf[OFPacketIn])
    case OFType.ECHO_REQUEST =>
      processEcho(msg.asInstanceOf[OFEchoRequest])
    case OFType.HELLO =>
      processHello(msg.asInstanceOf[OFHello])
    case OFType.ERROR =>
      processError(msg.asInstanceOf[OFErrorMsg])
    case OFType.STATS_REPLY =>
      processStatsReply(msg.asInstanceOf[OFStatsReply])
    case OFType.FEATURES_REPLY =>
      Future.failed(new IllegalArgumentException("Not Implemented yet"))
    case other =>
      Future.failed(new IllegalArgumentException(s"Not Implemented yet: $other"))
  }

  def processMessage(msg: OFMessage)(implicit ec: ExecutionContext, dpid: DatapathId): Future[Result] = {
//    withErrorHandling(withBenchmark(processMessageInternal)).apply(msg)
    withBenchmark(processMessageInternal).apply(msg)
  }

  def withBenchmark(f: OFMessage => Future[Result])(implicit ec: ExecutionContext, dpid: DatapathId): OFMessage => Future[Result] = msg => {
    val start = System.nanoTime()
    val res = f(msg)

    res.map { res =>
      val spentTime = (System.nanoTime() - start) / 1000000
      res.tell(List(ProcessingTime -> spentTime))
    }
  }

  def withErrorHandling(msg: OFMessage => Future[Result])(implicit ec: ExecutionContext, dpid: DatapathId): OFMessage => Future[Result] = ???
}
