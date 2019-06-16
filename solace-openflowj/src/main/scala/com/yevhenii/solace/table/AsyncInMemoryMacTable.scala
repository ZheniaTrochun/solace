package com.yevhenii.solace.table

import java.util.concurrent.ConcurrentHashMap

import org.projectfloodlight.openflow.types.DatapathId

import scala.concurrent.{ExecutionContext, Future}

class AsyncInMemoryMacTable(ec: ExecutionContext)
  extends MacTable[String, Short, Future] {
  implicit val context = ec
  private val table = new ConcurrentHashMap[String, Short]()

  override def contains(key: String)(implicit dpid: DatapathId): Future[Boolean] = Future { table.containsKey(s"$dpid-$key") }

  override def get(key: String)(implicit dpid: DatapathId): Future[Option[Short]] = Future { Option(table.get(s"$dpid-$key")) }

  override def put(key: String, value: Short)(implicit dpid: DatapathId): Future[Boolean] = Future {
    table.put(s"$dpid-$key", value)
    true
  }
}

