package com.yevhenii.solace.metrics

import cats.data.Writer

object Metrics {
  type Metrics = List[(String, Any)]

  val IncomingOF = "of_incoming_message"
  val ResultOF = "of_result_message"
  val SizeOF = "of_message_size"
  val ProcessingTime = "processing_time"

  val Sender = "sender"
  val DPID = "dpid"

  val EthernetSender = "ethernet_sender"
  val EthernetReceiver = "ethernet_receiver"
  val SizeEthernet = "ethernet_size"

  val NetworkPacketCount = "network_packet_count"
  val NetworkByteCount = "network_byte_count"
  val NetworkFlowCount = "network_flow_count"

  def recordMetric(key: String, value: Any): Writer[Metrics, Unit] = Writer.tell(List(key -> value))
}
