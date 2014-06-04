package controllers

//import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
//import play.api.libs.iteratee._
//import scala.concurrent.ExecutionContext.Implicits.global
//import model.XandOGame
import model.Users
import model.User
//import com.fasterxml.jackson.databind.JsonNode
//import play.api.libs.json._
//import play.api.libs.json.Json._
import play.api.data.Forms

object LoginController extends Controller {

  val userForm = Form(
	mapping(
      "name" -> nonEmptyText,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)verifying(user => Users.exists(user))
  )
  
  def login = Action { implicit request =>
    request.session.get("user") match {
      case Some(user) => Redirect(routes.Application.home())
      case _ => Ok(views.html.login(userForm))
    }
  }
  
  def doLogin = Action {implicit request =>    
    userForm.bindFromRequest.fold(
        formWithErrors => {
          println("Error in login form: " + formWithErrors)
          BadRequest(views.html.login(formWithErrors))
        },
    	user => {
          println("User logged in successfully: " + user)
    	  Redirect(routes.Application.home()).withSession(session + ("user" -> user.name))
    	}
    )
//  	val params: Option[Map[String, Seq[String]]] = request.body.asFormUrlEncoded
//  	println("body:" + request.body)
//  	println("params:" + params.getOrElse("no param"))
//  	val userList: Seq[String] = params.flatMap(map => map.get("user")).getOrElse(List())
//  	val user: String = userList match {
//  	  case List(x) => x
//  	  case _ => ""
//  	}
//  	println(s"user: $user")
////  	request.session = request.session + ("user" -> user)
////  	println(s"session: $session")
//  	Redirect(routes.Application.home()).withSession(session + ("user" -> user))
  }
  
  def logout = Action { implicit request =>
    println("Logout!!")
    Redirect(routes.Application.index).withNewSession
  }
  

}