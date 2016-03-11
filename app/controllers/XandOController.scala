package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import model.XandOGame
import model.XandOGame._
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._
import java.util.Timer
import model.MessageQueue

class XandOController extends Controller {  
  
  val games: scala.collection.mutable.Map[String, XandOGame] = scala.collection.mutable.Map.empty
  val waitQueue: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map.empty
  
  //message queue, user, JsValue
  val messageQueue = new MessageQueue

  /**
   * The game page where user will be redirected to when game starts
   */
  def xando(gameId: String) = Action { implicit request =>
    println("XandO.game -> " + gameId)
    if (request.session.get("user") == None ) Redirect(routes.IndexController.index)
    else {
      val user = request.session("user")
      //TODO Add redirect to error page if game not exist
      val game: XandOGame = XandOGame.getGame(gameId).get
      val playerSymbol = game.getPlayerSymbol(user)
      if (playerSymbol == "X") {
        messageQueue.addToQueue(user, Json.obj("type" -> "yourMove"))
      }
      else {
        messageQueue.addToQueue(user, Json.obj("type" -> "oppMove"))
      }
      Ok(views.html.XandO(game.size, game.nrToWin,  user, playerSymbol, game.otherPlayer(user)))
    }
  }
 
  /**
   * Client call to retrieve messages
   */
  def getMessages = Action { implicit request =>
    val user = request.session("user")
    println(s"XandO.getMessages -> : user: ${user}, messageQueue: $messageQueue")
    val message = messageQueue.removeFromQueue(user)
    message match {
      case Some(jsValue) => returnJSON(jsValue)
      case _ => returnJSON(Json.obj("type"->"empty"))
    }     
  }
  
  private def returnJSON(json: JsValue): Result = Ok(json).withHeaders(CACHE_CONTROL -> "max-age=0, no-store", EXPIRES -> new java.util.Date().toString)
  
 
  /**
   * JSON client message
   */
  def clientMessage() = Action { implicit request =>
    val user = request.session("user")
    val jsonMessage: Option[JsValue] = request.body.asJson
    println(s"XandO.clientMessage -> user: $user, json: $jsonMessage")
    jsonMessage match {
      case Some(value) => handleClientMessage(value, request.session("user"))
      case None => {
        println("clientMessage -> None")
        Ok("")
      }
    }
  }
  
  private def handleClientMessage(jsValue: JsValue, user: String): Result = {
    println(s"XandO.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
    val gameId = (jsValue \ "gameId").as[String]
    val game = XandOGame.getGame(gameId).get
    val mType = (jsValue \ "type").asOpt[String]
    mType match {
      case Some(sType) => {
        if (sType == "click") {
          handleClick(game, jsValue, user)
        }
      }
      case _ => {
        println(s"Unknown client message: ${Json.prettyPrint(jsValue)}")
        Ok("")
      } 
    }
    Ok("") 
  }
    
  private def handleClick(game: XandOGame, jsValue: JsValue, user: String) = {
      val moveResult = game.doClick(jsValue, user)
      val posString = (jsValue \ "position").as[String]    
      moveResult match {
	      case PlayerWon => {
	        val jsUser = Json.obj(
	                "type" -> "gameOver",
	                "result" -> (user + " has won!!!"))
	        val jsOtherUser = Json.obj(
	                "type" -> "gameOver",
	                "result" -> (user + " has won!!!"),
	                "prevMove" -> posString)
	        messageQueue.addToQueue(user, jsUser)
	        messageQueue.addToQueue(game.otherPlayer(user), jsOtherUser)
	
	      }
	      case Draw => {
	        val jsUser = Json.obj(
	                "type" -> "gameOver",
	                "result" -> "It is a draw!")
	        val jsOtherUser = Json.obj(
	                "type" -> "gameOver",
	                "result" -> "It is a draw!",
	                "prevMove" -> posString)
	        messageQueue.addToQueue(user, jsUser)
	        messageQueue.addToQueue(game.otherPlayer(user), jsOtherUser)
	      }
	      case _ => {
	        val jsUser = Json.obj(
	                "type" -> "oppMove")
	        val jsOtherUser = Json.obj(
	                "type" -> "yourMove",
	                "prevMove" -> posString)
	        messageQueue.addToQueue(user, jsUser)
	        messageQueue.addToQueue(game.otherPlayer(user), jsOtherUser)           
          
	      }
      }
    
  }
 
}