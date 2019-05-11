package com.yevhenii.solace.table

import scala.language.higherKinds

trait MacTable[K, V, F[_]] {
  def contains(key: K): Boolean

  def get(key: K): F[Option[V]]

  def put(key: K, value: V): F[Option[V]]
}
