package controllers

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.google.inject.Inject
import com.google.inject.Singleton
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.ActorSelection
import akka.pattern.ask
import akka.util.Timeout
import model.GameCreator
import model.Connect4Game
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import play.api.libs.json.Json
import scala.concurrent.Await
import play.api.libs.json.JsString

@Singleton
class Connect4Controller @Inject() (system: ActorSystem) extends Controller {  
//  val logger = Logger(this.getClass)
  val gameCreator: ActorSelection = system.actorSelection("/user/GameCreator")
//  system.actorSelection("").
  implicit val timeout = Timeout(2 seconds) 
  Logger.info(s"FourInARowController -> Got actor gameCreator ${gameCreator.toString}")

  val games: scala.collection.mutable.Map[String, ActorRef] = scala.collection.mutable.Map.empty
  val waitQueue: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map.empty
  
  //message queue, user, JsValue
  val messageQueue = scala.collection.mutable.Map.empty[String, JsValue]

  /**
   * The game page where user will be redirected to when game starts
   */
  def fourinarow(gameId: String) = Action.async { implicit request =>
    Logger.info(s"FourInARowController.fourinarow -> gameId: $gameId")
    if (request.session.get("user") == None ) {
      Logger.warn(s"FourInARowController.fourinarow, no user session found!!")
      Future{Redirect(routes.IndexController.index)}
    }
    else {
      val user = request.session("user")
      if (!storeGameActor(gameId)) {
        Logger.warn(s"FourInARowController.fourinarow, no game found!!")
        Future { 
          Redirect(routes.IndexController.index)
        }
      }
      else {
        val gameActor = games(gameId)
        //Get (otherPlayer, color)
        val otherInfoFuture: Future[(String, String)] = (gameActor ? Connect4Game.OtherPlayer(user)).mapTo[(String, String)]
        otherInfoFuture.map(otherInfo => {
          val currentPlayer = (gameActor ? Connect4Game.CurrentPlayer).mapTo[String]
          currentPlayer.onSuccess { case curPlayer =>
            if (curPlayer == user) {
              messageQueue += ((user, Json.obj("message" -> "yourMove")))
            }
            else {
              messageQueue += ((user, Json.obj("message" -> "oppMove")))
            }
          }
          
          Ok(views.html.connect4(user, otherInfo._1, otherInfo._2))
        })
      }
      
    }
  }
    
    private def storeGameActor(gameId: String): Boolean = {
      val futureGameActor: Future[Option[ActorRef]] = (gameCreator ? GameCreator.GetGame(gameId)).mapTo[Option[ActorRef]]
      val gameActorOption: Option[ActorRef] = Await.result(futureGameActor, 2 seconds)
      gameActorOption match {
        case Some(actor) => {
          games += ((gameId, actor))
          true
        }
        case _ => false        
      }
    }
  
