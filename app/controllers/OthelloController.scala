package controllers

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.collection.mutable.{Map => MutableMap, Queue}
import com.google.inject.Inject
import com.google.inject.Singleton
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.ActorSelection
import akka.pattern.ask
import akka.util.Timeout
import model.GameCreator
import model.OthelloGame
import model.OthelloGame.Position
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import play.api.libs.json.Json
import scala.concurrent.Await
import play.api.libs.json.JsString
import model.MessageQueue

@Singleton
class OthelloController @Inject() (system: ActorSystem) extends Controller {  
//  val logger = Logger(this.getClass)
  val gameCreator: ActorSelection = system.actorSelection("/user/GameCreator")
//  system.actorSelection("").
  implicit val timeout = Timeout(2 seconds) 
  Logger.info(s"OthelloController -> Got actor gameCreator ${gameCreator.toString}")

  val games: MutableMap[String, ActorRef] = MutableMap.empty
  
  //message queue, user, JsValue
  val messageQueue = new MessageQueue()

  /**
   * The game page where user will be redirected to when game starts
   */
  def othello(gameId: String) = Action.async { implicit request =>
    import OthelloGame.{GameInfo, GetGameInfo}
    Logger.info(s"OthelloController.othello -> gameId: $gameId")
    if (request.session.get("user") == None ) {
      Logger.warn(s"OthelloController.othello, no user session found!!")
      Future{Redirect(routes.IndexController.index)}
    }
    else {
      val user = request.session("user")
      if (!storeGameActor(gameId)) {
        Logger.warn(s"OthelloController.othello, no game found!!")
        Future { 
          Redirect(routes.IndexController.index)
        }
      }
      else {
        val gameActor = games(gameId)
        //Get (otherPlayer, color)
        val gameInfoFuture: Future[GameInfo] = (gameActor ? GetGameInfo(user)).mapTo[GameInfo]
        gameInfoFuture.map(gameInfo => {
          Logger.info(s"OthelloController:GameInfo: player: ${user}, gameInfo: ${gameInfo}")
          if (gameInfo.isGameOver) {
           Redirect(routes.IndexController.index) 
          }
          else {
            if (gameInfo.isCurrentPlayer) {
              messageQueue.addToQueue(user, Json.obj("message" -> "yourMove"))
            }
            else {
              messageQueue.addToQueue(user, Json.obj("message" -> "oppMove"))
            }
            Ok(views.html.othello(user, gameInfo.size, gameInfo.otherPlayer, gameInfo.color))
          }
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
//      Logger.info(s"OthelloController.getMessages -> : user: ${user}, messageQueue: $messageQueue")
      val message = messageQueue.removeFromQueue(user)
      message match {
        case Some(jsValue) => {
          Logger.info(s"OthelloController.getMessages -> : user: ${user}, message: $message")
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
    if (request.session.get("user") != None && games.contains(gameId)) {
      val user = request.session("user")
      val gameActor: ActorRef = games(gameId)
 
      val jsonMessage: Option[JsValue] = request.body.asJson
      Logger.info(s"message: $jsonMessage")
      val message: Option[String] = jsonMessage.flatMap(js => (js \ "message").asOpt[String])
      message match {
        case Some("positionSelected") => {
          val row = (jsonMessage.get \ "row").as[Int]
          val column = (jsonMessage.get \ "col").as[Int]
          val position: Option[Position] = if (row < 0) None else Some(row,column)
          val response: Future[OthelloGame.Result] = (gameActor ? OthelloGame.Move(user, position)).mapTo[OthelloGame.Result]
          handleResponse(user, response)
        }
        case _ => {
          Logger.warn(s"OthelloController.clientMessage -> Unknown message: $message, user: $user, game: $gameId")
        }
      }
    }
    Ok("")
  }
  
  private def handleResponse(user: String, response: Future[OthelloGame.Result]) = {
    response.map(_ match {
        case OthelloGame.InvalidMove(position) => {
          val response = Json.obj(
            "message" -> "invalidMove",
            "prevMove" -> Json.obj(
              "row" -> position._1,
              "column" -> position._2
            )
          )
          messageQueue.addToQueue(user, response)
        }
        case OthelloGame.NextPlayer(nextPlayer, prevColor, prevPosition) => {
          val prevPosJsonArray = prevPosition.map { case (r,c) => Json.obj("row" -> r, "column" -> c) }
          val response1 = Json.obj(
              "message" -> "yourMove",
              "prevColor" -> prevColor,
              "prevMove" ->  prevPosJsonArray
          )
          messageQueue.addToQueue(nextPlayer, response1)
          val response2 = Json.obj(
              "message" -> "oppMove",
              "prevColor" -> prevColor,
              "prevMove" ->  prevPosJsonArray              
          ) 
          messageQueue.addToQueue(user, response2)
        }
        case OthelloGame.GameOver(nextPlayer, prevColor, prevPosition, winner) => {
          val prevPosJsonArray = prevPosition.map { case (r,c) => Json.obj("row" -> r, "column" -> c) }
          var nextPlayerResponse = Json.obj(
              "message" -> "gameOver",
              "prevColor" -> prevColor,
              "prevMove" -> prevPosJsonArray
          )
          if (winner.nonEmpty) nextPlayerResponse += (("winner", JsString(winner.get)))
          messageQueue.addToQueue(nextPlayer, nextPlayerResponse)
          
          var currentPlayerResponse = Json.obj(
              "message" -> "gameOver",
              "prevColor" -> prevColor,
              "prevMove" -> prevPosJsonArray
          )
          if (winner.nonEmpty) currentPlayerResponse += (("winner", JsString(winner.get)))
          messageQueue.addToQueue(user, currentPlayerResponse)
       }
    })
  } 
}