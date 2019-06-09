package com.yevhenii.solace.metrics

import org.projectfloodlight.openflow.protocol.OFType

sealed trait Metrics

case class OpenFlowMetrics(
  inMsgType: OFType,
  outMsgType: OFType,
  size: Int,
  sender: String,
  time: Long,
  dpid: String
) extends Metrics

case class EthernetMetrics(
  response: OFType,
  size: Int,
  sender: String,
  receiver: String,
  time: Long,
  dpid: String
) extends Metrics
