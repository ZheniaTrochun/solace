package com.yevhenii.solace.processing

import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.formatting.{Formatter, MessageFormatter}
import com.yevhenii.solace.l2.L2Table
import com.yevhenii.solace.messages.Messages.{EthernetMessage, Message, MessageHolder}
import com.yevhenii.solace.processing.MessageProcessor.ProcessingResult

class MessageProcessor(
                        l2Table: L2Table,
                        formatter: Formatter
                      ) extends MessageFormatter {
  val logger = Logger("MessageProcessor")

  type Type = String

  def processMessage(messageHolder: MessageHolder, sessionId: String): Either[String, ProcessingResult] = {
    logger.info(s"processing message ${messageHolder.message.header.`type`}")

    messageHolder.message.header.`type` match {
      case msgType @ "OFPT_HELLO" =>
        Right(ProcessingResult(
          setSyncMessage("OFPT_HELLO", messageHolder, messageHolder.dpid),
          msgType
        ))
//        sender.sendPacket(setSyncMessage("OFPT_FEATURES_REQUEST", messageHolder, messageHolder.dpid), sessionId, t) // TODO
      case "OFPT_ERROR" =>
      //        TODO log error
        Left("Not inmplemented yet")
      case msgType @ "OFPT_ECHO_REQUEST" =>
        Right(ProcessingResult(
          setSyncMessage("OFPT_ECHO_REPLY", messageHolder, messageHolder.dpid),
          msgType
        ))
      case "OFPT_PACKET_IN" =>
        // TODO check this stuff IMMEDIATELY !!!
        logger.info("MESSAGE_IN here")
        messageIn(messageHolder, sessionId)
      case "OFPT_FEATURES_REPLY" =>
//        TODO set dpid
        Left("Not inmplemented yet")
      case "OFPT_PORT_STATUS" =>
        Left("Not inmplemented yet")
      case "OFPT_FLOW_REMOVED" =>
        Left("Not inmplemented yet")
      case t =>
        Left("Not inmplemented yet")
    }
  }

  def messageIn(messageHolder: MessageHolder, sessionId: String): Either[String, ProcessingResult] = {
    logger.info(s"started MESSAGE_IN (${messageHolder.message.decodedEthernet.get})")
    messageHolder.message.decodedEthernet
      .fold[Either[String, ProcessingResult]] {
        logger.error("Ethernet message not present!")
        Left("Ethernet message not present!")
      } { decoded =>
        logger.info("starting learning...")
        l2Table.learn(messageHolder.message, decoded, messageHolder.dpid.get)
        Right(
          createForwardPacket(messageHolder.message, decoded, messageHolder.dpid.get, sessionId)
        )
      }
  }

  def createForwardPacket(message: Message, packet: EthernetMessage, dpid: String, sessionId: String): ProcessingResult = {
    val dlDst = packet.dhost
    val dlSrc = packet.shost
    val table = l2Table.get(dpid)
    val inPort = table.get(dlSrc)

    if (!L2Table.isBroadcast(dlDst) && table.contains(dlDst)) {
      val port = table.get(dlDst)
       if (port == inPort) {
         logger.warn(s"Learned port $inPort, system = solace")
         ProcessingResult(
           setOutFloodPacket(message, inPort, dpid),
           "OFTP_PACKET_OUT"
         )
       } else {
         val outPort = port.toString // TODO
         logger.info(s"Installing flow for destination: $dlDst, source: $dlSrc, in_port: $inPort, out_port: $outPort, system = Solace")
         ProcessingResult(
           setFlowModPacket(message, packet, inPort, outPort, dpid),
           "OFPT_FLOW_MOD"
         )
       }
    } else {
      logger.info(s"Flooding unknown Buffer id: ${message.body.get.buffer_id}")
      ProcessingResult(
        setOutFloodPacket(message, inPort, dpid),
        "OFPT_PACKET_OUT"
      )
    }
  }
}

object MessageProcessor {
  case class ProcessingResult(outMessage: MessageHolder, msgType: String)
}