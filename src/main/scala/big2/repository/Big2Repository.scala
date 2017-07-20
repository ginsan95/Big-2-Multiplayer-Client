package big2.repository

import big2.model._;

import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};
import akka.actor._

/**
 * @author AveryChoke
 */

object GameState extends Enumeration
{
    type GameState = Value;
    val GAME_ROOM, GAMEPLAY, SCORE_BOARD = Value
}

import GameState._;

object Big2Repository {
  
  val STARTING_SCORE = 500;
  
  //the game state
  private var _gameState:GameState = null;
  //the game room name
  private var _roomName:String = "Game Room";
  //buffer of players
  private var _players:Buffer[Player] = new ArrayBuffer[Player](); 
  //the player index in the buffer
  private var _myIndex:Int = 0;
  //current turn player index
  private var _currentTurnIndex:Int = 0;
  //the cards on the game table
  private var _tableCards:Buffer[Card] = new ArrayBuffer[Card]();
  //amount of passes
  private var _passCount = Integer.MAX_VALUE;
  //player who get penalty
  private var _penaltyIndex:Int = -1;
  //winner player index
  private var _winnerIndex:Int = -1;
  //the winning message - indicate how the player win
  private var _winningMsg:String = "";
  //boolean that shows whether there is already a scoreboard action - replay or return room
  private var noScoreboardAction = true;
  
  def initialize(plays:Buffer[Player], index:Int)
  {
    players = plays;
    myIndex = index;
  }
  
  //reset all the round data of the repository
  def resetRound()
  {
    gameState = GAMEPLAY;
    for(player <- players)
    {
      player.cardsAmount = 13;
    }
    myPlayer.selectedCards.clear();
    tableCards.clear();
    passCount = Integer.MAX_VALUE;
    penaltyIndex = -1;
    noScoreboardAction = true;
  }
  
  //reset the score when go back to game room
  def resetScore()
  {
    for(player <- players)
    {
      player.score = STARTING_SCORE;
    }
  }
  
  //for host to add player into the room
  def addPlayer(index:Int, player:Player)
  {
    players.insert(index,player);
  }
  
  //return the player himself
  def myPlayer:Player = players(myIndex);
  
  //return the current player
  def currentPlayer:Player = players(currentTurnIndex);
  
  //return the next player
  def nextPlayer:Player = players(nextPlayerIndex(currentTurnIndex,1));
  
  //return the previous player
  def prePlayer:Player = players(prePlayerIndex(currentTurnIndex,1));
  
  //get the next player index
  def nextPlayerIndex(index:Int, count:Int):Int =
  {
    var nextIndex = index+count;
    if(nextIndex>=players.length)
    {
      nextIndex -= (players.length);
    }
    return nextIndex;
  }
  
  //get the previous player index
  def prePlayerIndex(index:Int, count:Int):Int =
  {
    var preIndex = index-count;
    if(preIndex<0)
    {
      preIndex += players.length;
    }
    return preIndex;
  }
  
  //check if is my turn
  def isMyTurn:Boolean =
  {
    return myIndex==currentTurnIndex;
  }
  
  //check if all the other player passed
  def isAllPass:Boolean =
  {
    return passCount >= (players.length-1);
  }
  
  def isAllReady:Boolean =
  {
    for(i <- 0 until players.length)
    {
      if(i!=myIndex && !players(i).isReady)
      {
        return false;
      }
    }
    return true;
  }
  
  def checkNoScoreboardAction():Boolean = synchronized {
    if(noScoreboardAction)
    {
      noScoreboardAction = false;
      return true;
    }
    else
    {
      return false;
    }
  }
  
  //get set
  def gameState:GameState = _gameState;
  def gameState_= (value:GameState){ _gameState=value }
  
  def roomName:String = _roomName;
  def roomName_= (value:String){ _roomName=value }
  
  def players:Buffer[Player] = _players;
  def players_= (value:Buffer[Player]){ _players=value }
  
  def myIndex:Int = _myIndex;
  def myIndex_= (value:Int){ _myIndex=value }
  
  def currentTurnIndex:Int = _currentTurnIndex;
  def currentTurnIndex_= (value:Int){ _currentTurnIndex=value }
  
  def tableCards:Buffer[Card] = _tableCards;
  def tableCards_= (value:Buffer[Card]){ _tableCards=value }
  
  def passCount:Int = _passCount;
  def passCount_= (value:Int){ _passCount=value }
  
  def penaltyIndex:Int = _penaltyIndex;
  def penaltyIndex_= (value:Int){ _penaltyIndex=value }
  
  def winnerIndex:Int = _winnerIndex;
  def winnerIndex_= (value:Int){ _winnerIndex=value }

  def winningMsg:String = _winningMsg;
  def winningMsg_= (value:String){ _winningMsg=value }
}