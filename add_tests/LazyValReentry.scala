object LazyValReentry {
  lazy val x: String = y
  lazy val y: String = x // warn
  val z: String = x
}
