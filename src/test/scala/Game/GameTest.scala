package Game

import NetGraphAlgebraDefs.NodeObject
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GameTest extends AnyFlatSpec with Matchers {
  behavior of "Check winner"

  it should "provide 2 if the policeman is null" in {
    val game = new Game()
    game.policeman = null

    game.checkWinner() shouldEqual 2
  }

  it should "provide 1 if the thief is null" in {
    val game = new Game()
    game.thief = null

    game.checkWinner() shouldEqual 1
  }

  it should "provide 1 if the policeman is on the same node as the thief" in {
    val game = new Game()
    game.policeman = game.thief

    game.checkWinner() shouldEqual 1
  }

  it should "provide 2 if the thief is on a valuable node" in {
    val game = new Game()
    game.thief = new NodeObject(1, 1, 1, 1, 1, 1, 1, 1, 2.0, true)

    game.checkWinner() shouldEqual 2
  }

  it should "provide 0 if the thief is not on a valuable node and the policeman is on a different node" in {
    val game = new Game()
    game.thief = new NodeObject(1, 1, 1, 1, 1, 1, 1, 1, 2.0, false)
    game.policeman = new NodeObject(2, 1, 1, 1, 1, 1, 1, 1, 2.0, true)

    game.checkWinner() shouldEqual 0
  }
}
