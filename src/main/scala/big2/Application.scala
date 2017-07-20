package big2

import big2.model._
import big2.controller._
import big2.repository.Big2Repository
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.stage.Stage;
import scalafx.scene.Scene
import scalafx.Includes._
import javafx.scene.layout.{BorderPane,AnchorPane};
import scalafxml.core.{ NoDependencyResolver, FXMLView, FXMLLoader }
import javafx.scene.layout.GridPane
import scalafx.scene.control._
import scala.collection.mutable.{Buffer,ArrayBuffer}
import scala.util.control.Breaks
import scalafx.application.Platform
import java.net.Socket
import java.io.{DataOutputStream,PrintWriter}
import scala.io.StdIn.{readLine,readInt}
import akka.actor._
import akka.remote.RemoteScope
import akka.pattern.{ask,pipe}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Future,Await}
import com.typesafe.config.ConfigFactory;
import big2.controller.HostServeRequestActor

/**
 * @author AveryChoke
 */
object Application extends JFXApp {
  
  var serverAddress:String = "p4big2.ddns.net";
  val PORT_NUM:Int = 6767;
  
  //create the actor system
  var actorSystem:ActorSystem = null;
  
  //the actors
  var clientActor:ActorRef = null;
  var hostActor:ActorRef = null;
  var playerActor:ActorRef = null;
  var playerSendActor:ActorSelection = null;
  
  implicit val timeout: Timeout = 20.seconds;
  
  def startActorSystem():Future[Boolean] =
  {
    if(actorSystem == null)
    {
      actorSystem = ActorSystem("Big2System");
      clientActor = actorSystem.actorOf(Props[ClientActor], name="ClientActor");
      
      return (clientActor ? "START").mapTo[Boolean];
    }
    else
    {
      return Future {
        false;
      }
    }
  }
  
  def endActorSystem()
  {
    if(actorSystem != null)
    {
      val future = actorSystem.terminate();
      future.foreach {
        case _ => 
          actorSystem = null;
          hostActor = null;
          playerActor = null;
      }
    }
  }
  
  def startHostActor()
  {
    if(hostActor==null)
    {
      hostActor = actorSystem.actorOf(Props[HostServeRequestActor], name="HostActor");
      playerSendActor = actorSystem.actorSelection("/user/HostActor/PlayerSendActor");
    }
  }
  
  def endHostActor()
  {
    if(hostActor != null)
    {
      actorSystem.stop(hostActor);
      hostActor = null;
    }
  }
  
  def startPlayerActor(playerName:String, hostActor:ActorRef) = synchronized
  {
    if(playerActor==null)
    {
      playerActor = actorSystem.actorOf(
          Props(classOf[PlayerReceiveActor], playerName, hostActor), name="PlayerActor");
      playerSendActor = actorSystem.actorSelection("/user/PlayerActor/PlayerSendActor");
    }
  }
  
  def endPlayerActor()
  {
    if(playerActor != null)
    {
      actorSystem.stop(playerActor);
      playerActor = null;
    }
  }
  
  def kickOut()
  {
    //end all actors
    Application.endPlayerActor();
    
    //change the scene back to lobby
    Platform.runLater {
      changeScene(lobbyScene);
      
      //display error message
      new Alert(Alert.AlertType.Error) {
        title = "Leave Room"
        headerText = "Forced to leave room"
        contentText = "The host kicked you, leaved the room or is dead"
      }.showAndWait();
    }
    
    //subscribe to the server
    clientActor ! ClientActor.ClientSubscribeServer;
  }
  
