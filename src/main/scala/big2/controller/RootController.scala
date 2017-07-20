package big2.controller

import big2.Application;
import scalafx.application.Platform
import scalafx.scene.control._;
import scalafx.scene.layout._;
import scalafx.event.ActionEvent;
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.stage.Stage;
import scalafx.scene.Scene;

/**
 * @author AveryChoke
 */
@sfxml
class RootController {
  
  def handleExit(action : ActionEvent) = 
  {
    Platform.exit();
  }
  
  def handleGuide(action : ActionEvent) =
  {
    if(!Application.guideStage.showing.value)
    {
      Application.guideStage.show();
    }
    Application.guideStage.requestFocus();
  }
}