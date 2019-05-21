package com.yevhenii.solace.table

import scala.language.higherKinds

trait MacTable[K, V, M[_]] {
  def contains(key: K): M[Boolean]

  def get(key: K): M[Option[V]]

  def put(key: K, value: V): M[Boolean]
}
