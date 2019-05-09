package com.yevhenii.solace

import java.net.ServerSocket

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.formatting.Formatter
import com.yevhenii.solace.l2.{InMemoryL2, L2Table}
import com.yevhenii.solace.processing.{MessageProcessor, Sender}
import com.yevhenii.solace.sockets.{IoSocketServer, SocketListener}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object SolaceServer {

  val logger = Logger(SolaceServer.getClass)

  val config = ConfigFactory.load()
  val port = config.getInt("server.port")
  val parallelism = config.getInt("server.parallelism")

  implicit val system = ActorSystem("SolaceServer", config)

  def main(args: Array[String]): Unit = {

    try run()
    finally system.terminate()

  }

  def run(): Unit = {
    import akka.actor.ActorDSL._
    import scala.concurrent.duration._

    val watcher = inbox()
    watcher.watch(system.actorOf(Props(classOf[Manager], classOf[SocketProcessor]), "solace"))
    watcher.receive(10 minutes)
  }
}
