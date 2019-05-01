package com.yevhenii.solace.formatting

import com.softwaremill.sttp._
import com.typesafe.config.ConfigFactory
import com.yevhenii.solace.messages.Messages._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class Formatter(ec: ExecutionContext) {
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
