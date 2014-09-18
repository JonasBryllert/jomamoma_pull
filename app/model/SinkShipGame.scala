package model

import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.json.Json._
import scala.collection._
import play.api.mvc.RequestHeader
import scala.concurrent.ExecutionContext.Implicits.global
import Ship.Position

object SinkShipGame {
  
  sealed trait BombResult
  object Miss extends BombResult
  object Hit extends BombResult
  case class ShipSunk(ship: Ship) extends BombResult
  case class AllShipsSunk(ships: List[Ship]) extends BombResult

  var gameIdCounter = 0
  val gameMap: mutable.Map[String, SinkShipGame] = mutable.Map.empty
  val SHIP_LENGTHS = List(2,2,3,3,5);
  
  def newGame(size: Int, player1: String, player2: String): String = {
    gameIdCounter += 1
    val gameId = gameIdCounter.toString
    gameMap += ((gameId, new SinkShipGame(size, player1, player2)))
    gameId
  }  
  
  def getGame(gameId: String): Option[SinkShipGame] = gameMap.get(gameId)
  
  def endGame(gameId: String) = gameMap.remove(gameId)
  
}

class SinkShipGame(val gameSize: Int, player1: String, player2: String) {
  import SinkShipGame._
  println(s"SinkShipGame.new, size: $gameSize, players: $player1 $player2")
  
  var currentPlayer: String = player1
  val shipsMap: Map[String, List[Ship]] = Map(player1 -> generateShipPositions(), player2 -> generateShipPositions());
  val hitsMap: Map[String, mutable.ListBuffer[Position]] = Map(player1 -> mutable.ListBuffer.empty, player2 -> mutable.ListBuffer.empty);
  
  //stores message per player
  val playerToMessageQueue = Map(player1 -> new mutable.ListBuffer[JsValue], player2 -> new mutable.ListBuffer[JsValue])
      
  def otherPlayer(player: String): String = if (player == player1) player2 else player1
  
  def nextPlayer(): String = {
    if (currentPlayer == player1) currentPlayer = player2
    else currentPlayer = player1
    currentPlayer
  }  
  
  def getShips(user: String): List[Ship] = shipsMap(user)
    
  def doClick(pos: Position, player: String): BombResult = {
	hitsMap(player) += pos
    println(s"SinkShipGame.doClick player: $player , pos: $pos, hits size: ${hitsMap(player).size}")
	    
    //check score and send FINISH if game over
    moveResult(player, pos)
  }
  
  def moveResult(player: String, pos: Position): BombResult = {
    val ships = shipsMap(otherPlayer(player))
    val hitShip = ships.filter(s => s.isHit(pos)).headOption
    hitShip match {
      case None => Miss
      case Some(ship) => {
        if (ship.isSunk(hitsMap(player).toList)) {
          if (ships.forall(s => s.isSunk(hitsMap(player).toList))) AllShipsSunk(ships)
          else ShipSunk(ship)
        }
        else Hit
      }
    }    
  }
    
  private def generateShipPositions(): List[Ship] = {
    import mutable.ListBuffer
    
    val ships: ListBuffer[Ship] = ListBuffer.empty
    
    for (i <- 0 until SHIP_LENGTHS.length) {           
      var ship: Ship = null;
      do {
        val direction = Math.floor(Math.random() * 2).toInt; //0 = horizontal, 1 = vertical
        val startingPointX = 
          if (direction == 0) Math.floor(Math.random() * (gameSize  - SHIP_LENGTHS(i)) + 1).toInt //1 to 8 minus ship length
          else Math.floor(Math.random() * gameSize + 1).toInt //; //1 to 8
      println("startPosX: " + startingPointX)
        val startingPointY = 
          if (direction == 1) Math.floor(Math.random() * (gameSize  - SHIP_LENGTHS(i)) + 1).toInt //1 to 8 minus ship length
          else Math.floor(Math.random() * gameSize + 1).toInt //; //1 to 8
      println("startPosY: " + startingPointY)

        val shipPositions = 
        	if (direction == 0) (startingPointX until (startingPointX + SHIP_LENGTHS(i))).toList.map(x => (x, startingPointY))
            else (startingPointY until (startingPointY + SHIP_LENGTHS(i))).toList.map(y => (startingPointX, y))
      println("shipPos: " + shipPositions)

      println("ships.length: " + ships.length)
        //Check no collision
        var collision = false;
        for (s <- ships; p <- shipPositions) {
          if (s.isHit(p)) {
            println("s, p: " + s + " " + p)
            collision = true;
          }
        }
    
        if (!collision) {
          //create and sort the ship positions
          ship = Ship(shipPositions)
//          ship = Ship(shipPositions.sortWith(((p1), (p2)) => {
//            if (p1._1 < p2._1) true
//            else if (p1._1 > p2._1) false
//            else if (p1._2 < p2._2) true
//            else if (p1._2 > p2._2) false
//            true
//          }));
        }
    
      }
      while (ship == null);
 
      ships += ship;
 
    }
    ships.toList
  }
  
}