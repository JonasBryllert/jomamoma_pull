package model

import akka.actor.Actor
import akka.actor.Props
import com.google.inject.Singleton
import akka.actor.ActorRef
import collection.mutable.Map
import play.api.Logger

object GameCreator {
  case class Connect4(challenger: String, challengee: String)
  case class GetGame(id: String)
}

@Singleton
class GameCreator extends Actor {
  import GameCreator._
  
  var currentGameId = 0;
  val gameMap: Map[String, ActorRef] = Map.empty
  
  def receive: Receive = {
    case Connect4(challenger, challengee) => {
      Logger.info(s"GameCreator.NewGame -> challenger: $challenger, challengee: $challengee")
      val gameId = currentGameId.toString()
      //step current game id for next game
      currentGameId += 1
      val fourInARowGame: ActorRef = this.context.actorOf(Props(new Connect4Game(gameId, challenger, challengee)), "FourInARowGame-" + gameId)
      gameMap += ((gameId, fourInARowGame))
      sender ! gameId
      
    }
    case GetGame(id) => {
      val gameActor: Option[ActorRef] = gameMap.get(id)
      sender ! gameActor
    }
  }
}