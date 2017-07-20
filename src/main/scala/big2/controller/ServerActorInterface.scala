package big2.controller

import akka.actor.ActorRef;

/**
 * @author AveryChoke
 */
object ServerActorInterface {
  case object ServerSubscribe;
  case class ServerHostGame(roomName:String, clientActor:ActorRef);
  case class ServerJoinRoom(roomName:String, clientActor:ActorRef);
  case class ServerStartGame(roomName:String);
  case class ServerLeaveRoom(roomName:String);
  case class ServerReturnRoom(roomName:String);
}