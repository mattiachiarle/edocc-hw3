# Homework 3
Author: Mattia Chiarle, mchiar2@uic.edu

## How to execute my program

I will report a list of all the steps needed to run my program locally.

    a. NetGameSim

    1a. Update application.conf
    2a. Generate the .ngs files. Save them in the desired location

    b. homework3

    1b. Run the server with sbt clean compile run
    2b. Run the client with sbt run (or interact with the server with any other method, such as Postman)

## How to deploy my program on AWS

[Link to the YouTube Video](https://youtu.be/ip3fU_ZaNCc)

Here you can find the list of commands I used in the video:

```bash
chmod 700 Homework3.pem
ssh -i Homework3.pem ec2-user@[IPv4 DNS record of the instance]

sudo yum install git
sudo yum update -y
sudo yum install java-11-amazon-corretto-devel -y
echo "export SBT_VERSION=1.9.7" >> ~/.bashrc
echo "export SBT_HOME=/usr/local/sbt" >> ~/.bashrc
echo "export PATH=\$PATH:\$SBT_HOME/bin" >> ~/.bashrc
source ~/.bashrc
sudo curl -L "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" -o /tmp/sbt-${SBT_VERSION}.tgz
sudo tar -xzvf /tmp/sbt-${SBT_VERSION}.tgz -C /usr/local

git clone https://github.com/mattiachiarle/edocc-hw3.git

cd edocc-hw3
sbt clean compile run
```

## API

Here you can find all the available APIs in my webserver, as well as useful request/response payload format.

    GET /api/game: start the game
    GET /api/status: check if me/the other has won/lost
    GET /api/nodes: get my nodes and the ones of the opponent
        Response: { policeman_node: ..., policeman_confidence: ..., policeman_neighbors: ..., thief_node: ..., thief_confidence: ..., thief_neighbors: ...}
    GET /api/{role}/valuableDistance: get the distance from the nearest valuable node
        role = policeman, thief
    POST /api/{role}/move: make a move
        role = policeman, thief
        Body: {next: int}, where int = node id to which we want to move

## Game logic

One of the two players starts the game by performing a GET request to /api/game. Then, the players will alternate their
turns. I left the turn handling on client side for an implementation choice, but it could be moved to server side too.
In each turn, the player checks the status of the game by performing a GET request to /api/status. Depending on the 
status code of the response, we can understand if someone won or if we can continue. In the latter case, we retrieve the current
situation with a GET request to /api/nodes, and based on that we decide our move, and we communicate it with a POST on
/api/{role}/move. Additionally, the thief (and the policeman, if he wants to) can ask for their distance from a valuable
node at any time.

## Server

The server logic is pretty straightforward. I defined the format of all the data exchanged with case classes, and I 
decided the routes to expose. Lastly, I start the server on port 9090. For all the logic, it relies on Game.

Currently, my server supports only one active game at a time, i.e. if we create a new game, we'll delete the old one.

## Game

Here, I implemented all the operations available through APIs. A new game instance is created after each GET on /api/game.
When it's created, it reads the graph files, and it assigns the policeman and the thief to random nodes.

In my implementation, I supposed that the users can move only to their successors. Possible improvements could be
to allow coming back to the predecessors, as well as understanding at the beginning of the game if the policeman can
reach the thief. I decided to remove the feature that made the thief automatically lose if he couldn't reach a valuable
node, because he can still win if the policeman doesn't have any valid move or performs an invalid move.

## Client

The client operates with the same logic described in the game logic section. I decided to use a quite rough strategy for
the move selection. Unless there's an evident best choice (a valuable node for the thief or the thief's node for the
policeman), we select it randomly. In fact, even when I was manually testing the game, I found myself performing random
moves, because the provided information doesn't allow the player to perform more advanced reasoning. The only information
that could be used is the confidence score. However, again, without any additional information I didn't find it useful,
and I didn't include it in the logic.

In the client, all the requests are blocking, i.e. we wait for them to be completed before moving on. This is achieved by
using Duration.Inf in the requests.

## Results

The results highly depend on the graph provided. My graphs have a high number of valuable nodes (44 out of 300), and 
thus it's more probable for the thief to win. However, I also encountered cases in which either the thief or the policeman
made an illegal move, as well as cases in which the thief couldn't reach any valuable node after some moves. To get a
more balanced game, it would be ideal to have few valuable nodes and a situation in which the policeman can reach the
thief.