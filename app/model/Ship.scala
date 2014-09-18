package model

import play.api.libs.json._
import play.api.libs.json.Json._

object Ship {
  type Position = (Int, Int)
  
  def apply(location: List[Position]): Ship = {
    return new Ship(location)
  }
  
}

import Ship.Position
import WriteConverters.ShipWrites
class Ship(val location: List[Position]) {
    def isHit(pos: Position): Boolean = {
    	if (location.contains(pos)) true
    	else false
    }
    val length = location.size
    def isSunk(positions: List[Position]): Boolean = {
      location.toSet.subsetOf(positions.toSet)
    }
    
//    val getLocation = location
    
    override def toString() = location.toString()
}
  
object WriteConverters {
  implicit val PositionWrites = new Writes[Position] {
	def writes(p: Position): JsValue = {
	  Json.obj(
    	"x" -> p._1,
    	"y" -> p._2
      )
    }
  }
  implicit val ShipWrites = new Writes[Ship] {
	def writes(s: Ship): JsValue = {
	  Json.toJson(s.location)
    }
  }
//  implicit val ShipsWrites = new Writes[Set[Ship]] {
//	def writes(s: Set[Ship]): JsValue = {
//	  Json.arr(s)
//    }
//  }
  
}


//class Ship(val start: Position, val end: Position) {
//    def isHit(pos: Position): Boolean = {
//    	if (start._1 <= pos._1 && end._1 >= pos._1 && start._2 <= pos._2 && end._2 >= pos._2) true
//    	else false;
//    }
//}
  
