package big2.controller

import big2.model.GameRoom;
import scala.collection.mutable.Buffer;

/**
 * @author AveryChoke
 */
object ClientActorInterface {
  case class ClientUpdateRoom(rooms:Buffer[GameRoom]);
}