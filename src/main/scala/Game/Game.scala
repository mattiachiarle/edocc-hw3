package Game

import NetGraphAlgebraDefs._
import Server.{NodePosition, ValuableDistance}
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.convert.ImplicitConversions.`set asScala`
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class Game {

  val config: Config = ConfigFactory.load()

  val logger: Logger = LoggerFactory.getLogger(getClass)

  logger.debug("Loading the graphs...")

  private val original : NetGraph = NetGraph.load(config.getString("Graphs.fileName"),config.getString("Graphs.dir")).get //Get is used to directly obtain the graph instead of Option
  private val perturbed : NetGraph = NetGraph.load(s"${config.getString("Graphs.fileName")}.perturbed",config.getString("Graphs.dir")).get

  logger.debug("Graph load completed")

  private val rand = new scala.util.Random
  private val originalLength = original.sm.nodes().size()

  private val perturbedId = perturbed.sm.nodes().map(n => n.id)

  private var policemanId : Int = -1

  while(!perturbedId.contains(policemanId)){  //We place the policeman on a node that's present in both the original and perturbed graph
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

  /**
   * Function to check if someone won
   * @return 0 if nobody won, 1 if the policeman won, 2 if the thief won
   */
  def checkWinner(): Int = {
    if(policeman == null){ //The policeman made an illegal move
      logger.info("The policeman is null")
      return 2
    }
    if(thief==null) { //The thief made an illegal move
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

  def getNodes(): NodePosition = {
    val original_policeman = original.sm.successors(policeman)
    var policeman_confidence = 0
    val policeman_neighbors : ArrayBuffer[NodeObject] = mutable.ArrayBuffer[NodeObject]()
    //Uncomment it to include also the predecessors
    /*perturbed.sm.predecessors(policeman).forEach(n => {
      if(original_policeman.contains(n)){
        policeman_confidence += 1
      }
      policeman_neighbors += n
    })*/
    val perturbed_policeman = perturbed.sm.nodes().filter(n => n.id == policeman.id).head //We must retrieve it in this way since the perturbed node could be different even if it has the same id
    val perturbed_thief = perturbed.sm.nodes().filter(n => n.id == thief.id).head

    perturbed.sm.successors(perturbed_policeman).forEach(n => {
      if (original_policeman.contains(n)) {
        logger.debug(s"The policeman's neighbor ${n.id} exists also in the original graph")
        policeman_confidence += 1
      }
      policeman_neighbors += n
    })

    val original_thief = original.sm.successors(thief)
    var thief_confidence = 0
    val thief_neighbors: ArrayBuffer[NodeObject] = mutable.ArrayBuffer[NodeObject]()
    //Uncomment it to include also the predecessors
    /*perturbed.sm.predecessors(thief).forEach(n => {
      if (original_thief.contains(n)) {
        thief_confidence += 1
      }
      thief_neighbors += n
    })*/
    perturbed.sm.successors(perturbed_thief).forEach(n => {
      if (original_thief.contains(n)) {
        logger.debug(s"The thief's neighbor ${n.id} exists also in the original graph")
        thief_confidence += 1
      }
      thief_neighbors += n
    })

   NodePosition(policeman, policeman_confidence.toFloat/policeman_neighbors.size.toFloat,policeman_neighbors.toList,
     thief, thief_confidence.toFloat/thief_neighbors.size.toFloat,thief_neighbors.toList)
  }

  /**
   * Get the distance from a valuable node
   * @param role either the policeman or the thief, i.e. for whom we perform the check
   * @return -1 if there's no valuable node reachable, the distance otherwise
   */
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
          tmp = tmp.union(succ) //We perform the union of all successors, that will be used in the next iteration
        }
      })
      if(tmp.isEmpty){ //There's not successor available
        logger.warn(s"The $role can't reach any valuable node!")
        return ValuableDistance(-1)
      }
      distance += 1
      neighbors = tmp
    }
    ValuableDistance(-1)
  }

  /**
   * Function used to perform a move
   * @param role who's performing the move
   * @param next the id of the next step
   */
  def makeMove(role: String, next: Int): Unit = {
    var node : NodeObject = null
    if (role == "policeman") {
      node = policeman
    }
    else {
      node = thief
    }

    val successors = original.sm.successors(node).map(n => n.id)
    if(successors.contains(next)){ //We update the variable
      node = original.sm.nodes().filter(n => n.id == next).head
      if (role == "policeman") {
        policeman = node
      }
      else {
        thief = node
      }
    }
    else{ //Illegal move, so we assign to the node null
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
