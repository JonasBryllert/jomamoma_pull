package model

import akka.actor.Actor
import play.api.Logger
import scala.collection.mutable.ListBuffer

object OthelloGame {
  type Position = (Int, Int) //row, column (row start from 0 at top to size-1 at bottom)

  case class GetGameInfo(player: String)
  case class GameInfo(isGameOver: Boolean, size: Int, isCurrentPlayer: Boolean, color: String, otherPlayer: String)
  case class CurrentPlayer()
  case class Move(player: String, position: Option[Position])
  
  sealed trait Result
  case class NextPlayer(nextPlayer: String, prevColor: String, changes: List[Position]) extends Result
  case class InvalidMove(position: Position) extends Result
  case class GameOver(nextPlayer: String, prevColor: String, changes: List[Position], winner: Option[String]) extends Result
}

class OthelloGame(gameId: String, size: Int, challenger: String, challengee: String) extends Actor {
  import OthelloGame._
  
  var gameOver = false;
  var lastMoveIsPass = false
  //  val entries: ListBuffer[(Position, String)] = new ListBuffer  //player, position
  val entries = Array.ofDim[String](size, size)
  //Set start entries
  setInitialEntries(entries, size)

  var currentPlayer: String = challenger
  
  private def setInitialEntries(entries: Array[Array[String]], size: Int) = {
    val low = size / 2 - 1
    val high = size / 2
    entries(low)(low) = "red"
    entries(high)(high) = "red"
    entries(low)(high) = "blue"
    entries(high)(low) = "blue"
  }

  def receive: Receive = {
    case GetGameInfo(player) => {
      val otherPlayer = getOtherPlayer(player)
      val color = getColor(player)
      Logger.info(s"OthelloGame.OtherPlayer, id: $gameId, player: $player, otherPlayer: $otherPlayer")
      sender ! GameInfo(gameOver, size, player == currentPlayer, color, otherPlayer)
    }
    case CurrentPlayer => {
      Logger.info(s"OthelloGame.CurrentPlayer, id: $gameId, currentPlayer: $currentPlayer")
      sender ! currentPlayer
    }
    case Move(player, positionOption) => {
      Logger.info(s"OthelloGame.Move, id: $gameId, player: $player, position: $positionOption")
      if (positionOption.isDefined) {
        lastMoveIsPass = false
        val position = positionOption.get
        val changes:List[Position] = toggleEntries(entries, player, position)
        if (changes.isEmpty) {
          sender ! InvalidMove(position)
        }
        else {
          if (isGameOver(entries)) {
            gameOver = true
            val winner: Option[String] = calculateWinner(entries)
            sender ! GameOver(getOtherPlayer(player), getColor(player), changes, winner)
          }
          else {
            sender ! NextPlayer(getOtherPlayer(player), getColor(player), changes)
          }
        }
      }
      else {
        Logger.info(s"OthelloGame.Move, id: $gameId, pass, lastMoveIsPass: $lastMoveIsPass")
        //two passes means game over
        if (lastMoveIsPass) {
          gameOver = true
          val winner: Option[String] = calculateWinner(entries)
          sender ! GameOver(getOtherPlayer(player), getColor(player), List.empty, winner)
          
        }
        else {
          lastMoveIsPass = true
          sender ! NextPlayer(getOtherPlayer(player), getColor(player), List.empty)          
        }
      }
 
    }
  }
  
  private def getOtherPlayer(player: String): String = {
    if (player == challenger) challengee else challenger 
  }
  
  private def getColor(player: String): String = {
    if (player == challenger) "red" else "blue"
  }
  
  private def colorToPlayer(color: String): String = {
    if (color == "red") challenger
    else challengee
  }

  private def isGameOver(entries: Array[Array[String]]): Boolean = {
    val flatEntries = entries.flatMap { x => x }
    Logger.info(s"flatEntries: ${flatEntries.mkString}")
    if (!flatEntries.exists { x => x == null }) return true
    if (!flatEntries.exists { x => x == "red" }) return true
    if (!flatEntries.exists { x => x == "blue" }) return true
    false
  }
  
  private def swapColor(entries: Array[Array[String]], list: List[Position], playerColor: String, pos: Position, next: (Position) => Position): List[Position] = {
    val (row, col) = next(pos)
    if (row < 0 || col < 0 || row >= size || col >= size) {
      return List.empty
    }
    else {
      val posColor = entries(row)(col)
//      Logger.info(s"Entries($row, $col) = $posColor")
      if (posColor == null) return List.empty
      else if (posColor == playerColor) return list
      else {
        swapColor(entries, ((row, col)) :: list, playerColor, (row, col), next)
      }
    }
  }

  private def toggleEntries(entries: Array[Array[String]], player: String, position: Position): List[Position] = {
    val result = ListBuffer[Position]()
    val playerColor = getColor(player)
    
    //straight
    result ++= swapColor(entries, List(), playerColor, position, {case(row, col) => (row - 1, col)})
    result ++= swapColor(entries, List(), playerColor, position, {case(row, col) => (row + 1, col)})
    result ++= swapColor(entries, List(), playerColor, position, {case(row, col) => (row, col - 1)})
    result ++= swapColor(entries, List(), playerColor, position, {case(row, col) => (row, col + 1)})

    //diagonal
    result ++= swapColor(entries, List(), playerColor, position, {case(row, col) => (row - 1, col - 1)})
    result ++= swapColor(entries, List(), playerColor, position, {case(row, col) => (row + 1, col + 1)})
    result ++= swapColor(entries, List(), playerColor, position, {case(row, col) => (row - 1, col + 1)})
    result ++= swapColor(entries, List(), playerColor, position, {case(row, col) => (row + 1, col - 1)})
    
    //Only add play position if not empty...otherwise it is an invalid move!!
    if (!result.isEmpty) {
      result += (position)
      result.foreach { p => entries(p._1)(p._2) = playerColor }
    }
    result.toList
  }

  def calculateWinner(entries: Array[Array[String]]): Option[String] = {
    val flatEntries = entries.flatMap { x => x }
    val nrRed = flatEntries.filter { x => x == "red" }.size
    val nrBlue= flatEntries.filter { x => x == "blue" }.size
    if (nrRed > nrBlue) return Some(colorToPlayer("red"))
    if (nrRed < nrBlue) return Some(colorToPlayer("blue"))
    None        
  }

}