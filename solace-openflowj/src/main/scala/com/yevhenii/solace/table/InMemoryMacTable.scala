package com.yevhenii.solace.table

import java.util.concurrent.ConcurrentHashMap

import scala.util.Try

class InMemoryMacTable extends MacTable[Int, Short, IdentityEffect] {
  private val table = new ConcurrentHashMap[Int, Short]()

  override def contains(key: Int): Boolean = table.containsKey(key)

  override def get(key: Int): IdentityEffect[Option[Short]] = {
    println(s"table size: ${table.size()}")
    println(s"table get: ${table.get(key)}")
    IdentityEffect(
      Option(table.get(key))
    )
  }

  override def put(key: Int, value: Short): IdentityEffect[Short] = {
    println(s"table before put size: ${table.size()}")
    IdentityEffect(
      table.put(key, value)
    )
  }
}

case class IdentityEffect[A](value: A)