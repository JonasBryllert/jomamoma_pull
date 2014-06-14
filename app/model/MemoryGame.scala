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
  
//  case class ImageWithId(id: String, image: String)
  
  class Score(val player1: String, val player2: String) {
    private var player1Score = 0
    private var player2Score = 0
    def increment(player: String) {
      if (player.equals(player1)) player1Score += 1
      else player2Score += 1
    }
    def getScore(player: String): Int = {
      if (player.equals(player1)) player1Score
      else player2Score
    }
    def getTotalScore(): Int = player1Score + player2Score
    override def toString(): String = {
      ""  + player1 + ": " + player1Score + ", " + player2 +  ": " + player2Score
    }
  }

  sealed trait MoveResult
  case class PlayerWon(winner: String) extends MoveResult 
  object Draw extends MoveResult
  case class NextMove(isScore: Boolean) extends MoveResult

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
  private val score = new Score(player1,player2)
  
  //The map with <id, images>
  val shuffledIdImageMap: scala.collection.immutable.Map[String, String] = {
    val images = for (i <- 1 to size /2) yield "/assets/images/memory/pic" + i + ".jpg"
    val duplicatedImages = images ++ images
    val shuffledImages: Seq[String] = scala.util.Random.shuffle(duplicatedImages)
    val shuffledImageWithIds: Seq[(String, String)] = for(i <- 1 to shuffledImages.length) yield (("pos-" + i, shuffledImages(i-1)));
    shuffledImageWithIds.toMap
  }
    
  def getOtherPlayer(player: String): String = if (player == player1) player2 else player1
  
  private def nextPlayer(): String = {
    if (currentPlayer == player1) currentPlayer = player2
    else currentPlayer = player1
    currentPlayer
  }  
    
  def secondCellSelected(player: String, firstCell: String, secondCell: String): (Score, MoveResult) = {
    val isScore = shuffledIdImageMap(firstCell).equals(shuffledIdImageMap(secondCell))
    if (isScore) score.increment(player)
    
    val moveResult: MoveResult = {
      if (score.getTotalScore() >= size/2) {
        //Game over, calculate winner
        if (score.getScore(player1) == score.getScore(player2)) {
          Draw
        }
        else {
          val winner =  {
            if (score.getScore(player1) > score.getScore(player2)) player1
            else player2
          }
          new PlayerWon(winner)
        }
      } else {
        new NextMove(isScore)
      }
    }
    println(s"MemoryGame.secondCellSelected player: $player , firstCell: $firstCell, secondCell: $secondCell, score: $score")
	(score, moveResult)  
  }
  
}