  /**
   * Client call to retrieve messages
   */
  def getMessages(gameId: String) = Action { implicit request =>
//    returnJSON(Json.obj("message" -> "empty"))
    if (request.session.get("user") == None || !games.contains(gameId)) returnJSON(Json.obj("message"->"empty"))
    else { 
      val user = request.session("user")
//      Logger.info(s"FourInARowController.getMessages -> : user: ${user}, messageQueue: $messageQueue")
      val message = messageQueue.remove(user)
      message match {
        case Some(jsValue) => {
          Logger.info(s"FourInARowController.getMessages -> : user: ${user}, message: $message")
          returnJSON(jsValue)
        }
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
//    Ok("")
    if (request.session.get("user") != None || games.contains(gameId)) {
      val user = request.session("user")
      val gameActor: ActorRef = games(gameId)
 
      val jsonMessage: Option[JsValue] = request.body.asJson
      val message: Option[String] = jsonMessage.flatMap(js => (js \ "message").asOpt[String])
      message match {
        case Some("columnSelected") => {
          val column = (jsonMessage.get \ "column").as[Int]
          val response: Future[Connect4Game.Result] = (gameActor ? Connect4Game.Move(user, column)).mapTo[Connect4Game.Result]
          handleResponse(user, response)
        }
        case _ => {
          Logger.warn(s"FourInARowController.clientMessage -> Message: $message, user: $user, game: gameId")
        }
      }
    }
    Ok("")
  }
  
  private def handleResponse(user: String, response: Future[Connect4Game.Result]) = {
    response.map(_ match {
        case Connect4Game.NextPlayer(player, prevPosition) => {
          val response1 = Json.obj(
              "message" -> "yourMove",
              "prevMove" ->  Json.obj(
                "row" -> prevPosition._1,
                "column" -> prevPosition._2
              )
          ) 
          messageQueue += ((player, response1))
          val response2 = Json.obj(
              "message" -> "oppMove"
          ) 
          messageQueue += ((user, response2))
        }
        case Connect4Game.GameOver(nextPlayer, prevPosition, winner) => {
          var nextPlayerResponse = Json.obj(
              "message" -> "gameOver",
              "prevMove" -> Json.obj(
                "row" -> prevPosition._1,
                "column" -> prevPosition._2
              )
          )
          if (winner.nonEmpty) nextPlayerResponse += (("winner", JsString(winner.get)))
          messageQueue += ((nextPlayer, nextPlayerResponse))
          
          var currentPlayerResponse = Json.obj(
              "message" -> "gameOver"
          )
          if (winner.nonEmpty) currentPlayerResponse += (("winner", JsString(winner.get)))
          messageQueue += ((user, currentPlayerResponse))
       }
    })
  }
  /**
   * Convert a String of syntax: pos-@row-@column to a Position
   */
//  private def toPos(pos: String): Position = {
//    val arr = pos.split("-")
//    val row = arr(1).toInt
//    val col = arr(2).toInt
//    (row, col)
//  }
  

//  private def handleClientMessage(game: SinkShipGame, user: String, jsValue: JsValue): Result = {
//    println(s"SinkShipController.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
//    (jsValue \ "message").asOpt[String].map(message => {
//      if (message == "cellSelected") {
//        val positionString = (jsValue \ "position").as[String]
//        val pos = toPos(positionString)
//        handleCellSelected(game, pos, positionString, user)
////        game.doClick(pos, user)
//      }
//      else println(s"Unknown client message: ${Json.prettyPrint(jsValue)}")
//    })
//    Ok("") 
//
//  }
//    
//  private def handleCellSelected(game: SinkShipGame, pos: Position, posString: String, user: String) = {
//      val moveResult = game.doClick(pos, user)
//      moveResult match {
//	      case AllShipsSunk(ship) => {
//	        val jsUser = Json.obj(
//	                "message" -> "gameOver",
//	                "winner" -> user,
//	                "prevMove" -> Json.obj(
//	                    "user" -> user,
//	                    "pos" -> posString,
//	                    "isHit" -> true, 
//	                    "isSunk" -> true,
//	                    "shipPositions" -> Json.toJson(ship)
//	                ))
//	        val jsOpponent = Json.obj(
//	                "message" -> "gameOver",
//	                "winner" -> user,
//	                "prevMove" -> Json.obj(
//	                    "user" -> user,
//	                    "pos" -> posString,
//	                    "isHit" -> true, 
//	                    "isSunk" -> true,
//	                    "shipPositions" -> Json.toJson(ship)
//	                ))
//	        messageQueue += ((user, jsUser))
//	        messageQueue += ((game.otherPlayer(user), jsOpponent))
//	      }
//	      
//	      case ShipSunk(ship) => {
//	         val jsUser = Json.obj(
//	                "message" -> "oppMove",
//	                "prevMove" -> Json.obj(
//	                    "user" -> user,
//	                    "pos" -> posString, 
//	                    "isHit" -> true,
//	                    "isSunk" -> true,
//	                    "shipPositions" -> Json.toJson(ship)))
//	         val jsOpponent = Json.obj(
//	                "message" -> "yourMove",
//	                "prevMove" -> Json.obj(
//	                    "user" -> user,
//	                    "pos" -> posString, 
//	                    "isHit" -> true,
//	                    "isSunk" -> true,
//	                    "shipPositions" -> Json.toJson(ship)))
//	         messageQueue += ((user, jsUser))
//	         messageQueue += ((game.otherPlayer(user), jsOpponent))                   
//	      }
//	      
//	      case Hit => {
//	         val jsUser = Json.obj(
//	                "message" -> "oppMove",
//	                "prevMove" -> Json.obj(
//	                    "user" -> user,
//	                    "pos" -> posString, 
//	                    "isHit" -> true))
//	         val jsOpponent = Json.obj(
//	                "message" -> "yourMove",
//	                "prevMove" -> Json.obj(
//	                    "user" -> user,
//	                    "pos" -> posString, 
//	                    "isHit" -> true))
//	         messageQueue += ((user, jsUser))
//	         messageQueue += ((game.otherPlayer(user), jsOpponent))                   
//	      }
//	      
//	      case Miss => {
//	         val jsUser = Json.obj(
//	                "message" -> "oppMove",
//	                "prevMove" -> Json.obj(
//	                    "user" -> user,
//	                    "pos" -> posString, 
//	                    "isHit" -> false))
//	         val jsOpponent = Json.obj(
//	                "message" -> "yourMove",
//	                "prevMove" -> Json.obj(
//	                    "user" -> user,
//	                    "pos" -> posString, 
//	                    "isHit" -> false))
//	         messageQueue += ((user, jsUser))
//	         messageQueue += ((game.otherPlayer(user), jsOpponent))                   
//	      }   
//      }    
//  }
// 
}