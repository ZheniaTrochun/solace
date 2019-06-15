package com.yevhenii.solace.table

import java.util.concurrent.ConcurrentHashMap

import org.projectfloodlight.openflow.types.DatapathId

import scala.concurrent.{ExecutionContext, Future}

// TODO make dpid implicit
class AsyncInMemoryMacTable(implicit ec: ExecutionContext)
  extends MacTable[String, Short, Future] {
  private val table = new ConcurrentHashMap[String, Short]()

  override def contains(key: String)(implicit dpid: DatapathId): Future[Boolean] = Future.successful(table.containsKey(key))

  override def get(key: String)(implicit dpid: DatapathId): Future[Option[Short]] = Future {Option(table.get(key))}

  override def put(key: String, value: Short)(implicit dpid: DatapathId): Future[Boolean] = Future {
    table.put(key, value)
    true
  }
}

