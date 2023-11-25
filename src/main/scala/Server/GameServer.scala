package Server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import org.slf4j.LoggerFactory
import Game._

import scala.concurrent.ExecutionContext.Implicits.global

object GameServer extends App with JsonSupport {

  private var game: Game = _
  val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem = ActorSystem("GameServer")
  val route = concat(
    get {
      pathPrefix("api" / "game") {
        logger.info("Creating new game...")
        game = new Game()
        logger.info("Game created!")
        complete(StatusCodes.OK)
      }
    },
    get {
      pathPrefix("api" / "status") {
        val winner = game.checkWinner()
        if (winner == 1) {
          complete(StatusCodes.custom(201, "", "The policeman won"))
        }
        else if (winner == 2) {
          complete(StatusCodes.custom(202, "", "The thief won"))
        }
        else {
          complete(StatusCodes.custom(200, "", "Nobody won yet!"))
        }
      }
    },
    get {
      pathPrefix("api" / "nodes") {
        logger.debug("Getting the current situation...")
        val situation = game.getNodes()
        logger.debug("Situation retrieved")
        complete(situation)
      }
    },
    get {
      pathPrefix("api" / Segment / "valuableDistance") {
        role => {
          val dist: ValuableDistance = game.getValuableDistance(role)
          val msg: String = s"The current distance from a valuable node for $role is ${dist.distance}"
          logger.info(msg)
          complete(dist)
        }
      }
    },
    post {
      pathPrefix("api" / Segment / "move") {
        role => {
          entity(as[Move]) { move =>
            val msg: String = s"The $role wants to move to ${move.next}"
            logger.info(msg)
            game.makeMove(role, move.next)
            complete(StatusCodes.OK)
          }
        }
      }
    })

  val server = Http().newServerAt("0.0.0.0", 9090).bind(route)
  server.map { _ =>
    logger.info("Successfully started on localhost:9090 ")
  } recover { case ex =>
    logger.error("Failed to start the server due to: " + ex.getMessage)
  }
}
