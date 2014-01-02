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
//        waitQueue -= opponent
//	    val game = new FiveInARowGame(opponent, user)
//        games += ((user, game))
//        games += ((opponent, game))
//        
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
//      val game = new FiveInARowGame()
//      games += ((user, game))
//      game.addPlayer(user)
      Ok(views.html.game(10, request.session("user")))
    }
  }
  
//  def joinGame(player1: String) = Action { implicit request => 
//    val user = request.session("user")
//    val game = games(player1)
//    games += ((request.session("user"), game))
//    game.addPlayer(user)
//    Ok(views.html.game(10, request.session("user")))
//  } 
  
  def wsgame: WebSocket[JsValue] = WebSocket.using[JsValue] { implicit request => 
    println(s"wsGame: user: ${request.session("user")}")
    println("games:" + games)
  	val game = games(request.session("user"))
  	println()
  	game.webSocketStarted
  }
//  	//Get session by user name.
//    //store channel in session
//    
//    //Concurernt.broadcast returns (Enumerator, Concurrent.Channel)
//    val (out: Enumerator[JsValue], channel) = Concurrent.broadcast[JsValue]
//
//    //log the message to stdout and send response back to client
//    val in: Iteratee[JsValue, Unit] = Iteratee.foreach[JsValue] {
//      msg =>
//        println(msg)
//        val typ = (msg \ "type").as[String]
//        typ match {
//          case "click" => {
//            val pos = (msg \ "position").as[String]
//            val jsonResponse = toJson(
//              Map(
//                "type" -> toJson("entry"),
//                "position" -> toJson(pos)
//              ))
//                      
//            channel.push(jsonResponse)
//          }
//          case "quit" => channel.eofAndEnd()
//        }
///*        if ("quit" == msg) {
//          channel.eofAndEnd()
//        }
//        else {
//        	//the Enumerator returned by Concurrent.broadcast subscribes to the channel and will 
//        	//receive the pushed messages
//        	channel push ("RESPONSE: " + msg)
//        }
//*/    }
//    (in, out)
//  }

}