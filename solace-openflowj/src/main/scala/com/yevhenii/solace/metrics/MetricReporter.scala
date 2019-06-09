package com.yevhenii.solace.metrics

import com.timgroup.statsd.NonBlockingStatsDClient
import com.typesafe.config.Config
import com.yevhenii.solace.config.ScalaConfig._

class MetricReporter(config: Config, containerId: String = "localhost") {

  val statsdClient = new NonBlockingStatsDClient(
    config.getOrElse("metrics.prefix", ""),
    config.getOrElse("metrics.host", "localhost"),
    config.getOrElse("metrics.port", 8125),
    s"instance:$containerId"
  )

  def report(metrics: Metrics): Unit = metrics match {
    case OpenFlowMetrics(inMsgType, outMsgType, size, sender, time, dpid) =>
      statsdClient.increment("message")
      statsdClient.increment(s"of_in_$inMsgType", s"dpid:$dpid")
      statsdClient.increment(s"of_out_$outMsgType", s"dpid:$dpid")
      statsdClient.count(s"of_size", size, s"dpid:$dpid", s"sender:$sender")
      statsdClient.time(s"of_in_time", time, s"type:$inMsgType")

    case EthernetMetrics(response, size, sender, receiver, time, dpid) =>
      statsdClient.increment("message")
      statsdClient.increment(s"ethernet_in", s"dpid:$dpid", s"from:$sender", s"to:$receiver")
      statsdClient.increment(s"ethernet_out_$response", s"dpid:$dpid", s"from:$sender", s"to:$receiver")
      statsdClient.count(s"ethernet_size", size, s"dpid:$dpid", s"from:$sender", s"to:$receiver")

    case _ =>

  }
}
