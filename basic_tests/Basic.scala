@main def Basic(): Unit =
  val name = "Scala"
  println(Greeter.message(name))

object Greeter:
  def message(name: String): String =
    s"Hello, $name!"
