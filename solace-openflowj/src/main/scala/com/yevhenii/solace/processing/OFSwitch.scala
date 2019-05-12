package com.yevhenii.solace.processing

import java.nio.ByteBuffer

import akka.actor.{Actor, ActorLogging}
import com.yevhenii.solace.table.InMemoryMacTable
import org.projectfloodlight.openflow.protocol._
import org.projectfloodlight.openflow.protocol.action.OFAction
import org.projectfloodlight.openflow.types.{MacAddress, OFBufferId, OFPort, U16, U64}
import java.util

import akka.io.Tcp.Write
import akka.util.ByteString
import com.yevhenii.solace.protocol.{OFActionOld, OFActionOutput, OFMatch, OFFlowMod => OFFlowModOld}
import com.yevhenii.solace.sockets.SocketProcessor.{DefaultBufferSize, WriteMessage, WriteRawMessage}
import io.netty.buffer.Unpooled
import org.projectfloodlight.openflow.protocol.`match`.{Match, MatchField}

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
//    val inMatch = pi.getMatch
//    val dlDst = inMatch.get(MatchField.ETH_DST).getBytes
//    val dlDstKey = util.Arrays.hashCode(dlDst)
//    val dlSrc = inMatch.get(MatchField.ETH_SRC).getBytes
//    val dlSrcKey = util.Arrays.hashCode(dlSrc)
//    val bufferId = pi.getBufferId
//
    val inMatch = new OFMatch()
    inMatch.loadFromPacket(pi.getData, pi.getInPort.getShortPortNumber)
    val dlDst = inMatch.getDataLayerDestination
    val dlDstKey = util.Arrays.hashCode(dlDst)
    val dlSrc = inMatch.getDataLayerSource
    val dlSrcKey = util.Arrays.hashCode(dlSrc)
    val bufferId = pi.getBufferId

    learnTable(dlSrc, dlDstKey, pi.getInPort.getShortPortNumber)

    // if the destination is not multicast, look it up
    val outPort =
      if ((dlDst(0) & 0x1) == 0) table.get(dlDstKey).value
      else None

    // push a flow mod if we know where the packet should be going
    outPort.foreach(p => flowAdd(bufferId, inMatch, p, pi.getInPort.getShortPortNumber))

    // Send a packet out
    if (outPort.isEmpty || (pi.getBufferId.getInt == 0xffffffff)) {
      packetOut(bufferId, pi, outPort)
    }
  }

  def flowAdd(bufferId: OFBufferId, inMatch: OFMatch, outPort: Short, inPort: Short): Unit = {
    // todo
    val fm = new OFFlowModOld()
    fm.setBufferId(bufferId.getInt)
    fm.setCommand(0.toShort)
    fm.setCookie(0L)
    fm.setFlags(0.toShort)
    fm.setHardTimeout(0.toShort)
    fm.setIdleTimeout(5.toShort)

    inMatch.setInputPort(inPort)
    inMatch.setWildcards(0)
    fm.setMatch(inMatch)

    fm.setOutPort(OFPort.ANY)
    fm.setPriority(0.toShort)

    val output = new OFActionOutput()
    output.setMaxLength(0.toShort)
    output.setPort(outPort)
    val actions = new util.ArrayList[OFActionOld]()
    actions.add(output)
    fm.setActions(actions)
    fm.setLength(U16.t(OFFlowModOld.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH))

//    val fm = factory.buildFlowAdd()
//    fm.setBufferId(bufferId)
//    fm.setCookie(U64.ZERO)
//    fm.setHardTimeout(0.toShort)
//    fm.setIdleTimeout(5.toShort)
//
//    val newMatch = factory.buildMatch()
//      .setExact(MatchField.ETH_DST, MacAddress.of(inMatch.getDataLayerDestination))
//      .setExact(MatchField.ETH_SRC, MacAddress.of(inMatch.getDataLayerSource))
//      .setExact(MatchField.IN_PORT, OFPort.ofShort(inPort))
//      .build()
//
//    fm.setMatch(newMatch)
//    fm.setOutPort(OFPort.ANY)
//    fm.setPriority(0.toShort)
//    val action = factory.actions().buildOutput()
//      .setMaxLen(0)
//      .setPort(OFPort.ofShort(outPort))
//      .build()
//    val actions = new util.ArrayList[OFAction]
//    actions.add(action)
//    fm.setActions(actions)

//    val buffer = Unpooled.buffer(0, DefaultBufferSize)
    val buffer = ByteBuffer.allocate(DefaultBufferSize)
    fm.writeTo(buffer)
    val bytes = ByteString(buffer)
    sender() ! WriteRawMessage(bytes)
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