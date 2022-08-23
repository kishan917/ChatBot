package org.kyadav.scala.chatbot.chatserver

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

case class MessagePayloadWrapper(messagePayload: MessagePayload, chatServer: ChatServer)

object WriterActor {
  def apply(): Behavior[MessagePayloadWrapper] =
    Behaviors.setup(context => new WriterActor(context))
}

class WriterActor(context: ActorContext[MessagePayloadWrapper]) extends AbstractBehavior[MessagePayloadWrapper](context) {
  override def onMessage(messagePayload: MessagePayloadWrapper): Behavior[MessagePayloadWrapper] = {
    println("Inside onMessage method..")
    messagePayload match {
      case MessagePayloadWrapper(messagePayload, chatServer) => ChatServerHelper.sendMessageToUser(messagePayload.sender, messagePayload.receiver, messagePayload.message, chatServer)
        this
    }
  }
}
