package com.yevhenii.solace.table

import java.util.concurrent.ConcurrentHashMap

import scala.util.Try

class InMemoryMacTable extends MacTable[Int, Short, IdentityEffect] {
  private val table = new ConcurrentHashMap[Int, Short]()

  override def contains(key: Int): Boolean = table.containsKey(key)

  override def get(key: Int): IdentityEffect[Option[Short]] = IdentityEffect(
    Try(table.get(key)).toOption
  )

  override def put(key: Int, value: Short): IdentityEffect[Option[Short]] = IdentityEffect(
    Option(table.put(key, value))
  )
}

case class IdentityEffect[A](value: A)