  //the controllers
  private var lobbyControl:Option[LobbyController#Controller] = Option(null);
  private var hostRoomControl:Option[HostRoomController#Controller] = Option(null);
  private var playerRoomControl:Option[PlayerRoomController#Controller] = Option(null);
  private var gameplayControl:Option[GameplayController#Controller] = Option(null);
  
  //special root to change the scene
  private var myRoot:Option[BorderPane] = Option(null);
  //special method to change the scene
  def changeScene(newScene:Scene)
  {
    if(myRoot.isDefined)
    {
      myRoot.get.setCenter(newScene.root.value);
    }
  }
  
  //the root layout scene
  lazy val rootLayoutScene = new Scene
  {
    val resourceIS = getClass.getResourceAsStream("view/RootLayout.fxml");
    val loader = new FXMLLoader( null, NoDependencyResolver)
    loader.load(resourceIS);
    myRoot = Some(loader.getRoot[javafx.scene.layout.BorderPane]);
    root = myRoot.get;
  }
  
  //main menu scene
  lazy val mainMenuScene = new Scene
  {
    val resourceIS = getClass.getResourceAsStream("view/MainMenu.fxml");
    val loader = new FXMLLoader( null, NoDependencyResolver)
    loader.load(resourceIS);
    root = loader.getRoot[javafx.scene.layout.AnchorPane];
  }
  
  //guide stage
  lazy val guideStage = new Stage()
  {
    title = "Game Guide";
    val resourceIS = getClass.getResourceAsStream("view/Guide.fxml");
    val loader = new FXMLLoader( null, NoDependencyResolver)
    loader.load(resourceIS);
    scene = new Scene { 
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
    }
  }
  
  //lobby scene
  lazy val lobbyScene = new Scene
  {
    val resourceIS = getClass.getResourceAsStream("view/Lobby.fxml");
    val loader = new FXMLLoader( null, NoDependencyResolver)
    loader.load(resourceIS);
    root = loader.getRoot[javafx.scene.layout.AnchorPane];
    lobbyControl = Option(loader.getController[LobbyController#Controller]);
  }
  
  //update the lobby available rooms
  def updateLobbyRoom(rooms:Buffer[GameRoom])
  {
    while(!lobbyControl.isDefined)
    {
      Thread.sleep(500);
    }
    Platform.runLater {
      lobbyControl.get.displayData(rooms);
    }
  }
  
  //change to host room scene  
  def changeHostRoomScene()
  {
    //create the scene
    val hostRoomScene = new Scene
    {
      val resourceIS = getClass.getResourceAsStream("view/HostRoom.fxml");
      val loader = new FXMLLoader( null, NoDependencyResolver)
      loader.load(resourceIS);
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
      hostRoomControl = Option(loader.getController[HostRoomController#Controller]);
    }
    Platform.runLater {
      changeScene(hostRoomScene);
    }
  }
  
  //update host room view
  def updateHostRoomView()
  {
    if(hostRoomControl.isDefined)
    {
      Platform.runLater {
        hostRoomControl.get.updateRoom();
      }
    }
  }
  
  //update host room ready view
  def updateHostRoomReadyView(playerIndex:Int)
  {
    if(hostRoomControl.isDefined)
    {
      Platform.runLater {
        hostRoomControl.get.updateReadyView(playerIndex);
      }
    }
  }
  
  //change to player room scene  
  def changePlayerRoomScene()
  {
    //create the scene
    val playerRoomScene = new Scene
    {
      val resourceIS = getClass.getResourceAsStream("view/PlayerRoom.fxml");
      val loader = new FXMLLoader( null, NoDependencyResolver)
      loader.load(resourceIS);
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
      playerRoomControl = Option(loader.getController[PlayerRoomController#Controller]);
    }
    Platform.runLater {
      changeScene(playerRoomScene);
    }
  }

  //update player room view
  def updatePlayerRoomView()
  { 
    if(playerRoomControl.isDefined)
    {
      Platform.runLater {
        playerRoomControl.get.updateRoom();
      }
    }
  }
  
  //update host room ready view
  def updatePlayerRoomReadyView(playerIndex:Int)
  {
    if(playerRoomControl.isDefined)
    {
      Platform.runLater {
        playerRoomControl.get.updateReadyView(playerIndex);
      }
    }
  }
  
  //change gameplay scene
  def changeGameplayScene()
  {
    //create the scene
    val gameplayScene = new Scene
    {
      val resourceIS = getClass.getResourceAsStream("view/Gameplay.fxml");
      val loader = new FXMLLoader( null, NoDependencyResolver)
      loader.load(resourceIS);
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
      gameplayControl = Option(loader.getController[GameplayController#Controller]);
    }
    Platform.runLater {
      changeScene(gameplayScene);
    }
  }
  
  //update the gameplay view
  def updateGameplay()
  {
    if(gameplayControl.isDefined)
    {
      Platform.runLater {
        gameplayControl.get.displayData();
      }
    }
  }
  
  //change to score board scene
  def changeScoreBoardScene()
  {
    //create the scene
    val scoreScene = new Scene
    {
      val resourceIS = getClass.getResourceAsStream("view/ScoreBoard.fxml");
      val loader = new FXMLLoader( null, NoDependencyResolver)
      loader.load(resourceIS);
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
    }
    //change the scene
    Platform.runLater {
      changeScene(scoreScene);
    }
  }
  
  //set the stage
  stage = new PrimaryStage
  {
    title = "P4 Big 2 Game"
    resizable = false;
    scene = rootLayoutScene;
  }
  changeScene(mainMenuScene);
  
  //Kill all the threads if the user suddenly close the window
  stage.onCloseRequest() = handle
  {
    Platform.exit();
    System.exit(0);
  }
  
  //host only - player should just change the gameplay scene
  def hostNewRound()
  {
    if(hostActor != null)
    {
      //perform new round distribution
      val future = hostActor ? HostServeRequestActor.HostStartNewRound;
      
      future.foreach {
        case _ =>
          //start the scene
          changeGameplayScene();
      }
    }
  }
}