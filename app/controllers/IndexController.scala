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

object IndexController extends Controller {

  val userForm = Form(
	mapping(
      "name" -> nonEmptyText,
      "group" -> optional(text)
    )(User.apply)(User.unapply)verifying("User is not unique. Please try another name.", user => !Users.exists(user))
  )
  
  /**
   * This is the index page where you end up before you log in and after you log out.
   */
  def index = Action { implicit request =>
    val user = request.session.get("user")
    println(s"IndexController.index -> user: $user")
    user match {
      case None => Ok(views.html.index(userForm))
      case Some(uName) => {
        //Check that hte user also exist in the DB
        if (Users.isLoggedIn(uName)) Redirect(routes.Application.home)
        else {
          Users.logout(uName)
          println(s"IndexController.index: logged out user: $uName and refirecting to index")
          Ok(views.html.index(userForm))
        }
      }
    }
  }
  
//  def login = Action { implicit request =>
//    request.session.get("user") match {
//      case Some(user) => Redirect(routes.Application.home())
//      case _ => Ok(views.html.login(userForm))
//    }
//  }
  
  def doLogin = Action {implicit request =>    
    userForm.bindFromRequest.fold(
        formWithErrors => {
          println("Error in login form: " + formWithErrors)
          BadRequest(views.html.index(formWithErrors))
        },
    	user => {
          Users.logon(user);
          println("IndexController.doLogin: User logged in successfully: " + user)
    	  Redirect(routes.Application.home()).withSession(request.session + ("user" -> user.name))
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
  
  def doLogout = Action { implicit request =>
    println("IndexController.doLogout ->")
    request.session.get("user").foreach(s => {
      println(s"IndexController.doLogout: logging out $s")
      Users.logout(s)
    })
    Redirect(routes.IndexController.loggedOut).withNewSession
  }
  
  def loggedOut = Action { implicit request =>
    println("IndexController.loggedOut ->")
    Ok(views.html.loggedout())
  }
  

}