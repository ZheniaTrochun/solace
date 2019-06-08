package com.yevhenii.solace.processing

import cats.data.Writer
import com.yevhenii.solace.metrics.Metrics
import org.projectfloodlight.openflow.protocol.{OFMessage, OFPacketIn}

import scala.concurrent.Future

trait PacketInMessageProcessor {

  def processPacketInMessage(msg: OFPacketIn): Future[Writer[Metrics, OFMessage]] = ???
}
