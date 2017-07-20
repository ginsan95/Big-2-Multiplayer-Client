package big2.controller

import big2.repository.Big2Repository;
import scala.util.control.Breaks;
import akka.actor.{Actor,ActorRef,Props};
import scala.collection.mutable.{Buffer,ArrayBuffer,Map};
import big2.model.{Card,Deck};

/**
 * @author AveryChoke
 */

object NewRoundActor {
  case class Start(playerActors:Map[String,ActorRef]);
  case class StartNewRound(cards:ArrayBuffer[Card], currentTurn:Int);
}

import NewRoundActor._;

class NewRoundActor extends Actor{
  
  override def receive = {
    case Start(playerActors) =>
      //shuffle the cards
      Deck.shuffle();
      
      //distribute the cards
      val handCards:Array[Buffer[Card]] = new Array(Big2Repository.players.length);
      for(i<-0 until handCards.length)
      {
        handCards(i) = Deck.cards.slice(i*13,(i+1)*13).sorted.toBuffer;
      }
      
      //immediately scramble the cards for security
      Deck.shuffle();
      
      //find who contain diamond 3 and start first
      val loop = new Breaks;
      loop.breakable{
        for(i <- 0 until handCards.length)
        {
          if(handCards(i).contains(Deck.d3Card))
          {
            Big2Repository.currentTurnIndex = i;
            loop.break;
          }
        }
      }
      
      //set players cards
      for(i<-0 until handCards.length)
      { 
        if(Big2Repository.myIndex == i) //is my cards
        {
          Big2Repository.players(i).cards = handCards(i);
        }
        else //other players card
        {
          //find host player actor
          val actor = playerActors.get(Big2Repository.players(i).name).get;
          actor ! StartNewRound(handCards(i).asInstanceOf[ArrayBuffer[Card]], Big2Repository.currentTurnIndex);
        }
      }
      sender ! "ACK";
  }
}