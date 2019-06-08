package com.yevhenii.solace.table

import akka.actor.ActorSystem
import scredis._

import scala.concurrent.{ExecutionContext, Future}

class RedisMacTable(
  host: String,
  port: Int
)(
  implicit system: ActorSystem,
  ec: ExecutionContext
) extends MacTable[String, String, Future] {

  val redis = Client(host = host, port = port)

  override def contains(key: String): Future[Boolean] = get(key).map(_.isDefined)

  override def get(key: String): Future[Option[String]] = redis.get(key)

  override def put(key: String, value: String): Future[Boolean] = redis.set(key, value)
}
