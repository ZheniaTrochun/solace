package com.yevhenii.solace.processing

import cats.data.Writer
import com.yevhenii.solace.metrics.Metrics._
import org.projectfloodlight.openflow.protocol.{OFFactory, OFHello, OFMessage}

import scala.concurrent.Future

trait HelloMessageProcessor {
  val factory: OFFactory

  def processHello(msg: OFHello): Future[Writer[Metrics, List[OFMessage]]] = Future.successful {
    Writer(
      List(IncomingOF -> "HELLO", ResultOF -> "HELLO"),
      Nil
    )
  }
}
