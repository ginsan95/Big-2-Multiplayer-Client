package big2.controller

import big2.Application;
import big2.repository.{Big2Repository,GameState};
import big2.model.Player;
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafxml.core.macros.sfxml
import scala.collection.mutable.ArrayBuffer;
import big2.repository.Big2Repository;
import scalafx.scene.Scene._
import scalafx.Includes._
import scalafx.scene.control._
import scala.collection.mutable.{ Buffer, ArrayBuffer };
import scala.concurrent.Await
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import scala.concurrent.duration._
import scalafx.application.Platform;
import scalafx.scene.text.Text
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.image.{ Image, ImageView };

@sfxml
class HostRoomController(
    private val roomLabel: Label,
    private val hostLabel: Label,
    private val player1Label: Label,
    private val player2Label: Label,
    private val player3Label: Label,
    private val readyLabel1: Label,
    private val readyLabel2: Label,
    private val readyLabel3: Label,
    private val kickButton1:Button,
    private val kickButton2:Button,
    private val kickButton3:Button,
    private val imageView1: ImageView,
    private val imageView2: ImageView)
{
  //define image
  private val resource1 = getClass.getResourceAsStream("/big2/view/big2logo.png");
  private val resource2 = getClass.getResourceAsStream("/big2/view/daidee.png");
  private val big2Logo = new Image(resource1);
  private val bigDee = new Image(resource2);
  imageView1.image = big2Logo;
  imageView2.image = bigDee;

  //define labels
  private val playersLabel: Array[Label] = new Array[Label](4);
  playersLabel(0) = hostLabel;
  playersLabel(1) = player1Label;
  playersLabel(2) = player2Label;
  playersLabel(3) = player3Label;
  
  //define buttons
  private val kickButtons:Array[Button] = new Array[Button](3);
  kickButtons(0) = kickButton1;
  kickButtons(1) = kickButton2;
  kickButtons(2) = kickButton3;
  
  //room name
  roomLabel.text = "Room: " + Big2Repository.roomName;

  //set the game state
  Big2Repository.gameState = GameState.GAME_ROOM;
  
  //reset the score
  Big2Repository.resetScore();
  
  //display
  updateRoom();

  def updateRoom() {
    for (i <- 0 until playersLabel.length)
    {
      if (i < Big2Repository.players.length)
      {
        val name = Big2Repository.players(i).name;
        playersLabel(i).text = s"$name";
        if(i>=1)
        {
          kickButtons(i-1).disable = false;
        }
      }
      else
      {
        playersLabel(i).text = "No Player";
        kickButtons(i-1).disable = true;
      }
      updateReadyView(i);
    }
  }

  def updateReadyView(playerIndex: Int) {
    val playerReadyLabel: Label = playerIndex match {
      case 1 => readyLabel1;
      case 2 => readyLabel2;
      case 3 => readyLabel3;
      case _ => return ;
    }

    if (playerIndex < Big2Repository.players.length && Big2Repository.players(playerIndex).isReady) {
      playerReadyLabel.setStyle("-fx-border-color:black; -fx-background-color: green;");
    } else {
      playerReadyLabel.setStyle("-fx-border-color:black; -fx-background-color: grey;");
    }
  }
  
  
  
  def handleKickPlayer(event: ActionEvent): Unit = {
    var playerIndex = -1;
    
    //identify which button is clicked which correspond to the correct player index
    event.source match {
      case this.kickButton1 => playerIndex = 1;
      case this.kickButton2 => playerIndex = 2;
      case this.kickButton3 => playerIndex = 3;
    }
    
    //tell actor to kick that player
    Application.hostActor ! HostServeRequestActor.HostKickPlayer(playerIndex);
  }

  def handleStartGame(event: ActionEvent): Unit = {
    if (Big2Repository.players.length >= 4 && Big2Repository.isAllReady) {
      //start new round
      Application.hostNewRound();
      //tell client actor to tell server that I start my game
      Application.clientActor ! ClientActor.ClientStartGame(Big2Repository.roomName);
    } else {
      new Alert(AlertType.Warning) {
        title = "Warning Dialog"
        headerText = "Unable to start game"
        contentText = "The room is not full or everyone is not ready"
      }.showAndWait()
    }
  }
  
  def handleLeaveRoom(event: ActionEvent): Unit = {
    implicit val timeout: Timeout = 1.minute;
    
    //tell actor that I want to leave room
    val futureAck = Application.hostActor ? HostServeRequestActor.HostForceLeaveRoom;
    
    //wait for acknowledgement
    Await.result(futureAck, Duration.Inf);
    
    //end the host actor after everything is done
    Application.endHostActor();

    //tell client actor to tell server that I leave room
    Application.clientActor ! ClientActor.ClientLeaveRoom(Big2Repository.roomName);

    Application.changeScene(Application.lobbyScene);
  }
}