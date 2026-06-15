# Lazy Global Initialization Cycle

## The Repro

```scala
object A {
  lazy val y: String = B.x
}

object B {
  val x: String = "a"
  val z: String = A.y
}

object Main {
  def main(args: Array[String]): Unit =
    println(A.y)
}
```

With `-Ysafe-init-global`, the init checker should warn here because forcing `A.y`
can enter object `B`, and `B`'s strict initializer reaches back to `A.y`.

## Runtime Sequence

The problematic execution is:

1. `Main` reads `A.y`.
2. `A.y` starts lazy initialization.
3. The RHS of `A.y` reads `B.x`.
4. Reading `B.x` triggers initialization of object `B`.
5. `B.x` initializes successfully.
6. `B.z` initializes and reads `A.y`.
7. `A.y` is already being initialized.

The key detail is that `A` the object and `A.y` the lazy val have different
initialization states. Accessing `A.y` first ensures the module `A` exists, but
the RHS of `A.y` still runs later as its own lazy-val initialization. At the
dangerous point, `A` may already be constructed; the active initializer is
`A.y`.

So the real cycle is:

```text
object B -> object A.y -> object B
```

or, from the original first force:

```text
A.y -> B -> A.y
```

This is not resolvable just because `B.x` appears before `B.z`. Reading `B.x`
requires entering object `B`'s initialization, and object `B` is not complete
until its whole body finishes, including `z`. But `z` needs `A.y`, which is the
lazy val currently waiting for `B`.

## Why The Old Checker Missed It

The global init checker already had a stack for objects currently being checked:

```scala
checkingObjects: ArrayBuffer[ObjectRef]
```

This stack catches ordinary object cycles like:

```scala
object A { val a = B.b }
object B { val b = A.a }
```

because checking `A` pushes `A`, checking `B` pushes `B`, and reading `A` again
finds `A` already on the object stack.

Lazy vals were different.

During object body initialization, the checker intentionally skips lazy vals:

```scala
case vdef: ValDef if !vdef.symbol.is(Flags.Lazy) && !vdef.rhs.isEmpty =>
```

That part is correct. A lazy val RHS does not run when the object itself is
constructed. Eagerly evaluating lazy vals in `init` would create false warnings
and would model lazy vals as strict vals.

The bug was in the later path, when a lazy val is actually selected. In `select`,
the checker handled:

```scala
if target.is(Flags.Lazy) then
```

and, for an uninitialized lazy val, directly evaluated the RHS:

```scala
val rhs = target.defTree.asInstanceOf[ValDef].rhs
val result = eval(rhs, ref, target.owner.asClass, cacheResult = true)
ref.initVal(target, result)
```

But this did not record that "`A.y` is currently initializing".

So when checking `B.z = A.y`, the checker evaluated `A.y`'s RHS (`B.x`) while the
object stack still looked like:

```text
B
```

It did not look like:

```text
B -> A.y
```

When the RHS touched `B.x`, the checker saw that `B` was already the current
object and treated it like a harmless self-access. It had no representation for
the fact that this self-access occurred through an active lazy-val initialization.

## Why Not Just Analyze Lazy Vals In Object Initialization?

Removing the lazy filter from the object body walk would be wrong.

For example:

```scala
object A {
  lazy val y = B.x
}
```

The RHS `B.x` does not run when `A` is initialized. It only runs when `A.y` is
first forced. If the checker analyzed this RHS during `A` initialization, it
would report many false positives and would store an abstract value for `A.y`
even though the runtime lazy val is still uninitialized.

The right place to model lazy initialization is the lazy-select path, not the
object-body initialization path.

Similarly, pushing `A` onto `checkingObjects` while forcing `A.y` would be the
wrong model. It would pretend object `A` is being initialized again, when the
runtime state is specifically "lazy field `A.y` is being initialized". The
checker also needs to distinguish `A.x` from `A.y`; an object-only stack cannot
tell `A.x -> A.y` apart from `A.x -> A.x`.

## The Fix

The fix adds a small separate stack for lazy vals that are currently being
evaluated:

```scala
private case class LazyValRef(obj: ObjectRef, field: Symbol, objectStackDepth: Int)
private[State] val checkingLazyVals = new mutable.ArrayBuffer[LazyValRef]
```

Each lazy frame records:

- `obj`: the object that owns the lazy val, for example `A`
- `field`: the lazy field symbol, for example `y`
- `objectStackDepth`: how deep the object-initialization stack was when the lazy
  val was forced

The checker now wraps object lazy-val RHS evaluation:

```scala
State.withLazyVal(ref.asObjectRef, target) {
  eval(rhs, ref, target.owner.asClass, cacheResult = true)
}
```

This means that while evaluating `A.y`'s RHS, the checker knows `A.y` is active.

## What The New Checks Detect

### Direct Lazy Re-entry

Before evaluating a lazy val RHS, the checker calls:

```scala
State.checkLazyValAccess(ref.asObjectRef, target)
```

If the same lazy val is already in `checkingLazyVals`, it reports:

```text
Cyclic lazy value initialization: object A.y -> object A.y
```

This catches direct or indirect lazy-val recursion.

### Object Cycle Through A Lazy Val

The repro needs a second check. The repeated thing is not simply "select `A.y`
again" from the same static analysis path. The checker is checking `B`, then
temporarily evaluates `A.y`, then reaches back to `B`.

When `checkObjectAccess` finds that the requested object is already on
`checkingObjects`, it now also asks whether that access happened through a lazy
val frame that was pushed after the object frame:

```scala
checkObjectAccessThroughLazyVal(clazz, index)
```

For the repro:

```text
checkingObjects    = [B]
checkingLazyVals   = [A.y, objectStackDepth = 1]
accessed object    = B
objectStackIndex   = 0
```

Since `A.y` was pushed after `B` entered the object stack, touching `B` again is
reported as:

```text
Cyclic initialization: object B -> object A.y -> object B.
```

## Why This Is Minimal

The fix does not change how normal object initialization is checked.

It also does not eagerly analyze lazy vals when an object body is initialized.
Lazy vals are still skipped during `init`, as before.

The only new behavior is:

1. When an object lazy val is actually forced, track it while its RHS is being
   evaluated.
2. If that active lazy val re-enters itself, warn.
3. If that active lazy val reaches back into an object already being initialized,
   warn.

## Regression Tests

The object-through-lazy regression test is:

```text
tests/init-global/warn/lazy-global-cycle.scala
```

It expects the warning:

```text
Cyclic initialization: object LazyCycleB -> object LazyCycleA.y -> object LazyCycleB.
```

The direct lazy-val re-entry regression test is:

```text
tests/init-global/warn/lazy-val-reentry.scala
```

It expects the warning:

```text
Cyclic lazy value initialization: object LazyValReentry.x -> object LazyValReentry.y -> object LazyValReentry.x.
```

From inside the sbt shell, run the two focused tests with:

```sbt
testCompilation lazy-global-cycle
testCompilation lazy-val-reentry
```

For a manual repro of the original local bug, still from inside the sbt shell:

```sbt
scalac -Werror -Ysafe-init-global local/Bug.scala
```

This should fail because the warning is emitted and `-Werror` turns it into an
error.

Note: the repo's `sbt scalac` helper may not print warning text unless the
warning is turned into a failure with `-Werror`. The warning is still emitted by
the compiler, and direct compiler invocation prints it normally.
