package inittests.methodObjectCycle

object A:
  val value: Int = B.compute()

object B:
  def compute(): Int = A.value + 1 // old checker should warn
