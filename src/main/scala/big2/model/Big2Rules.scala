package big2.model

/**
 * @author AveryChoke
 */

import scala.collection.mutable.Buffer;

object Combo extends Enumeration
{
    type Combo = Value;
    val PASS, SINGLE, PAIR, TRIPLE, FOUR_CARDS, STRAIGHT, FLUSH, 
    FULL_HOUSE, FOUR_OF_A_KIND, STRAIGHT_FLUSH = Value
}

import Combo._;

object Big2Rules {
  
  //evaluate if my cards can be played
  def evaluate(myCards:Buffer[Card], tableCards:Buffer[Card]):Boolean =
  {
    //check if same amount of cards
    if(myCards.length == tableCards.length)
    {
      //find my combo and high card
      val (myCombo, myHighCard) = evaluateValidity(myCards);
      //find table combo and high card
      val (tableCombo, tableHighCard) = evaluateValidity(tableCards);
      
      //check if my cards are valid combo
      if(myCombo != null)
      {
        //my combo is bigger
        if(myCombo.compareTo(tableCombo)>0)
        {
          return true;
        }
        //same combo, need to check who got bigger card
        else if(myCombo.compareTo(tableCombo)==0)
        {
          //compare flush first as flush is special where the bigger card is based on the suit
          if(myCombo==Combo.FLUSH && myHighCard.suit.compareTo(tableHighCard.suit)>0)
          {
            return true;
          }
          else if(myCombo==Combo.FLUSH && myHighCard.suit.compareTo(tableHighCard.suit)<0)
          {
            return false;
          }
          //if same suit then only compare the card itself
          
          //compare the cards
          if(myHighCard.compareTo(tableHighCard)>0)
          {
            return true;
          }
        }
      }
    }
    
    //everything failed, so my card cannot be played
    return false;
  }
  
  //evaluate if the cards is a valid combo
  //to work correctly the cards must be sorted
  def evaluateValidity(cards:Buffer[Card]):(Combo,Card) =
  { 
    cards.length match {
      case 1 =>
        //check single
        var tuple:(Boolean,Card) = isSameCard(cards,1);
        if(tuple._1)
        {
          return (Combo.SINGLE, tuple._2);
        }
      case 2 =>
        //check pair
        var tuple = isSameCard(cards,2);
        if(tuple._1)
        {
          return (Combo.PAIR, tuple._2);
        }
      case 3 =>
        //check triple
        var tuple = isSameCard(cards,3);
        if(tuple._1)
        {
          return (Combo.TRIPLE, tuple._2);
        }
      case 4 =>
        //check four cards
        var tuple = isSameCard(cards,4);
        if(tuple._1)
        {
          return (Combo.FOUR_CARDS, tuple._2);
        }
      case 5 =>
        //check from strongest combo to least combo because stronger combo
        //contain element of weaker combo
        
        //check straight flush
        var tuple = isStraightFlush(cards);
        if(tuple._1)
        {
          return (Combo.STRAIGHT_FLUSH, tuple._2);
        }
        //check four of a kind
        tuple = isFourOfAKind(cards);
        if(tuple._1)
        {
          return (Combo.FOUR_OF_A_KIND, tuple._2);
        }
        //check full house
        tuple = isFullHouse(cards);
        if(tuple._1)
        {
          return (Combo.FULL_HOUSE, tuple._2);
        }
        //check flush
        tuple = isFlush(cards);
        if(tuple._1)
        {
          return (Combo.FLUSH, tuple._2);
        }
        //check straight
        tuple = isStraight(cards);
        if(tuple._1)
        {
          return (Combo.STRAIGHT, tuple._2);
        }
    }
    return (null, null);
  }
  
  //pair, triple, four cards
  def isSameCard(cards:Buffer[Card], amount:Int):(Boolean,Card) =
  {
    if(cards.length==amount && sameNumber(cards))
    {
      return (true,cards(amount-1));
    }
    else
    {
      return (false,null);
    }
  }
  
  private def sameNumber(cards:Buffer[Card]):Boolean =
  {
    //check if all cards same number as the first card
    for(i<-1 until cards.length)
    {
      if(cards(i).number != cards(0).number)
      {
        return false;
      }
    }
    return true;
  }
  
