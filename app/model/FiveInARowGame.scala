package model

import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext.Implicits.global
//import scala.collection.immutable.Map

class FiveInARowGame(player1: String, player2: String) {
  println(s"new game, players: $player1 $player2")
  type Position = (Int, Int)
  
  val (out, channel) = Concurrent.broadcast[JsValue]
  
  val playerToSymbol: Map[String, String] = Map(player1 -> "X", player2 -> "O")
  var currentPlayer: String = player1
  val entries: ListBuffer[(String, Position)] = new ListBuffer
    
  def nextPlayer(): String = {
    if (currentPlayer == player1) currentPlayer = player2
    else currentPlayer = player1
    currentPlayer
  }
  
  def webSocketStarted(implicit request: RequestHeader): (Iteratee[JsValue, Unit], Enumerator[JsValue]) = {
    
    val player = request.session("user")
    val in: Iteratee[JsValue, Unit] = Iteratee.foreach[JsValue] { msg =>
        println(s"player: $player , msg: $msg")
        val typ = (msg \ "type").as[String]
        typ match {
          case "click" => if (player == currentPlayer) doClick(msg, player)
          case "quit" => channel.eofAndEnd()
        }
/*        if ("quit" == msg) {
          channel.eofAndEnd()
        }
        else {
        	//the Enumerator returned by Concurrent.broadcast subscribes to the channel and will 
        	//receive the pushed messages
        	channel push ("RESPONSE: " + msg)
        }
*/  }
    val t = new java.util.Timer
    t.schedule(new java.util.TimerTask() {
      def run() {
	    if (player == player1) {
	      channel.push(jsonResponseWaiting)
	    } 
	    else {
//	      channel.push(jsonResponseJoin)
	      startGame
	    }        
      }
    }, 10)
    
    (in, out)
  }
  
  def startGame(implicit request: RequestHeader) = {
     channel.push(jsonResponseNextPlayer(currentPlayer))  
  }
  
  def toPos(pos: String): Position = {
//    row-col-@row-@column
    val arr = pos.split("-")
    val row = arr(2).toInt
    val col = arr(3).toInt
    (row, col)
  }
  
  def doClick(msg: JsValue, player: String) = {
    val posString = (msg \ "position").as[String]    
    val pos: Position = toPos(posString)
    println(s"doClick player: $player , pos: $posString")
    if (entries.exists(e => e._2 == pos)) {
      channel.push(jsonResponseInvalidPosition(player))
    }
    else {
	    entries += ((player, pos))
	    channel.push(jsonResponseEntry(posString, player))
	    
	    //check score and send FINISH if game over
	    val (gameOver, resultString) = checkGameScore(player)
	    
	    println("gameOver: " + gameOver)
	    if (gameOver) {
	 	    channel.push(jsonResponseGameOver(resultString))     
	    }
	    else {
		    channel.push(jsonResponseNextPlayer(nextPlayer))
	    }  
    }
  }
  
  def checkGameScore(player: String): (Boolean, String) = {
    //TODO code for diagonals
    val playerEntries: List[Position] = entries.filter(e => e._1 == player).map(e => e._2).toList
    var gameOver = false
    for (row <- 1 to 10) {
      val thisRowEntries = playerEntries.filter(e => e._1 == row).map(e => e._2).sorted
      println(s"thisRowEntries: $thisRowEntries")
      if (has5InARow(thisRowEntries)) {
        gameOver = true
      }
    }
    for (col <- 1 to 10) {
      val thisColEntries = playerEntries.filter(e => e._2 == col).map(e => e._1)
      if (has5InARow(thisColEntries.sorted)) {
        gameOver = true
      }
    }
    
    (gameOver, if (gameOver) "Player " + player + " has won!!!!" else "")
  }
  
  def has5InARow(list: List[Int], prev: Int = 0, count: Int = 0): Boolean = {
    if (count == 5) true
    else if (list.isEmpty) false
    else if (prev == 0) has5InARow(list.tail, list.head, count + 1)
    else {
      if (list.head == prev + 1) has5InARow(list.tail, list.head, count + 1)
      else has5InARow(list.tail, list.head, 0)
    }
  }
  
  //************* JSON repsonses *****************
    def jsonResponseEntry(pos: String, player: String) = toJson(
     scala.collection.immutable.Map(
        "type" -> toJson("entry"),
        "position" -> toJson(pos),
        "symbol" -> toJson(playerToSymbol(player))
      )
    )             

    def jsonResponseWaiting = toJson(
      scala.collection.immutable.Map[String, JsValue](
        "type" -> toJson("waiting")
      )
    )  
    
    val jsonResponseJoin = toJson(
      scala.collection.immutable.Map[String, JsValue](
        "type" -> toJson("join")
      )
    )    

    def jsonResponseNextPlayer(player: String) = {
      val symbol = playerToSymbol(player)
      toJson( 
        scala.collection.immutable.Map[String, JsValue](
          "type" -> toJson("nextPlayer"),
          "player" -> toJson(player),
          "symbol" -> toJson(symbol)
       )
     )        
    }

    def jsonResponseInvalidPosition(player: String) = {
      val symbol = playerToSymbol(player)
      toJson( 
        scala.collection.immutable.Map[String, JsValue](
          "type" -> toJson("invalidPosition"),
          "player" -> toJson(player),
          "symbol" -> toJson(symbol)
       )
     )        
    }

    def jsonResponseGameOver(resultString: String) = toJson(
      scala.collection.immutable.Map[String, JsValue](
        "type" -> toJson("gameOver"),
        "result" -> toJson(resultString)
      )
    )    
      

  
}