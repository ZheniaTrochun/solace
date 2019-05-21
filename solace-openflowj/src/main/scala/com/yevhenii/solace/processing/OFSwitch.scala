package com.yevhenii.solace.processing

import akka.actor.{Actor, ActorLogging}
import com.yevhenii.solace.table.InMemoryMacTable
import org.projectfloodlight.openflow.protocol._
import org.projectfloodlight.openflow.protocol.action.OFAction
import org.projectfloodlight.openflow.types.{MacAddress, OFBufferId, OFPort, U64}
import java.util

import com.yevhenii.solace.protocol.OFMatch
import com.yevhenii.solace.sockets.SocketProcessor.WriteMessage
import org.projectfloodlight.openflow.protocol.`match`.MatchField

class OFSwitch(factory: OFFactory) extends Actor with ActorLogging {
  import OFSwitch._

  private val table = new InMemoryMacTable()

  override def receive: Receive = {
    case PacketIn(message) =>
      processPacketIn(message)

    case m =>
      log.warning(s"Unknown message $m")
  }

  def processPacketIn(pi: OFPacketIn): Unit = {
    val inMatch = new OFMatch()
    inMatch.loadFromPacket(pi.getData, pi.getInPort.getShortPortNumber)
    val dlDst = inMatch.getDataLayerDestination
    val dlDstKey = util.Arrays.hashCode(dlDst)
    val dlSrc = inMatch.getDataLayerSource
    val dlSrcKey = util.Arrays.hashCode(dlSrc)
    val bufferId = pi.getBufferId

    learnTable(dlSrc, dlSrcKey, pi.getInPort.getShortPortNumber)

    // if the destination is not multicast, look it up
    val outPort =
      if ((dlDst(0) & 0x1) == 0) table.get(dlDstKey).value
      else None

    outPort.foreach { p =>
      println(p)
    }

    // push a flow mod if we know where the packet should be going
    outPort.foreach(p => flowAdd(bufferId, inMatch, p, pi.getInPort.getShortPortNumber))

    // Send a packet out
    if (outPort.isEmpty || (pi.getBufferId.getInt == 0xffffffff)) {
      packetOut(bufferId, pi, outPort)
    }
  }

  def flowAdd(bufferId: OFBufferId, inMatch: OFMatch, outPort: Short, inPort: Short): Unit = {
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

    sender() ! WriteMessage(fm.build())
  }

  def packetOut(bufferId: OFBufferId, pi: OFPacketIn, outPortOpt: Option[Short]): Unit = {
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

    sender() ! WriteMessage(po.build())
  }

  def learnTable(dlSrc: Array[Byte], dlSrcKey: Int, inPort: Short): Unit = {
    // if the src is not multicast, learn it
    if ((dlSrc(0) & 0x1) == 0) {
      table.get(dlSrcKey).value.filterNot(_ == inPort).fold[Unit] {
        table.put(dlSrcKey, inPort)
      }{ p =>
        log.debug(s"Table is already contains port $p for $dlSrc")
      }
    }
  }
}

object OFSwitch {
  case class PacketIn(message: OFPacketIn)
}