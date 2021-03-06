package com.yevhenii.solace.formatting

import akka.util.ByteString
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.messages.Messages._
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Formatter(ec: ExecutionContext) {
  val logger = Logger(classOf[Formatter])
  private val config = ConfigFactory.load()

  private val host = config.getString("formatter.host")
  private val port = config.getInt("formatter.port")

  val unpackUri = uri"http://$host:$port/unpack"
  val packUri = uri"http://$host:$port/pack"

  private implicit val backend = AkkaHttpBackend()
  private implicit val httpEC = ec

  @deprecated def pack(outMessage: MessageHolder): Future[String] = {
    val request = sttp.post(packUri)
      .body(outMessage.toJson.toString)
      .header("Content-Type", "application/json", replaceExisting = true)

    val response = request.send()

    response.andThen {
      case Success(res) =>
        logger.info(s"Pack response code: ${res.code}, response: [$res]")
      case Failure(e) =>
        logger.error("Pack request failed", e)
    }

    response.map(_.unsafeBody)
  }

  def pack(outMessage: Message, `type`: String): Future[String] = {
    import Formatter.PackRequest
    import Formatter.packRequestFormat

    val request = sttp.post(packUri)
      .body(PackRequest(outMessage, `type`).toJson.toString)
      .header("Content-Type", "application/json", replaceExisting = true)

    val response = request.send()

    response.andThen {
      case Success(res) =>
        logger.info(s"Pack response code: ${res.code}, response: [$res]")
      case Failure(e) =>
        logger.error("Pack request failed", e)
    }

    response.map(_.unsafeBody)
  }


  def unpack(bytes: ByteString): Future[List[MessageHolder]] = {
    import Formatter.RawMessage
    import Formatter.rawMessageFormat

    logger.info("unpacking...")
    val rawMessage = RawMessage(bytes.toByteBuffer.array()).toJson.toString()
    logger.info(s"raw message: $rawMessage")
    val request = sttp.post(unpackUri).body(rawMessage).header("Content-Type", "application/json", replaceExisting = true)

    val resultFuture = request.send()
      .map(resp => {
        logger.info(s"response code: ${resp.code}, response: [$resp]")
        resp
      })
      .map(resp => {
        resp
          .unsafeBody
          .parseJson
          .convertTo[List[MessageHolder]]
      })

    resultFuture.andThen {
      case Success(parsed) =>
        logger.info(s"Parsed response: $parsed")
        parsed

      case Failure(e) =>
        logger.error("Failed parsing response", e)
    }
  }

  def unpackOne(data: String): Future[MessageHolder] = {
    val request = sttp.post(unpackUri).body(data)
    request.send()
      .map(_.unsafeBody.parseJson.convertTo[MessageHolder])
  }
}

object Formatter {
  case class RawMessage(rawData: Array[Byte])

  case class PackRequest(message: Message, `type`: String)

  implicit val rawMessageFormat: JsonFormat[RawMessage] = jsonFormat1(RawMessage)
  implicit val packRequestFormat: JsonFormat[PackRequest] = jsonFormat2(PackRequest)
}
