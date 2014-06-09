package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global
import model.XandOGame
import model.MemoryGame
import model.Users
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._

object Application extends Controller {
  
  //message queue, user, JsValue
  val messageQueue = scala.collection.mutable.Map.empty[String, JsValue]
  

  /**
   * This is the index page where you end up before you log in and after you log out.
   */
  def index = Action { implicit request =>
    val user = request.session.get("user")
    println(s"index -> user: $user")
    user match {
      case None => Ok(views.html.index())
      case _ => Redirect(routes.Application.home)
    }
  }
  
  /**
   * This is the home page where you select game and opponent.
   */
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
   * Client call to retrieve messages as JSON
   */
  def getMessages = Action { implicit request =>
    val user = request.session("user")
    println(s"Application.getMessages -> user: ${user}, messageQueue: $messageQueue")
    val message = messageQueue.remove(user)
    message match {
      case Some(jsValue) => returnJSON(jsValue)
      case _ => returnJSON(Json.obj("type"->"empty"))
    }     
  }
  
  /**
   * JSON client message
   */
  def clientMessage() = Action { implicit request =>
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
  
  private def handleClientMessage(jsValue: JsValue, user: String): Result = {
    println(s"Application.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
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
  
  private def handleClientChallenge(jsValue: JsValue, user: String): Result = {
    println(s"Application.handleClientChallenge -> user: $user")
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
  }
  
  private def handleClientChallengeAccepted(jsValue: JsValue, user: String): Result = {
    println(s"Application.handleClientChallengeAccepted -> user: $user")
    val challenger = (jsValue \ "challenger").as[String]
    val game = (jsValue \ "game").as[String]
    println(s"handleClientChallengeAccepted -> user: $user, game: $game, challenger: $challenger")
     
    //Create game
    val gameId = 
    	if (game == "ThreeInARow") XandOGame.newGame(3, 3, challenger, user)
    	else if (game == "FiveInARow") XandOGame.newGame(10, 5, challenger, user)
    	else if (game == "Memory") MemoryGame.newGame(4, challenger, user)
    	else "-1"
    
    val url = "/xando/" + gameId;
    val jsObject = Json.obj(
      "type" -> "challengeAccepted",
      "challenger" -> challenger,
      "challengee" -> user,
      "url" -> url,
      "gameId" -> gameId
    )
    
    messageQueue +=((challenger, jsObject) )  
    messageQueue +=((user, jsObject) )  
    Ok("")
  }

  private def handleClientChallengeRejected(jsValue: JsValue, user: String): Result = {
    println(s"Application.handleClientChallengeRejected -> user: $user")
    val challenger = (jsValue \ "challenger").as[String]
    
    val jsObject = Json.obj(
      "type" -> "challengeRejected",
      "challenger" -> challenger,
      "challengee" -> user
    )
    
    messageQueue +=((challenger, jsObject) )  
    messageQueue +=((user, jsObject) )  
    Ok("")
  }
  
  private def returnJSON(json: JsValue): Result = Ok(json).withHeaders(CACHE_CONTROL -> "max-age=0, no-store", EXPIRES -> new java.util.Date().toString)
   
}