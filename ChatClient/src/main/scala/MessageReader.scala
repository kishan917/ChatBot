package org.kyadav.scala.chatbot.chatclient

import org.slf4j.{Logger, LoggerFactory}

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.util
import scala.util.control.Breaks.{break, breakable}
import scala.util.{Failure, Success, Try}

object MessageReader {

  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
  val buffer: ByteBuffer = ByteBuffer.allocate(1024)

  def init(): Unit = {
    logger.debug("MessageReader started.")
    messageReaderLoop()
  }

  def messageReaderLoop(): Unit = {
    breakable {
      while(true){
        if(Thread.currentThread().isInterrupted){
          logger.info("Client reader thread interrupted.")
          break
        }
        Try {
          val message = readMessageFromBuffer(ChatClient.clientSocketChannel)
          //      val message = ""; Thread.sleep(2000)
          if(message!=null && message.trim.nonEmpty){
            println("\n" + message)
            if (ChatClient.getUsername != null) {
              System.out.print("You[" + ChatClient.getUsername + "]: ")
            }
          }
          if("[SYSTEM] -> @ALL: ChatServer is shutting down.".equals(message)){
            ChatClient.stopChatClient()
            break
          }
        }match {
          case Failure(exception) =>
            logger.error("Error in reader thread. " + exception.getMessage)
            ChatClient.stopChatClient()
            break
          case Success(_) =>
        }
      }
    }
  }


  def readMessageFromBuffer(clientSocketChannel: SocketChannel): String = {
    var messageBytes = new Array[Byte](0)
    val buffer: ByteBuffer = ByteBuffer.allocate(1024)
    buffer.clear()
    breakable {
      while (true) {
        try {
          buffer.clear()
          val bufferReadCount: Int = clientSocketChannel.read(buffer)
          buffer.flip()
          if (bufferReadCount == 0) {
            break
          } else if (bufferReadCount == -1) {
            clientSocketChannel.close()
          }else messageBytes = ChatClientHelper.mergeByteArrays(messageBytes, util.Arrays.copyOfRange(buffer.array(), 0, bufferReadCount))
        } catch {
          case e: IOException =>
            logger.error("Error while reading message.", e)
            clientSocketChannel.close()
            throw e
        }
      }
    }
    val message = new String(messageBytes).trim()
    if(message!=null) message else ""
  }

  def closeReaderThreadResources(): Unit = {
    System.out.println("Closing reader thread resources")
    ChatClient.readerThread.interrupt()
    System.out.println("Closed reader thread resources.")
  }

}
