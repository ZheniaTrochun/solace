package com.yevhenii.solace.sockets

import com.typesafe.scalalogging.Logger


class IoSocketServer (port: Int) {

  val logger = Logger(this.getClass)

  def start(port: Int) = {

  }

}
//package com.yevhenii.solace.sockets
//
//import com.typesafe.scalalogging.Logger
//import io.socket.client.{IO, Socket}
//import io.socket.emitter.Emitter
//
//class IoSocketServer (port: Int) {
//  val socket = IO.socket(s"http://localhost:${port}")
//  val logger = Logger(this.getClass)
//
//  val onConnect = new Emitter.Listener {
//    override def call(args: AnyRef*): Unit = {
//      logger.info("CONNECT")
//      logger.info(args.map(_.toString).mkString("\n"))
//    }
//  }
//
//  val onReceive = new Emitter.Listener {
//    override def call(args: AnyRef*): Unit = {
//      logger.info("DATA RECEIVED")
//      logger.info(args.map(_.toString).mkString("\n"))
//    }
//  }
//
//  socket
//    .on(Socket.EVENT_CONNECT, onConnect)
//    .on(Socket.EVENT_MESSAGE, onReceive)
//
//}
