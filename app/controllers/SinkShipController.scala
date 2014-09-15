package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global
import model.SinkShipGame
import model.SinkShipGame._
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._

object SinkShipController extends Controller {  
  
  val games: scala.collection.mutable.Map[String, SinkShipGame] = scala.collection.mutable.Map.empty
  val waitQueue: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map.empty
  
  //message queue, user, JsValue
  val messageQueue = scala.collection.mutable.Map.empty[String, JsValue]

  /**
   * The game page where user will be redirected to when game starts
   */
  def sinkShip(gameId: String) = Action { implicit request =>
    println("SinkShipController.sinkShip -> " + gameId)
    if (request.session.get("user") == None ) Redirect(routes.IndexController.index)
    else {
      val user = request.session("user")
      //TODO Add redirect to error page if game not exist
      val game: SinkShipGame = SinkShipGame.getGame(gameId).get
      if (game.currentPlayer == user) {
        messageQueue += ((user, Json.obj("type" -> "yourMove")))
      }
      Ok(views.html.sinkship(game.gameSize, request.session("user")))
    }
  }
 
  /**
   * Client call to retrieve messages
   */
  def getMessages(gameId: String) = Action { implicit request =>
    val user = request.session("user")
    println(s"SinkShipController.getMessages -> : user: ${user}, messageQueue: $messageQueue")
    val message = messageQueue.remove(user)
    message match {
      case Some(jsValue) => returnJSON(jsValue)
      case _ => returnJSON(Json.obj("type"->"empty"))
    }     
  }
  
  private def returnJSON(json: JsValue): Result = Ok(json).withHeaders(CACHE_CONTROL -> "max-age=0, no-store", EXPIRES -> new java.util.Date().toString)
  
 
  /**
   * JSON client message
   */
  def clientMessage(gameId: String) = Action { implicit request =>
    val user = request.session("user")
    val jsonMessage: Option[JsValue] = request.body.asJson
    println(s"SinkShipController.clientMessage -> user: $user, json: $jsonMessage")
    jsonMessage match {
      case Some(value) => handleClientMessage(value, request.session("user"))
      case None => {
        println("clientMessage -> None")
        Ok("")
      }
    }
  }
  
  private def handleClientMessage(jsValue: JsValue, user: String): Result = {
    println(s"SinkShipController.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
    val gameId = (jsValue \ "gameId").as[String]
    val game = SinkShipGame.getGame(gameId).get
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
    
  private def handleClick(game: SinkShipGame, jsValue: JsValue, user: String) = {
      val moveResult = game.doClick(jsValue, user)
      val posString = (jsValue \ "position").as[String]    
      Miss
//      moveResult match {
//	      case PlayerWon => {
//	        val jsUser = Json.obj(
//	                "type" -> "gameOver",
//	                "result" -> (user + " has won!!!"))
//	        val jsOtherUser = Json.obj(
//	                "type" -> "gameOver",
//	                "result" -> (user + " has won!!!"),
//	                "prevMove" -> posString)
//	        messageQueue += ((user, jsUser))
//	        messageQueue += ((game.otherPlayer(user), jsOtherUser))
//	
//	      }
//	      case Draw => {
//	        val jsUser = Json.obj(
//	                "type" -> "gameOver",
//	                "result" -> "It is a draw!")
//	        val jsOtherUser = Json.obj(
//	                "type" -> "gameOver",
//	                "result" -> "It is a draw!",
//	                "prevMove" -> posString)
//	        messageQueue += ((user, jsUser))
//	        messageQueue += ((game.otherPlayer(user), jsOtherUser))
//	      }
//	      case _ => {
//	    	 val posString = (jsValue \ "position").as[String]    
//	         val js = Json.obj(
//	                "type" -> "yourMove",
//	                "prevMove" -> posString)
//	        messageQueue += ((game.otherPlayer(user), js))           
//	      }
//      }
    
  }
 
}