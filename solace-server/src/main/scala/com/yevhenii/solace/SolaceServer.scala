package com.yevhenii.solace

import java.net.ServerSocket

import com.corundumstudio.socketio.listener.{ConnectListener, DataListener}
import com.corundumstudio.socketio.{AckRequest, Configuration, SocketIOClient, SocketIOServer}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.formatting.Formatter
import com.yevhenii.solace.l2.{InMemoryL2, L2Table}
import com.yevhenii.solace.processing.{MessageProcessor, Sender}
import com.yevhenii.solace.sockets.{IoSocketServer, SocketListener}

import scala.concurrent.{ExecutionContext, Future}

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

  def main(args: Array[String]): Unit = {
//    listen()
    logger.info("before start")
//    new IoSocketServer(port)

    val serverConf = new Configuration()
    serverConf.setHostname("localhost")
    serverConf.setPort(port)

    val server = new SocketIOServer(serverConf)

    server.addConnectListener { (client: SocketIOClient) =>
      logger.info(s"Connected id: ${client.getSessionId} address: ${client.getRemoteAddress}")
    }

    server.addEventListener[String](
      "message",
      classOf[String],
      (client: SocketIOClient, data: String, ackSender: AckRequest) => {
        logger.info(s"Data received id: ${client.getSessionId} address: ${client.getRemoteAddress} data: $data")
      }
    )

    server.addListeners(new DataListener[String] {
      override def onData(client: SocketIOClient, data: String, ackSender: AckRequest): Unit = {
        logger.info(s"LISTENER 2 data received id: ${client.getSessionId} address: ${client.getRemoteAddress} data: $data")
      }
    })

    server.start()

    logger.info("after start")

    Thread.sleep(Int.MaxValue)

    server.stop()
  }

}
