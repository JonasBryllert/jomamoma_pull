package controllers

//import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import scala.concurrent.ExecutionContext.Implicits.global
import model.FiveInARowGame
import model.Users
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import play.api.libs.json.Json._

object Application extends Controller {
  
  val waitList = scala.collection.mutable.Map.empty[String, String]

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def index2 = Action { implicit request =>
    val user = request.session.get("user")
    println(s"index2 -> user: $user")
    if (user == None ) Redirect(routes.LoginController.login)
    else Ok(views.html.index2(user.get))
  }
  
  def loadUsers = Action { implicit request =>
    val user = request.session("user")
    println(s"entering loadUsers: user: ${user}")
    val userNames: List[String] = Users.users.map(user => user.name).filter(u => u != user)
    println(s"exiting loadUsers: ${userNames}")
    println(s"exiting loadUsers: ${toJson(userNames)}")
    Ok(toJson(userNames))
  }
  
//  def fiveInARowGame = Action {
//    Ok(views.html.game(FiveInARowGame.size))
//  }
//
  def ws = WebSocket.using[String] { request =>

    // Send a single 'Hello!' message
    val out = Enumerator("Hello!").andThen(Enumerator(" and hello")).andThen(Enumerator.eof)
 //   val out = Enumerator.repeat(e)

    // Log events to the console
    val in = Iteratee.foreach[String]{ msg =>
      println(msg)
      out.andThen(Enumerator("test"))
    }

    (in, out)
  }

  def ws2 = WebSocket.using[String] { request =>

    //Concurernt.broadcast returns (Enumerator, Concurrent.Channel)
    val (out, channel) = Concurrent.broadcast[String]

    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[String] {
      msg =>
        println(msg)
        if ("quit" == msg) {
          channel.eofAndEnd()
        }
        else {
        	//the Enumerator returned by Concurrent.broadcast subscribes to the channel and will 
        	//receive the pushed messages
        	channel push ("RESPONSE: " + msg)
        }
    }
    (in, out)
  }

}