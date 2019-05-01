package com.yevhenii.solace.l2

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.messages.Messages.{EthernetMessage, Message}

class L2Table {
  import L2Table._

  val logger = Logger("L2Table")

  val table: ConcurrentHashMap[String, ConcurrentHashMap[String, String]] = new ConcurrentHashMap[String, ConcurrentHashMap[String, String]]()

  def learn(msg: Message, ethernetMessage: EthernetMessage, dpid: String): Unit = {
    val dlSrc = ethernetMessage.shost
    val dlDst = ethernetMessage.dhost
    val inPort = msg.body.get.in_port.get

    if (isBroadcast(dlSrc)) {
      logger.warn("Source set to broadcast!")
    } else {
      table.computeIfAbsent(dpid, _ => new ConcurrentHashMap[String, String]())
      val map = table.get(dpid)
      if (map.containsKey(dlSrc)) {
        val dst = map.get(dlSrc)
        if (dst != inPort) {
          logger.info(s"MAC has moved from $dst to $inPort")
//          map.put(dlSrc, inPort) // TODO investigate
        } else {
          logger.info("This entry was already learnt")
        }
      } else {
        logger.info(s"Learned mac $dlSrc port: $inPort")
        map.put(dlSrc, inPort)
      }
    }
  }

  def get(dpid: String): ConcurrentHashMap[String, String] = {
    table.get(dpid)
  }
}

object L2Table {
  def isBroadcast(dlSrc: String) = dlSrc == "ff:ff:ff:ff:ff:ff"
}
