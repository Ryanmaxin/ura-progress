object ClosureProbeA:
  var value = 1

object ClosureProbeB:
  val read = () => ClosureProbeA.value // warn
  val alias = read
  val result = alias()

object ClosureProbeC:
  val neverCalled = () => ClosureProbeA.value
