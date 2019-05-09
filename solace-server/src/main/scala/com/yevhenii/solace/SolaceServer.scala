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

//  def listen(): Unit = {
//    val server = new ServerSocket(port)
//
//    implicit val ec = ExecutionContext.Implicits.global
//
//    val formatter = new Formatter(ec)
//    val sender = new Sender(formatter)
//    val l2Table = new InMemoryL2()
//    val processor = new MessageProcessor(l2Table, formatter)
//
//    logger.info(s"Solace server started on $port")
//
//    for {
//      i <- (0 until parallelism).par
//    } yield {
//      acceptLoop(i)
//    }
//
//    def acceptLoop(i: Int): Future[Unit] = {
//      logger.info(s"Waiting for connection [$i]")
//      SocketListener(server, processor, sender, formatter)
//        .acceptConnection()
//        .processLoop()
//        .flatMap(_ => acceptLoop(i))
//    }
//  }

  implicit val system = ActorSystem("SolaceServer", config)

  def main(args: Array[String]): Unit = {
//    listen()
    logger.info("before start")


  }

  def run(): Unit = {
    import akka.actor.ActorDSL._
    import scala.concurrent.duration._

    val watcher = inbox()
    watcher.watch(system.actorOf(Props(classOf[EchoManager], classOf[EchoHandler]), "echo"))
    watcher.watch(system.actorOf(Props(classOf[EchoManager], classOf[SimpleEchoHandler]), "simple-echo"))
    watcher.receive(10 minutes)
  }
}
