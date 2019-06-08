package com.yevhenii.solace.processing

import cats.data.Writer
import com.yevhenii.solace.metrics.Metrics
import org.projectfloodlight.openflow.protocol.{OFEchoRequest, OFMessage}

import scala.concurrent.Future

trait EchoMessageProcessor {

  def processEcho(msg: OFEchoRequest): Future[Writer[Metrics, OFMessage]] = ???
}
