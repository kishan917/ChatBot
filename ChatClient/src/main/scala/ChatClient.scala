package org.kyadav.scala.chatbot.chatclient

import org.slf4j.{Logger, LoggerFactory}

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import scala.util.{Failure, Try}

object ChatClient extends App {
  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
    val serverIP = System.console.readLine("\n==> Enter IP address for ChatServer: ")
    val serverPort = System.console.readLine("\n==> Enter Port number for ChatServer: ").toInt
//  val serverIP = "localhost"
//  val serverPort = "9090".toInt
    val username = System.console.readLine("\n==> Enter your username: ")
//  val username = "Test_User"

  logger.debug(s"Connecting to server {$serverIP}:$serverPort...")
  val clientSocketChannel = SocketChannel.open(new InetSocketAddress(serverIP, serverPort))
  clientSocketChannel.configureBlocking(false)
  logger.debug(s"Connected to server {$serverIP}:$serverPort")
  System.out.println("\n*** Usage: @ReceiverUser your message here " +
    "\n*** \tIf @ReceiverUser is not specified then message goes to all connected users." +
    "\n*** \tType 'exit' to leave the chat room." +
    " \n")

  val readerThread = new Thread(() => MessageReader.init())
  readerThread.start()
  val writerThread = new Thread(() => MessageWriter.init())
  writerThread.start()


  def getUsername: String = username

  def stopChatClient(): Unit = {
    println("Stopping chat client..")
    stopClientSocket()
    MessageReader.closeReaderThreadResources()
    MessageWriter.closeWriterThreadResources()
    println("Stopped chat client.")
  }

  def stopClientSocket(): Unit = {
    println("Closing client socket channel.")
    Try{
      clientSocketChannel.close()
    } match {
      case Failure(exception) => println("Error while closing client socket channel" + ": " + exception.getMessage)
      case _ =>
    }
    println("Closed client socket channel.")
  }






}
