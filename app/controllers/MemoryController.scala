package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global
import model.MemoryGame
import model.MemoryGame._;
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._
import java.util.Timer
import scala.collection._

object MemoryController extends Controller {  
  
  val games: mutable.Map[String, MemoryGame] = mutable.Map.empty
  val waitQueue: scala.collection.mutable.Map[String, String] = mutable.Map.empty
  
  //message queue, user, JsValue
  val messageQueue = mutable.Map.empty[String, mutable.Queue[JsValue]]
  
//  //Json conversion
//  implicit val locationWrites = new Writes[ImageWithId] {
//    def writes(imageWithId: ImageWithId) = Json.obj(
//      "id" -> imageWithId.id,
//      "image" -> imageWithId.image
//    )
//  }

  /**
   * The game page where user will be redirected to when game starts
   */
  def memory(gameId: String) = Action { implicit request =>
    println("\nMemoryController.memory -> " + gameId)
    if (request.session.get("user") == None ) Redirect(routes.LoginController.login)
    else {
      val user = request.session("user")
      //TODO Add redirect to error page if game not exist
      val game: MemoryGame = MemoryGame.getGame(gameId).get
      //Add a queue for player
      messageQueue += ((user, new mutable.Queue))
      
      val isFirst = (user == game.currentPlayer)
      val jsonMessageGameInfo = Json.obj(
    		  "message" -> "gameInfo",
    		  "messageObject" -> Json.obj(
     		      "player1" -> game.player1,
    		      "player2" -> game.player2,
   		          "images" -> Json.toJson(game.shuffledIdImageMap),
   		          "yourMove" -> isFirst
    		   ))
      println(s"\nMemoryController.memory game: $gameId user: $user sending message <gameInfo>")
      messageQueue(user).enqueue(jsonMessageGameInfo)

      Ok(views.html.memory(game.size, request.session("user")))
    }
  }
 
  /**
   * Client call to retrieve messages
   */
  def getMessages(gameId: String) = Action { implicit request =>
    val user = request.session("user")
    if (!messageQueue(user).isEmpty) {
      val message = messageQueue(user).dequeue
      println(s"\nMemoryController.getMessages -> : user: ${user}, message: ${message}, messageQueue: $messageQueue")
      returnJSON(message)      
    }
    else {
    	returnJSON(Json.obj("message" -> "empty"))
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
                  "firstCell" -> (jsonMessage \ "messageObject" \ "firstCell")
              )
      )
      println(s"\nMemoryController.clientMessage -> user: $user, adding response message json: ${responseJson}")
      messageQueue(game.getOtherPlayer(user)).enqueue(responseJson)
      
    } else if ("secondCellSelected".equals(message)) {
      val firstCell = (jsonMessage \ "messageObject" \ "firstCell").as[String]
      val secondCell = (jsonMessage \ "messageObject" \ "secondCell").as[String]
      //Send update to opponent, and then send to game.
      val responseJson: JsValue = Json.obj(
              "message" -> "secondCellSelected",
              "messageObject" -> Json.obj(
                  "firstCell" -> firstCell,
                  "secondCell" -> secondCell
              )
      )
      println(s"\nMemoryController.clientMessage -> user: $user, adding response message json: ${responseJson}")
      messageQueue(game.getOtherPlayer(user)).enqueue(responseJson) 
      
      val (score, result) = game.secondCellSelected(user, firstCell, secondCell)
      val scoreJson: JsValue = Json.obj(
          "message" -> "showScore",
          "messageObject" -> Json.obj(
              "player1" -> score.getScore(game.player1),
              "player2" -> score.getScore(game.player2)
          )
      )
      println(s"\nMemoryController.clientMessage -> user: $user, adding response message json: ${scoreJson}")
      messageQueue(user).enqueue(scoreJson) 
      messageQueue(game.getOtherPlayer(user))enqueue(scoreJson) 
      
      result match {
        case PlayerWon(p) => {
          val resultJson: JsValue = Json.obj(
            "message" -> "gameOver",
            "messageObject" -> p
          )          
          messageQueue(user).enqueue(resultJson) 
          messageQueue(game.getOtherPlayer(user))enqueue(resultJson) 
        }
        case Draw => {
          val resultJson: JsValue = Json.obj(
            "message" -> "gameOver"
          )          
          messageQueue(user).enqueue(resultJson) 
          messageQueue(game.getOtherPlayer(user))enqueue(resultJson) 
        }
        case NextMove => {
          val nextMoveJson: JsValue = Json.obj(
            "message" -> "yourMove"
          )          
          messageQueue(game.getOtherPlayer(user))enqueue(nextMoveJson) 
        }
      }
    } else {
      println(s"MemoryController.clientMessage -> Unknown message")  
    }
    Ok("")
  }
  
//  private def handleClientMessage(jsValue: JsValue, user: String): Result = {
//    println(s"XandO.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
//    val gameId = (jsValue \ "gameId").as[String]
//    val mType = (jsValue \ "type").asOpt[String]
//    mType match {
//      case Some(sType) => {
//        if (sType == "click") {
////          handleClick(game, jsValue, user)
//        }
//      }
//      case _ => {
//        println(s"Unknown client message: ${Json.prettyPrint(jsValue)}")
//        Ok("")
//      } 
//    }
//    Ok("") 
//  }
    
//  private def handleClick(game: MemoryGame, jsValue: JsValue, user: String) = {
//      val moveResult = game.doClick(jsValue, user)
//      val posString = (jsValue \ "position").as[String]    
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
//    
//  }
 
}