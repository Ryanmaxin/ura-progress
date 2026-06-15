package inittests.directObjectCycle

object A:
  val value: Int = B.value

object B:
  val value: Int = A.value // old checker should warn
