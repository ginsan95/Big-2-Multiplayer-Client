package big2.model

import big2.view.CardView;
import big2.repository.Big2Repository;
import scala.collection.mutable.{Buffer,ArrayBuffer};

/**
 * @author AveryChoke
 */
class Player(private var _name:String, private var _score:Int) extends Serializable{
  
  private var _cards:Buffer[Card] = new ArrayBuffer[Card]();
  private var _cardsAmount:Int = 13;
  val selectedCards:Buffer[Card] = new ArrayBuffer[Card]();
  private var _earnedScore:Int = 0;
  private var _isReady:Boolean = false;
  
  def selectCard(card:Card)
  {
    selectedCards += card;
  }
  
  def unselectCard(card:Card)
  {
    selectedCards -= card;
  }
  
  def playCard()
  {
    //remove cards from my hand
    cards --= selectedCards;
    //clear selected cards
    selectedCards.clear();
    //update card amount
    updateCardsAmount();
  }
  
  def updateCardsAmount()
  {
    cardsAmount = cards.length;
  }
  
  def calculateLoserScore(multiplier:Double)
  {
    var multiplierCards=0
    if(cardsAmount<10)
    {
      multiplierCards = 1
    }
    else if(cardsAmount<13)
    {
      multiplierCards = 2
    }
    else
    {
      multiplierCards = 3
    }
    earnedScore=((cardsAmount * multiplierCards * multiplier)*(-1)).ceil.toInt
    score+=earnedScore
  }
  
  def calculateWinnerScore(loser:Buffer[Player])
  {
    earnedScore = 0;
    for(x<-0 until loser.size)
    {
      earnedScore+=(loser(x).earnedScore*(-1))
    }
    score+=earnedScore
  }
  
  //get set
  def name:String = _name;
  def name_= (value:String){ _name=value }
  
  def cardsAmount:Int = _cardsAmount;
  def cardsAmount_= (value:Int){ _cardsAmount=value }
  
  def earnedScore:Int = _earnedScore;
  def earnedScore_= (value:Int){ _earnedScore=value }
  
  def score:Int = _score;
  def score_= (value:Int){ _score=value }
  
  def cards:Buffer[Card] = _cards;
  def cards_= (value:Buffer[Card]){ _cards=value }
  
  def isReady:Boolean = _isReady;
  def isReady_= (value:Boolean){ _isReady=value }
}