package com.yevhenii.solace.table

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.{ExecutionContext, Future}

class AsyncInMemoryMacTable(implicit ec: ExecutionContext)
  extends MacTable[String, Short, Future] {
  private val table = new ConcurrentHashMap[String, Short]()

  override def contains(key: String): Future[Boolean] = Future.successful(table.containsKey(key))

  override def get(key: String): Future[Option[Short]] = Future {Option(table.get(key))}

  override def put(key: String, value: Short): Future[Boolean] = Future {
    table.put(key, value)
    true
  }
}

