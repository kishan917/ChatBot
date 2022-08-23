package org.kyadav.scala.chatbot.chatserver

import org.slf4j.{Logger, LoggerFactory}

import java.net.InetSocketAddress
import java.nio.channels.{SelectionKey, Selector, ServerSocketChannel, SocketChannel}
import java.util.concurrent.ConcurrentHashMap

class ChatServer(val port: Int) {

  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
  val userSocketChannelMap: ConcurrentHashMap[String, SocketChannel] = new ConcurrentHashMap
  val selector: Selector = Selector.open()
  val serverSocketChannel: ServerSocketChannel = ServerSocketChannel.open()

  def init(): Unit = {}

  def startChatServer(): Unit = {
    logger.debug("Starting server...")
    serverSocketChannel.configureBlocking(false)
    val listenAddress = new InetSocketAddress("localhost", port)
    serverSocketChannel.socket().bind(listenAddress)
    serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT)
    logger.info("Started server. Listening on port: " + this.port)

    while(this.selector.isOpen){
      logger.debug("Waiting for new event..")
      this.selector.select
      this.selector.selectedKeys.stream
        .filter(key => key.isValid)
        .forEach(key => {
          if (key.isAcceptable) {
            ChatServerHelper.acceptConnection(this)
          } else if (key.isReadable) {
            ChatServerHelper.processMessage(key, this)
          }
          this.selector.selectedKeys().remove(key)
        })
    }
  }

  def stopChatServer(): Unit = {
    println("Stopping server...")
    ChatServerHelper.cleanup(this)
    serverSocketChannel.close()
    selector.close()
  }

}