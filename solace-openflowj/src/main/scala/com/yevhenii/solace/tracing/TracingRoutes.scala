package com.yevhenii.solace.tracing

import java.util

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.yevhenii.solace.processing.OFSwitch
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.yevhenii.solace.tracing.TracingRoutes.DataHolder
import org.projectfloodlight.openflow.protocol.{OFFactories, OFPacketIn, OFVersion}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.processing.OFSwitch.PacketIn
import com.yevhenii.solace.sockets.SocketProcessor.{DefaultBufferSize, WriteMessage, WriteRawMessage}
import io.netty.buffer.Unpooled

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.io.StdIn
import spray.json._
import spray.json.DefaultJsonProtocol._

class TracingRoutes(
  implicit val mat: ActorMaterializer,
  implicit val sys: ActorSystem,
  implicit val ec: ExecutionContext,
  implicit val timeout: Timeout
) {

  val logger = Logger(TracingRoutes.getClass)
  val factory = OFFactories.getFactory(OFVersion.OF_10)
  val switch = sys.actorOf(Props(classOf[OFSwitch], factory))

  implicit val holderFormat = jsonFormat1(DataHolder)

  val routes = path("test") {
    post {
      entity(as[DataHolder]) { holder =>
        val data = Unpooled.copiedBuffer(holder.data)
        val msg = factory.getReader.readFrom(data)
        val futureResp = switch ? PacketIn(msg.asInstanceOf[OFPacketIn])
        val res = Await.result(futureResp, 20.seconds) match {
          case WriteMessage(m) =>
            logger.info(s"OFSwitch processed message, res = ${m.getType}")
            val buffer = Unpooled.buffer(0, DefaultBufferSize)
            m.writeTo(buffer)
            val nioBuffer = buffer.nioBuffer()
            val resArray = nioBuffer.array()
            println(util.Arrays.toString(resArray))
            println(ByteString(nioBuffer))
            resArray

          case WriteRawMessage(m) =>
            logger.info(s"OFSwitch processed message, res is raw")
            println(m)
            m.asByteBuffer.array()

          case _ => throw new RuntimeException("BAD")
        }

        complete(util.Arrays.toString(res))
      }
    } ~
    get {
      complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
    }
  }
}

object TracingRoutes {
  case class DataHolder(data: Array[Byte])
}