  def isStraight(cards:Buffer[Card]):(Boolean,Card) =
  {
    //need to perform extra sorting for the specific straight
    val sortedCards:Buffer[Card] = cards.sorted;
    
    //check if it is 5 cards
    if(cards.length==5)
    {
      //specific straight of 1,2,3,4,5
      if(sortedCards(0).number==CardNum.THREE
          && sortedCards(1).number==CardNum.FOUR
          && sortedCards(2).number==CardNum.FIVE
          && sortedCards(3).number==CardNum.ACE
          && sortedCards(4).number==CardNum.TWO)
      {
        //change the ordering
        rearrange(cards, 3);
      }
      //specific straight of 2,3,4,5,6
      else if(sortedCards(0).number==CardNum.THREE
          && sortedCards(1).number==CardNum.FOUR
          && sortedCards(2).number==CardNum.FIVE
          && sortedCards(3).number==CardNum.SIX
          && sortedCards(4).number==CardNum.TWO)
      {
        //change the ordering
        rearrange(cards, 4);
      }
      //other straights
      else
      {
        for(i <- 0 until cards.length-1)
        {
          //check if follow order and it must not end with a 2
          if(cards(i).number.id-cards(i+1).number.id != -1  || cards(i+1).number == CardNum.TWO)
          {
            return (false,null);
          }
        }
      }
      return (true,cards.last);
    }
    else
    {
      return (false,null);
    }
  }
  
  def isFlush(cards:Buffer[Card]):(Boolean,Card) =
  {
    //check if it is 5 cards
    if(cards.length==5)
    {
      //check if all cards same suit as the first card
      for(i<-1 until cards.length)
      {
        if(cards(i).suit != cards(0).suit)
        {
          return (false,null);
        }
      }
      return (true,cards.last);
    }
    else
    {
      return (false,null);
    }
  }
  
  def isFullHouse(cards:Buffer[Card]):(Boolean,Card) =
  {
    //check if it is 5 cards
    if(cards.length==5)
    {
      //check a triple then double
      var splitTuple = cards.splitAt(3);
      var tuple1 = isSameCard(splitTuple._1,3);
      var tuple2 = isSameCard(splitTuple._2,2);
      if(tuple1._1 && tuple2._1)
      {
        return (true, tuple1._2);
      }
      
      //check a double then triple
      splitTuple = cards.splitAt(2);
      tuple1 = isSameCard(splitTuple._1,2);
      tuple2 = isSameCard(splitTuple._2,3);
      if(tuple1._1 && tuple2._1)
      {
        //rearrange to triple then pair
        rearrange(cards, 2);
        return (true, tuple2._2);
      }
      
      //not a full house
      return (false,null);
    }
    else
    {
      return (false,null);
    }
  }
  
  def isFourOfAKind(cards:Buffer[Card]):(Boolean,Card) =
  {
    //check if it is 5 cards
    if(cards.length==5)
    {
      //check a four cards then single
      var tuple = isSameCard(cards.slice(0,4),4);
      if(tuple._1)
      {
        return (true, tuple._2);
      }
      
      //check a single then four cards
      tuple = isSameCard(cards.slice(1,5),4);
      if(tuple._1)
      {
        //rearrange to four cards then single
        rearrange(cards, 1);
        return (true, tuple._2);
      }
      
      //not a four of a kind
      return (false,null);
    }
    else
    {
      return (false,null);
    }
  }
  
  def isStraightFlush(cards:Buffer[Card]):(Boolean,Card) =
  {
    //check if it is 5 cards
    if(cards.length==5)
    {
      //check flush then check straight
      var tuple1 = isFlush(cards);
      if(tuple1._1)
      {
        var tuple2 = isStraight(cards);
        if(tuple2._1)
        {
          return (true, tuple2._2);
        }
      }
      return (false, null);
    }
    else
    {
      return (false,null);
    }
  }
  
  private def rearrange(cards:Buffer[Card], splitIndex:Int)
  {
    //split the cards
    val (cards1, cards2) = cards.splitAt(splitIndex);
    //set cards2
    for(i <- 0 until cards2.length)
    {
      cards(i) = cards2(i);
    }
    //set cards1
    for(i <- 0 until cards1.length)
    {
      cards(i+cards2.length) = cards1(i);
    }
  }
  
  def startWithAllBigTwo(cards:Buffer[Card]):Boolean =
  {
    val lastFewCards:Buffer[Card] = cards.takeRight(4);
    return allBigTwo(lastFewCards);
  }
  
  def endWithBigTwo(selectedCards:Buffer[Card], cards:Buffer[Card]):Boolean =
  {
    //only possible if remaining cards are <=3 as if 4 twos, the player will auto win already 
    if(cards.length - selectedCards.length <= 3)
    {
      val remainCards:Buffer[Card] = cards -- selectedCards;
      return allBigTwo(remainCards)
    }
    else
    {
      return false;
    }
  }
  
  private def allBigTwo(cards:Buffer[Card]):Boolean =
  {
    if(cards.length<=0)
    {
      return false;
    }
    for(card <- cards)
    {
      if(card.number != CardNum.TWO)
      {
        return false
      }
    }
    return true;
  }
}