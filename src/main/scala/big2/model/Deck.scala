package big2.model

import scala.util.Random;

/**
 * @author AveryChoke
 */
object Deck {
  val cards:Array[Card] = Array.ofDim(CardNum.values.size * Suit.values.size);
  
  //Initialize the cards
  var cardsI=0;
  for(number <- CardNum.values)
  {
    for(suit <- Suit.values)
    {
      cards(cardsI) = new Card(number,suit);
      cardsI += 1;
    }
  }
  
  //diamond 3 card. important to keep track of it to know who starts first
  val d3Card:Card = cards(0); 
  
  def shuffle()
  {
    for(x <- 1 to 50)
    {
      var index1 = Random.nextInt(cards.size);
      var index2 = Random.nextInt(cards.size);
      var temp = cards(index1);
      cards(index1) = cards(index2);
      cards(index2) = temp;
    }
  }
  
}