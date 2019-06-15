package com.yevhenii.solace.table

import org.projectfloodlight.openflow.types.DatapathId

import scala.language.higherKinds

trait MacTable[K, V, M[_]] {
  def contains(key: K)(implicit dpid: DatapathId): M[Boolean]

  def get(key: K)(implicit dpid: DatapathId): M[Option[V]]

  def put(key: K, value: V)(implicit dpid: DatapathId): M[Boolean]
}
