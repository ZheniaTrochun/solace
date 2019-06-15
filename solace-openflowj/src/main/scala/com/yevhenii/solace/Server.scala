package com.yevhenii.solace

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.metrics.MetricReporter
import com.yevhenii.solace.sockets.SocketManager
import com.yevhenii.solace.table.{AsyncInMemoryMacTable, RedisMacTable}
import com.yevhenii.solace.tracing.TracingRoutes
import com.yevhenii.solace.config.ScalaConfig._

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration._
import scala.language.postfixOps

object Server {
  val logger = Logger(Server.getClass)

  implicit val system = ActorSystem("SolaceSystem")
  implicit val mat = ActorMaterializer()
  implicit val ec = Implicits.global
  implicit val timeout = Timeout(20 seconds)

  val containerId = Option(System.getenv("HOSTNAME")).getOrElse("localhost")
  val config = ConfigFactory.load()
  val host = config.getString("solace.host")
  val ofPort = config.getInt("solace.of.port")

  val httpEnabled = config.getOrElse("solace.http.enabled", false)

  def main(args: Array[String]): Unit = {
//    val macTable = new RedisMacTable(config)
    val macTable = new AsyncInMemoryMacTable
    val metricReporter = new MetricReporter(config, containerId)

    val manager = system.actorOf(SocketManager.props(host, ofPort, macTable, metricReporter), "socket-manager")

    logger.info(s"Solace server instance started in container [$containerId] on [$host:$ofPort]")

    if (httpEnabled) {
      startHttpClient()
    } else {
      logger.info(s"No Solace http client available in $containerId")
    }
  }

  def startHttpClient(): Unit = {
    val httpPort = config.getOrElse("solace.http.port", 8081)

    val tracingRoutes = new TracingRoutes()
    Http().bindAndHandle(tracingRoutes.routes, "localhost", httpPort)

    logger.info(s"Solace server http client started in container [$containerId] on [$host:$httpPort]")
  }
}
