package controllers

import scala.collection.mutable.Map
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import javax.inject.Inject
import javax.inject.Singleton
import model.GameCreator
import model.MemoryGame
import model.SinkShipGame
import model.Users
import model.XandOGame
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result

@Singleton
class Application @Inject() (system: ActorSystem, users: Users) extends Controller {
  
  //message queue, user, JsValue
  val messageQueue = Map.empty[String, JsValue]
  val gameCreator = system.actorOf(Props[GameCreator], "GameCreator")
  implicit val timeout = Timeout(2 seconds) 
  

  /**
   * This is the home page where you select game and opponent.
   */
  def home = Action { implicit request =>
    val userName = request.session.get("user")
    Logger.debug(s"Application -> home, user: userName")
    userName match {
      case None => Redirect(routes.IndexController.index())
      case Some(uName) => {
        if (users.isLoggedIn(uName)) {
          val user = users.userForName(uName).get
          val group = user.group.getOrElse("")
          Ok(views.html.home(user.name, group))
        }
        else Redirect(routes.IndexController.index())        
      }
    }
//    if (user == None ) Redirect(routes.IndexController.index())
//    else {
//      if (Users.isLoggedIn(user.get)) Ok(views.html.home(user.get))
//      else Redirect(routes.IndexController.index())
//    }
  }
  
  /**
   * JSON to get user list
   */
  def loadUsers = Action { implicit request =>
    val user = request.session("user")
    println(s"Application -> loadUsers, user: ${user}")
    val userNames: List[String] = users.getLoggedOnUsers(user)
    println(s"Application <- loadUsers: ${userNames}")
    returnJSON(toJson(userNames))
  }
  
  /**
   * Client call to retrieve messages as JSON
   */
  def getMessages = Action { implicit request =>
    if (request.session.get("user") == None) {
      println(s"Application.getMessage -> WARNING getMessage with no session")
      returnJSON(Json.obj("message"->"empty"))
    }
    else {
      val user = request.session("user")

//      println(s"Application.getMessages -> user: ${user}, messageQueue: $messageQueue")
      val message = messageQueue.remove(user)
      message match {
        //Send message from queeu if there is one!
        case Some(jsValue) => returnJSON(jsValue)
        case _ => {
            //2nd choice see if there are any new users logged on/off
          val usersOption: Option[(List[String], List[String])] = users.getLoggedOnOffUsers(user)
//          println(s"Application.getMessages -> user: ${user}, users: $usersOption")
          usersOption match {
            case Some(users) => {
              returnJSON(Json.obj(
                  "message" -> "users",
                  "messageObject" -> Json.obj(
                      "loggedOn" -> toJson(users._1),
            		  "loggedOff" -> toJson(users._2)
             	)
              ))
            }
            //last case return empty message
            case _ => returnJSON(Json.obj("message"->"empty"))
          }
        }
      }
    }     
  }
  
  /**
   * JSON client message
   */
  def clientMessage() = Action { implicit request =>
    if (request.session.get("user") == None) returnJSON(Json.obj("message"->"empty"))
    else {
      val user = request.session("user")
      println(s"Application.clientMessage -> user: $user")
      val jsonMessage: Option[JsValue] = request.body.asJson
      println(s"clientMessage -> user: $user, json: $jsonMessage")
      jsonMessage match {
        case Some(value) => handleClientMessage(value, request.session("user"))
        case None => {
          println("clientMessage -> None")
          Ok("")
        }
      }
    }
  }
    
  
  private def handleClientMessage(jsValue: JsValue, user: String): Result = {
    println(s"Application.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
    val mType = (jsValue \ "message").asOpt[String]
    mType match {
      case Some(sType) => {
        if (sType == "challenge") handleClientChallenge(jsValue, user)
        else if (sType == "challengeAccepted") handleClientChallengeAccepted(jsValue, user)
        else if (sType == "challengeRejected") handleClientChallengeRejected(jsValue, user)
        else Ok("")
      }
      case _ => {
        println(s"Unknown client message: ${Json.prettyPrint(jsValue)}")
        Ok("")
      } 
        
    }
  }
  
  private def handleClientChallenge(jsValue: JsValue, user: String): Result = {
    println(s"Application.handleClientChallenge -> user: $user")
    val opponent = (jsValue \ "opponent").as[String]
    val game = (jsValue \ "game").as[String]
    println(s"handleClientChallenge $opponent")
    val jsObject = Json.obj(
      "message" -> "challenge",
      "game" -> game,
      "challenger" -> user
    )
    messageQueue +=((opponent, jsObject)) 
    Ok("")
  }
  
  private def handleClientChallengeAccepted(jsValue: JsValue, user: String): Result = {
    println(s"Application.handleClientChallengeAccepted -> user: $user")
    val challenger = (jsValue \ "challenger").as[String]
    val game = (jsValue \ "game").as[String]
    println(s"handleClientChallengeAccepted -> user: $user, game: $game, challenger: $challenger")
     
    //Create game
    val gameId: String = 
    	if ("XandO-3".equals(game)) XandOGame.newGame(3, 3, challenger, user)
    	else if ("XandO-5".equals(game)) XandOGame.newGame(10, 5, challenger, user)
      else if ("Memory-10".equals(game)) MemoryGame.newGame(10, challenger, user)
    	else if ("Memory-20".equals(game)) MemoryGame.newGame(20, challenger, user)
      else if ("SinkShip".equals(game)) SinkShipGame.newGame(6, challenger, user)
      else if ("Connect4".equals(game)) {
        val future: Future[String] = (gameCreator ? GameCreator.Connect4(challenger, user)).mapTo[String]
        Await.result(future, 2 seconds)
      }
      else if ("Othello".equals(game)) {
        val future: Future[String] = (gameCreator ? GameCreator.Othello(6, challenger, user)).mapTo[String]
        Await.result(future, 2 seconds)
      }
      else if ("Chess".equals(game)) {
        val future: Future[String] = (gameCreator ? GameCreator.Chess(challenger, user)).mapTo[String]
        Await.result(future, 2 seconds)
      }
    	else "-1"
    val url = 
      if (game.startsWith("Memory")) "/memory/" + gameId
    	else if (game.startsWith("XandO")) "/xando/" + gameId
      else if ("SinkShip".equals(game)) "/sinkship/" + gameId
      else if ("Connect4".equals(game)) "/connect4/" + gameId
      else if ("Othello".equals(game)) "/othello/" + gameId
      else if ("Chess".equals(game)) "/chess/" + gameId
      else "-1"
    
    val jsObject = Json.obj(
      "message" -> "challengeAccepted",
      "challenger" -> challenger,
      "challengee" -> user,
      "url" -> url,
      "gameId" -> gameId
    )
    
    messageQueue += ((challenger, jsObject) )  
    messageQueue += ((user, jsObject) )  
    Ok("")
  }

  private def handleClientChallengeRejected(jsValue: JsValue, user: String): Result = {
    println(s"Application.handleClientChallengeRejected -> user: $user")
    val challenger = (jsValue \ "challenger").as[String]
    
    val jsObject = Json.obj(
      "message" -> "challengeRejected",
      "challenger" -> challenger,
      "challengee" -> user
    )
    
    messageQueue +=((challenger, jsObject) )  
    messageQueue +=((user, jsObject) )  
    Ok("")
  }
  
  private def returnJSON(json: JsValue): Result = Ok(json).withHeaders(CACHE_CONTROL -> "max-age=0, no-store", EXPIRES -> new java.util.Date().toString)
   
}