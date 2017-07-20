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
import scalafx.application.Platform;
import scalafx.scene.image.{ Image, ImageView };
import scalafx.scene.text.{ Font, FontWeight }

import PlayerSendActor._;

@sfxml
class PlayerRoomController(
    private val roomLabel: Label,
    private val hostLabel: Label,
    private val player1Label: Label,
    private val player2Label: Label,
    private val player3Label: Label,
    private val readyLabel1: Label,
    private val readyLabel2: Label,
    private val readyLabel3: Label,
    private val imageView1: ImageView,
    private val imageView2: ImageView) {
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

  //room name
  roomLabel.text = "Room: " + Big2Repository.roomName;

  //set the game state
  Big2Repository.gameState = GameState.GAME_ROOM;
  
  //reset the score
  Big2Repository.resetScore();
  
  //display
  updateRoom();

  def updateRoom() {
    for (i <- 0 until playersLabel.length) {
      playersLabel(i).setFont(Font.font(null, FontWeight.NORMAL, 24));
      if (i < Big2Repository.players.length) {
        val name = Big2Repository.players(i).name;
        playersLabel(i).text = s"$name";
        if (i == Big2Repository.myIndex) {
          playersLabel(i).setFont(Font.font(null, FontWeight.BOLD, 24));
        }
      } else {
        playersLabel(i).text = "No Player";
      }
      updateReadyView(i);
    }
  }

  //change the view of the ready
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
  
  def handleReady(event: ActionEvent): Unit = {
    //change my ready status
    Big2Repository.myPlayer.isReady = !Big2Repository.myPlayer.isReady

    //update the view
    updateReadyView(Big2Repository.myIndex);

    //tell actor to inform the host that he is ready
    Application.playerSendActor ! SendReady(Big2Repository.myIndex);
  }

  def handleLeaveRoom(event: ActionEvent): Unit = {
    //tell actor to inform host to leave room
    Application.playerSendActor ! SendLeaveRoom(Big2Repository.myIndex);

    //tell client actor to tell server that I leave room
    Application.clientActor ! ClientActor.ClientLeaveRoom(Big2Repository.roomName);

    //kill the actor
    Application.endPlayerActor();

    //leave the room
    Application.changeScene(Application.lobbyScene);
  }

}