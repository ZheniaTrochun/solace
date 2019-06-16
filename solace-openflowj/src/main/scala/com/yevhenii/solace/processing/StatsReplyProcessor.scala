package com.yevhenii.solace.processing

import cats.data.Writer
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.solace.metrics.Metrics._
import org.projectfloodlight.openflow.protocol.{OFAggregateStatsReply, OFMessage, OFStatsReply, OFStatsType}

import scala.concurrent.Future

trait StatsReplyProcessor extends LazyLogging {

  def processStatsReply(msg: OFStatsReply): Future[Writer[Metrics, List[OFMessage]]] = {
    msg.getStatsType match {
      case OFStatsType.AGGREGATE =>
        processAggregateStatsReply(msg.asInstanceOf[OFAggregateStatsReply])
      case t =>
        logger.warn(s"Received not supported stats $msg")
        Future.failed(new IllegalArgumentException(s"$t not supported"))
    }
  }

  def processAggregateStatsReply(msg: OFAggregateStatsReply): Future[Writer[Metrics, List[OFMessage]]] = {
    Future.successful {
      Writer(
        List(
          NetworkByteCount -> msg.getByteCount.getValue,
          NetworkFlowCount -> msg.getFlowCount,
          NetworkPacketCount -> msg.getPacketCount.getValue
        ),
        Nil
      )
    }
  }
}
