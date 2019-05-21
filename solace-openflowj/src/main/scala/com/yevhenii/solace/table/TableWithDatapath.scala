package com.yevhenii.solace.table

class TableWithDatapath[V, M[_]](delegate: MacTable[String, V, M])(datapath: String) extends MacTable[String, V, M] {

  override def contains(key: String): M[Boolean] = delegate.contains(s"$datapath-$key")

  override def get(key: String): M[Option[V]] = delegate.get(s"$datapath-$key")

  override def put(key: String, value: V): M[Boolean] = delegate.put(s"$datapath-$key", value)
}
