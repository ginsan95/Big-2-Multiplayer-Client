package big2.model

/**
 * @author AveryChoke
 */
object CardNum extends Enumeration
{
    type CardNum = Value;
    val THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE,
    TEN, JACK, QUEEN, KING, ACE, TWO = Value
}
  
object Suit extends Enumeration
{
    type Suit = Value;
    val DIAMONDS, CLUBS, HEARTS, SPADES = Value
}

import CardNum._;
import Suit._;

class Card(private var _number:CardNum, private var _suit:Suit) extends Comparable[Card] with Serializable{
  
  override def toString():String =
  {
    return number + " of " + suit;
  }
  
  override def compareTo(card2:Card):Int =
  {
    if(number.compareTo(card2.number) == 0)
    {
      return suit.compareTo(card2.suit);
    }
    else
    {
      return number.compareTo(card2.number);
    }
  }
  
  //get set
  def number:CardNum = _number;
  def number_= (value:CardNum){ _number=value }
  
  def suit:Suit = _suit;
  def suit_= (value:Suit) { _suit=value }
}