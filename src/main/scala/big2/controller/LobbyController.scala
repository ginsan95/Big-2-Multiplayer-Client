package big2.controller

import big2.Application;
import big2.repository.Big2Repository;
import big2.model.{GameRoom,Player};

import scala.collection.mutable.{Buffer,ArrayBuffer};

import scalafx.scene.control._;
import scalafx.scene.layout._;
import scalafx.event.ActionEvent;
import scalafx.scene.input.MouseEvent;
import scalafx.scene.control.Alert.AlertType
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.scene.text.{Font,FontWeight};
import scalafx.collections.ObservableBuffer
import scalafx.application.Platform;

import akka.actor.ActorRef;
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};

/**
 * @author AveryChoke
 */
import ClientActor._;

@sfxml
class LobbyController (
    private val playerNameTextField:TextField,
    private val hostListView:ListView[GameRoom])
{ 
  implicit val timeout: Timeout = 30.seconds;
  val nullNameAlert = new Alert(AlertType.Warning) {
    title = "No Name"
    headerText = "The player name is empty"
    contentText = "Please make sure the player name is not empty"
  };
  
  def displayData(rooms:Buffer[GameRoom])
  {
    hostListView.items = ObservableBuffer(rooms);
  }
  
  def handleHostGame(action : ActionEvent) = 
  {
    val playerText = playerNameTextField.getText;
    if(playerText=="" || playerText.trim.length<=0)
    {
      nullNameAlert.showAndWait();
    }
    else
    {
      val roomNameOption = new TextInputDialog() {
        title = "Room Name"
        headerText = "Enter the room name"
        contentText = "Enter here"
      }.showAndWait();
      
      if(roomNameOption.isDefined && roomNameOption.get.trim.length>0)
      {
        val playerName = if(playerText.length>15) playerText.substring(0,15) else playerText;
        val roomName = if(roomNameOption.get.length>20) roomNameOption.get.substring(0,20)
          else roomNameOption.get;
        //tell the actor to inform server to create room
        val roomNameFuture = Application.clientActor ? ClientHostGame(roomName);
        
        roomNameFuture.foreach {
          case newName:String =>
            //initialize the repository
            val buf = new ArrayBuffer[Player]();
            buf += new Player(playerName, Big2Repository.STARTING_SCORE);
            Big2Repository.initialize(buf, 0);
            Big2Repository.roomName = newName;
            
            //start the host actor
            Application.startHostActor();
            
            //change to game room scene
            Application.changeHostRoomScene();
        }
      }
      else if(roomNameOption.isDefined)
      {
        new Alert(AlertType.Warning) {
          title = "No Name"
          headerText = "The room name is empty"
          contentText = "Please make sure the room name is not empty"
        }.showAndWait();
      }
    }
  }
  
  def handleJoinRoom(action : ActionEvent) = 
  {
    val playerText = playerNameTextField.getText;
    if(playerText=="" || playerText.trim.length<=0)
    {
      nullNameAlert.showAndWait();
    }
    else if(hostListView.selectionModel.value.selectedItem.value == null)
    {
      new Alert(AlertType.Warning) {
        title = "Empty Game Room"
        headerText = "No game room is selected"
        contentText = "Please make sure to select a game room"
      }.showAndWait();
    }
    else
    {
      val playerName = if(playerText.length>15) playerText.substring(0,15) else playerText;
      val roomName:String = hostListView.selectionModel.value.selectedItem.value.roomName;
      
      //tell the actor to inform server to join that room
      val hostFuture = Application.clientActor ? ClientJoinRoom(roomName);
      
      hostFuture.foreach {
        case hostActor:ActorRef =>
          //start the player actor
          Big2Repository.roomName = roomName;
          Application.startPlayerActor(playerName, hostActor);
        case "UNAVAILABLE" =>
          Platform.runLater {
            new Alert(AlertType.Warning) {
              title = "Room Unavailable"
              headerText = s"$roomName is unavailable"
              contentText = "The room could be full, not in your LAN or something unexpected happend." + 
                "Please try again";
            }.showAndWait();
          }
      }
    }
  }
  
  def handleQuitGame(action : ActionEvent) = 
  {
    Application.endActorSystem();
    Application.changeScene(Application.mainMenuScene);
  }
}