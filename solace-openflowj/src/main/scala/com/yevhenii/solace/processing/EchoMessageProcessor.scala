package com.yevhenii.solace.processing

import cats.data.Writer
import com.yevhenii.solace.metrics.Metrics._
import org.projectfloodlight.openflow.protocol.{OFEchoRequest, OFFactory, OFMessage, OFType}

import scala.concurrent.Future

trait EchoMessageProcessor {

  val factory: OFFactory

  def processEcho(msg: OFEchoRequest): Future[Writer[Metrics, List[OFMessage]]] = Future.successful {
    Writer(
      List(IncomingOF -> OFType.ECHO_REQUEST, ResultOF -> OFType.ECHO_REPLY),
      List(factory.echoReply(msg.getData))
    )
  }
}
