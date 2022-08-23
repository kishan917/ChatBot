package org.kyadav.scala.chatbot.chatserver

import com.google.gson.Gson

case class PayloadHelper(payloadType: String)
object PayloadHelper{
  def serialize(payload: Payload): String = payload.serialize
  def deserialize(serializeString: String): Payload = {
    val payloadHelper = new Gson().fromJson(serializeString, classOf[PayloadHelper])
    if(PayloadType.USER_MESSAGE == payloadHelper.payloadType) new Gson().fromJson(serializeString, classOf[MessagePayload])
    else if(PayloadType.USER_LOGIN == payloadHelper.payloadType) new Gson().fromJson(serializeString, classOf[UserLoginPayload])
    else if(PayloadType.USER_REGISTRATION == payloadHelper.payloadType) new Gson().fromJson(serializeString, classOf[UserRegistrationPayload])
    else throw new RuntimeException("No Payload found with type : " + payloadHelper.payloadType)
  }

  def toUserMessagePayload(payload: Payload): MessagePayload = payload.asInstanceOf[MessagePayload]
  def toUserLoginPayload(payload: Payload): UserLoginPayload = payload.asInstanceOf[UserLoginPayload]
  def toUserRegistrationPayload(payload: Payload): UserRegistrationPayload = payload.asInstanceOf[UserRegistrationPayload]

}
