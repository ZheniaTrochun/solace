package com.yevhenii.solace.table

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.projectfloodlight.openflow.types.DatapathId
import scredis._

import scala.concurrent.{ExecutionContext, Future}

class RedisMacTable(config: Config)(implicit system: ActorSystem, ec: ExecutionContext)
  extends MacTable[String, Short, Future] {

  val redis = Client(host = config.getString("redis.host"), port = config.getInt("redis.port"))

  override def contains(key: String)(implicit dpid: DatapathId): Future[Boolean] = get(s"$dpid-$key").map(_.isDefined)

  override def get(key: String)(implicit dpid: DatapathId): Future[Option[Short]] = redis.get(s"$dpid-$key").map(_.map(_.toShort))

  override def put(key: String, value: Short)(implicit dpid: DatapathId): Future[Boolean] = redis.set(s"$dpid-$key", value)
}
