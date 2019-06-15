package com.yevhenii.solace.table

import org.projectfloodlight.openflow.types.DatapathId

class TableWithDatapath[V, M[_]](delegate: MacTable[String, V, M]) extends MacTable[String, V, M] {

  override def contains(key: String)(implicit dpid: DatapathId): M[Boolean] = delegate.contains(s"$dpid-$key")

  override def get(key: String)(implicit dpid: DatapathId): M[Option[V]] = delegate.get(s"$dpid-$key")

  override def put(key: String, value: V)(implicit dpid: DatapathId): M[Boolean] = delegate.put(s"$dpid-$key", value)
}
