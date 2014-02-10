package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global
import model.FiveInARowGame
import model.FiveInARowGame._;
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._
import java.util.Timer

object FiveInARowController extends Controller {  
  
  val games: scala.collection.mutable.Map[String, FiveInARowGame] = scala.collection.mutable.Map.empty
  val waitQueue: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map.empty
  
  //message queue, user, JsValue
  val messageQueue = scala.collection.mutable.Map.empty[String, JsValue]

  
//  def game(gameId: String) = Action { implicit request =>
  def game(gameId: String) = Action { implicit request =>
    println("game -> " + gameId)
    if (request.session.get("user") == None ) Redirect(routes.LoginController.login)
    else {
      val user = request.session("user")
      //TODO Add redirect to error page if game not exist
      val game: FiveInARowGame = FiveInARowGame.getGame(gameId).get
      val playerSymbol = game.getPlayerSymbol(user)
      if (playerSymbol == "X") {
        messageQueue += ((user, Json.obj("type" -> "yourMove")))
      }
      Ok(views.html.game(game.size, request.session("user"), playerSymbol))
    }
  }
 
  /**
   * Client call to retrieve messages
   */
  def getMessages = Action { implicit request =>
    val user = request.session("user")
    println(s"game.getMessages -> : user: ${user}, messageQueue: $messageQueue")
    val message = messageQueue.remove(user)
    message match {
      case Some(jsValue) => returnJSON(jsValue)
      case _ => returnJSON(Json.obj("type"->"empty"))
    }     
  }
  
  def returnJSON(json: JsValue): Result = Ok(json).withHeaders(CACHE_CONTROL -> "max-age=0, no-store", EXPIRES -> new java.util.Date().toString)
  
 
  /**
   * JSON client message
   */
  def clientMessage() = Action { implicit request =>
    val user = request.session("user")
    val jsonMessage: Option[JsValue] = request.body.asJson
    println(s"game.clientMessage -> user: $user, json: $jsonMessage")
    jsonMessage match {
      case Some(value) => handleClientMessage(value, request.session("user"))
      case None => {
        println("clientMessage -> None")
        Ok("")
      }
    }
  }
  
  def handleClientMessage(jsValue: JsValue, user: String): Result = {
    println(s"game.handleClientMessage -> user: $user, ${Json.prettyPrint(jsValue)}")
    val gameId = (jsValue \ "gameId").as[String]
    val game = FiveInARowGame.getGame(gameId).get
    val mType = (jsValue \ "type").asOpt[String]
    mType match {
      case Some(sType) => {
        if (sType == "click") {
          handleClick(game, jsValue, user)
        }
//        else if (sType == "challengeAccepted") handleClientChallengeAccepted(jsValue, user)
      }
      case _ => {
        println(s"Unknown client message: ${Json.prettyPrint(jsValue)}")
        Ok("")
      } 
    }
    Ok("") 
  }
    
  def handleClick(game: FiveInARowGame, jsValue: JsValue, user: String) = {
      val moveResult = game.doClick(jsValue, user)
      val posString = (jsValue \ "position").as[String]    
      moveResult match {
	      case PlayerWon => {
	        val js = Json.obj(
	                "type" -> "gameOver",
	                "result" -> (user + " has won!!!"),
	                "prevMove" -> posString)
	        messageQueue += ((user, js))
	        messageQueue += ((game.otherPlayer(user), js))
	
	      }
	      case Draw => {
	        val js = Json.obj(
	                "type" -> "gameOver",
	                "result" -> "It is a draw!",
	                "prevMove" -> posString)
	        messageQueue += ((user, js))
	        messageQueue += ((game.otherPlayer(user), js))
	      }
	      case _ => {
	    	 val posString = (jsValue \ "position").as[String]    
	         val js = Json.obj(
	                "type" -> "yourMove",
	                "prevMove" -> posString)
	        messageQueue += ((game.otherPlayer(user), js))           
	      }
      }
    
  }
//  def startGame = Action { implicit request =>
//    println("startGame")
//    if (request.session.get("user") == None ) Redirect(routes.LoginController.login)
//    else {
//      //Unpack form to get opponent
//      //Start session with user name and opponent name
//  	  val params: Option[Map[String, Seq[String]]] = request.body.asFormUrlEncoded
//  	  println("body:" + request.body)
//  	  println("params:" + params.getOrElse("no param"))
//      val user = request.session("user")
//      val opponent: String = params.get("opponent")(0)
//      println(s"startGame: $user $opponent")
//      if (waitQueue.contains(opponent) && waitQueue(opponent) == user) {
//        println(s"startGame: second opponent joined: $games")
//      }
//      else {
//        //Have to wait for opponent to start
//        waitQueue += ((user, opponent))
//        val game = new FiveInARowGame(user, opponent)
//        games += ((user, game))
//        games += ((opponent, game))
//        println(s"startGame: new game created $games")
//      }
//      Ok(views.html.game(10, request.session("user")))
//    }
//  }
  
 
}