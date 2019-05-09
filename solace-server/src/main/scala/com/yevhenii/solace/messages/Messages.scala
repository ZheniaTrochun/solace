package com.yevhenii.solace.messages

import com.yevhenii.solace.messages.Actions._
import com.yevhenii.solace.messages.Matches._

object Messages {
  case class MessageHolder(message: Message, dpid: Option[String])

  case class Header(`type`: String, xid: Int)

  case class MessageBody(
                          buffer_id: Int,
                          actions: List[Action],
                          data: String = "",
                          in_port: Option[String] = None,
                          command: Option[String] = None,
                          hard_timeout: Option[Int] = None,
                          idle_timeout: Option[Int] = None,
                          priority: Option[Int] = None,
                          out_port: Option[String] = None,
                          flags: Option[List[String]] = None,
                          `match`: Option[Match] = None
                        )

  case class Icmp(`type`: String, code: String)

  case class Ip(
                 saddr: String,
                 daddr: String,
                 protocol: Int,
                 tcp: Option[Int] = None,
                 udp: Option[Int] = None,
                 icmp: Option[Icmp] = None
               )

  case class Message(
                      header: Header,
                      body: Option[MessageBody],
                      version: String,
                      decodedEthernet: Option[EthernetMessage] = None
                    )

  case class EthernetMessage(
                              body: String,
                              shost: String,
                              dhost: String,
                              ethertype: String,
                              vlan: Option[Int] = None,
                              priority: Option[Int] = None,
                              ip: Option[Ip] = None,
                            )

  import spray.json._
  import spray.json.DefaultJsonProtocol._

  implicit val headerFormat = jsonFormat2(Header)
  implicit val icmpFormat = jsonFormat2(Icmp)
  implicit val ipFormat = jsonFormat6(Ip)
  implicit val messageBodyFormat = jsonFormat11(MessageBody)
  implicit val ethernetMessageFormat = jsonFormat7(EthernetMessage)
  implicit val messageFormat = jsonFormat4(Message)
  implicit val messageHolderFormat = jsonFormat2(MessageHolder)
}