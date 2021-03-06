package com.yevhenii.solace.sockets

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props, SupervisorStrategy}
import akka.io._
import com.yevhenii.solace.metrics.MetricReporter
import com.yevhenii.solace.table.MacTable

import scala.concurrent.{ExecutionContext, Future}

class SocketManager(host: String, port: Int, macTable: MacTable[String, Short, Future], metricReporter: MetricReporter)(implicit ec: ExecutionContext)
  extends Actor with ActorLogging {

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
      log.error(s"cannot bind to [$local]")
      context.stop(self)

    case Connected(remote, _) =>
      log.info("received connection from {}", remote)
      val handler = context.actorOf(SocketProcessor.props(sender(), remote, macTable, metricReporter))
      sender() ! Register(handler, keepOpenOnPeerClosed = true)

    case msg =>
      log.warning(s"unhandled message [$msg]")
  }
}

object SocketManager {
  def props(host: String, port: Int, macTable: MacTable[String, Short, Future], metricReporter: MetricReporter)
    (implicit ec: ExecutionContext): Props = Props(
      new SocketManager(host, port, macTable, metricReporter)
  )
}
