package Server

import NetGraphAlgebraDefs.NodeObject
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val nodeObjectFormat: RootJsonFormat[NodeObject] = jsonFormat(
    NodeObject.apply,
    "id",
    "children",
    "props",
    "currentDepth",
    "propValueRange",
    "maxDepth",
    "maxBranchingFactor",
    "maxProperties",
    "storedValue",
    "valuableData"
  )
  implicit val positionFormat: RootJsonFormat[NodePosition] = jsonFormat6(NodePosition.apply)
  implicit val valuableDistanceFormat: RootJsonFormat[ValuableDistance] = jsonFormat1(ValuableDistance.apply)
  implicit val moveFormat: RootJsonFormat[Move] = jsonFormat1(Move.apply)
}
