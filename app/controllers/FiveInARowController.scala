package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global
import model.FiveInARowGame
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._
import java.util.Timer

object FiveInARowController extends Controller {  
  
  val games: scala.collection.mutable.Map[String, FiveInARowGame] = scala.collection.mutable.Map.empty
  val waitQueue: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map.empty

  def startGame = Action { implicit request =>
    println("startGame")
    if (request.session.get("user") == None ) Redirect(routes.LoginController.login)
    else {
      //Unpack form to get opponent
      //Start session with user name and opponent name
  	  val params: Option[Map[String, Seq[String]]] = request.body.asFormUrlEncoded
  	  println("body:" + request.body)
  	  println("params:" + params.getOrElse("no param"))
      val user = request.session("user")
      val opponent: String = params.get("opponent")(0)
      println(s"startGame: $user $opponent")
      if (waitQueue.contains(opponent) && waitQueue(opponent) == user) {
        println(s"startGame: second opponent joined: $games")
      }
      else {
        //Have to wait for opponent to start
        waitQueue += ((user, opponent))
        val game = new FiveInARowGame(user, opponent)
        games += ((user, game))
        games += ((opponent, game))
        println(s"startGame: new game created $games")
      }
      Ok(views.html.game(10, request.session("user")))
    }
  }
  
  
  def wsgame: WebSocket[JsValue] = WebSocket.using[JsValue] { implicit request => 
    println(s"wsGame: user: ${request.session("user")}")
    println("games:" + games)
  	val game = games(request.session("user"))
  	println()
  	game.webSocketStarted
  }
  
}