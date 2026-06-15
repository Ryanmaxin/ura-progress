object LazyClassMemberSource:
  var value = 1

class LazyClassMember:
  lazy val captured = LazyClassMemberSource.value // warn

object LazyClassMemberRoot:
  val member = new LazyClassMember
  val result = member.captured
