package big2.controller

import big2.Application;
import big2.repository.Big2Repository;
import big2.model.{Card,Player};
import big2.util.ActorUtil;
import scala.util.control.Breaks;
import java.util.concurrent.{Executors,ExecutorService,TimeUnit,CountDownLatch};
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};
import java.io.{DataInputStream,PrintWriter,ObjectInputStream};
import akka.actor.{Actor,ActorRef,Props,Address};
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.remote.{DisassociatedEvent}
import scalafx.scene.control.Alert;

/**
 * @author AveryChoke
 */

object PlayerReceiveActor {
  //case classes
  case class PlayerJoinRoom(playerIndex:Int, players:ArrayBuffer[Player]);
  case class PlayerUpdateRoom(players:ArrayBuffer[Player]);
  case class PlayerReady(playerIndex:Int);
  case class PlayerLeaveRoom(playerIndex:Int);
  case object PlayerForceLeaveRoom;
  case object PlayerKickPlayer;
  case class PlayerNewRound(cards:ArrayBuffer[Card], turnIndex:Int);
  case class PlayerPlayCard(tableCards:ArrayBuffer[Card]);
  case class PlayerPass(passCount:Int);
  case class PlayerWinGame(winnerIndex:Int, penaltyIndex:Int, winningMsg:String);
  case object PlayerPenalty;
  case object PlayerReturnRoom;
  case object PlayerPing;
}

import PlayerReceiveActor._;

class PlayerReceiveActor(private val playerName:String, private val hostActor:ActorRef) extends Actor{
  
  private var playerSendActor:ActorRef = null;
  
  override def preStart = {
    //tell the host to join his game
    hostActor ! HostServeRequestActor.HostJoinRoom(playerName);
    playerSendActor = context.actorOf(Props(classOf[PlayerSendActor],hostActor), name = "PlayerSendActor");
    
    //subscribe to error
    context.system.eventStream.subscribe(self, classOf[DisassociatedEvent]);
  }
  
  override def receive = {
    case PlayerJoinRoom(playerIndex, players) => joinRoom(playerIndex, players);
    case PlayerUpdateRoom(players) => updateRoom(players);
    case PlayerReady(playerIndex) => ready(playerIndex);
    case PlayerLeaveRoom(playerIndex) => leaveRoom(playerIndex);
    case PlayerForceLeaveRoom => forceLeaveRoom();
    case PlayerKickPlayer => kickPlayer();
    case PlayerNewRound(cards, turnIndex) => newRound(cards, turnIndex);
    case PlayerPlayCard(tableCards) => playCard(tableCards);
    case PlayerPass(passCount) => pass(passCount);
    case PlayerWinGame(winnerIndex, penaltyIndex, winningMsg) => 
      winGame(winnerIndex, penaltyIndex, winningMsg);
    case PlayerPenalty => penalty();
    case PlayerReturnRoom => returnRoom();
    
    //fault handling
    //case DeadLetter(message, sender, recipient) => unsubscribeServer(recipient);
    case DisassociatedEvent(localAddress, remoteAddress, inbound) =>
      disconnectHost(remoteAddress);
  }
  
  //join room
  private def joinRoom(playerIndex:Int, players:Buffer[Player])
  {
    //initialize
    Big2Repository.initialize(players, playerIndex);
    
    //change to game room scene
    Application.changePlayerRoomScene();
  }
  
  //update room
  private def updateRoom(players:Buffer[Player])
  {
    //update repository player list
    Big2Repository.players = players;
    
    //update game room view
    Application.updatePlayerRoomView();
  }
  
  //ready
  private def ready(playerIndex:Int)
  {
    //update the player ready
    val player = Big2Repository.players(playerIndex);
    player.isReady = !player.isReady;
    
    //update the view
    Application.updatePlayerRoomReadyView(playerIndex);
  }
  
  //a player leave room
  private def leaveRoom(playerIndex:Int)
  {
    //remove the player from the repository
    Big2Repository.players.remove(playerIndex);
    //update my index
    if(playerIndex < Big2Repository.myIndex)
    {
      Big2Repository.myIndex -= 1;
    }
    
    //update game room view
    Application.updatePlayerRoomView();
  }
  
  //the host leave room
  private def forceLeaveRoom()
  {
    //reply the acknowledge
    sender ! "ACK";
    
    //force kick my self our of the game
    Application.kickOut();
  }
  
  private def kickPlayer()
  {
    //forced to leave room
    forceLeaveRoom();
    
    //tell client actor to tell server that I leave room
    Application.clientActor ! ClientActor.ClientLeaveRoom(Big2Repository.roomName);
  }
  
  //new round
  private def newRound(cards:Buffer[Card], turnIndex:Int)
  {
    //obtain the cards distributed to my player
    Big2Repository.myPlayer.cards = cards;
    //get to know who start first
    Big2Repository.currentTurnIndex = turnIndex;
    
    //start gameplay
    Application.changeGameplayScene();
  }
  
  //play card
  private def playCard(tableCards:Buffer[Card])
  {
    //obtain the new table cards
    Big2Repository.tableCards = tableCards;
    
    //reduce the current player hand
    Big2Repository.currentPlayer.cardsAmount -= Big2Repository.tableCards.length; 
    
    //go to next player
    Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
    
    //update gameplay view
    Application.updateGameplay();
  }
  
  //pass
  private def pass(passCount:Int)
  {
    //obtain the new pass count
    Big2Repository.passCount = passCount;
    
    //go to next player
    Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
    
    //update gameplay view
    Application.updateGameplay();
  }
  
  //win game
  private def winGame(winnerIndex:Int, penaltyIndex:Int, winningMsg:String)
  {
    //get the player who won
    Big2Repository.winnerIndex = winnerIndex;
    //get the penalty
    Big2Repository.penaltyIndex = penaltyIndex;
    //get the winningMsg
    Big2Repository.winningMsg = winningMsg;
    
    //change to score board scene
    Application.changeScoreBoardScene();
  }
  
  //send penalty
  private def penalty()
  {
    //send back to server his penalty index
    sender ! Big2Repository.penaltyIndex;
  }
  
  //return room
  private def returnRoom()
  {
    //change scene and display pop up
    Application.changePlayerRoomScene();
    scalafx.application.Platform.runLater {
      new Alert(Alert.AlertType.Information) {
        title = "Return Game Room"
        headerText = "Player return game room"
        contentText = "A player has selected to return to the game room"
      }.showAndWait()
    }
  }
  
  //fault handling mechanism
  private def disconnectHost(address:Address)
  {
    //only disconnect if the disconnection come from the host
    if(ActorUtil.compareActorByAddress(address, hostActor))
    {
      //assume the forced to leave the room
      forceLeaveRoom();
    }
  }
}