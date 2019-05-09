//package com.yevhenii.solace.sockets
//
//import java.net.ServerSocket
//
//import com.yevhenii.solace.formatting.Formatter
//import com.yevhenii.solace.processing.{MessageProcessor, Sender}
//
//case class SocketListener(
//                           serverSocket: ServerSocket,
//                           messageProcessor: MessageProcessor,
//                           sender: Sender,
//                           formatter: Formatter
//                         ) {
//
//  def acceptConnection(): ClientSocket = {
//    new ClientSocket(serverSocket.accept(), messageProcessor, sender, formatter)
//  }
//}
