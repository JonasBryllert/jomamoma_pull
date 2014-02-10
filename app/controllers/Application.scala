package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global
import model.FiveInARowGame
import model.Users
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._

object Application extends Controller {
  
  //message queue, user, JsValue
  val messageQueue = scala.collection.mutable.Map.empty[String, JsValue]
  

  def index = Action { implicit request =>
    val user = request.session.get("user")
    println(s"index -> user: $user")
    user match {
      case None => Ok(views.html.index())
      case _ => Redirect(routes.Application.home)
    }
  }
  
  def home = Action { implicit request =>
    val user = request.session.get("user")
    println(s"home -> user: $user")
    if (user == None ) Redirect(routes.LoginController.login)
    else Ok(views.html.home(user.get))
  }
  
  /**
   * JSON to get user list
   */
  def loadUsers = Action { implicit request =>
    val user = request.session("user")
    println(s"entering loadUsers: user: ${user}")
    val userNames: List[String] = Users.users.map(user => user.name).filter(u => u != user)
    println(s"exiting loadUsers: ${userNames}")
    println(s"exiting loadUsers: ${toJson(userNames)}")
    returnJSON(toJson(userNames))
  }
  
  /**
   * JSON client message
   */
  def clientMessage() = Action { implicit request =>
    val user = request.session("user")
    println(s"clientMessage -> user: $user")
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
  
  def handleClientMessage(jsValue: JsValue, user: String): Result = {
    println(s"handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
    val mType = (jsValue \ "type").asOpt[String]
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
  
  def handleClientChallenge(jsValue: JsValue, user: String): Result = {
    println(s"handleClientChallenge -> user: $user")
    val opponent = (jsValue \ "opponent").as[String]
    val game = (jsValue \ "game").as[String]
    println(s"handleClientChallenge $opponent")
    val jsObject = Json.obj(
      "type" -> "challenge",
      "game" -> game,
      "challenger" -> user
    )
    messageQueue +=((opponent, jsObject)) 
    Ok("")
//    messageQueue +=((opponent, jsObject) )    
  }
  
  def handleClientChallengeAccepted(jsValue: JsValue, user: String): Result = {
    println(s"handleClientChallengeAccepted -> user: $user")
    val challenger = (jsValue \ "challenger").as[String]
    val game = (jsValue \ "game").as[String]
    println(s"handleClientChallengeAccepted -> user: $user, game: $game, challenger: $challenger")
     
    //Create game
    val gameId = 
    	if (game == "ThreeInARow") FiveInARowGame.newGame(3, 3, challenger, user)
    	else FiveInARowGame.newGame(10, 5, challenger, user)
    
    val jsObject = Json.obj(
      "type" -> "challengeAccepted",
      "challenger" -> challenger,
      "challengee" -> user,
      "gameId" -> gameId
    )
    
    messageQueue +=((challenger, jsObject) )  
    messageQueue +=((user, jsObject) )  
//    Redirect(routes.FiveInARowController.game(gameId))
    Ok("")
//    messageQueue +=((opponent, jsObject) )        
  }

  def handleClientChallengeRejected(jsValue: JsValue, user: String): Result = {
    println(s"handleClientChallengeRejected -> user: $user")
    val challenger = (jsValue \ "challenger").as[String]
    
    val jsObject = Json.obj(
      "type" -> "challengeRejected",
      "challenger" -> challenger,
      "challengee" -> user
    )
    
    messageQueue +=((challenger, jsObject) )  
    messageQueue +=((user, jsObject) )  
//    Redirect(routes.FiveInARowController.game(gameId))
    Ok("")
//    messageQueue +=((opponent, jsObject) )        
  }
  
  /**
   * Client call to retrieve messages
   */
  def getMessages = Action { implicit request =>
    val user = request.session("user")
    println(s"entering checkMessages: user: ${user}, messageQueue: $messageQueue")
    val message = messageQueue.remove(user)
    message match {
      case Some(jsValue) => returnJSON(jsValue)
      case _ => returnJSON(Json.obj("type"->"empty"))
    }     
  }
  
  def returnJSON(json: JsValue): Result = Ok(json).withHeaders(CACHE_CONTROL -> "max-age=0, no-store", EXPIRES -> new java.util.Date().toString)
  
//  def fiveInARowGame = Action {
//    Ok(views.html.game(FiveInARowGame.size))
//  }
//
 
}