package com.yevhenii.solace.messages

import com.yevhenii.solace.messages.Actions._
import com.yevhenii.solace.messages.Matches._

object Messages {
  case class MessageHolder(message: Message, dpid: Option[String])

  case class Header(`type`: String, xid: Int)

  case class MessageBody(
                          buffer_id: Option[Int] = None,
                          actions: Option[List[Action]] = None,
                          data: Option[MessageBodyData] = None,
                          in_port: Option[Int] = None,
                          command: Option[String] = None,
                          hard_timeout: Option[Int] = None,
                          idle_timeout: Option[Int] = None,
                          priority: Option[Int] = None,
                          out_port: Option[Int] = None,
                          flags: Option[List[String]] = None,
                          `match`: Option[Match] = None,
                          total_len: Option[Int] = None,
                          reason: Option[String] = None,
                          table_id: Option[Int] = None,
                          datapath_id: Option[String] = None,
                          actions_len: Option[Int] = None
                        )

  case class MessageBodyData(`type`: String, data: Array[Byte])

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
                              body: Option[String] = None,
                              shost: String,
                              dhost: String,
                              ethertype: Int,
                              vlan: Option[Int] = None,
                              priority: Option[Int] = None,
                              ip: Option[Ip] = None,
                            )

  import spray.json._
  import spray.json.DefaultJsonProtocol._

  implicit val messageBodyDataFormat = jsonFormat2(MessageBodyData)
  implicit val headerFormat = jsonFormat2(Header)
  implicit val icmpFormat = jsonFormat2(Icmp)
  implicit val ipFormat = jsonFormat6(Ip)
  implicit val messageBodyFormat = jsonFormat16(MessageBody)
  implicit val ethernetMessageFormat = jsonFormat7(EthernetMessage)
  implicit val messageFormat = jsonFormat4(Message)
  implicit val messageHolderFormat = jsonFormat2(MessageHolder)
}