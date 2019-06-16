package com.yevhenii.solace.metrics

import com.timgroup.statsd.NonBlockingStatsDClient
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import com.yevhenii.solace.config.ScalaConfig._
import com.yevhenii.solace.metrics.Metrics._

class MetricReporter(config: Config, containerId: String = "localhost") extends LazyLogging {

  val statsdClient = new NonBlockingStatsDClient(
    config.getOrElse("metrics.prefix", ""),
    config.getOrElse("metrics.host", "localhost"),
    config.getOrElse("metrics.port", 8125),
    s"instance:$containerId"
  )

  def report(metrics: Metrics): Unit = {
    val asMap = metrics.toMap
    val dpid = asMap.getOrElse(DPID, "")
    val sender = asMap.getOrElse(Sender, "")
    val inMsgType = asMap.getOrElse(IncomingOF, "")

    val ethernetSender = asMap.get(EthernetSender)
    val ethernetReceiver = asMap.get(EthernetReceiver)
    val ethernetSize = asMap.get(SizeEthernet)

    for {
      ethernetSender <- asMap.get(EthernetSender)
      ethernetReceiver <- asMap.get(EthernetReceiver)
      ethernetSize <- asMap.get(SizeEthernet)
    } statsdClient.count("ethernet_size", s"$ethernetSize".toLong, s"dpid:$dpid", s"from:$ethernetSender", s"to:$ethernetReceiver")

    statsdClient.increment("message")

    asMap.foreach { case (key, value) => logger.debug(s"[$key] -> [$value]") }

    asMap.foreach {
      case (IncomingOF, msg) => statsdClient.increment(s"of_in_$msg", s"dpid:$dpid")
      case (ResultOF, msg) => statsdClient.increment(s"of_out_$msg", s"dpid:$dpid")
      case (SizeOF, size: Long) => statsdClient.count("of_size", size, s"dpid:$dpid", s"sender:$sender")
      case (ProcessingTime, time: Long) => statsdClient.count("of_in_time", time, s"dpid:$dpid", s"type:$inMsgType")
      case (key, value) =>
    }
  }
}
