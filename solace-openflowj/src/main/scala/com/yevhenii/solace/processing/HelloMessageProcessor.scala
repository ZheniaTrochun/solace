package com.yevhenii.solace.processing

import cats.data.Writer
import com.yevhenii.solace.metrics.Metrics
import org.projectfloodlight.openflow.protocol.{OFHello, OFMessage}

import scala.concurrent.Future

trait HelloMessageProcessor {
  def processHello(msg: OFHello): Future[Writer[Metrics, OFMessage]] = ???
}
