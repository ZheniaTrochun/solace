package com.yevhenii.solace.formatting

import akka.util.ByteString
import com.softwaremill.sttp._
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

  private implicit val backend = HttpURLConnectionBackend()
  private implicit val httpEC = ec

  def pack(outMessage: MessageHolder): Future[String] = {
    val request = sttp.post(packUri).body(outMessage.toJson.toString)
    Future { request.send().unsafeBody }
  }

//  todo check
//  def unpack(bytes: Array[Byte]): Future[List[MessageHolder]] = {
//    val request = sttp.post(unpackUri).body(bytes)
//    Future {
//      request.send()
//        .unsafeBody
//        .parseJson
//        .convertTo[List[MessageHolder]]
//    }
//  }
  def unpack(bytes: ByteString): Future[List[MessageHolder]] = {
    import Formatter._
    import Formatter.rawMessageFormat

    logger.info("unpacking...")
    val rawMessage = RawMessage(bytes.toByteBuffer.array()).toJson.toString()
    logger.info(s"raw message: $rawMessage")
    val request = sttp.post(unpackUri).body(rawMessage).header("Content-Type", "application/json", replaceExisting = true)

    val resultFuture = Future {
      val resp = request.send()
      logger.info(s"response code: ${resp.code}, response: [$resp]")
      resp
    }.map(resp => {
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
//
//  def unpack(data: String): Future[List[MessageHolder]] = {
//    val request = sttp.post(unpackUri).body(data)
//    Future {
//      request.send()
//        .unsafeBody
//        .parseJson
//        .convertTo[List[MessageHolder]]
//    }
//  }

  def unpackOne(data: String): Future[MessageHolder] = {
    val request = sttp.post(unpackUri).body(data)
    Future {
      request.send()
        .unsafeBody
        .parseJson
        .convertTo[MessageHolder]
    }
  }
}

object Formatter {
  case class RawMessage(rawData: Array[Byte])

  implicit val rawMessageFormat: JsonFormat[RawMessage] = jsonFormat1(RawMessage)
}
