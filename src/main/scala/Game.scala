import GameServer.getClass
import NetGraphAlgebraDefs._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.collection.convert.ImplicitConversions.{`collection AsScalaIterable`, `set asScala`}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Game {

  val config = ConfigFactory.load()

  val logger = LoggerFactory.getLogger(getClass)

  logger.debug("Loading the graphs...")

  private val original : NetGraph = NetGraph.load(config.getString("Graphs.fileName"),"./").get
  private val perturbed : NetGraph = NetGraph.load(s"${config.getString("Graphs.fileName")}.perturbed","./").get

  logger.debug("Graph load completed")

  private val rand = new scala.util.Random
  private val originalLength = original.sm.nodes().size()

  private val perturbedId = perturbed.sm.nodes().map(n => n.id)

  private var policemanId : Int = -1

  while(!perturbedId.contains(policemanId)){
    policemanId = rand.nextInt(originalLength)
  }

  logger.info(s"The policeman will start from node $policemanId")

  private var thiefId: Int = -1

  while (!perturbedId.contains(thiefId)) {
    thiefId = rand.nextInt(originalLength)
  }

  logger.info(s"The thief will start from node $thiefId")

  var policeman : NodeObject = original.sm.nodes().filter(_.id == policemanId).head
  var thief : NodeObject = original.sm.nodes().filter(_.id == thiefId).head

  def checkWinner(): Int = {
    if(policeman == null){
      logger.info("The policeman is null")
      return 2
    }
    if(thief==null){
      logger.info("The thief is null")
      return 1
    }
    if(policeman.id == thief.id){
      logger.info("The policeman reached the thief")
      return 1
    }
    if(thief.valuableData){
      logger.info("The thief found valuable data")
      return 2
    }
    logger.info("The game proceeds")
    0
  }

  def getNodes() : NodePosition = {
    val original_policeman = original.sm.successors(policeman)
    var policeman_confidence = 0
    val policeman_neighbors : ArrayBuffer[NodeObject] = mutable.ArrayBuffer[NodeObject]()
//    perturbed.sm.predecessors(policeman).forEach(n => {
//      if(original_policeman.contains(n)){
//        policeman_confidence += 1
//      }
//      policeman_neighbors += n
//    })
    val perturbed_policeman = perturbed.sm.nodes().filter(n => n.id == policeman.id).head
    val perturbed_thief = perturbed.sm.nodes().filter(n => n.id == thief.id).head

    perturbed.sm.successors(perturbed_policeman).forEach(n => {
      if (original_policeman.contains(n)) {
        logger.info(s"The policeman's neighbor ${n.id} exists also in the original graph")
        policeman_confidence += 1
      }
      policeman_neighbors += n
    })

    val original_thief = original.sm.successors(thief)
    var thief_confidence = 0
    val thief_neighbors: ArrayBuffer[NodeObject] = mutable.ArrayBuffer[NodeObject]()
//    perturbed.sm.predecessors(thief).forEach(n => {
//      if (original_thief.contains(n)) {
//        thief_confidence += 1
//      }
//      thief_neighbors += n
//    })
    perturbed.sm.successors(perturbed_thief).forEach(n => {
      if (original_thief.contains(n)) {
        logger.info(s"The thief's neighbor ${n.id} exists also in the original graph")
        thief_confidence += 1
      }
      thief_neighbors += n
    })

   NodePosition(policeman, policeman_confidence.toFloat/policeman_neighbors.size.toFloat,policeman_neighbors.toList,
     thief, thief_confidence.toFloat/thief_neighbors.size.toFloat,thief_neighbors.toList)
  }

  def getValuableDistance(role: String): ValuableDistance = {
    var node : NodeObject = null
    if(role == "policeman"){
      node = policeman
    }
    else{
      node = thief
    }
    var distance = 1
    val perturbed_node = perturbed.sm.nodes().filter(n => n.id == node.id).head
    var neighbors = perturbed.sm.successors(perturbed_node).toSet
    while(true){
      var tmp : Set[NodeObject] = Set[NodeObject]()
      neighbors.foreach(n => {
        if(n.valuableData){
          logger.info("We found valuable data!")
          return ValuableDistance(distance)
        }
        val succ = perturbed.sm.successors(n)
        if(succ != null) {
          tmp = tmp.union(succ)
        }
      })
      if(tmp.isEmpty){
        logger.warn(s"The $role can't reach any valuable node!")
        return ValuableDistance(-1)
      }
      distance += 1
      neighbors = tmp
    }
    ValuableDistance(-1)
  }

  def makeMove(role: String, next: Int): Unit = {
    var node : NodeObject = null
    if (role == "policeman") {
      node = policeman
    }
    else {
      node = thief
    }

    val successors = original.sm.successors(node).map(n => n.id)
    if(successors.contains(next)){
      node = original.sm.nodes().filter(n => n.id == next).head
      if (role == "policeman") {
        policeman = node
      }
      else {
        thief = node
      }
    }
    else{
      logger.error(s"Illegal mode for the $role!")
      if (role == "policeman") {
        policeman = null
      }
      else {
        thief = null
      }
    }
  }
}
