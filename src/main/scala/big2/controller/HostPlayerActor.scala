package big2.controller

import big2.Application;
import big2.repository.{Big2Repository,GameState};
import big2.util.ActorUtil;
import akka.actor.{Actor,ActorRef,Props};
import scala.concurrent.Await;
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.util.Timeout
import scala.concurrent.duration._
import akka.remote.{DisassociatedEvent}

/**
 * @author AveryChoke
 */
import PlayerReceiveActor._;
  
object HostPlayerActor {
  //fault tolerance mechanism
  case object GetData;
  case class DecreasePlayerIndex(playerIndex:Int)
}

import HostPlayerActor._;
import HostServeRequestActor._;

class HostPlayerActor(playerActor:ActorRef, private var playerIndex:Int) extends Actor{
  
  implicit val timeout: Timeout = 10.second;
  
  override def preStart = {
    //subscribe to error
    context.system.eventStream.subscribe(self, classOf[DisassociatedEvent]);
  }
  
  override def receive = {
    case NewRoundActor.StartNewRound(cards, currentTurn) =>
      //send players their cards and the first turn
      playerActor ! PlayerNewRound(cards, currentTurn);
    case PlayerUpdateRoom(players) => 
      playerActor ! PlayerUpdateRoom(players);
    case PlayerReady(playerIndex) => 
      playerActor ! PlayerReady(playerIndex);
    case PlayerLeaveRoom(playerIndex) => 
      playerActor ! PlayerLeaveRoom(playerIndex);
    case PlayerForceLeaveRoom =>
      val realSender = sender;
      val future = playerActor ? PlayerForceLeaveRoom;
      future.foreach {
        case ack => realSender ! ack;
      }
      future.failed.foreach {
        case _ => realSender ! "ACK";
      }
    case PlayerKickPlayer =>
      val realSender = sender;
      val future = playerActor ? PlayerKickPlayer;
      future.foreach {
        case ack => realSender ! ack;
      }
      future.failed.foreach {
        case _ => realSender ! "ACK";
      }
    case PlayerPlayCard(tableCards) => 
      playerActor ! PlayerPlayCard(tableCards);
    case PlayerPass(passCount) => 
      playerActor ! PlayerPass(passCount);
    case PlayerWinGame(winnerIndex, penaltyIndex, winningMsg) => 
      playerActor ! PlayerWinGame(winnerIndex, penaltyIndex, winningMsg);
    case PlayerPenalty =>
      val realSender = sender;
      implicit val timeout: Timeout = 10.seconds;
      val penaltyFuture = playerActor ? PlayerPenalty;
      penaltyFuture.foreach {
        case num:Int => realSender ! num;
      }
    case PlayerReturnRoom =>
      playerActor ! PlayerReturnRoom;
      
    //get set of player index
    case GetData =>
      sender ! (playerActor, playerIndex);
    case DecreasePlayerIndex(index) =>
      //decrease player index if my index is higher than it
      if(index < playerIndex)
      {
        playerIndex -= 1;
      }
      
    case DisassociatedEvent(localAddress, remoteAddress, inbound) =>
      //fault tolerance mechanism
      //this particular player disconnected
      if(ActorUtil.compareActorByAddress(remoteAddress, playerActor))
      {        
        //kill myself immediately first to prevent same disconnection message being processed
        context.stop(self);
        
        //tell the host and assume the player leave game
        Application.hostActor ! HostLeaveRoom(playerIndex);
        
        //if not in game room then perform return room to go back to game room
        if(Big2Repository.gameState != GameState.GAME_ROOM)
        {
          Application.hostActor ! HostForceReturnRoom;
        }
      }
  }
  
}