package model
import scala.collection.mutable.{Map => MutableMap, Queue}
import play.api.libs.json.JsValue


class MessageQueue {
    val messageQueue = MutableMap.empty[String, Queue[JsValue]]

    def addToQueue(user: String, jsValue: JsValue) = synchronized {
    if  (messageQueue.contains(user)) {
      messageQueue(user).enqueue(jsValue)
    }
    else {
      messageQueue.put(user, Queue(jsValue))
    }
  }
  
  def removeFromQueue(user: String): Option[JsValue] = synchronized {
    if  (messageQueue.contains(user)) {
      val jsValue = messageQueue(user).dequeue()
      if (messageQueue(user).isEmpty) messageQueue.remove(user)
      Some(jsValue)
    }
    else {
      None
    }
  }
  
  def contains(user: String): Boolean = synchronized {
    messageQueue.contains(user) 
  }
  
  override def toString(): String = messageQueue.toString()

}