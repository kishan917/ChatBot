package org.kyadav.scala.chatbot.chatserver

import akka.actor.typed.ActorSystem
import org.slf4j.{Logger, LoggerFactory}

import java.io.IOException
import java.net.{Socket, SocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, SocketChannel}
import java.util
import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}
import scala.util.control.Breaks.{break, breakable}

object ChatServerHelper {

  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)
  val readBuffer: ByteBuffer = ByteBuffer.allocate(1024)
  val chatBotWriterActorSystem: ActorSystem[MessagePayloadWrapper] = ActorSystem(WriterActor(), "ChatBotWriterActorSystem")

  @tailrec
  final def infiniteLoop(f: () => Unit): Unit = {
    f()
    infiniteLoop(f)
  }



  def notifyAllClient(chatServer: ChatServer): Unit = {
    println("Signaling clients for shutdown..")
    systemBroadcast("ChatServer is shutting down.", chatServer)
    println("Signaled clients for shutdown.")
  }

  def closeAllUserResources(chatServer: ChatServer): Unit = {
    chatServer.userSocketChannelMap.entrySet().stream().forEach( entry => {
      val username = entry.getKey
      val socketChannel = entry.getValue
      println("Closing resources for client with username: " + username)
      Try {
        println("Closing socket for client with username: " + username)
        socketChannel.close()
        println("Closed socket for client with username: " + username)
        println("Closed resources for client with username: " + username)
      }.getOrElse({
        logger.error("Error while closing socket for client with username: " + username)
      })
    })
  }

  def cleanup(chatServer: ChatServer): Unit = {
    println("Cleanup started..")
    notifyAllClient(chatServer)
    closeAllUserResources(chatServer)
    chatBotWriterActorSystem.terminate()
    println("Cleanup completed.")
  }



  def addShutdownHookToJvm(chatServer: ChatServer): Unit = {
    logger.debug("Adding shutdown hook ")
    Runtime.getRuntime.addShutdownHook(new Thread(() => chatServer.stopChatServer()))
    logger.debug("Added shutdown hook.")
  }



  def acceptConnection(chatServer: ChatServer): SelectionKey = {
    val channel : SocketChannel = chatServer.serverSocketChannel.accept()
    channel.configureBlocking(false)
    val socket : Socket = channel.socket()
    val remoteAddress : SocketAddress = socket.getRemoteSocketAddress
    System.out.println("Connected to: " + remoteAddress)
    channel.register(chatServer.selector, SelectionKey.OP_READ)
  }

  def processMessage(key: SelectionKey, chatServer: ChatServer): Unit = {
    Try {
      val userSocketChannel: SocketChannel = key.channel().asInstanceOf[SocketChannel]
      Try {
        var input: String = ""
        try {
          input = readMessageFromBuffer(userSocketChannel, chatServer)
        } catch {
          case e: IOException => logger.error("Error while reading message from client buffer. Error: " + e.getMessage)
          case _: Throwable =>
        }
        logger.debug("input: " + input)
        val inputPayload = PayloadHelper.deserialize(input)
        if (inputPayload.payloadType.equals(PayloadType.USER_LOGIN)) {
          val userLoginPayload = PayloadHelper.toUserLoginPayload(inputPayload)
          addUser(userLoginPayload.username, userSocketChannel, chatServer)
          printUserListAtClientSide(userLoginPayload.username, chatServer)
        } else if (inputPayload.payloadType.equals(PayloadType.USER_MESSAGE)) {
          val userMessagePayload = PayloadHelper.toUserMessagePayload(inputPayload)
          if ("exit".equals(userMessagePayload.message)) {
            closeClientConnection(userMessagePayload.sender, chatServer)
            return
          }
//          sendMessageToUser(userMessagePayload.sender, userMessagePayload.receiver, userMessagePayload.message, chatServer)
          chatBotWriterActorSystem ! MessagePayloadWrapper(userMessagePayload, chatServer)
        } else if (inputPayload.payloadType.equals(PayloadType.USER_REGISTRATION)) {}
        else logger.debug("No Payload found with type : " + inputPayload.payloadType)
      } match {
        case Success(_) =>
        case Failure(_) =>
          logger.info("Error while processing message for client. Closing its connection.")
          closeClientConnection(userSocketChannel, chatServer)
      }
    } match {
      case Success(_) =>
      case Failure(exception) =>
        exception.printStackTrace()
        logger.debug("Failed to process message from client. Cause: " + exception)
    }
  }


  def readMessageFromBuffer(userSocketChannel: SocketChannel, chatServer: ChatServer): String =  {
    readBuffer.clear()
    var bufferReadCount = 0
    var messageBytes: scala.Array[Byte] = new Array[Byte](0)
    breakable {
      while (true) {
        try {
          val tempCount = userSocketChannel.read(readBuffer)
          System.out.println("bufferReadCount: " + bufferReadCount)
          if (tempCount == 0) {
            break
          } else if (tempCount == -1) {
            closeClientConnection(userSocketChannel, chatServer)
            return ""
          }
          bufferReadCount = tempCount
        } catch {
          case e: IOException =>
            logger.error("Error while reading message.", e)
            closeClientConnection(userSocketChannel, chatServer)
            break
        }
        messageBytes = mergeByteArrays(messageBytes, util.Arrays.copyOfRange(readBuffer.array(), 0, bufferReadCount))
        System.out.println("Server Got: " + new String(messageBytes))
      }
    }
    val message = new String(messageBytes).trim()
    message
  }


  def sendMessageToUser(sender: String, receiver: String, message: String, chatServer: ChatServer): Unit = {
    if("".equals(receiver) || "@ALL".equals(receiver)) { //Send message to all connected users
      sendMessageToAllExcept(sender, sender, message, chatServer)
    }else{
      logger.debug("Sending message from " + sender + " to " + receiver + ".")
      if (validateUser(receiver, chatServer)) {
        writeMessageToClient(Option(chatServer.userSocketChannelMap.get(receiver)), "[" + sender + "]: " + message)
      }
    }
  }

  def sendMessageToAllExcept(sender: String, doNotSendToThisUser: String, message: String, chatServer: ChatServer): Unit = {
    logger.debug("Sending message from " + sender + " to All connected users except: " + doNotSendToThisUser)
    chatServer.userSocketChannelMap.keySet().stream()
      .filter(key => !key.equals(doNotSendToThisUser))
      .forEach(key => writeMessageToClient(Option(chatServer.userSocketChannelMap.get(key)), "[" + sender + "] -> @ALL: " + message))
  }

  def systemBroadcast(message: String, chatServer: ChatServer): Unit = {
    logger.debug("Sending message from SYSTEM to All connected users.")
    chatServer.userSocketChannelMap.keySet()
      .forEach(key => writeMessageToClient(Option(chatServer.userSocketChannelMap.get(key)), "[SYSTEM] -> @ALL: " + message))
  }

  def writeMessageToClient( userSocketChannel: Option[SocketChannel],  message: String): Unit = {
    userSocketChannel match {
      case None => logger.debug("Client socket channel: null")
      case Some(socketChannel) =>
        Try{
          println("Message to client: " + message + "\n Size: " + message.length())
          val byteArray = message.getBytes()
          val buffer = ByteBuffer.wrap(byteArray)
          buffer.clear()
          socketChannel.write(buffer)
          buffer.flip()
        }  match {
          case Success(_) =>
          case Failure(ex) => logger.error("Exception while sending message.", ex)
        }
    }
  }

  def closeClientConnection(userSocketChannel : SocketChannel, chatServer: ChatServer): Unit = {
    userSocketChannel.close()
    val username = getUsernameBySocketChannel(userSocketChannel, chatServer)
    if(username.nonEmpty){
      removeUser(username.get, chatServer)
      systemBroadcast(username.get + " left the chatroom.", chatServer)

    }
  }

  def closeClientConnection(username: String, chatServer: ChatServer): Unit = {
    if (chatServer.userSocketChannelMap.contains(username)) {
      chatServer.userSocketChannelMap.get(username).close()
      removeUser(username, chatServer)
      systemBroadcast(username + " left the chatroom.", chatServer)
    }
  }

  def hasUsers(chatServer: ChatServer): Boolean = !chatServer.userSocketChannelMap.isEmpty

  def addUser(username: String, userSocketChannel: SocketChannel, chatServer: ChatServer): Unit = {
    logger.debug("Adding new user with username: " + username + " into system.")
    if(chatServer.userSocketChannelMap.containsKey(username)){
      sendMessageToUser("SYSTEM", username, "User with " + username + " already exist in system.", chatServer)
      logger.info("User with username " + username + " already present.")
      Try{
        println("############################ closing")
        userSocketChannel.close()
      }
    }else {
      chatServer.userSocketChannelMap.put(username, userSocketChannel)
      sendMessageToAllExcept("SYSTEM", username, "New user joined: " + username, chatServer)
      logger.info("User with username " + username + " added to system.")
    }
  }

  def validateUser(username: String, chatServer: ChatServer): Boolean = {
    logger.debug("Validating receiver: " + username)
    chatServer.userSocketChannelMap.containsKey(username)
  }

  def removeUser(username: String, chatServer: ChatServer): Unit = {
    logger.debug("Removing user with username: " + username + " from system.")
    chatServer.userSocketChannelMap.remove(username)
    logger.info("User with username: " + username + " removed from system.")
  }

  def getUsernames(chatServer: ChatServer): String = chatServer.userSocketChannelMap.keySet.toString

  def printUserListAtClientSide(username : String, chatServer: ChatServer): Unit = {
    if (hasUsers(chatServer)) {
      writeMessageToClient(Option(chatServer.userSocketChannelMap.get(username)), s"*** Connected users: ${getUsernames(chatServer)}")
    } else {
      writeMessageToClient(Option(chatServer.userSocketChannelMap.get(username)), "*** No other users connected.")
    }
  }

  def mergeByteArrays( arr1 : Array[Byte], arr2: Array[Byte]): Array[Byte] = {
    val out = new Array[Byte](arr1.length + arr2.length)
    System.arraycopy(arr1, 0, out, 0, arr1.length)
    System.arraycopy(arr2, 0, out, arr1.length, arr2.length)
    out
  }

  def getUsernameBySocketChannel( userSocketChannel : SocketChannel, chatServer: ChatServer): Option[String] = {
    val entry = chatServer.userSocketChannelMap.entrySet().stream()
      .filter(entry => entry.getValue==userSocketChannel)
      .findFirst()
    if(entry.isPresent) Option(entry.get().getKey)
    else None
  }

}

