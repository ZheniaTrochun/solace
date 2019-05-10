package com.yevhenii.solace.l2

import java.util.concurrent.ConcurrentHashMap

import com.typesafe.scalalogging.Logger
import com.yevhenii.solace.messages.Messages.{EthernetMessage, Message}

class InMemoryL2 extends L2Table {

  import L2Table._

  val logger = Logger("InMemoryL2")

  val table: ConcurrentHashMap[String, ConcurrentHashMap[String, Int]] = new ConcurrentHashMap[String, ConcurrentHashMap[String, Int]]()

  def learn(msg: Message, ethernetMessage: EthernetMessage, dpid: String): Unit = {
    val dlSrc = ethernetMessage.shost
    val dlDst = ethernetMessage.dhost
    val inPort = msg.body.get.in_port.get

    if (isBroadcast(dlSrc)) {
      logger.warn("Source set to broadcast!")
    } else {
      table.computeIfAbsent(dpid, _ => new ConcurrentHashMap[String, Int]())
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

  def get(dpid: String): ConcurrentHashMap[String, Int] = {
    table.get(dpid)
  }
}
