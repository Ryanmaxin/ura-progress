package inittests.mutableReadInnerClass

object Source:
  class Box(var value: Int):
    val initial: Int = value

  val box: Box = new Box(0)

object Reader:
  val box: Source.Box = Source.box

  val stableRead: Int = box.initial
  val mutableRead: Int = box.value // old checker should warn
