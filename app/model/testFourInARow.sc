package model

object test {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  val game = new SinkShipGame(6, "a", "b")        //> SinkShipGame.new, size: 6, players: a b
                                                  //| startPosX: 3
                                                  //| startPosY: 6
                                                  //| shipPos: List((3,6), (4,6))
                                                  //| ships.length: 0
                                                  //| startPosX: 4
                                                  //| startPosY: 4
                                                  //| shipPos: List((4,4), (4,5))
                                                  //| ships.length: 1
                                                  //| startPosX: 3
                                                  //| startPosY: 1
                                                  //| shipPos: List((3,1), (3,2), (3,3))
                                                  //| ships.length: 2
                                                  //| startPosX: 1
                                                  //| startPosY: 4
                                                  //| shipPos: List((1,4), (2,4), (3,4))
                                                  //| ships.length: 3
                                                  //| startPosX: 2
                                                  //| startPosY: 1
                                                  //| shipPos: List((2,1), (2,2), (2,3), (2,4), (2,5))
                                                  //| ships.length: 4
                                                  //| s, p: Set((1,4), (2,4), (3,4)) (2,4)
                                                  //| startPosX: 3
                                                  //| startPosY: 1
                                                  //| shipPos: List((3,1), (3,2), (3,3), (3,4), (3,5))
                                                  //| ships.length: 4
                                                  //| s, p: Set((3,1), (3,2), (3,3)) (3,1)
                                                  //| s, p: Set((3,1), (3,2), (3,3)) (3,2)
                                                  //| s, p: Set((3,1), (3,2), (3,3)) (3,3)
                                                  //| s, p: Set((1,4), (2,4), (3,4)) (3,4)
                                                  //| startPosX: 1
                                                  //| startPosY: 3
                                                  //| shipPos: List((1,3), (2,3), (3,3), (4,3), (5,3))
                                                  //| ships.length: 4
                                                  //| s, p: Set((3,1), (3,2), (3,3)) (3,3)
                                                  //| startPosX: 1
                                                  //| startPosY:
                                                  //| Output exceeds cutoff limit.
  game.shipPositions                              //> res0: Set[model.Ship] = Set(Set((3,1), (3,2), (3,3)), Set((1,4), (2,4), (3,4
                                                  //| )), Set((4,4), (4,5)), Set((6,4), (6,1), (6,2), (6,5), (6,3)), Set((3,6), (4
                                                  //| ,6)))
  
}