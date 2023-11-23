package Server

import NetGraphAlgebraDefs.NodeObject

case class NodePosition(policeman_node: NodeObject, policeman_confidence: Float, policeman_neighbors: List[NodeObject], thief_node: NodeObject, thief_confidence: Float, thief_neighbors: List[NodeObject])
