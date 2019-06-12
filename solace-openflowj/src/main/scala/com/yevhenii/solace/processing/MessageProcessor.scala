package com.yevhenii.solace.processing

import cats.data.Writer
import com.yevhenii.solace.metrics.Metrics._
import com.yevhenii.solace.table.RedisMacTable
import org.projectfloodlight.openflow.protocol.{OFEchoRequest, OFErrorMsg, OFFactory, OFHello, OFMessage, OFPacketIn, OFType}

import scala.concurrent.{ExecutionContext, Future}

class MessageProcessor(
  val table: RedisMacTable,
  val factory: OFFactory
) extends HelloMessageProcessor with EchoMessageProcessor with PacketInMessageProcessor with ErrorMessageProcessor {

  def processMessage(msg: OFMessage)(implicit ec: ExecutionContext): Future[Writer[Metrics, List[OFMessage]]] = msg.getType match {
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
}
