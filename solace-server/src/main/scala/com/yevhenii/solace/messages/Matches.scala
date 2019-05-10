package com.yevhenii.solace.messages

object Matches {
  case class MatchHeader(`type`: String)

  case class MatchBody(
                        wildcards: Int,
                        in_port: Int,
                        dl_src: String,
                        dl_dst: String,
                        dl_type: String,
                        dl_vlan: Int,
                        dl_vlan_pcp: Int,
                        nw_src: String,
                        nw_dst: String,
                        nw_proto: Int,
                        tp_src: String,
                        tp_dst: String
                      )

  case class Match(header: MatchHeader, body: MatchBody)

  import spray.json._
  import spray.json.DefaultJsonProtocol._

  implicit val matchHeaderFormat = jsonFormat1(MatchHeader)
  implicit val matchBodyFormat = jsonFormat12(MatchBody)
  implicit val matchFormat = jsonFormat2(Match)
}
