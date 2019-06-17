package com.yevhenii.solace.table

import java.util.concurrent.ConcurrentHashMap

import org.projectfloodlight.openflow.types.DatapathId

import scala.concurrent.Future

class AsyncInMemoryMacTable()
  extends MacTable[String, Short, Future] {
  private val table = new ConcurrentHashMap[String, Short]()

  override def contains(key: String)(implicit dpid: DatapathId): Future[Boolean] = Future.successful { table.containsKey(key) }

  override def get(key: String)(implicit dpid: DatapathId): Future[Option[Short]] = Future.successful { Option(table.get(key)) }

  override def put(key: String, value: Short)(implicit dpid: DatapathId): Future[Boolean] = Future.successful {
    table.put(key, value)
    true
  }
}

