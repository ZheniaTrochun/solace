package com.yevhenii.solace.processing

import java.net.Socket

import akka.actor.ActorRef
import akka.io.Tcp.Write
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.formatting.Formatter
import com.yevhenii.solace.messages.Messages._

import scala.concurrent.{ExecutionContext, Future}

class Sender(formatter: Formatter)(implicit val ec: ExecutionContext) {
  val logger = Logger(classOf[Sender])

  def sendPacket(`type`: String, messages: Message, sessionId: String)(socket: ActorRef): Future[Unit] = {
    Future.successful()
  }

  def sendPacket(message: MessageHolder, sessionId: String, `type`: String)(socket: ActorRef): Future[Unit] = {
    logger.info(s"sending message: $`type`, sessionId: $sessionId")
    formatter.pack(message)
      .map(serialized => {
        logger.info(s"Serialized message: [$serialized]")
        serialized
      })
      .map(serialized => socket ! Write(ByteString(serialized)))
  }

//  TODO logging, analytics
  def sendPacket(data: String, socket: ActorRef): Future[Unit] = {
    Future {
      socket ! Write(ByteString(data))
    }
  }
}
