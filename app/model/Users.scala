package model

import scala.collection.mutable.ListBuffer

object Users {
  val users = List(new User("Moya", "moya"), new User("Jonas", "jonas"))
  
  val loggedInUsers: ListBuffer[User] = new ListBuffer
  
  def exists(user: User) : Boolean = users.contains(user)
  
  def logon(user: User) = loggedInUsers += user
  def logout(user: User) = loggedInUsers -= user

}

case class User(name: String, password: String)