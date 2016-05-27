package model

import akka.actor.Actor
import akka.actor.Props
import com.google.inject.Singleton
import akka.actor.ActorRef
import collection.mutable.Map
import play.api.Logger

object GameCreator {
  case class Connect4(challenger: String, challengee: String)
  case class Othello(size: Int, challenger: String, challengee: String)
  case class Chess(challenger: String, challengee: String)
  case class GetGame(id: String)
}

@Singleton
class GameCreator extends Actor {
  import GameCreator._
  
  var currentGameId = 0;
  val gameMap: Map[String, ActorRef] = Map.empty
  
  def receive: Receive = {
    case Connect4(challenger, challengee) => {
      Logger.info(s"GameCreator.Connect4 -> challenger: $challenger, challengee: $challengee")
      val gameId = currentGameId.toString()
      //step current game id for next game
      currentGameId += 1
      val connect4Game: ActorRef = this.context.actorOf(Props(new Connect4Game(gameId, challenger, challengee)), "Connect4Game-" + gameId)
      gameMap += ((gameId, connect4Game))
      sender ! gameId
      
    }
    case Othello(size, challenger, challengee) => {
      Logger.info(s"GameCreator.Othello -> size: $size, challenger: $challenger, challengee: $challengee")
      val gameId = currentGameId.toString()
      //step current game id for next game
      currentGameId += 1
      val connect4Game: ActorRef = this.context.actorOf(Props(new OthelloGame(gameId, size, challenger, challengee)), "Connect4Game-" + gameId)
      gameMap += ((gameId, connect4Game))
      sender ! gameId
      
    }
    case Chess(challenger, challengee) => {
      Logger.info(s"GameCreator.Chess -> challenger: $challenger, challengee: $challengee")
      val gameId = currentGameId.toString()
      //step current game id for next game
      currentGameId += 1
      val chessGame: ActorRef = this.context.actorOf(Props(new ChessGame(gameId, challenger, challengee)), "ChessGame-" + gameId)
      gameMap += ((gameId, chessGame))
      sender ! gameId
      
    }
    case GetGame(id) => {
      val gameActor: Option[ActorRef] = gameMap.get(id)
      sender ! gameActor
    }
  }
}