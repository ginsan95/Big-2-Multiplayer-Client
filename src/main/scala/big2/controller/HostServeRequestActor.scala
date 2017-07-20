package big2.controller

import big2.Application;
import big2.repository.{Big2Repository,GameState};
import big2.model.{Card,Player};
import big2.util.ActorUtil;
import scala.util.control.Breaks;
import java.util.concurrent.{Executor, Executors,ExecutorService,TimeUnit,CountDownLatch};
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap,Set,HashSet};
import java.io.{DataInputStream,PrintWriter,ObjectInputStream};
import akka.actor.{Actor,ActorRef,Props,Address};
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Await,Future};
import akka.remote.{DisassociatedEvent}
import scalafx.scene.control.Alert;

/**
 * @author AveryChoke
 */

object HostServeRequestActor {
  //case classes
  case class HostJoinRoom(playerName:String);
  case class HostReady(playerIndex:Int);
  case class HostLeaveRoom(playerIndex:Int);
  case class HostPlayCard(tableCards:ArrayBuffer[Card]);
  case class HostPass(passCount:Int);
  case class HostWinGame(winnerIndex:Int, winningMsg:String);
  case object HostReturnRoom;
  case object HostForceReturnRoom; //force host and all player to return room - fault tolerance
  
  case object HostStartNewRound;
  case object HostForceLeaveRoom;
  case class HostKickPlayer(playerIndex:Int);
}

import HostServeRequestActor._;
import PlayerReceiveActor._;

class HostServeRequestActor extends Actor{
  
  //a counter to store unique name for the host player actor;
  private var counter = 1000;
  
  private var newRoundActor:ActorRef = null;
  private var playerSendActor:ActorRef = null;
  private val playerActors:Map[String,ActorRef] = new HashMap();
  implicit val timeout: Timeout = 20.seconds;
  
  override def preStart()
  {
    newRoundActor = context.actorOf(Props[NewRoundActor], name = "NewRoundActor");
    playerSendActor = context.actorOf(Props(classOf[PlayerSendActor],self), name = "PlayerSendActor");
  }
  
  override def receive = {
    case HostJoinRoom(playerName) => joinRoom(playerName);
    case HostReady(playerIndex) => ready(playerIndex);
    case HostLeaveRoom(playerIndex) => leaveRoom(playerIndex);
    case HostPlayCard(tableCards) => playCard(tableCards);
    case HostPass(passCount) => pass(passCount);
    case HostWinGame(winnerIndex, winningMsg) => winGame(winnerIndex, winningMsg);
    case HostReturnRoom => returnRoom();
    case HostForceReturnRoom => returnRoom(true);
    
    case HostStartNewRound =>
      val realSender = sender;
      val future = newRoundActor ? NewRoundActor.Start(playerActors);
      future.foreach {
        case ack:String => realSender ! ack;
      }
    case HostForceLeaveRoom => forceLeaveRoom();
    case HostKickPlayer(playerIndex) => kickPlayer(playerIndex);
  }
  
  //join room
  private def joinRoom(playerName:String)
  {
    val playerIndex:Int = Big2Repository.players.length; //the player index
    
    if(playerIndex < 4)
    {
      //read data
      //change the name if exist
      val newPlayerName = changeName(playerName);
      //add the player
      Big2Repository.addPlayer(playerIndex, new Player(newPlayerName, Big2Repository.STARTING_SCORE));
      
      //send data to the player back
      //send the player index
      //send the player list
      sender ! PlayerJoinRoom(playerIndex, Big2Repository.players.asInstanceOf[ArrayBuffer[Player]]);
      
      //tell other players his existence
      playerActors.foreach
      {
        case (name, actor) =>
          actor ! PlayerUpdateRoom(Big2Repository.players.asInstanceOf[ArrayBuffer[Player]]);
      }
  
      //create host player actor to manage the player
      playerActors += newPlayerName -> context.actorOf(Props(classOf[HostPlayerActor],sender,playerIndex),
          name = String.valueOf(counter));
      counter += 1;
      
      //update game room view
      Application.updateHostRoomView();
    }
    else
    {
      sender ! PlayerKickPlayer;
    }
  }
  
  //ready
  private def ready(playerIndex:Int)
  {
    //send the data to other players
    playerActors.foreach
    {
      case (name, actor) =>
        //do not send back to the same player
        if(!name.equals(Big2Repository.players(playerIndex).name))
        {
          actor ! PlayerReady(playerIndex);
        }
    }
    
    //update the player ready
    val player = Big2Repository.players(playerIndex);
    player.isReady = !player.isReady;
    
    //update the view
    Application.updateHostRoomReadyView(playerIndex);
  }
  
  //leave room
  private def leaveRoom(playerIndex:Int)
  {
    val playerName = Big2Repository.players(playerIndex).name;
    
    //remove the host player actor
    val actorOption = playerActors.get(playerName);
    if(actorOption.isDefined)
    {
      //kill the player actor
      context.stop(actorOption.get);
    }
    //remove player actor
    playerActors -= playerName;
    
    //remove player from repository
    Big2Repository.players.remove(playerIndex);
    
    //inform all the other players
    playerActors.foreach
    {
      case (name, actor) =>
        //send the player index to all the other players
        actor ! PlayerLeaveRoom(playerIndex);
        
        //fault tolerance mechanism
        actor ! HostPlayerActor.DecreasePlayerIndex(playerIndex);
    }
    
    //update game room view
    Application.updateHostRoomView();
  }
  
