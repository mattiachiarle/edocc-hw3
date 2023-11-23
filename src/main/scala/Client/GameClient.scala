package Client

import Server._
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.json.enrichAny

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.sys.exit

object GameClient extends App with JsonSupport {
  implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext = system.executionContext
  val logger = LoggerFactory.getLogger(getClass)
  var turn = "thief"
  var valuableDistance = 0
  var move = -1
  private val rand = new scala.util.Random
  val config = ConfigFactory.load()
  val url = config.getString("Server.address")


  val gameRequest = Http().singleRequest(HttpRequest(uri = s"$url/api/game"))
  val gameResponse = Await.result(gameRequest, Duration.Inf) // Blocking to get the game response synchronously

  gameResponse.status match {
    case StatusCodes.OK =>
      // The game is active
      while (true) {
        val statusRequest = Http().singleRequest(HttpRequest(uri = s"$url/api/status"))
        val statusResponse = Await.result(statusRequest, Duration.Inf) // Blocking to get the status response synchronously

        val continue = checkWinner(statusResponse)
        if (continue != 0) {
          exit(continue)
        }

        move = -1

        if (turn == "thief") {
          val distanceRequest = Http().singleRequest(HttpRequest(uri = s"$url/api/thief/valuableDistance"))
          val distanceResponse = Await.result(distanceRequest, Duration.Inf)

          val continue = Await.result(Unmarshal(distanceResponse).to[ValuableDistance], Duration.Inf)
//          if (continue.distance == -1) {
//            logger.warn("The thief can't reach any valuable node, so we'll end")
//            exit(0)
//          }
          valuableDistance = continue.distance
          logger.info(s"Valuable distance: $valuableDistance")
        }

        val neighborsRequest = Http().singleRequest(HttpRequest(uri = s"$url/api/nodes"))
        val neighborsResponse = Await.result(neighborsRequest, Duration.Inf)

        val situation = Await.result(Unmarshal(neighborsResponse).to[NodePosition], Duration.Inf)
        logger.info(situation.toString)

        if (turn == "thief") {
          if (valuableDistance == 1) {
            move = situation.thief_neighbors.filter(n => n.valuableData).head.id
          } else {
            logger.info(s"Successors: ${situation.thief_neighbors}")
            if(situation.thief_neighbors.isEmpty){
              move = -1
            }
            else {
              val index = rand.nextInt(situation.thief_neighbors.length)
              move = situation.thief_neighbors(index).id
            }
          }
          logger.info(s"The thief moves from ${situation.thief_node.id} to $move")
//          turn = "policeman"
        } else {
          val successors = situation.policeman_neighbors.map(n => n.id)
          logger.info(s"Successors: $successors")
          move = if (successors.contains(situation.thief_node.id)) {
            situation.thief_node.id
          } else {
            if(situation.policeman_neighbors.isEmpty){
              -1
            }
            else {
              val index = rand.nextInt(situation.policeman_neighbors.length)
              situation.policeman_neighbors(index).id
            }
          }
          logger.info(s"The policeman moves from ${situation.policeman_node.id} to $move")
//          turn = "thief"
        }

        val moveParameter = Move(move)
        val moveRequest = HttpRequest(
          method = HttpMethods.POST,
          uri = s"$url/api/$turn/move",
          entity = HttpEntity(ContentTypes.`application/json`, moveParameter.toJson.toString)
        )
        val moveResponse = Await.result(Http().singleRequest(moveRequest), Duration.Inf)
        moveResponse.status match {
          case StatusCodes.OK =>
            logger.info("Move registered correctly")
          case _ =>
            logger.error(s"Unexpected response from the server: ${moveResponse.status}")
        }
        if(turn == "thief"){
          turn = "policeman"
        }
        else{
          turn = "thief"
        }
      }

    case _ => sys.error("Something went wrong with the initial game request")
  }

  def checkWinner(res: HttpResponse) : Int = {
    if (res._1.intValue() == 201) {
      logger.info("The policeman won")
      return 1
    }
    if (res._1.intValue() == 202) {
      logger.info("The thief won")
      return 1
    }
    if(res.status==StatusCodes.OK){
      logger.info("The game goes on")
      return 0
    }
    -1
  }
}
