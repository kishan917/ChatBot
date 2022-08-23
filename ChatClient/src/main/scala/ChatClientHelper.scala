package org.kyadav.scala.chatbot.chatclient

import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec

object ChatClientHelper {
  val logger: Logger = LoggerFactory.getLogger(this.getClass.getName)

  def addShutdownHookToJvm(): Unit = {
    logger.debug("Adding shutdown hook ")
    Runtime.getRuntime.addShutdownHook(new Thread(() => ChatClient.stopChatClient()))
    logger.debug("Added shutdown hook.")
  }

  def mergeByteArrays( arr1 : Array[Byte], arr2: Array[Byte]): Array[Byte] = {
    val out = new Array[Byte](arr1.length + arr2.length)
    System.arraycopy(arr1, 0, out, 0, arr1.length)
    System.arraycopy(arr2, 0, out, arr1.length, arr2.length)
    out
  }


}
