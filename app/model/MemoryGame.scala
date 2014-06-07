package model

//import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext.Implicits.global
import controllers.Assets
//import scala.collection.immutable.Map

object MemoryGame {
  
  case class ImageWithId(id: String, image: String)

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
  type Position = String
  
  var currentPlayer: String = player1
  val score = Map(player1 -> 0, player2 -> 0)
  
  //stores message
  val playerToMessageQueue = Map(player1 -> new ListBuffer[JsValue], player2 -> new ListBuffer[JsValue])
  
//  val imageWithIdArray = for(i <- 1 to game.shuffledImages.length) yield new ImageWithId("pos-" + i, game.shuffledImages(i-1));
//  val shuffledImageWithIds: Array[ImageWithId] = scala.util.Random.shuffle(1 to size).map(i => "images/memory/pic" + i + ".jpg").toArray
  val shuffledImageWithIds: Array[ImageWithId] = {
    val shuffledImages: Array[String] = scala.util.Random.shuffle(1 to size).map(i => "images/memory/pic" + i + ".jpg").toArray
    val shuffledImageWithIds = for(i <- 1 to shuffledImages.length) yield new ImageWithId("pos-" + i, shuffledImages(i-1));
    shuffledImageWithIds.toArray
//    scala.util.Random.shuffle(1 to size).map(i => new ImageWithId("pos-" + count++, "images/memory/pic" + i + ".jpg")).toArray
  }
    
  def getOtherPlayer(player: String): String = if (player == player1) player2 else player1
  
  def nextPlayer(): String = {
    if (currentPlayer == player1) currentPlayer = player2
    else currentPlayer = player1
    currentPlayer
  }  
    
  def firstCellSelected(msg: JsValue, player: String): MoveResult = {
    val position = (msg \ "position").as[String]    
	entries += ((player, position))
    println(s"MemoryGame.firstCellSelected player: $player , pos: $position, entris size: ${entries.size}")
	    
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