package com.yevhenii.solace.processing

import cats.data.Writer
import com.yevhenii.solace.metrics.Metrics
import org.projectfloodlight.openflow.protocol.{OFEchoRequest, OFErrorMsg, OFHello, OFMessage, OFPacketIn, OFType}

import scala.concurrent.Future

class MessageProcessor extends HelloMessageProcessor
  with EchoMessageProcessor with PacketInMessageProcessor with ErrorMessageProcessor {

  def processMessage(msg: OFMessage): Future[Writer[Metrics, OFMessage]] = msg.getType match {
    case OFType.PACKET_IN =>
      processPacketInMessage(msg.asInstanceOf[OFPacketIn])
    case OFType.ECHO_REQUEST =>
      processEcho(msg.asInstanceOf[OFEchoRequest])
    case OFType.HELLO =>
      processHello(msg.asInstanceOf[OFHello])
    case OFType.ERROR =>
      processError(msg.asInstanceOf[OFErrorMsg])
    case other =>
      Future.failed(new IllegalArgumentException(other.toString))
  }
}
