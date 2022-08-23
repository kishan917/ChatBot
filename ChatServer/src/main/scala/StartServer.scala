package org.kyadav.scala.chatbot.chatserver

import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object StartServer extends App {
  val logger = LoggerFactory.getLogger(this.getClass.getName)
  logger.debug("Starting application..")

  Try{
    val console = System.console()
    val port = console.readLine("\n==> Enter port on which server will be running: ").toInt
//    val port = 9090

    val chatServer = new ChatServer(port)
    ChatServerHelper.addShutdownHookToJvm(chatServer)
    chatServer.startChatServer()

  } match {
    case Success(value) =>
    case Failure(exception) =>
      exception.printStackTrace()
      logger.debug("Application failed to start. Cause: " + exception)
  }

}
