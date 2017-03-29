package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import model.MemoryGame
import model.MemoryGame._
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._
import java.util.Timer
import scala.collection._
import model.MessageQueue

class MemoryController extends Controller {  
  
  val games: mutable.Map[String, MemoryGame] = mutable.Map.empty
  
  //message queue, user, JsValue
  //val messageQueue = mutable.Map.empty[String, mutable.Queue[JsValue]]
  val messageQueue = new MessageQueue
  
//  //Json conversion
//  implicit val locationWrites = new Writes[ImageWithId] {
//    def writes(imageWithId: ImageWithId) = Json.obj(
//      "id" -> imageWithId.id,
//      "image" -> imageWithId.image
//    )
//  }

  //Json conversion
//  implicit val imgFormat = Json.format[(String, String)];
  implicit val tjs = new Writes[(String, String)] {
    def writes(image: (String, String)) = Json.obj(
      image._1 -> image._2
   )
  }

  /**
   * The game page where user will be redirected to when game starts
   */
  def memory(gameId: String) = Action { implicit request =>
    println("\nMemoryController.memory -> " + gameId)
    if (request.session.get("user") == None ) Redirect(routes.IndexController.index)
    else {
      val user = request.session("user")
      //TODO Add redirect to error page if game not exist
      val game: MemoryGame = MemoryGame.getGame(gameId).get
      //Add a queue for player
      
      val isFirst = (user == game.currentPlayer)
      val jsonMessageGameInfo = Json.obj(
    		  "message" -> "gameInfo",
    		  "messageObject" -> Json.obj(
     		      "player1" -> game.player1,
    		      "player2" -> game.player2,
                "images" -> Json.toJson(game.shuffledIdImageList),
   		          "yourMove" -> isFirst
    		   ))
      println(s"\nMemoryController.memory game: $gameId user: $user sending message <gameInfo>")
      messageQueue.addToQueue(user, jsonMessageGameInfo)

      Ok(views.html.memoryAng(game.size, request.session("user")))
    }
  }
 
  /**
   * Client call to retrieve messages
   */
  def getMessages(gameId: String) = Action { implicit request =>
    val user = request.session("user")
    val message = messageQueue.removeFromQueue(user)
    message match {
      case Some(msg) => returnJSON(msg)
      case None => returnJSON(Json.obj("message" -> "empty"))
    }
  }
  
  private def returnJSON(json: JsValue): Result = Ok(json).withHeaders(CACHE_CONTROL -> "max-age=0, no-store", EXPIRES -> new java.util.Date().toString)
  
 
  /**
   * JSON client message
   */
  def clientMessage(gameId: String) = Action { implicit request =>
    val user = request.session("user")
//    println(s"XandO.clientMessage -> user: $user, body: ${request.body}")
    val jsonMessageO: Option[JsValue] = request.body.asJson
//    println(s"XandO.clientMessage -> user: $user, json: $jsonMessageO")
    val jsonMessage = jsonMessageO.get
//    println(s"XandO.clientMessage -> user: $user, json: $jsonMessage")
//    val user = request.session("user")
//    val jsonMessage: JsValue = request.body
    val game = MemoryGame.getGame(gameId).get
    println(s"\nMemoryController.clientMessage -> user: $user, json: ${jsonMessage}")
    val message = (jsonMessage \ "message").as[String]
    if ("firstCellSelected".equals(message)) {
      //Just forward selected cell to opponent
      val responseJson: JsValue = Json.obj(
              "message" -> "firstCellSelected",
              "messageObject" -> Json.obj(
                  "pos" -> (jsonMessage \ "messageObject" \ "pos").get
              )
      )
      println(s"\nMemoryController.clientMessage -> user: $user, adding response message json: ${responseJson}")
      messageQueue.addToQueue(game.getOtherPlayer(user), responseJson)
      
    } else if ("secondCellSelected".equals(message)) {
      val firstCell = (jsonMessage \ "messageObject" \ "firstCell").as[Int]
      val secondCell = (jsonMessage \ "messageObject" \ "secondCell").as[Int]
      //Send update to opponent, and then send to game.
      val responseJson: JsValue = Json.obj(
              "message" -> "secondCellSelected",
              "messageObject" -> Json.obj(
                  "firstCell" -> firstCell,
                  "secondCell" -> secondCell
              )
      )
      println(s"\nMemoryController.clientMessage -> user: $user, adding response message json: ${responseJson}")
      messageQueue.addToQueue(game.getOtherPlayer(user), responseJson) 
      
      val (score, result) = game.secondCellSelected(user, firstCell, secondCell)
      val scoreJson: JsValue = Json.obj(
          "message" -> "showScore",
          "messageObject" -> Json.obj(
              "player1" -> score.getScore(game.player1),
              "player2" -> score.getScore(game.player2)
          )
      )
      println(s"\nMemoryController.clientMessage -> user: $user, adding response message json: ${scoreJson}")
      messageQueue.addToQueue(user, scoreJson) 
      messageQueue.addToQueue(game.getOtherPlayer(user), scoreJson)  
      
      result match {
        case PlayerWon(p) => {
          val resultJson: JsValue = Json.obj(
            "message" -> "gameOver",
            "messageObject" -> Json.obj("winner" -> p)
          )          
          messageQueue.addToQueue(user, resultJson) 
          messageQueue.addToQueue(game.getOtherPlayer(user), resultJson) 
        }
        case Draw => {
          val resultJson: JsValue = Json.obj(
            "message" -> "gameOver",
            "messageObject" -> Json.obj("draw" -> true)
          )          
          messageQueue.addToQueue(user, resultJson) 
          messageQueue.addToQueue(game.getOtherPlayer(user), resultJson) 
        }
        case NextMove(isScore) => {
          val yourMoveJson: JsValue = Json.obj(
            "message" -> "yourMove",
            "messageObject" -> Json.obj("isFirstMove" -> !isScore)        
          )       
          val oppMoveJson: JsValue = Json.obj(
            "message" -> "oppMove"       
          )       
          if (isScore) {
            messageQueue.addToQueue(user, yourMoveJson)
            messageQueue.addToQueue(game.getOtherPlayer(user), oppMoveJson)
          }
          else {
            messageQueue.addToQueue(user, oppMoveJson)
            messageQueue.addToQueue(game.getOtherPlayer(user), yourMoveJson)
          }
        }
      }
    } else {
      println(s"MemoryController.clientMessage -> Unknown message")  
    }
    Ok("")
  }
  
}