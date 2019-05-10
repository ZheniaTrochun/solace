package com.yevhenii.solace.formatting

import com.yevhenii.solace.messages.Actions._
import com.yevhenii.solace.messages.Flow
import com.yevhenii.solace.messages.Matches._
import com.yevhenii.solace.messages.Messages._

trait MessageFormatter {

  def setSyncMessage(name: String, msg: MessageHolder, dpid: Option[String]): MessageHolder = {
    MessageHolder(
      dpid = dpid, // todo think
      message = Message(
        header = Header(
          `type` = name,
          xid = msg.message.header.xid
        ),
        version = "1.1",
        body = None
      )
    )
  }

  def setOutFloodPacket(message: Message, inPort: Int, dpid: String): MessageHolder = {
    MessageHolder(
      dpid = Some(dpid), // todo think
      message = Message(
        header = Header(
          `type` = "OFPT_PACKET_OUT",
          xid = message.header.xid
        ),
        body = Some(MessageBody(
          buffer_id = message.body.get.buffer_id,
          in_port = Some(inPort),
          actions = Some(List(
            Action(
              ActionHeader(`type` = "OFPAT_OUTPUT"),
              ActionBody(port = "OFPP_FLOOD")
            )
          ))
        )),
        version = "1.1"
      )
    )
  }

  def setFlowModPacket(message: Message, packet: EthernetMessage, inPort: Int, outPort: String, dpid: String): MessageHolder = {
    val flow = extractFlow(packet)
    MessageHolder(
      dpid = Some(dpid), // todo think
      message = Message(
        version = "1.1",
        header = Header(
          `type` = "OFPT_FLOW_MOD",
          xid = message.header.xid
        ),
        body = Some(MessageBody(
          command = Some("OFPFC_ADD"),
          hard_timeout = Some(0),
          idle_timeout = Some(100),
          priority = Some(0x8000),
          buffer_id = message.body.get.buffer_id,
          out_port = Some("OFPP_NONE"),
          flags = Some(List("OFPFF_SEND_FLOW_REM")),
          `match` = Some(Match(
            header = MatchHeader("OFPMT_STANDARD"),
            body = MatchBody(
              wildcards = 0,
              in_port = inPort,
              dl_src = flow.dlSrc,
              dl_dst = flow.dlDst,
              dl_vlan = flow.dlVlan,
              dl_vlan_pcp = flow.dlVlanPcp,
              dl_type = flow.dlType,
              nw_src = flow.nwSrc,
              nw_dst = flow.nwDst,
              nw_proto = flow.nwProto,
              tp_src = flow.tpSrc,
              tp_dst = flow.tpDst
            )
          )),
          actions = Some(List(
            Action(
              ActionHeader("OFPAT_OUTPUT"),
              ActionBody(outPort)
            )
          )),
          in_port = None
        ))
      )
    )
  }

  def extractFlow(packet: EthernetMessage): Flow = {
    import MessageFormatter._

    def extractIp: (String, String, Int) =
      packet.ip.map(ip => (ip.saddr, ip.daddr, ip.protocol)).getOrElse((defaultAddr, defaultAddr, 0))

    def extractTP: (String, String) = {
      packet.ip
        .flatMap(ip =>
          if (ip.udp.isDefined || ip.tcp.isDefined) {
            Some((ip.saddr, ip.daddr))
          } else {
            ip.icmp.map(icmp => (icmp.`type`, icmp.code))
          }
        )
        .getOrElse((defaultAddr, defaultAddr))
    }

    val (dlVlan, dlVlanPcp) = (packet.vlan.getOrElse(0xfff), packet.priority.getOrElse(0))
    val (nwSrc, nwDst, nwProto) = extractIp
    val (tpSrc, tpDst) = extractTP

    Flow(
      dlSrc = packet.shost,
      dlDst = packet.dhost,
      dlType = packet.ethertype,
      dlVlan,
      dlVlanPcp,
      nwSrc,
      nwDst,
      nwProto,
      tpSrc,
      tpDst
    )
  }
}

object MessageFormatter {
  val defaultAddr = "0.0.0.0"
}
