package com.yevhenii.solace.processing

import java.net.Socket

import akka.actor.ActorRef
import akka.io.Tcp.Write
import akka.util.ByteString
import com.yevhenii.solace.formatting.Formatter
import com.yevhenii.solace.messages.Messages._

import scala.concurrent.{ExecutionContext, Future}

class Sender(formatter: Formatter)(implicit val ec: ExecutionContext) {

  def sendPacket(`type`: String, messages: Message, sessionId: String)(socket: ActorRef): Future[Unit] = {
    Future.successful()
  }

  def sendPacket(messages: MessageHolder, sessionId: String, `type`: String)(socket: ActorRef): Future[Unit] = {
    Future.successful()
  }

//  TODO logging, analytics
  def sendPacket(data: String, socket: ActorRef): Future[Unit] = {
    Future{
      socket ! Write(ByteString(data))
    }
  }
}
