package com.yevhenii.solace.processing

import cats.data.Writer
import cats.instances.list._
import com.yevhenii.solace.metrics.Metrics._
import com.yevhenii.solace.table.{MacTable, RedisMacTable}
import org.projectfloodlight.openflow.protocol.{OFEchoRequest, OFErrorMsg, OFFactory, OFHello, OFMessage, OFPacketIn, OFType}

import scala.concurrent.{ExecutionContext, Future}

class MessageProcessor(
  val table: MacTable[String, Short, Future],
  val factory: OFFactory
) extends HelloMessageProcessor
  with EchoMessageProcessor
  with PacketInMessageProcessor
  with ErrorMessageProcessor {

  type Result = Writer[Metrics, List[OFMessage]]

  def processMessageInternal(msg: OFMessage)(implicit ec: ExecutionContext): Future[Result] = msg.getType match {
    case OFType.PACKET_IN =>
      processPacketIn(msg.asInstanceOf[OFPacketIn])
    case OFType.ECHO_REQUEST =>
      processEcho(msg.asInstanceOf[OFEchoRequest])
    case OFType.HELLO =>
      processHello(msg.asInstanceOf[OFHello])
    case OFType.ERROR =>
      processError(msg.asInstanceOf[OFErrorMsg])
    case OFType.FEATURES_REPLY =>
      Future.failed(new IllegalArgumentException("Not Implemented yet"))
    case other =>
      Future.failed(new IllegalArgumentException(other.toString))
  }

  def processMessage(msg: OFMessage)(implicit ec: ExecutionContext): Future[Result] = {
//    withErrorHandling(withBenchmark(processMessageInternal)).apply(msg)
    withBenchmark(processMessageInternal).apply(msg)
  }

  def withBenchmark(f: OFMessage => Future[Result])(implicit ec: ExecutionContext): OFMessage => Future[Result] = msg => {
    val start = System.nanoTime()
    val res = f(msg)

    res.map { res =>
      val spentTime = (System.nanoTime() - start) / 1000000
      res.tell(List(ProcessingTime -> spentTime))
    }
  }

  def withErrorHandling(msg: OFMessage => Future[Result])(implicit ec: ExecutionContext): OFMessage => Future[Result] = ???
}
