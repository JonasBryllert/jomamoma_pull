package model

object Ship {
  type Position = (Int, Int)
  def apply(location: Set[Position]): Ship = {
    return new Ship(location)
  }
}

import Ship.Position
class Ship(location: Set[Position]) {
    def isHit(pos: Position): Boolean = {
    	if (location.contains(pos)) true
    	else false
    }
    val length = location.size
    def isSunk(positions: Set[Position]): Boolean = {
      location.subsetOf(positions)
    }
    override def toString() = location.toString()
}
  
//class Ship(val start: Position, val end: Position) {
//    def isHit(pos: Position): Boolean = {
//    	if (start._1 <= pos._1 && end._1 >= pos._1 && start._2 <= pos._2 && end._2 >= pos._2) true
//    	else false;
//    }
//}
  
