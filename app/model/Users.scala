package model

import scala.collection._

object Users {
  private val users = immutable.List(new User("Moya", "moya"), new User("Jonas", "jonas"), new User("Mary", "mary"), new User("Gosta", "gosta"), new User("Birgitta", "birgitta"))
  
  private val loggedInUsers: mutable.Set[User] = mutable.Set.empty
  
  private val userToUserLoggedOnOffmap: mutable.Map[String, (mutable.Set[String], mutable.Set[String])] = mutable.Map.empty
   
  private def userForName(uName: String): Option[User] = users.filter(u => u.name == uName).headOption
  
  def exists(user: User) : Boolean = users.contains(user)
  
  private def addLoggedOnUser(uName: String, loggedOnUser: String) = {
    println(s"Users -> addLoggedOnUser, user: $uName, map: $userToUserLoggedOnOffmap")
    val (loggedOn, loggedOff)  = userToUserLoggedOnOffmap.getOrElse(uName, (mutable.Set.empty[String], mutable.Set.empty[String]))
    loggedOn += loggedOnUser
    userToUserLoggedOnOffmap.put(uName, (loggedOn, loggedOff))
    println(s"Users <- addLoggedOnUser, user: $uName, map: $userToUserLoggedOnOffmap")
  }
  
  private def addLoggedOffUser(uName: String, loggedOffUser: String) = {
    println(s"Users -> addLoggedOffUser, user: $uName, map: $userToUserLoggedOnOffmap")
    val (loggedOn, loggedOff) = userToUserLoggedOnOffmap.getOrElse(uName, (mutable.Set.empty[String], mutable.Set.empty[String]))
    loggedOff += loggedOffUser
    userToUserLoggedOnOffmap.put(uName, (loggedOn, loggedOff))
    println(s"Users <- addLoggedOffUser, user: $uName, map: $userToUserLoggedOnOffmap")
  }
  
  def logon(user: User) = {
    println(s"Users -> logon, user: ${user.name}, loggedinUsers: $loggedInUsers")
    for (u <- loggedInUsers) {
      addLoggedOnUser(u.name, user.name)
    }
    loggedInUsers += user
    println(s"Users <- logon, user: ${user.name}, loggedinUsers: $loggedInUsers")
  } 

  def logout(uName: String) = {
    for (u <- loggedInUsers) {
      addLoggedOffUser(u.name, uName)
    }

    val user = userForName(uName)
    user match {
      case Some(u) => loggedInUsers -= u
      case _ =>
    }
  }
  
  def isLoggedIn(uName: String): Boolean = {
    loggedInUsers.map(u => u.name).exists(_ == uName)
  }
  
  def getLoggedOnUsers() = loggedInUsers.map(_.name).toList
  
  def getLoggedOnOffUsers(uName: String): Option[(List[String],List[String])] = {
    println(s"Users -> getLoggedOnOffUsers, user: $uName, map: ${userToUserLoggedOnOffmap.get(uName)}")
    userToUserLoggedOnOffmap.remove(uName).map(e => (e._1.toList, e._2.toList))
  }

}

case class User(name: String, password: String)