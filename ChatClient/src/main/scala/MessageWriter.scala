package org.kyadav.scala.chatbot.chatclient

import org.slf4j.{Logger, LoggerFactory}

import java.io.Console
import java.nio.ByteBuffer
import scala.util.control.Breaks.{break, breakable}
import scala.util.{Failure, Success, Try}

object MessageWriter {

  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
  val consoleReader: Console = System.console()

  def init(): Unit = {
    logger.debug("MessageWriter started.")
    val userLoginPayload = UserLoginPayload(ChatClient.getUsername, "")
    ChatClient.clientSocketChannel.write(ByteBuffer.wrap(userLoginPayload.serialize.getBytes()))
    messageWriterLoop()
  }

  def messageWriterLoop():Unit = {
    breakable {
      while(true){
        print("You[" + ChatClient.getUsername + "]: ")
        val input = consoleReader.readLine()

        if(Thread.currentThread().isInterrupted){
          logger.info("Client writer thread interrupted.")
          break
        }

        Try {
          var receiver = ""
          var message = ""
          if (input != null) {
            message = input
            if (input.contains("@")) {
              receiver = input.split("@", 2)(1).split(" ", 2)(0)
              message = input.split("@", 2)(1).split(" ", 2)(1)
            }
          }
          if ("exit".equals(message)) {
            ChatClient.stopChatClient()
            break
          }
          val messagePayload = MessagePayload(ChatClient.getUsername, receiver, message)
          logger.debug("messagePayload.toString(): " + messagePayload.serialize)
          ChatClient.clientSocketChannel.write(ByteBuffer.wrap(messagePayload.serialize.getBytes()))
        }match {
          case Failure(exception) =>
            logger.error("Error in writer thread. " + exception.getMessage)
            ChatClient.stopChatClient()
            break
          case Success(_) =>
        }
      }
    }
  }

  def closeWriterThreadResources(): Unit = {
    System.out.println("Closing writer thread resources")
    ChatClient.writerThread.interrupt()
    System.out.println("Closed writer thread resources")
  }
}
