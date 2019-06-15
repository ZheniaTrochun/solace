package com.yevhenii.solace.processing

import cats.data.Writer
import com.yevhenii.solace.metrics.Metrics._
import org.projectfloodlight.openflow.protocol.{OFErrorMsg, OFMessage}

import scala.concurrent.Future

trait ErrorMessageProcessor {

  def processError(msg: OFErrorMsg): Future[Writer[Metrics, Option[OFMessage]]] = Future.successful {
    Writer(List(IncomingOF -> "ERROR"), None)
  }
}
