package com.yevhenii.solace.metrics

object Metrics {
  type Metrics = List[(String, Any)]

  val IncomingOF = "of_incoming_message"
  val ResultOF = "of_result_message"
  val SizeOF = "of_message_size"
  val ProcessingTime = "processing_time"
}
