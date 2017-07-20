package big2.util

import akka.actor._;
import scala.collection.mutable.{Buffer,Iterable};

/**
 * @author AveryChoke
 */
object ActorUtil {
  
  def searchActorRefByAddress(address:Address, actors:Iterable[ActorRef]):Option[ActorRef] =
  {
    for(actor <- actors)
    {
      if(compareActorByAddress(address, actor))
      {
        return Some(actor);
      }
    }
    return Option(null);
  }
  
  def compareActorByAddress(address:Address, actor:ActorRef):Boolean =
  {
    return actor.path.address == address;
  }
  
}