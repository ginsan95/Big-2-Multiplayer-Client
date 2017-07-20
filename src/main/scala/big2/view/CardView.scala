package big2.view

import big2.model.{Card,Player,Deck};
import scalafx.scene.Group
import scalafx.scene.shape.Rectangle;
import scalafx.scene.image.{Image,ImageView};
import scalafx.scene.text.Text;
import scalafx.scene.layout.StackPane;
import scalafx.scene.paint.Color;
import scalafx.scene.effect.BlendMode;
import scalafx.scene.input.{KeyEvent,KeyCode,MouseEvent}
import scalafx.Includes._
import scala.collection.mutable.Buffer;

/**
 * @author AveryChoke
 */
class CardView(private var _card:Card, width:Double, height:Double) extends Group {
  
  //private var cardBuffer:Option[Buffer[CardView]] = Option(null);
  private var player:Option[Player] = Option(null);
  private var _isSelected:Boolean = false;
  
  //constructor to indicate can click
  def this(c:Card, ply:Player, w:Double, h:Double)
  {
    this(c, w, h);
    player = Some(ply);
  }
  
  private val rec = Rectangle(width,height,Color.Yellow);
  private val cardName = card.toString();
  private val resourceIS = getClass.getResourceAsStream(s"card_image/$cardName.png");
  private val cardImg = new Image(resourceIS);
  private val cardIV = new ImageView(cardImg)
  {
    fitWidth = width;
    fitHeight = height;
  };
  private val stack = new StackPane();
  stack.children.addAll(rec,cardIV);
  children = stack;
  
  def select()
  {
    //max only can select 5 cards
    if(!isSelected && player.get.selectedCards.length<5)
    {
      player.get.selectCard(card);
      //rec.fill = Color.Yellow;
      cardIV.blendMode = BlendMode.Multiply;
      isSelected = true;
    }
  }
  
  def unselect()
  {
    //cannot unselect diamond 3
    if(isSelected && card.compareTo(Deck.d3Card)!=0)
    {
      player.get.unselectCard(card);
      //rec.fill = Color.White;
      cardIV.blendMode = BlendMode.SrcOver;
      isSelected = false;
    }
  }
  
  //handle click
  this.onMouseClicked() = handle
  {
    //if no player then cannot click
    if(player.isDefined)
    {
      if(isSelected)
      {
        unselect();
      }
      else
      {
        select();
      }
    }
  }
  
  //get set
  def card:Card = _card;
  def card_= (value:Card){ _card=value }
  
  def isSelected:Boolean = _isSelected;
  def isSelected_= (value:Boolean){ _isSelected=value } 
}