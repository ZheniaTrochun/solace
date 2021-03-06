package com.yevhenii.solace

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props, SupervisorStrategy}
import akka.io._
import akka.util.ByteString
import com.yevhenii.solace.formatting.Formatter
import com.yevhenii.solace.l2.InMemoryL2
import com.yevhenii.solace.messages.Messages.MessageHolder
import com.yevhenii.solace.processing.{MessageProcessor, Sender}
import com.yevhenii.solace.processing.MessageProcessor.ProcessingResult

import scala.concurrent.{ExecutionContext, Future}

class Manager(handlerClass: Class[_]) extends Actor with ActorLogging {

  import Tcp._
  import context.system

    implicit val ec = ExecutionContext.Implicits.global

    val formatter = new Formatter(ec)
    val senderManager = new Sender(formatter)
    val l2Table = new InMemoryL2()
    val processor = new MessageProcessor(l2Table, formatter)

  // there is not recovery for broken connections
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  // bind to the listen port; the port will automatically be closed once this actor dies
  override def preStart(): Unit = {
    IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", 6633))
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
      val handler = context.actorOf(Props(handlerClass, sender(), remote, processor, senderManager, formatter))
      sender() ! Register(handler, keepOpenOnPeerClosed = true)
  }

}

object SocketProcessor {
  final case class Ack(offset: Int) extends Tcp.Event

  final case class SetDPID(dpid: String)

  def props(connection: ActorRef, remote: InetSocketAddress,
            messageProcessor: MessageProcessor, senderManager: Sender,
            formatter: Formatter): Props =
    Props(classOf[SocketProcessor], connection, remote, messageProcessor, senderManager, formatter)
}

class SocketProcessor(connection: ActorRef,
                  remote: InetSocketAddress,
                  messageProcessor: MessageProcessor,
                  senderManager: Sender,
                  formatter: Formatter
                 ) extends Actor with ActorLogging {

  import Tcp._
  import SocketProcessor._

  implicit val ec = context.dispatcher

  val localAddress = new InetSocketAddress("0.0.0.0", 6633)

  private var dpid = "0"

  // sign death pact: this actor terminates when connection breaks
  context.watch(connection)

  // start out in optimistic write-through mode
  def receive: Receive = {
    case Received(data) =>
//      connection ! Write(data, Connected(remote, localAddress))
      log.info(s"received raw data, size: ${data.size}")
      formatter.unpack(data).flatMap(list => processDecoded(list, remote.toString)(self))(context.dispatcher)

    case Ack(ack) =>
      log.warning("ack")

    case CommandFailed(Write(_, Ack(ack))) =>
      log.warning("command failed")

    case PeerClosed =>
      log.warning("peer closed")

    case SetDPID(id) =>
      dpid = id
  }

  override def postStop(): Unit = {
    log.info(s"transferred $transferred bytes from/to [$remote]")
  }

  //#storage-omitted
  private var storageOffset = 0
  private var storage = Vector.empty[ByteString]
  private var stored = 0L
  private var transferred = 0L

  val maxStored = 100000000L
  val highWatermark = maxStored * 5 / 10
  val lowWatermark = maxStored * 3 / 10
  private var suspended = false

  private def currentOffset = storageOffset + storage.size

  //#helpers
  private def buffer(data: ByteString): Unit = {
    storage :+= data
    stored += data.size

    if (stored > maxStored) {
      log.warning(s"drop connection to [$remote] (buffer overrun)")
      context.stop(self)

    } else if (stored > highWatermark) {
      log.debug(s"suspending reading at $currentOffset")
      connection ! SuspendReading
      suspended = true
    }
  }

  def processDecoded(decoded: List[MessageHolder], sessionId: String)(socket: ActorRef)(implicit ec: ExecutionContext): Future[Unit] = {
    val processed: List[Either[String, List[ProcessingResult]]] =
      decoded
        .map(msg => messageProcessor.processMessage(msg, sessionId)(socket, dpid))

    val errors =
      processed
        .filter(_.isLeft)
        .map(_.left.get)

    val successes = processed
      .filter(_.isRight)
      .flatMap(_.right.get)

    errors.foreach(e => log.warning(s"error during processing input for $sessionId, error: $e, decoded: ${decoded.mkString("")}"))
    successes.foreach(res => log.info(s"Successful processed: [$res]"))

    Future.traverse(successes) { res =>
      senderManager.sendPacket(res.outMessage, sessionId, res.msgType)(connection)
    }.map(_ => ())
  }

  def processDecodedOne(decoded: MessageHolder, sessionId: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val processed: Either[String, List[ProcessingResult]] = messageProcessor.processMessage(decoded, sessionId)(self, dpid)

    processed.fold(
      e => {
        log.warning(s"error during processing input for $sessionId, error: $e, decoded: $decoded")
        Future.failed(new IllegalArgumentException(s"error during processing input for $sessionId, error: $e, decoded: $decoded"))
      },
      res =>
        Future.traverse(res) {
          msg => senderManager.sendPacket(msg.outMessage, sessionId, msg.msgType)(connection)
        }.map(_ => ())
    )
  }
}