package inittests.mutableReadDirect

class Box(var value: Int)

object Source:
  val box: Box = new Box(4)

object Reader:
  val localBox: Box = new Box(5)
  val remoteBox: Box = Source.box

  val localRead: Int = localBox.value
  val remoteRead: Int = remoteBox.value // old checker should warn
