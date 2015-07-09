package model

import akka.actor.Actor
import play.api.Logger
import scala.collection.mutable.ListBuffer

object FourInARowGame {
  type Position = (Int, Int) //row, column (row start from 1 at top to 6 at bottom)

  case class OtherPlayer(player: String)
  case class CurrentPlayer()
  case class Move(player: String, column: Int)
  
  sealed trait Result
  case class NextPlayer(player: String, row: Int, col: Int) extends Result
  case class GameOver(player: String, row: Int, col: Int, winner: String) extends Result
}

class FourInARowGame(gameId: String, challenger: String, challengee: String) extends Actor {
  import FourInARowGame._
  
  val entries: ListBuffer[(Position, String)] = new ListBuffer  //player, position

  var currentPlayer: String = challenger
  
  def receive: Receive = {
    case OtherPlayer(player) => {
      val otherPlayer = getOtherPlayer(player)
      val color = if (player == challenger) "red" else "blue"
      Logger.info(s"FourInARowGame.OtherPlayer, id: $gameId, player: $player, otherPlayer: $otherPlayer")
      sender ! (otherPlayer, color)
    }
    case CurrentPlayer => {
      Logger.info(s"FourInARowGame.CurrentPlayer, id: $gameId, currentPlayer: $currentPlayer")
      sender ! currentPlayer
    }
    case Move(player, column) => {
      Logger.info(s"FourInARowGame.Move, id: $gameId, column: $column")
      val row = entries.map(e => e._1).filter(p => p._2 == column).foldLeft(6)((c, s) => if (s._1 <= c ) s._1 - 1 else c)
      entries += (((row, column), player))
      if (isGameOver(player, row, column, entries)) {
        sender ! GameOver(getOtherPlayer(player), row, column, player)
      }
      else {
        sender ! NextPlayer(getOtherPlayer(player), row, column)
      }
    }
  }
  
  private def getOtherPlayer(player: String): String = {
    if (player == challenger) challengee else challenger 
  }

  private def isGameOver(player: String, row: Int, column: Int, entries: ListBuffer[(Position, String)]): Boolean = {
    val playerPositions: ListBuffer[Position] = entries.filter(e => e._2 == player).map(pe => pe._1)
    val rowEntries = playerPositions.filter(pp => pp._2 == column).map(pp => pp._1).sorted.toList 
    val colEntries = playerPositions.filter(pp => pp._1 == row).map(pp => pp._2).sorted.toList 
    val diag1Entries = playerPositions.filter(pp => pp._1 - pp._2 == row - column).map(pp => pp._2).sorted.toList 
    val diag2Entries = playerPositions.filter(pp => pp._1 + pp._2 == row + column).map(pp => pp._2).sorted.toList
    return countItems(rowEntries) || countItems(colEntries) || countItems(diag1Entries) || countItems(diag2Entries)  
  }
  
  private def countItems(list: List[Int], prevNr: Int = -1, count: Int = 0): Boolean = {
    if (count >= 4) true
    else if ((list.size + count) < 4) false
    else {
      list match {
        case List() => false
        case x :: xs => {
          val newCount = if (x == prevNr + 1) count + 1 else 1
           countItems(xs, x, newCount) 
        }
      }
    }
  }
  
}