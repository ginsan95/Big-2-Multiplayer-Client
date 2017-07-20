package big2.model

import scala.collection.mutable.{Set,HashSet};
import akka.actor.ActorRef;

/**
 * @author AveryChoke
 */
class GameRoom(private var _roomName:String, private var _hostClientActor:ActorRef) extends Serializable
{
  val MAX_ROOM_COUNT = 4;
  private var _visible:Boolean = true;
  
  //list of actors
  val playerActors:Set[ActorRef] = new HashSet();
  //add the host into the actor
  playerActors += hostClientActor;
  
  //the player count is the size of the actor
  def playerCount:Int = playerActors.size;
  
  //the availability of the room
  def available:Boolean = playerCount<MAX_ROOM_COUNT && visible;
  
  override def toString():String =
  {
    return s"$roomName ($playerCount / $MAX_ROOM_COUNT)";
  }
  
  //get set
  def roomName:String = _roomName;
  def roomName_= (value:String){ _roomName=value }
  
  def hostClientActor:ActorRef = _hostClientActor;
  def hostClientActor_= (value:ActorRef){ _hostClientActor=value }
  
  def visible:Boolean = _visible;
  def visible_= (value:Boolean){ _visible=value }
}