  //play card
  private def playCard(tableCards:Buffer[Card])
  {    
    //send the data to other players
    playerActors.foreach
    {
      case (name, actor) =>
        //do not send back to the same player
        if(!name.equals(Big2Repository.players(Big2Repository.currentTurnIndex).name))
        {
          //send the table cards to other players
          actor ! PlayerPlayCard(tableCards.asInstanceOf[ArrayBuffer[Card]]);
        }
    }
    
    //update the repository if haven
    if(!Big2Repository.isMyTurn)
    {
      //update table
      Big2Repository.tableCards = tableCards;
      //reduce the current player hand
      Big2Repository.currentPlayer.cardsAmount -= Big2Repository.tableCards.length; 
      //go to next player
      Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
      //update gameplay view
      Application.updateGameplay();
    }
    
    //send acknowledgement to player
    sender ! "ACK";
  }
  
  //pass
  private def pass(passCount:Int)
  {
    //send the data to other players
    playerActors.foreach
    {
      case (name, actor) =>
        //do not send back to the same player
        if(!name.equals(Big2Repository.players(Big2Repository.currentTurnIndex).name))
        {
          //send the table cards to other players
          actor ! PlayerPass(passCount);
        }
    }
    
    //update the repository if haven
    if(!Big2Repository.isMyTurn)
    {
      //update pass count
      Big2Repository.passCount = passCount;
      //go to next player
      Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
      //update gameplay view
      Application.updateGameplay();
    }
    
    //send acknowledgement to player
    sender ! "ACK";
  }
  
  //win game
  private def winGame(winnerIndex:Int, winningMsg:String)
  { 
    //update winnerIndex
    Big2Repository.winnerIndex = winnerIndex;
    //update winning message
    Big2Repository.winningMsg = winningMsg;
    
    //check penalty from previous player
    val preIndex = Big2Repository.prePlayerIndex(Big2Repository.winnerIndex, 1);
    
    //create a future to wait for the penalty index value
    var penaltyFuture:Future[Int] = null;
    if(preIndex == Big2Repository.myIndex)
    {
      //if host is the previous player direct return the value stored in repository
      penaltyFuture = Future {
        Big2Repository.penaltyIndex;
      }
    }
    else
    {
      val preActor = playerActors(Big2Repository.players(preIndex).name);
      //ask for penalty
      penaltyFuture = (preActor ? PlayerPenalty).mapTo[Int];
    }
    
    //wait for reply from possible penalty player
    penaltyFuture.foreach {
      case penaltyIndex:Int =>
        //update repository
        Big2Repository.penaltyIndex = penaltyIndex;
        
        //send the data to other players
        playerActors.foreach
        {
          case (name, actor) =>
            actor ! PlayerWinGame(Big2Repository.winnerIndex, penaltyIndex, winningMsg);
        }
        
        //change to score board scene
        Application.changeScoreBoardScene();
    }
  }
  
  //return room for normal case
  private def returnRoom()
  {
    returnRoom(false); //the force return is false
  }
  
  //return room
  private def returnRoom(forceReturn:Boolean)
  {
    if(Big2Repository.checkNoScoreboardAction() || forceReturn)
    {
      //change scene and display pop up
      Application.changeHostRoomScene();
      scalafx.application.Platform.runLater {
        new Alert(Alert.AlertType.Information) {
          title = "Return Game Room"
          headerText = "Player return game room"
          contentText = "A player has selected to return to the game room"
        }.showAndWait()
      }
      
      playerActors.foreach
      {
        case (name, actor) =>
          //send the player index to all the other players
          actor ! PlayerReturnRoom;
      }
      Application.clientActor ! ClientActor.ClientReturnRoom(Big2Repository.roomName);
    }
  }
  
  //the host is the one who leave room
  private def forceLeaveRoom()
  {
    val realSender = sender;
    
    val futureBuf:Buffer[Future[Any]] = new ArrayBuffer();
    playerActors.foreach
    {
      case (name, actor) =>
        //tell all player that the host leave room - receive ACK
        futureBuf += actor ? PlayerForceLeaveRoom;
    }
    
    //wait for all the player to successfully receive the msg then only terminate the host actor
    val future = Future.sequence(futureBuf.toList);
    future.foreach {
      case ack =>
        //inform controller everything is done
        realSender ! ack;
    }
  }
  
  //kick a player
  private def kickPlayer(playerIndex:Int)
  {
    //get the kicking player actor
    val actorOption = playerActors.get(Big2Repository.players(playerIndex).name);
    if(actorOption.isDefined)
    {
      //inform the player that he is being kicked
      val futureAck = actorOption.get ? PlayerKickPlayer;
      
      futureAck.foreach {
        case _ =>
          //process as if the player leave room
          leaveRoom(playerIndex);
      }
    }
  }
  
  private def changeName(name:String):String =
  {
    var count = 1;
    var newName = name;
    while(playerActors.contains(newName) || Big2Repository.myPlayer.name.equals(newName))
    {
      newName = s"$name-$count";
      count += 1;
    }
    return newName;
  }
}