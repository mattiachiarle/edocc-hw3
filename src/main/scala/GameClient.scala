import Main.getClass
import NetGraphAlgebraDefs.NodeObject
import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.slf4j.LoggerFactory
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.sys.exit
import scala.util.{Failure, Success}

//case class NodePosition(policeman_node: NodeObject, policeman_confidence: Float, policeman_neighbors: List[NodeObject], thief_node: NodeObject, thief_confidence: Float, thief_neighbors: List[NodeObject])
//case class ValuableDistance(distance: Int)
//case class Move(next: Int)
//
//trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
//  implicit val nodeObjectFormat: RootJsonFormat[NodeObject] = jsonFormat(
//    NodeObject.apply,
//    "id",
//    "children",
//    "props",
//    "currentDepth",
//    "propValueRange",
//    "maxDepth",
//    "maxBranchingFactor",
//    "maxProperties",
//    "storedValue",
//    "valuableData"
//  )
//  implicit val positionFormat: RootJsonFormat[NodePosition] = jsonFormat6(NodePosition.apply)
//  implicit val valuableDistanceFormat: RootJsonFormat[ValuableDistance] = jsonFormat1(ValuableDistance.apply)
//  implicit val moveFormat: RootJsonFormat[Move] = jsonFormat1(Move.apply)
//}

object GameClient extends App with JsonSupport {
  implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
  implicit val executionContext = system.executionContext
  val logger = LoggerFactory.getLogger(getClass)
  var turn = "thief"
  var valuableDistance = 0
  var move = -1
  private val rand = new scala.util.Random

  val gameRequest = Http().singleRequest(HttpRequest(uri = "http://localhost:9090/api/game"))
  val gameResponse = Await.result(gameRequest, Duration.Inf) // Blocking to get the game response synchronously

  gameResponse.status match {
    case StatusCodes.OK =>
      // The game is active
      while (true) {
        val statusRequest = Http().singleRequest(HttpRequest(uri = "http://localhost:9090/api/status"))
        val statusResponse = Await.result(statusRequest, Duration.Inf) // Blocking to get the status response synchronously

        val continue = checkWinner(statusResponse)
        if (continue != 0) {
          exit(continue)
        }

        move = -1

        if (turn == "thief") {
          val distanceRequest = Http().singleRequest(HttpRequest(uri = "http://localhost:9090/api/thief/valuableDistance"))
          val distanceResponse = Await.result(distanceRequest, Duration.Inf)

          val continue = Await.result(Unmarshal(distanceResponse).to[ValuableDistance], Duration.Inf)
//          if (continue.distance == -1) {
//            logger.warn("The thief can't reach any valuable node, so we'll end")
//            exit(0)
//          }
          valuableDistance = continue.distance
          logger.info(s"Valuable distance: $valuableDistance")
        }

        val neighborsRequest = Http().singleRequest(HttpRequest(uri = s"http://localhost:9090/api/nodes"))
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
          uri = s"http://localhost:9090/api/$turn/move",
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
