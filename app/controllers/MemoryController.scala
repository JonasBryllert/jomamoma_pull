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

object MemoryController extends Controller {  
  
  val games: scala.collection.mutable.Map[String, MemoryGame] = scala.collection.mutable.Map.empty
  val waitQueue: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map.empty
  
  //message queue, user, JsValue
  val messageQueue = scala.collection.mutable.Map.empty[String, JsValue]
  
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
    println("MemoryController.memory -> " + gameId)
    if (request.session.get("user") == None ) Redirect(routes.LoginController.login)
    else {
      val user = request.session("user")
      //TODO Add redirect to error page if game not exist
      val game: MemoryGame = MemoryGame.getGame(gameId).get
      
      val jsonMessageGameInfo = Json.obj(
    		  "functionName" -> "gameInfo",
    		  "args" -> Json.obj(
    		      "images" -> Json.toJson(game.shuffledIdImageMap),
    		      "player1" -> game.player1,
    		      "player2" -> game.player2
    		   ))
      messageQueue += ((user, jsonMessageGameInfo))
      if (user == game.currentPlayer) {
        //Add json to your turn
        messageQueue += ((user, Json.obj("type" -> "yourMove")))
      }

      Ok(views.html.memory(game.size, request.session("user")))
    }
  }
 
  /**
   * Client call to retrieve messages
   */
  def getMessages(gameId: String) = Action { implicit request =>
    val user = request.session("user")
    println(s"XandO.getMessages -> : user: ${user}, messageQueue: $messageQueue")
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
  def clientMessage(gameId: String) = Action(BodyParsers.parse.json) { implicit request =>
    val user = request.session("user")
    val jsonMessage: JsValue = request.body
    val game = MemoryGame.getGame(gameId).get
    println(s"XandO.clientMessage -> user: $user, json: ${jsonMessage}")
    val message = (jsonMessage \ "message").as[String]
    if ("firstCellSelected".equals(message)) {
      //Just forward selected cell to opponent
      val responseJson: JsValue = Json.obj(
              "message" -> "firstCellSelected",
              "messageObject" -> Json.obj(
                  "firstCell" -> (jsonMessage \ "messageObject" \ "firstCell")
              )
      )
      messageQueue += ((game.getOtherPlayer(user), responseJson))              
    } else if ("secondCellSelected".equals(message)) {
      val firstCell = (jsonMessage \ "messageObject" \ "firstCell").as[String]
      val secondCell = (jsonMessage \ "messageObject" \ "secondCell").as[String]
      game.secondCellSelected(user, firstCell, secondCell)
    } else {
      println(s"MemoryController.clientMessage -> Unknown message")  
    }
    Ok("")
  }
  
  private def handleClientMessage(jsValue: JsValue, user: String): Result = {
    println(s"XandO.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
    val gameId = (jsValue \ "gameId").as[String]
    val mType = (jsValue \ "type").asOpt[String]
    mType match {
      case Some(sType) => {
        if (sType == "click") {
//          handleClick(game, jsValue, user)
        }
      }
      case _ => {
        println(s"Unknown client message: ${Json.prettyPrint(jsValue)}")
        Ok("")
      } 
    }
    Ok("") 
  }
    
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