package com.yevhenii.solace.l2

import java.util.concurrent.ConcurrentHashMap

import com.yevhenii.solace.messages.Messages.{EthernetMessage, Message}

import scala.concurrent.Future

trait L2Table {
  def learn(msg: Message, ethernetMessage: EthernetMessage, dpid: String): Unit

  def get(dpid: String): ConcurrentHashMap[String, String]
}

object L2Table {
  def isBroadcast(dlSrc: String) = dlSrc == "ff:ff:ff:ff:ff:ff"
}
