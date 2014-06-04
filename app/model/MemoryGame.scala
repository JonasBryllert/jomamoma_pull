package model

import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext.Implicits.global
import controllers.Assets
//import scala.collection.immutable.Map

object MemoryGame {
  
  sealed trait MoveResult
  object PlayerWon extends MoveResult
  object Draw extends MoveResult
  object NextMove extends MoveResult

  var gameIdCounter = 0
  val gameMap: Map[String, MemoryGame] = Map.empty
  
  def newGame(size: Int, player1: String, player2: String): String = {
    gameIdCounter += 1
    val gameId = gameIdCounter.toString
    gameMap += ((gameId, new MemoryGame(size, player1, player2)))
    gameId
  }  
  
  def getGame(gameId: String): Option[MemoryGame] = gameMap.get(gameId)
  
  def endGame(gameId: String) = gameMap.remove(gameId)
  
}

class MemoryGame(val size: Int, val player1: String, val player2: String) {
  import MemoryGame._
  println(s"new game, size: $size, players: $player1 $player2")
  type Position = (Int, Int)
  
  var currentPlayer: String = player1
  val entries: ListBuffer[(String, Position)] = new ListBuffer
  
  //stores message
  val playerToMessageQueue = Map(player1 -> new ListBuffer[JsValue], player2 -> new ListBuffer[JsValue])
  
  val shuffledImages: Array[String] = scala.util.Random.shuffle(1 to size).map(i => "images/memory/pic" + i + ".jpg").toArray
    
  def getOtherPlayer(player: String): String = if (player == player1) player2 else player1
  
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
  
  def firstCellSelected(msg: JsValue, player: String): MoveResult = {
    val posString = (msg \ "position").as[String]    
    val pos: Position = toPos(posString)
	entries += ((player, pos))
    println(s"doClick player: $player , pos: $posString, entris size: ${entries.size}")
	    
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
      println("Player won!!!")      
      PlayerWon
    }
    else if (entries.size == (size * size)) {
      println("Draw!!!")
      Draw
    }
    else NextMove
  }
  
  def hasWon(list: List[Int], prev: Int = 0, count: Int = 0): Boolean = {
    if (count == 0) true
    else if (list.isEmpty) false
    else if (prev == 0) hasWon(list.tail, list.head, count + 1)
    else {
      if (list.head == prev + 1) hasWon(list.tail, list.head, count + 1)
      else hasWon(list.tail, list.head, 0)
    }
  }
   
}