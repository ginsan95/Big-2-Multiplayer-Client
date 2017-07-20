package big2.controller

import big2.repository.Big2Repository;
import big2.model.{Card,Player};
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};
import akka.actor.{Actor,ActorRef};
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await;

/**
 * @author AveryChoke
 */
object PlayerSendActor {
  //case classes
  case class SendReady(playerIndex:Int)
  case class SendLeaveRoom(playerIndex:Int)
  case class SendPlayCard(tableCards:ArrayBuffer[Card])
  case class SendPass(passCount:Int)
  case class SendWinGame(winnerIndex:Int, winningMsg:String)
  case object SendReturnRoom;
}

import PlayerSendActor._;
import HostServeRequestActor._;

class PlayerSendActor(hostActor:ActorRef) extends Actor{
  
  implicit val timeout: Timeout = 20.seconds;
  
  override def receive = {
    case SendReady(playerIndex) => ready(playerIndex);
    case SendLeaveRoom(playerIndex) => leaveRoom(playerIndex);
    case SendPlayCard(tableCards) => playCard(tableCards);
    case SendPass(passCount) => pass(passCount);
    case SendWinGame(winnerIndex, winningMsg) => winGame(winnerIndex, winningMsg);
    case SendReturnRoom => returnRoom();
  }
 
  private def ready(playerIndex:Int)
  {
    //send the host the player who passed
    hostActor ! HostReady(playerIndex);
  }
  
  private def leaveRoom(playerIndex:Int)
  {
    //send the host the player who leave room
    hostActor ! HostLeaveRoom(playerIndex);
  }
  
  private def playCard(tableCards:ArrayBuffer[Card])
  {
    val realSender = sender;
    //send the host the table cards and wait for acknowledgement
    val futureAck = hostActor ? HostPlayCard(tableCards);
    
    //get acknowledgement and send back to sender
    futureAck.foreach {
      case ack:String => realSender ! ack;
    }
  }
  
  private def pass(passCount:Int)
  {
    val realSender = sender;
    //send the host the pass count and wait for acknowledgement
    val futureAck = hostActor ? HostPass(passCount);
    
    //get acknowledgement and send back to sender
    futureAck.foreach {
      case ack:String => realSender ! ack;
    }
  }
  
  private def winGame(winnerIndex:Int, winningMsg:String)
  {
    //send host the winner index
    hostActor ! HostWinGame(winnerIndex, winningMsg);
  }
  
  private def returnRoom()
  {
    //send host that I want to return room
    hostActor ! HostReturnRoom;
  }
}