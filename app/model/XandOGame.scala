package model

import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext.Implicits.global
//import scala.collection.immutable.Map

object XandOGame {
  
  sealed trait MoveResult
  object PlayerWon extends MoveResult
  object Draw extends MoveResult
  object NextMove extends MoveResult

  var gameIdCounter = 0
  val gameMap: Map[String, XandOGame] = Map.empty
  
  def newGame(size: Int, nrToWin: Int, player1: String, player2: String): String = {
    gameIdCounter += 1
    val gameId = gameIdCounter.toString
    gameMap += ((gameId, new XandOGame(size, nrToWin, player1, player2)))
    gameId
  }  
  
  def getGame(gameId: String): Option[XandOGame] = gameMap.get(gameId)
  
  def endGame(gameId: String) = gameMap.remove(gameId)
  
}

class XandOGame(val size: Int, val nrToWin: Int, player1: String, player2: String) {
  import XandOGame._
  println(s"XandOGame.new, size: $size, nrToWin: $nrToWin, players: $player1 $player2")
  type Position = (Int, Int)
  
  val playerToSymbol: Map[String, String] = Map(player1 -> "X", player2 -> "O")
  var currentPlayer: String = player1
  val entries: ListBuffer[(String, Position)] = new ListBuffer
  
  //stores message
  val playerToMessageQueue = Map(player1 -> new ListBuffer[JsValue], player2 -> new ListBuffer[JsValue])
    
  def getPlayerSymbol(player: String): String = playerToSymbol(player)
  
  def otherPlayer(player: String): String = if (player == player1) player2 else player1
  
  def nextPlayer(): String = {
    if (currentPlayer == player1) currentPlayer = player2
    else currentPlayer = player1
    currentPlayer
  }  
    
  def toPos(pos: String): Position = {
//    row-col-@row-@column
    val arr = pos.split("-")
    val row = arr(2).toInt
    val col = arr(3).toInt
    (row, col)
  }
  
  def doClick(msg: JsValue, player: String): MoveResult = {
    val posString = (msg \ "position").as[String]    
    val pos: Position = toPos(posString)
	entries += ((player, pos))
    println(s"XandOGame.doClick player: $player , pos: $posString, entris size: ${entries.size}")
	    
    //check score and send FINISH if game over
    moveResult(player, pos)
  }
  
  def moveResult(player: String, pos: Position): MoveResult = {
    //TODO code for diagonals 
    val playerEntries: List[Position] = entries.filter(e => e._1 == player).map(e => e._2).toList
    val rowEntries = playerEntries.filter(e => e._1 == pos._1).map(e => e._2).sorted
    val colEntries = playerEntries.filter(e => e._2 == pos._2).map(e => e._1).sorted
    val diag1Entries = playerEntries.filter(e => e._1 - e._2 == pos._1 - pos._2).map(e => e._1).sorted
    val diag2Entries = playerEntries.filter(e => e._1 + e._2 == pos._1 + pos._2).map(e => e._1).sorted
    
    val playerHasWon = hasWon(rowEntries) || hasWon(colEntries) || hasWon(diag1Entries) || hasWon(diag2Entries)
    println()
    if (playerHasWon) {
      println("XandOGame Player won!!!")      
      PlayerWon
    }
    else if (entries.size == (size * size)) {
      println("XandOGame Draw!!!")
      Draw
    }
    else NextMove
  }
  
  def hasWon(list: List[Int], prev: Int = 0, count: Int = 0): Boolean = {
    if (count == nrToWin) true
    else if (list.isEmpty) false
    else if (prev == 0) hasWon(list.tail, list.head, count + 1)
    else {
      if (list.head == prev + 1) hasWon(list.tail, list.head, count + 1)
      else hasWon(list.tail, list.head, 0)
    }
  }
   
}