package com.yevhenii.solace.table

import java.util.concurrent.ConcurrentHashMap

import cats.{Id, Monad}

class InMemoryMacTable extends MacTable[Int, Short, Id] {
  private val table = new ConcurrentHashMap[Int, Short]()

  override def contains(key: Int): Id[Boolean] = Monad[Id].pure(table.containsKey(key))

  override def get(key: Int): Id[Option[Short]] = Monad[Id].pure(
    Option(table.get(key))
  )

  override def put(key: Int, value: Short): Id[Boolean] = {
    table.put(key, value)
    true
  }
}