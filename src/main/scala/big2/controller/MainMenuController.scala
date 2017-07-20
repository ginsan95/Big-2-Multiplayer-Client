package big2.controller

import big2.Application;
import big2.repository.Big2Repository;
import scalafxml.core.macros.sfxml
import scalafx.event.ActionEvent;
import scalafx.scene.control.{ButtonType,Alert,TextInputDialog,Dialog,ProgressIndicator};
import scalafx.scene.control.ButtonBar.ButtonData;
import scalafx.scene.{Scene,Group};
import scalafx.stage.Stage;
import scalafx.application.Platform;
import scala.concurrent.Future;
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author AveryChoke
 */
@sfxml
class MainMenuController {
  
  val retryButton:ButtonType = new ButtonType("Retry", ButtonData.OKDone);
  val cancelButton:ButtonType = new ButtonType("Cancel", ButtonData.CancelClose);
  val connectionAlert:Alert = new Alert(Alert.AlertType.Warning) {
      title = "Game Error"
      headerText = "Unable to start the game"
      contentText = "The server is busy or disconnected, or some unexpected problem occured.";
      buttonTypes.clear();
      buttonTypes += (retryButton,cancelButton);
  };
  val connectionProgress:Dialog[String] = new Dialog[String] {
    title = "Connecting";
    headerText = "Connecting to the server";
    result = "close";
    graphic = new ProgressIndicator;
  };
  
  def playGame()
  { 
    //display loading
    val cancelConnection = connectionProgress.show();
    
    //try to connect to the server
    val successFuture:Future[Boolean] = Application.startActorSystem();
    
    successFuture.foreach {
      case true => //success
        Platform.runLater {
          //end the loading
          connectionProgress.close();
          //change to lobby
          Application.changeScene(Application.lobbyScene);
        }
      case false => //failed
        //end the actor system
        Application.endActorSystem();
        Platform.runLater {
          //end the loading
          connectionProgress.close();
          //ask for retry
          val option:Option[ButtonType] = connectionAlert.showAndWait();
          if(option.get == retryButton)
          {
            playGame();
          }
        }
    }    
  }
      
  def handlePlayGame(action : ActionEvent) =
  {
    playGame();
  }
  
  def handleGuide(action : ActionEvent) =
  {
    if(!Application.guideStage.showing.value)
    {
      Application.guideStage.show();
    }
    Application.guideStage.requestFocus();
  }
  
  def handleSetting(action : ActionEvent) =
  {
    val serverAddress = new TextInputDialog(Application.serverAddress) {
      title = "Serve IP Address"
      headerText = "Change the server IP Address here"
      contentText = "Enter here"
    }.showAndWait();
    
    if(serverAddress.isDefined)
    {
      Application.serverAddress = serverAddress.get;
    }
  }
}