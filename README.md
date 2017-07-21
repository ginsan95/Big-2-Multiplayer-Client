# Big-2-Multiplayer-Client
This project is a multiplayer Big 2 game written in Scala and used the AKKA middleware framework. 
It is my assignment for the subject Distributed System during my undergraduate semester 7.

This project consists of 2 modules, where this is the client side, which is the game of the project.
The client will connect to the server and obtain the current list of available game rooms.
It allow the player to create a game room and host the particular game, or join a game room.
Once inside game room is full, the host can choose to start the game and the Big 2 game will begin.

For the server module, it can be found here: <https://github.com/ginsan95/Big-2-Multiplayer-Server>

This game only works for playing in a LAN. If you wish to play over the Internet, the host of the game room will have to perform port forwarding. The port is 6767 for communicating with the Server.

**Screenshots:**

Main menu:

![Screenshot 1](https://github.com/ginsan95/Big-2-Multiplayer-Client/blob/master/demo/screenshots/screenshot%201.jpg?raw=true)

Lobby:

![Screenshot 2](https://github.com/ginsan95/Big-2-Multiplayer-Client/blob/master/demo/screenshots/screenshot%202.jpg?raw=true)

Game room:

![Screenshot 3](https://github.com/ginsan95/Big-2-Multiplayer-Client/blob/master/demo/screenshots/screenshot%203.jpg?raw=true)

Gameplay:

![Screenshot 4](https://github.com/ginsan95/Big-2-Multiplayer-Client/blob/master/demo/screenshots/screenshot%204.jpg?raw=true)

Scoreboard:

![Screenshot 5](https://github.com/ginsan95/Big-2-Multiplayer-Client/blob/master/demo/screenshots/screenshot%205.jpg?raw=true)
