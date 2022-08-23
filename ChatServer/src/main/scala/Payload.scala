package org.kyadav.scala.chatbot.chatserver

import com.google.gson.Gson

trait Payload{
  val payloadType: String
  def serialize: String
  def deserialize(serializeString: String): Payload
  def test: String
}

case class MessagePayload(sender: String, receiver: String, message: String) extends Payload {
  override val payloadType: String = PayloadType.USER_MESSAGE
  override def serialize: String = new Gson().toJson(this)
  override def deserialize(serializeString: String): Payload = new Gson().fromJson(serializeString, classOf[MessagePayload])

  override def test: String = "this is MessagePayload class"
}

case class UserLoginPayload(username: String, password: String) extends Payload {
  override val payloadType: String = PayloadType.USER_LOGIN
  override def serialize: String = new Gson().toJson(this)
  override def deserialize(serializeString: String): Payload = new Gson().fromJson(serializeString, classOf[UserLoginPayload])

  override def test: String = "this is UserLoginPayload class"
}

case class UserRegistrationPayload(username: String, newPassword: String) extends Payload {
  override val payloadType: String = PayloadType.USER_REGISTRATION
  override def serialize: String = new Gson().toJson(this)
  override def deserialize(serializeString: String): Payload = new Gson().fromJson(serializeString, classOf[UserRegistrationPayload])

  override def test: String = "this is UserRegistrationPayload class"
}

