
object A {
  lazy val y: String = B.x
}

object B {
  val x: String = "a"
  val z: String = A.y
}

object Main {
  def main(args: Array[String]): Unit = {
    println(A.y)
  }
}
