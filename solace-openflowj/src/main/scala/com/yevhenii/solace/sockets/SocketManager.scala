package com.yevhenii.solace.sockets

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props, SupervisorStrategy}
import akka.io._

class SocketManager(host: String, port: Int) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  // there is not recovery for broken connections
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  // bind to the listen port; the port will automatically be closed once this actor dies
  override def preStart(): Unit = {
    IO(Tcp) ! Bind(self, new InetSocketAddress(host, port))
  }

  // do not restart
  override def postRestart(thr: Throwable): Unit = context.stop(self)

  def receive = {
    case Bound(localAddress) =>
      log.info("listening on port {}", localAddress.getPort)

    case CommandFailed(Bind(_, local, _, _, _)) =>
      log.warning(s"cannot bind to [$local]")
      context.stop(self)

    case Connected(remote, local) =>
      log.info("received connection from {}", remote)
      val handler = context.actorOf(Props(classOf[SocketProcessor], sender(), remote))
      sender() ! Register(handler, keepOpenOnPeerClosed = false)
  }

}