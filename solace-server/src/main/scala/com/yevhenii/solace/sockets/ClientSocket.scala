package com.yevhenii.solace.sockets

import java.net.Socket

import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.formatting.{Formatter, MessageFormatter}
import com.yevhenii.solace.messages.Messages.MessageHolder
import com.yevhenii.solace.processing.{MessageProcessor, Sender}
import com.yevhenii.solace.processing.MessageProcessor.ProcessingResult

import scala.concurrent.{ExecutionContext, Future}
import scala.io.BufferedSource
import scala.concurrent.blocking

class ClientSocket(socket: Socket, messageProcessor: MessageProcessor, sender: Sender, formatter: Formatter)
  extends MessageFormatter {

  val logger = Logger("ClientSocket")

  def getAddress: String = s"${socket.getRemoteSocketAddress}:${socket.getPort}"

  def processLoop()(implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      blocking {
        while (!socket.isClosed && socket.isConnected) {
          process()
        }
      }
    }
  }

  def process()(implicit ec: ExecutionContext): Future[Unit] = {
    val sessionId = getAddress
    logger.info(s"connection accepted [$sessionId]")
//    val buffer: Array[Byte] = new Array[Byte](1024)
//    val output = socket.getInputStream.read(buffer)
    val data = new BufferedSource(socket.getInputStream).mkString

    if (data.isEmpty) {
      logger.error(s"output stream is empty! sessionId: $sessionId")
      Future.failed(new IllegalArgumentException(s"output stream is empty! sessionId: $sessionId"))
    } else {
      formatter
        .unpackOne(data)
        .flatMap(decoded => processDecoded(decoded, sessionId))
    }
  }

  def processDecoded(decoded: List[MessageHolder], sessionId: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val processed: List[Either[String, ProcessingResult]] =
      decoded
        .map(msg => messageProcessor.processMessage(msg, sessionId))

    val errors =
      processed
        .filter(_.isLeft)
        .map(_.left.get)

    val successes = processed
      .filter(_.isRight)
      .map(_.right.get)

    errors.foreach(e => logger.warn(s"error during processing input for $sessionId, error: $e, decoded: ${decoded.mkString("")}"))

    Future.traverse(successes) { res =>
      sender.sendPacket(res.outMessage, sessionId, res.msgType)(socket)
    }.map(_ => ())
  }
  def processDecoded(decoded: MessageHolder, sessionId: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val processed: Either[String, ProcessingResult] = messageProcessor.processMessage(decoded, sessionId)

    processed.fold(
      e => {
        logger.warn(s"error during processing input for $sessionId, error: $e, decoded: $decoded")
        Future.failed(new IllegalArgumentException(s"error during processing input for $sessionId, error: $e, decoded: $decoded"))
      },
      res => sender.sendPacket(res.outMessage, sessionId, res.msgType)(socket)
    )
  }
}
