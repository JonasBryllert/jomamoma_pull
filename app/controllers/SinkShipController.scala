package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Map => MMap, Queue}
import model.Ship
import model.Ship.Position
import model.SinkShipGame
import model.SinkShipGame._
import model.WriteConverters._;
import com.fasterxml.jackson.databind.JsonNode
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Json._

class SinkShipController extends Controller {  
  
  val games: MMap[String, SinkShipGame] = MMap.empty
  
  //message queue, user, JsValue
  val messageQueue = MMap.empty[String, Queue[JsValue]]
  val lastMessageQueue = MMap.empty[String, (Int, JsValue)]

  private def addToQueue(user: String, jsValue: JsValue) = {
    if  (messageQueue.contains(user)) {
      messageQueue(user).enqueue(jsValue)
    }
    else {
      messageQueue.put(user, Queue(jsValue))
    }
  }
  
  private def removeFromQueue(user: String): Option[JsValue] = {
    if  (messageQueue.contains(user)) {
      val jsValue = messageQueue(user).dequeue()
      if (messageQueue(user).isEmpty) messageQueue.remove(user)
      Some(jsValue)
    }
    else {
      None
    }
  }
  
  /**
   * The game page where user will be redirected to when game starts
   */
  def sinkShip(gameId: String) = Action { implicit request =>
    Logger.info("SinkShipController.sinkShip -> " + gameId)
    if (request.session.get("user") == None ) Redirect(routes.IndexController.index)
    else {
      val user = request.session("user")
      //TODO Add redirect to error page if game not exist
      val game: SinkShipGame = SinkShipGame.getGame(gameId).get
      if (game.currentPlayer == user) {
        addToQueue(user, Json.obj("message" -> "yourMove"))
      }
      else {
        addToQueue(user, Json.obj("message" -> "oppMove"))
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
  def getMessages(gameId: String, msgId: Int) = Action { implicit request =>
    if (request.session.get("user") == None || SinkShipGame.getGame(gameId) == None) returnJSON(Json.obj("message"->"empty"))
    else {
      val user = request.session("user")
      if (messageQueue.contains(user) || lastMessageQueue.contains(user)) {
        Logger.info(s"SinkShipController.getMessages -> : user: ${user}, msgId: $msgId, messageQueue: $messageQueue, lastMessageQueue: $lastMessageQueue")
      }
      val lastMessage = lastMessageQueue.remove(user)
      lastMessage match {
        case Some((lastMsgId, lastjsValue)) => {
          //check if last msgId is same as last message and resend if so
          if (lastMsgId == msgId) {
            lastMessageQueue += ((user, (msgId, lastjsValue)))
            Logger.info(s"SinkShipController.getMessages resend last message -> : user: ${user}, msgId: $msgId, jsValue: $lastjsValue")
            returnJSON(lastjsValue)    
          }
          else {
            val message = removeFromQueue(user)
            message match {
              case Some(jsValue) => {
                lastMessageQueue += ((user, (msgId, jsValue)))
                Logger.info(s"SinkShipController.getMessages message -> : user: ${user}, msgId: $msgId, jsValue: $jsValue")
                returnJSON(jsValue)
              }
              case None => returnJSON(Json.obj("message" -> "empty"))
            } 
          }
        }
        //Otherwise see if any new messages and put it in lastMessage
        case None => {
          val message = removeFromQueue(user)
          message match {
            case Some(jsValue) => {
              lastMessageQueue += ((user, (msgId, jsValue)))
              returnJSON(jsValue)
            }
            case None => returnJSON(Json.obj("message" -> "empty"))
    
          }
        }
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
          Logger.info("clientMessage -> None")
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
    Logger.info(s"SinkShipController.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
    (jsValue \ "message").asOpt[String].map(message => {
      if (message == "cellSelected") {
        val positionString = (jsValue \ "position").as[String]
        val pos = toPos(positionString)
        handleCellSelected(game, pos, positionString, user)
//        game.doClick(pos, user)
      }
      else Logger.info(s"Unknown client message: ${Json.prettyPrint(jsValue)}")
    })
    Ok("") 

  }
    
  private def handleCellSelected(game: SinkShipGame, pos: Position, posString: String, user: String) = {
      val moveResult = game.doClick(pos, user)
      moveResult match {
	      case AllShipsSunk(ship) => {
	        val jsUser = Json.obj(
	                "message" -> "gameOver",
	                "winner" -> user,
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString,
	                    "isHit" -> true, 
	                    "isSunk" -> true,
	                    "shipPositions" -> Json.toJson(ship)
	                ))
	        val jsOpponent = Json.obj(
	                "message" -> "gameOver",
	                "winner" -> user,
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString,
	                    "isHit" -> true, 
	                    "isSunk" -> true,
	                    "shipPositions" -> Json.toJson(ship)
	                ))
	        addToQueue(user, jsUser)
          addToQueue(game.otherPlayer(user), jsOpponent)
	      }
	      
	      case ShipSunk(ship) => {
	         val jsUser = Json.obj(
	                "message" -> "oppMove",
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString, 
	                    "isHit" -> true,
	                    "isSunk" -> true,
	                    "shipPositions" -> Json.toJson(ship)))
	         val jsOpponent = Json.obj(
	                "message" -> "yourMove",
	                "prevMove" -> Json.obj(
	                    "user" -> user,
	                    "pos" -> posString, 
	                    "isHit" -> true,
	                    "isSunk" -> true,
	                    "shipPositions" -> Json.toJson(ship)))
           addToQueue(user, jsUser)
           addToQueue(game.otherPlayer(user), jsOpponent)
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
           addToQueue(user, jsUser)
           addToQueue(game.otherPlayer(user), jsOpponent)
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
           addToQueue(user, jsUser)
           addToQueue(game.otherPlayer(user), jsOpponent)
	      }   
      }    
  }
 
}