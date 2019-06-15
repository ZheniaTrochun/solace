package com.yevhenii.solace.processing

import java.util

import cats.data.Writer
import cats.instances.list._
import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.metrics.Metrics._
import com.yevhenii.solace.protocol.OFMatch
import com.yevhenii.solace.table.{MacTable, RedisMacTable}
import org.projectfloodlight.openflow.protocol.`match`.MatchField
import org.projectfloodlight.openflow.protocol.action.OFAction
import org.projectfloodlight.openflow.protocol.{OFFactory, OFMessage, OFPacketIn, OFType}
import org.projectfloodlight.openflow.types.{DatapathId, MacAddress, OFBufferId, OFPort, U64}

import scala.concurrent.{ExecutionContext, Future}

trait PacketInMessageProcessor {

  val logger = Logger(classOf[PacketInMessageProcessor])

  val table: MacTable[String, Short, Future]
  val factory: OFFactory

  def processPacketIn(pi: OFPacketIn)(implicit ec: ExecutionContext, dpid: DatapathId): Future[Writer[Metrics, Option[OFMessage]]] = {
    val inMatch = new OFMatch()
    inMatch.loadFromPacket(pi.getData, pi.getInPort.getShortPortNumber)
    val dlDst = inMatch.getDataLayerDestination
    val dlSrc = inMatch.getDataLayerSource
    val bufferId = pi.getBufferId

    val dlSrcKey = MacAddress.of(dlSrc).toString
    val dlDstKey = MacAddress.of(dlDst).toString
    val msgSize = pi.getData.length

    learnTable(dlSrc, pi.getInPort.getShortPortNumber, dlSrcKey)

    // if the destination is not multicast, look it up
    val outPort: Future[Option[Short]] =
      if ((dlDst(0) & 0x1) == 0) table.get(dlDstKey)
      else Future.successful(None)

    outPort.foreach(_.foreach(p => logger.debug(s"Output port [$p]")))

    def processPort(optPort: Option[Short]): Writer[Metrics, OFMessage] = {
      logger.debug(s"out port = $optPort")
      if (bufferId.getInt == 0xffffffff) {
        packetOut(bufferId, pi, optPort)
      } else {
        optPort.fold(packetOut(bufferId, pi, None)) { p =>
          flowAdd(bufferId, inMatch, p, pi.getInPort.getShortPortNumber)
        }
      }
    }

    outPort.map(processPort)
      .map(_.tell(List(EthernetSender -> dlSrcKey, EthernetReceiver -> dlDstKey, SizeEthernet -> msgSize)))
      .map(_.map(Some(_)))
  }

  def flowAdd(bufferId: OFBufferId, inMatch: OFMatch, outPort: Short, inPort: Short): Writer[Metrics, OFMessage] = {
    logger.debug(s"building packet_out [$bufferId] [$inMatch] [$outPort] [$inPort]")
    val fm = factory.buildFlowAdd()
    fm.setBufferId(bufferId)
    fm.setCookie(U64.ZERO)
    fm.setHardTimeout(0.toShort)
    fm.setIdleTimeout(5.toShort)

    val newMatch = factory.buildMatch()
      .setExact(MatchField.ETH_DST, MacAddress.of(inMatch.getDataLayerDestination))
      .setExact(MatchField.ETH_SRC, MacAddress.of(inMatch.getDataLayerSource))
      .setExact(MatchField.IN_PORT, OFPort.ofShort(inPort))
      .build()

    fm.setMatch(newMatch)
    fm.setOutPort(OFPort.ANY)
    fm.setPriority(0.toShort)
    val action = factory.actions().buildOutput()
      .setMaxLen(0)
      .setPort(OFPort.ofShort(outPort))
      .build()
    val actions = new util.ArrayList[OFAction]
    actions.add(action)
    fm.setActions(actions)

    Writer(
      List(IncomingOF -> "PACKET_IN", ResultOF -> "FLOW_MOD"),
      fm.build()
    )
  }

  def packetOut(bufferId: OFBufferId, pi: OFPacketIn, outPortOpt: Option[Short]): Writer[Metrics, OFMessage] = {
    logger.debug(s"building packet_out [$bufferId] [$pi] [$outPortOpt]")
    val po = factory.buildPacketOut()
    po.setBufferId(bufferId)
    po.setInPort(pi.getInPort)
    // set actions
    val action = factory.actions().buildOutput()
      .setMaxLen(0)
      .setPort(outPortOpt.map(OFPort.ofShort).getOrElse(OFPort.FLOOD))
      .build()

    val actions = new util.ArrayList[OFAction]
    actions.add(action)
    po.setActions(actions)
    // set data if needed
    if (bufferId.getInt == 0xffffffff) {
      po.setData(pi.getData)
    }

    Writer(
      List(IncomingOF -> "PACKET_IN", ResultOF -> "PACKET_OUT"),
      po.build()
    )
  }

  def learnTable(dlSrc: Array[Byte], inPort: Short, dlSrcKey: String)(implicit ec: ExecutionContext, dpid: DatapathId): Future[Unit] = {
//    val key = new String(dlSrc)

    def addPort(optPort: Option[Short]): Unit = {
      optPort.filterNot(_ == inPort).fold[Unit] {
        table.put(dlSrcKey, inPort)
      } { p =>
        logger.debug(s"Table is already contains port $p for $dlSrcKey ($dlSrc)")
      }
    }
    // if the src is not multicast, learn it
    if ((dlSrc(0) & 0x1) == 0) {
      table.get(dlSrcKey).map(addPort)
    } else {
      Future.successful ()
    }
  }
}
