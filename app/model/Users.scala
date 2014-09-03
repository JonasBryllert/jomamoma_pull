package model

import scala.collection._

object Users {
  //  private val users = immutable.List(new User("Moya", "moya"), new User("Jonas", "jonas"), new User("Mary", "mary"), new User("Gosta", "gosta"), new User("Birgitta", "birgitta"))
  
//  private val loggedInUsers: mutable.Set[User] = mutable.Set.empty
  private val loggedInUsers: mutable.Map[User, Long] = mutable.Map.empty
  
  private val userToUserLoggedOnOffmap: mutable.Map[String, (mutable.Set[String], mutable.Set[String])] = mutable.Map.empty
   
  private def userForName(uName: String): Option[User] = loggedInUsers.keys.filter(u => u.name == uName).headOption
  
  def exists(user: User) : Boolean = loggedInUsers.keys.map(u => u.name).exists(_ == user.name)
  
  private def addLoggedOnUser(uName: String, loggedOnUser: String) = {
    println(s"Users -> addLoggedOnUser, user: $uName, map: $userToUserLoggedOnOffmap")
    val (loggedOn, loggedOff)  = userToUserLoggedOnOffmap.getOrElse(uName, (mutable.Set.empty[String], mutable.Set.empty[String]))
    loggedOn += loggedOnUser
    if (loggedOff.contains(loggedOnUser)) loggedOff -= loggedOnUser
    userToUserLoggedOnOffmap.put(uName, (loggedOn, loggedOff))
    println(s"Users <- addLoggedOnUser, user: $uName, map: $userToUserLoggedOnOffmap")
  }
  
  private def addLoggedOffUser(uName: String, loggedOffUser: String) = {
    println(s"Users -> addLoggedOffUser, user: $uName, map: $userToUserLoggedOnOffmap")
    val (loggedOn, loggedOff) = userToUserLoggedOnOffmap.getOrElse(uName, (mutable.Set.empty[String], mutable.Set.empty[String]))
    loggedOff += loggedOffUser
    if (loggedOn.contains(loggedOffUser)) loggedOn -= loggedOffUser
    userToUserLoggedOnOffmap.put(uName, (loggedOn, loggedOff))
    println(s"Users <- addLoggedOffUser, user: $uName, map: $userToUserLoggedOnOffmap")
  }
  
  def logon(user: User) = {
    //Message to all other users that user has logged in
    println(s"Users -> logon, user: ${user.name}, loggedinUsers: $loggedInUsers")
    for (u <- loggedInUsers.keys) {
      addLoggedOnUser(u.name, user.name)
    }
    
    //login the user
    loggedInUsers += (user -> compat.Platform.currentTime)
    println(s"Users <- logon, user: ${user.name}, loggedinUsers: $loggedInUsers")
  } 

  def logout(uName: String) = {
    //logout the user
    val user = userForName(uName)
    user match {
      case Some(u) => loggedInUsers -= u
      case _ =>
    }
    
    //Message to all other users that user has logged out
    for (u <- loggedInUsers.keys) {
      addLoggedOffUser(u.name, uName)
    }

  }
  
  def isLoggedIn(uName: String): Boolean = {
    loggedInUsers.keys.map(u => u.name).exists(_ == uName)
  }
  
  def getLoggedOnUsers() = loggedInUsers.keys.map(_.name).toList
  
  def getLoggedOnOffUsers(uName: String): Option[(List[String],List[String])] = {
    println(s"Users -> getLoggedOnOffUsers, user: $uName, map: ${userToUserLoggedOnOffmap.get(uName)}")
    userToUserLoggedOnOffmap.remove(uName).map(e => (e._1.toList, e._2.toList))
  }
  
  def logoutOldUsers(currentTime: Long): Unit = {
    val expiredUser:  mutable.ListBuffer[User] = mutable.ListBuffer.empty
    loggedInUsers.foreach { case(u, t) => {
      if (currentTime > t + 60 * 60 * 1000) { //More than one hour gone
        expiredUser += u
      }
    }}
    expiredUser.foreach(u => logout(u.name))
  }
  
  def updateTimeStamp(uName: String): Unit = {
    userForName(uName).foreach(user => {
      if (loggedInUsers.contains(user))loggedInUsers(user) = compat.Platform.currentTime
      else println("Warning: UpdateTime stamp for non existing user: " + user.name)
    })
  }

}

case class User(name: String, group: Option[String])