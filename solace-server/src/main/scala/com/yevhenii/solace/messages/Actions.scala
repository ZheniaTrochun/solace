package com.yevhenii.solace.messages

object Actions {
  case class ActionHeader(`type`: String)
  case class ActionBody(port: Int, max_len: Int = 0xffffffff)
  case class Action(header: ActionHeader, body: ActionBody)

  import spray.json._
  import spray.json.DefaultJsonProtocol._

  implicit val actionHeaderFormat = jsonFormat1(ActionHeader)
  implicit val actionBodyFormat = jsonFormat2(ActionBody)
  implicit val actionFormat = jsonFormat2(Action)
}

