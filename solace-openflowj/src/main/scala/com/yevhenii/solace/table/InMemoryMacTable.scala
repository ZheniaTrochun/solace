package com.yevhenii.solace.table

import java.util.concurrent.ConcurrentHashMap

import cats.{Id, Monad}
import org.projectfloodlight.openflow.types.DatapathId

class InMemoryMacTable extends MacTable[Int, Short, Id] {
  private val table = new ConcurrentHashMap[Int, Short]()

  override def contains(key: Int)(implicit dpid: DatapathId): Id[Boolean] = Monad[Id].pure(table.containsKey(key))

  override def get(key: Int)(implicit dpid: DatapathId): Id[Option[Short]] = Monad[Id].pure(
    Option(table.get(key))
  )

  override def put(key: Int, value: Short)(implicit dpid: DatapathId): Id[Boolean] = {
    table.put(key, value)
    true
  }
}