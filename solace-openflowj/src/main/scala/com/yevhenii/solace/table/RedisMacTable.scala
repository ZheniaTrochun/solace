package com.yevhenii.solace.table

import akka.actor.ActorSystem
import com.typesafe.config.Config
import scredis._

import scala.concurrent.{ExecutionContext, Future}

class RedisMacTable(config: Config)(implicit system: ActorSystem, ec: ExecutionContext)
  extends MacTable[String, Short, Future] {

  val redis = Client(host = config.getString("redis.host"), port = config.getInt("redis.port"))

  override def contains(key: String): Future[Boolean] = get(key).map(_.isDefined)

  override def get(key: String): Future[Option[Short]] = redis.get(key).map(_.map(_.toShort))

  override def put(key: String, value: Short): Future[Boolean] = redis.set(key, value)
}
