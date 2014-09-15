package model

import scala.collection._

object Users {
  //  private val users = immutable.List(new User("Moya", "moya"), new User("Jonas", "jonas"), new User("Mary", "mary"), new User("Gosta", "gosta"), new User("Birgitta", "birgitta"))
  
//  private val loggedInUsers: mutable.Set[User] = mutable.Set.empty
  private val loggedInUsers: mutable.Map[User, Long] = mutable.Map.empty
  
  private val userToUserLoggedOnOffmap: mutable.Map[User, (mutable.Set[User], mutable.Set[User])] = mutable.Map.empty
   
  def userForName(uName: String): Option[User] = loggedInUsers.keys.filter(u => u.name == uName).headOption
  
  def exists(user: User) : Boolean = loggedInUsers.keys.map(u => u.name).exists(_ == user.name)
  
  private def addLoggedOnUser(alreadyLoggedinUser: User, currentlyLogginInUser: User) = {
    println(s"Users -> addLoggedOnUser, alreadyLoggedinUser: ${alreadyLoggedinUser.name}, currentlyLogginInUser: ${currentlyLogginInUser.name}, map: $userToUserLoggedOnOffmap")
    val (loggedOn, loggedOff)  = userToUserLoggedOnOffmap.getOrElse(alreadyLoggedinUser, (mutable.Set.empty[User], mutable.Set.empty[User]))
    loggedOn += currentlyLogginInUser
    if (loggedOff.contains(currentlyLogginInUser)) loggedOff -= currentlyLogginInUser
    userToUserLoggedOnOffmap.put(alreadyLoggedinUser, (loggedOn, loggedOff))
    println(s"Users <- addLoggedOnUser, alreadyLoggedinUser: ${alreadyLoggedinUser.name}, currentlyLogginInUser: ${currentlyLogginInUser.name}, map: $userToUserLoggedOnOffmap")
  }
  
  private def addLoggedOffUser(alreadyLoggedinUser: User, currentlyLogginOffUser: User) = {
    println(s"Users -> addLoggedOffUser, alreadyLoggedinUser: ${alreadyLoggedinUser.name}, currentlyLogginInUser: ${currentlyLogginOffUser.name}, map: $userToUserLoggedOnOffmap")
    val (loggedOn, loggedOff) = userToUserLoggedOnOffmap.getOrElse(alreadyLoggedinUser, (mutable.Set.empty[User], mutable.Set.empty[User]))
    loggedOff += currentlyLogginOffUser
    if (loggedOn.contains(currentlyLogginOffUser)) loggedOn -= currentlyLogginOffUser
    userToUserLoggedOnOffmap.put(alreadyLoggedinUser, (loggedOn, loggedOff))
    println(s"Users -> addLoggedOffUser, alreadyLoggedinUser: ${alreadyLoggedinUser.name}, currentlyLogginInUser: ${currentlyLogginOffUser.name}, map: $userToUserLoggedOnOffmap")
  }
  
  def logon(user: User) = {
    //Message to all other users that user has logged in
    println(s"Users -> logon, user: ${user.name}, loggedinUsers: $loggedInUsers")
    for (u <- loggedInUsers.keys.filter(_.group == user.group)) {
      addLoggedOnUser(u, user)
    }
    
    //login the user
    loggedInUsers += (user -> compat.Platform.currentTime)
    println(s"Users <- logon, user: ${user.name}, loggedinUsers: $loggedInUsers")
  } 

  def logout(uName: String) = {
    //logout the user
    val userOption = userForName(uName)
    userOption match {
      case Some(user) => {
        loggedInUsers -= user
        //Message to all other users that user has logged out
        for (u <- loggedInUsers.keys.filter(_.group == user.group)) {
          addLoggedOffUser(u, user)
        }
        //Also remove the current user in case in map
        userToUserLoggedOnOffmap.remove(user)
      }
      case _ =>
    }
    

  }
  
  def isLoggedIn(uName: String): Boolean = {
    loggedInUsers.keys.map(u => u.name).exists(_ == uName)
  }
  
  /**
   * Return a list of the users logged on in same group
   */
  def getLoggedOnUsers(uName: String) = {
    val user: User = userForName(uName).get
    loggedInUsers.keys.filter(u => (u.group == user.group) && (u.name != user.name)).map(_.name).toList
  }
  
  def getLoggedOnOffUsers(uName: String): Option[(List[String],List[String])] = {
    val user = userForName(uName).get
    println(s"Users -> getLoggedOnOffUsers, user: $uName, map: ${userToUserLoggedOnOffmap.get(user)}")
    userToUserLoggedOnOffmap.remove(user).map(e => (e._1.map(_.name).toList, e._2.map(_.name).toList))
  }
  
  /**
   * Called periodically to log out users that have no intreaction for one hour.
   */
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

case class User(name: String, group: Option[String] = Option(""))