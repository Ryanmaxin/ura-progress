object LazyCycleA {
  lazy val y: String = LazyCycleB.x
}

object LazyCycleB { // warn
  val x: String = "a"
  val z: String = LazyCycleA.y
}
