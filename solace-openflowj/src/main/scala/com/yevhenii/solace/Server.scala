package com.yevhenii.solace

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.sockets.SocketManager
import com.yevhenii.solace.table.RedisMacTable
import com.yevhenii.solace.tracing.TracingRoutes

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration._
import scala.language.postfixOps

object Server {
  val logger = Logger(Server.getClass)

  implicit val system = ActorSystem("SolaceSystem")
  implicit val mat = ActorMaterializer()
  implicit val ec = Implicits.global
  implicit val timeout = Timeout(20 seconds)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    val host = config.getString("solace.host")
    val port = config.getInt("solace.port")

    val redisHost = config.getString("redis.host")
    val redisPort = config.getInt("redis.port")

    val metricsHost = config.getString("metrics.host")
    val metricsPort = config.getInt("metrics.port")

    val redis = new RedisMacTable(redisHost, redisPort)

    val manager = system.actorOf(Props(classOf[SocketManager], host, port), "socket-manager")

    val tracingRoutes = new TracingRoutes()
    Http().bindAndHandle(tracingRoutes.routes, "localhost", 8081)
  }
}
