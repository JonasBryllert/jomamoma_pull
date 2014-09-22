package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global
import model.Ship
import model.Ship.Position
import model.SinkShipGame
import model.SinkShipGame._
import model.WriteConverters._;
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
        messageQueue += ((user, Json.obj("message" -> "yourMove")))
      }
      else {
        messageQueue += ((user, Json.obj("message" -> "oppMove")))
      }
      Ok(views.html.sinkship(game.gameSize, user, game.otherPlayer(user)))
    }
  }
  
  def getShips(gameId: String) = Action { implicit request =>
    if (request.session.get("user") == None || SinkShipGame.getGame(gameId) == None) returnJSON(Json.obj("message"->"empty"))
    else {
      val ships: Set[Ship] = SinkShipGame.getGame(gameId).get.getShips(request.session("user")).toSet
      returnJSON(Json.toJson(ships))
    }
  }
 
  /**
   * Client call to retrieve messages
   */
  def getMessages(gameId: String) = Action { implicit request =>
    if (request.session.get("user") == None || SinkShipGame.getGame(gameId) == None) returnJSON(Json.obj("message"->"empty"))
    else {
      val user = request.session("user")
      println(s"SinkShipController.getMessages -> : user: ${user}, messageQueue: $messageQueue")
      val message = messageQueue.remove(user)
      message match {
        case Some(jsValue) => returnJSON(jsValue)
        case _ => returnJSON(Json.obj("message" -> "empty"))
      }   
    }
  }
  
  private def returnJSON(json: JsValue): Result = 
    Ok(json).withHeaders(CACHE_CONTROL -> "max-age=0, no-store", EXPIRES -> new java.util.Date().toString)
  
 
  /**
   * JSON client message
   */
  def clientMessage(gameId: String) = Action { implicit request =>
    if (request.session.get("user") == None || SinkShipGame.getGame(gameId) == None) Ok("")
    else {
      val user = request.session("user")
      val game = SinkShipGame.getGame(gameId).get
      val jsonMessage: Option[JsValue] = request.body.asJson
      jsonMessage match {
        case Some(json) => handleClientMessage(game, user, json)
        case None => {
          println("clientMessage -> None")
          Ok("")
        }
      }
    }
  }
  
  /**
   * Convert a String of syntax: pos-@row-@column to a Position
   */
  private def toPos(pos: String): Position = {
    val arr = pos.split("-")
    val row = arr(1).toInt
    val col = arr(2).toInt
    (row, col)
  }
  

  private def handleClientMessage(game: SinkShipGame, user: String, jsValue: JsValue): Result = {
    println(s"SinkShipController.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
    (jsValue \ "message").asOpt[String].map(message => {
      if (message == "cellSelected") {
        val positionString = (jsValue \ "position").as[String]
        val pos = toPos(positionString)
        handleCellSelected(game, pos, positionString, user)
//        game.doClick(pos, user)
      }
      else println(s"Unknown client message: ${Json.prettyPrint(jsValue)}")
    })
    Ok("") 

  }
    
  private def handleCellSelected(game: SinkShipGame, pos: Position, posString: String, user: String) = {
      val moveResult = game.doClick(pos, user)
      moveResult match {
	      case AllShipsSunk(ships) => {
	        val jsUser = Json.obj(
	                "message" -> "gameOver",
	                "winner" -> user,
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString,
	                    "isHit" -> true, 
	                    "isSunk" -> true,
	                    "ships" -> Json.toJson(ships)
	                ))
	        val jsOpponent = Json.obj(
	                "message" -> "gameOver",
	                "winner" -> user,
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString,
	                    "isHit" -> true, 
	                    "isSunk" -> true,
	                    "ships" -> Json.toJson(ships)
	                ))
	        messageQueue += ((user, jsUser))
	        messageQueue += ((game.otherPlayer(user), jsOpponent))
	      }
	      
	      case ShipSunk(ship) => {
	         val jsUser = Json.obj(
	                "message" -> "oppMove",
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString, 
	                    "isHit" -> true,
	                    "isSunk" -> true,
	                    "ship" -> Json.toJson(ship)))
	         val jsOpponent = Json.obj(
	                "message" -> "yourMove",
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString, 
	                    "isHit" -> true,
	                    "isSunk" -> true,
	                    "ship" -> Json.toJson(ship)))
	         messageQueue += ((user, jsUser))
	         messageQueue += ((game.otherPlayer(user), jsOpponent))                   
	      }
	      
	      case Hit => {
	         val jsUser = Json.obj(
	                "message" -> "oppMove",
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString, 
	                    "isHit" -> true))
	         val jsOpponent = Json.obj(
	                "message" -> "yourMove",
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString, 
	                    "isHit" -> true))
	         messageQueue += ((user, jsUser))
	         messageQueue += ((game.otherPlayer(user), jsOpponent))                   
	      }
	      
	      case Miss => {
	         val jsUser = Json.obj(
	                "message" -> "oppMove",
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString, 
	                    "isHit" -> false))
	         val jsOpponent = Json.obj(
	                "message" -> "yourMove",
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString, 
	                    "isHit" -> false))
	         messageQueue += ((user, jsUser))
	         messageQueue += ((game.otherPlayer(user), jsOpponent))                   
	      }   
      }    
  }
 
}