package big2.controller

import big2.Application;
import big2.repository.Big2Repository;
import big2.model.{GameRoom,Player};
import big2.util.ActorUtil;

import akka.actor.{Actor,ActorRef,Props,Address,ActorSelection};
import akka.util.Timeout
import akka.pattern.ask
import akka.remote.{DisassociatedEvent};

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};


/**
 * @author AveryChoke
 */
object ClientActor {
  case object ClientSubscribeServer;
  case class ClientHostGame(roomName:String);
  case class ClientJoinRoom(roomName:String);
  case class ClientStartGame(roomName:String);
  case class ClientLeaveRoom(roomName:String);
  case object ClientGetHostActor;
  case class ClientReturnRoom(roomName:String);
}

import ClientActor._;
import ClientActorInterface._;
import ServerActorInterface._;
  
class ClientActor extends Actor{
  
  private var serverActor:ActorRef = null;
  
  override def receive = {
    case "START" => start();
    case ClientSubscribeServer => subscribeServer();
    case ClientUpdateRoom(rooms) => updateRoom(rooms);
    case ClientHostGame(roomName) => hostGame(roomName);
    case ClientJoinRoom(roomName) => joinRoom(roomName);
    case ClientStartGame(roomName) => startGame(roomName);
    case ClientLeaveRoom(roomName) => leaveRoom(roomName);
    case ClientGetHostActor => sendHostActor();
    case ClientReturnRoom(roomName) => returnRoom(roomName);
    
    //fault handling
    case DisassociatedEvent(localAddress, remoteAddress, inbound) =>
      serverDisconnected(remoteAddress);
  }
  
  private def start()
  {
    val portNum = Application.PORT_NUM;
    val serverAddress = Application.serverAddress;
    implicit val timeout: Timeout = 10.second;
    val realSender = sender;
    
    //get the remote server or connect to the server
    val serverFuture = context.actorSelection(
        s"akka.tcp://Big2System@$serverAddress:$portNum/user/ServerActor").resolveOne();
    
    //if success
    serverFuture.foreach {
      case actor:ActorRef => 
        //get the server actor
        serverActor = actor;
        //register and subscribe itself to the server
        serverActor ! ServerSubscribe;
        //tell sender true
        realSender ! true;
        
        //subscribe to error
        context.system.eventStream.subscribe(self, classOf[DisassociatedEvent]);
    }
    //if fail then end the actor
    serverFuture.failed.foreach {
      case _ =>
        //tell sender false
        realSender ! false;
        //kill myself
        context.stop(self);
    }
  }
  
  //subscribe to server to obtain updated rooms
  private def subscribeServer()
  {
    serverActor ! ServerSubscribe;
  }
  
  //update the list of available rooms
  private def updateRoom(rooms:Buffer[GameRoom])
  {
    Application.updateLobbyRoom(rooms);
  }
  
  //client want to host the game
  private def hostGame(roomName:String)
  {
    implicit val timeout: Timeout = 30.seconds;
    val realSender = sender;
    
    //inform server to host game
    val roomNameFuture = serverActor ? ServerHostGame(roomName, self);
    //get the changed room name and send back to sender
    roomNameFuture.foreach {
      case name:String => realSender ! name;
    }
  }
  
  //player want to join a room
  private def joinRoom(roomName:String)
  {
    implicit val timeout: Timeout = 20.seconds;
    val realSender = sender;
    
    //inform server to want to join that particular room
    val hostClientFuture = serverActor ? ServerJoinRoom(roomName, self);

    //get the host actor and send back to sender
    hostClientFuture.foreach {
      case hostClientActor:ActorRef =>
        //check if able to connect to the host client actor in case not in same LAN
        val clientFuture = context.actorSelection(hostClientActor.path).resolveOne(10.second);
        //if in same LAN
        clientFuture.foreach {
          case _ =>
            //ask the host client actor for the host actor
            val hostFuture = hostClientActor ? ClientGetHostActor;
            hostFuture.foreach {
              case hostActor:ActorRef =>
                //send the sender the host actor
                realSender ! hostActor;
            }
        }
        //if not in same LAN
        clientFuture.failed.foreach {
          case _ => 
            realSender ! "UNAVAILABLE";
            leaveRoom(roomName); //leave the unavailable room
        }
      case "UNAVAILABLE" => realSender ! "UNAVAILABLE";
    }
  }
  
  //the host start the game
  private def startGame(roomName:String)
  {
    //inform server that he started a game
    serverActor ! ServerStartGame(roomName);
  }
  
  //someone leave the room
  private def leaveRoom(roomName:String)
  {
    //inform server that he left the room
    serverActor ! ServerLeaveRoom(roomName);
  }
  
  //send the player the host actor
  private def sendHostActor()
  {
    implicit val timeout: Timeout = 10.second;
    val realSender = sender;
    
    val hostFuture = context.actorSelection("/user/HostActor").resolveOne();
    
    //if success
    hostFuture.foreach {
      case hostActor:ActorRef => 
        //send the player the host actor
        realSender ! hostActor;
    }
  }
  
  //inform server to make the game room visible
  private def returnRoom(roomName:String)
  {
    serverActor ! ServerReturnRoom(roomName);
  }
  
  //fault handling
  private def serverDisconnected(address:Address)
  {
    //check if disconnected address belong to server
    if(serverActor!=null && ActorUtil.compareActorByAddress(address, serverActor))
    {
      //hide the error that the server is dead by just not displaying any game rooms
      updateRoom(new ArrayBuffer[GameRoom]());
    }
  }
}