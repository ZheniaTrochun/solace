package com.yevhenii.solace

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.sockets.SocketManager

object Server {
  val logger = Logger(Server.getClass)

  implicit val system = ActorSystem("SolaceSystem")

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    val host = config.getString("solace.host")
    val port = config.getInt("solace.port")

    val manager = system.actorOf(Props(classOf[SocketManager], host, port), "socket-manager")
  }
}
