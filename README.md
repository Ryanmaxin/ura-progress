# Global Initialization Checker Rewrite Progress

_Written by Ryan Maxin (rymaxin@gmail.com)_

[Host link of this page](https://ryanmaxin.github.io/ura-progress/)

[Implementation branch](https://github.com/Ryanmaxin/scala3/tree/upgrade_global_init_checker) · [Main implementation file](https://github.com/Ryanmaxin/scala3/blob/upgrade_global_init_checker/compiler/src/dotty/tools/dotc/transform/init/Objects_simple.scala)

## Contents of this directory

- **`README.md`** (this file) records the project's design, weekly progress, frozen test results, and remaining work.
- **`basic_tests/`** contains the bare-minimum functionality that an initialization checker needs to pass.
- **`add_tests/`** tests which would be useful to add to the official test suite in the future.
- **`deadlock_bug/`** contains `Bug.scala`, a lazy-global-initialization-cycle reproducer, and `lazy-global-init-cycle-explanation.md`, which explains the analysis and the implemented regression test. Ondrej suggested [Scala 3 issue #25370](https://github.com/scala/scala3/issues/25370) as an introduction to the code.
- **`pos_final.log`** and **`warn_final_log`** are the frozen outputs from the final positive- and warning-suite runs. They are evidence for the results below, not current test results.

## Handoff summary

- **Goal:** rewrite the Scala 3 global initialization checker to be smaller and mainly flow insensitive to improve performance.
- **Scope:** this is a research proof of concept, not a production-ready checker.
  Its diagnostics and pass counts must not be used as a soundness or
  false-positive-rate claim.
- **Pipeline:** with `-Ysafe-init-global -Ysafe-init-global-checker`, `Checker`
  runs `Semantic` first and then `ObjectsSimple`. Results below describe that
  combined pipeline unless a section explicitly assigns a diagnostic to one pass.
- **Current design:** `ObjectsSimple` follows global-object dependencies,
  reachable definitions, and instantiated classes. `Semantic` provides
  flow-sensitive field-order checking. The largest remaining gap is transferring
  enough state between the two passes.
- **Frozen results:** 2 of 46 warning tests and 60 of 62 positive tests pass.
  The classifications below distinguish genuine misses, false positives, and
  reporting-only differences.
- **Reproduce the frozen state:**

  ```sh
  sbt --client 'testCompilation tests/init-global/warn' # expected exit 1: 44/46 fail
  sbt --client 'testCompilation tests/init-global/pos'  # expected exit 1: 2/62 fail
  ```

  `testCompilation` filters test-source paths, so `checkInitGlobal` is not a
  valid filter. The suite conditionally also runs the TASTy-source case; record
  whether it is enabled with any future result.

- **Environment and revision:** Java 17.0.19; implementation baseline
  `f4db9c66dfc8f8dc897d5aaadf56d3a3f5302795`. Replace this hash with the final
  clean commit before handing the work off, and retain the command output with it.
- **Suggested reading order:** start with **Frozen State**, then use **Future
  todos** as the implementation backlog. The weekly notes provide development
  history and design context.

**I request that any future commits to the open source scala compiler that builds off of my checker add me as a contributor**

## Weeks

**Checkbox note:** under **Goals** and **Additional progress**, ☑ means completed.
Under **Topics and open questions**, ☑ means discussed with Ondrej, not
necessarily resolved. The answers were not always recorded, so these entries
mainly preserve the questions and design context from that point in the project.

### April 27, 2026 – June 7, 2026

Ondrej and I met about once a week during this period. These meetings focused on onboarding and building the background needed to begin the project.

### June 8, 2026 – June 15, 2026

#### Goals

- ☑ Add cycle detection and simple dependency analysis for the conservative checker.
  - The first implementation collected every dependency before checking for cycles, but this was less realistic.
- ☑ Decide on a structure for this progress document.
- ☑ Formalize the abstract domain and document it at the top of the code file.

#### Additional progress

- ☑ Built a small test harness for switching between the old and new initialization checkers, with the option to add the precise checker later.
- ☑ Started a small test suite that can be merged later.
- ☑ Learned more about lattices and solidified my understanding of Kleene's least fixed-point theorem.
- ☑ Published this progress document through GitHub Pages so that it can be shared and updated.

#### Topics and open questions

- ☑ Determine how to share code changes and eventually merge them into the Scala compiler.
  - Considered whether to work from a fork or request a branch in the official repository. The [repository's branch list](https://github.com/scala/scala3/branches/all) suggested that contributor branches are not generally available.
- ☑ Review the abstract domain:
  - The purpose of the domain is to form a lattice, which ensures that a fixed point exists and that the analysis terminates.
  - Could the fixed point still be prohibitively large in practice?
  - What should the top element be?
  - When should corrupting information—unknown or incomputable information—be introduced, and should it spread through joins?
  - Should `RD` and `dep` be part of the abstract domain, and should they also form lattices?
  - I needed more background in order theory, least upper bounds, greatest lower bounds, and Kleene's least fixed-point theorem.
    - Kleene's formula is simple in practice but often written in a complicated form. It seems almost trivially true for this checker.
    - The initial example yields a simple lattice; the difficult part is proving that the complete domain is a lattice.

### June 16, 2026 – June 26, 2026

#### Goals

- ☑ Implement `eval` as far as possible.
  - ☑ For reads, check whether the target is unowned mutable state.
  - ☑ Method-call cases must interact with `RD`.
  - ☑ Some cases interact with `IC`; fill these in as the design evolves.
  - ☑ Investigate repeated iteration and fixed-point computation.

#### Additional progress

- ☑ Decided how to share the implementation through the [fork](https://github.com/Ryanmaxin/scala3).
  - ☑ Scala staging allows branches, although special access may be required.
  - ☑ A staging branch would let others push to the pull request, but that was not important for this project.
  - ☑ Chose to use a fork.
- ☑ Completed the abstract domain.
  - ☑ The product of lattices, `L1 × L2`, is still a lattice.
  - ☑ A function space, `A → L`, is still a lattice: many small lattices form one larger lattice.

#### Topics and open questions

- ☑ Does the complete lattice have finite height? This seems intuitive, but nested classes have caused problems in the past.
- ☑ Does the abstract-domain documentation contain too much detail?
- ☑ How much complexity or creativity is appropriate in the data structures?
- ☑ Should the new implementation replicate the current checker?
- ☑ `eval` is more general than this checker needs; is it still worth keeping?
- ☑ For `ScanState`, what belongs to the abstract domain and what is analysis machinery?
- ☑ Should state be passed everywhere explicitly or stored on global objects such as summaries?
- ☑ Should `eval()` return `Set[OwnedClass]`?
- ☑ What exactly does an `Apply` tree represent?
- ☑ Is `visibleObjects` needed?
- ☑ Should mutable values carry a type?
- ☑ Could graph reachability be a useful URA direction?
- ☑ Could initializers be processed like methods?

### June 27, 2026 – July 3, 2026

#### Goals

- ☑ Cover every tree-matching case, initially making the default case fail so that missing cases can be fixed as they appear.
- ☑ Unify method exploration and class initialization.
- ☑ Finish handling `sel @ Select(...)`.
  - ☑ Clean up `valueOf`.
- ☑ Apply improvements from the previous notes.
- ☑ Run the checker against the existing tests and fix bugs.
  - ☑ Classify existing failures, including cases that may be handled by the more precise checker.
- ☑ Re-enable the instance-checker pass for global initialization.

#### Additional progress

- ☑ Warn when tree matching falls through instead of using `valueOf`.
- ☑ Pass `ScanState` implicitly with `given`/`using`.
- ☑ Extract `warnMutableAccess` into `checkForMutableAccess` for clarity.
- ☑ Stop representing a mutable read as `OwnedClass(A, A)`; the global object is no longer stored in the owned-class value.

#### Topics and open questions

- ☑ Should a global object own another global object?
- ☑ What receiver should an unqualified call use: the empty set or the current object?
- ☑ Revisit the proposed unification of method and class-initializer work:

  ```scala
  enum ReachableDef:
    case Method(sym: Symbol)
    case ClassInit(cls: ClassSymbol)
  ```

- ☑ The theory behind efficiency improvements is understood; decide whether those improvements should be implemented now.

- ☑ Does `selectedValue` need a fallback for indentation and selection cases?
- ☑ Confirm whether `Select` is fixed.
- ☑ Keep a distinct `Assign` case so writes do not fall through to the `Select` read logic. The current checker uses the same distinction.

- ☑ Decide how ownership should be recovered when a selection has no useful owner information.
  - Keep the two meanings of “owner” separate:
    - A **symbol owner** is the class where a member was declared. In the example below, the symbol for `config` is owned by `Server`; this does not mean a particular `Config` value is owned by `Server`.

      ```scala
      class Server:
        val config: Config = new Config
      ```

    - An **abstract value owner** is the global object that owns the runtime mutable state represented by `OwnedClass(owner, cls)`. While analyzing `A`, `B.server` should produce `OwnedClass(B, Server)`.

      ```scala
      object B:
        val server: Server = new Server
      ```

  - For a direct static-object member, the symbol owner is enough:

    ```scala
    object B:
      val box: Box = new Box

    // B.box => OwnedClass(B, Box)
    ```

  - For an ordinary instance field, inherit ownership from the receiver:

    ```scala
    class Server:
      val config: Config = new Config

    object B:
      val server: Server = new Server

    object A:
      val c = B.server.config

    // B.server.config => OwnedClass(B, Config)
    ```

  - Proposed ownership rules:
    - If a selected symbol is directly owned by a static object, use that object. For example, `B.box` becomes `OwnedClass(B, Box)`.
    - For an instance selection, inherit ownership from the receiver. For example, `B.server.config` inherits owner `B` from `B.server`.
    - If only a type is available, default to the current `root`. While analyzing `A`, a method result of type `Box` becomes `OwnedClass(A, Box)`.
  - Remaining implementation choice: should `valueFromTree(Select(...))` fall back to `valueFromType(sel.tpe, root)`, or should it use the receiver's owner set through something like `valueFromTypeOwnedBy(sel.tpe, receiverOwners)`?

- Check for redundant work on superclass calls between templates and `scanClassInit`.
- If a symbol is a static object, record an access.
- Remove the redundant `joinIC()` call.
- Use the following basic test cases while iterating:
  - `MutableReadDirect.scala`, which is currently too coarse.
  - `MethodObjectCycle`, which misses a cycle without overly complex logic.

### July 4, 2026 – July 10, 2026

#### Goals

- ☑ Confirm that an array assignment such as `a(42) = 5` is typed as `a.update(42, 5)` for `Assign` handling.
  - ☑ It is.
- ☑ Assert that an assignment target is an `Ident` or `Select`.
  - ☑ Revisit this after broader testing to see whether the assertion ever fails.
- ☑ Simplify `addObjectDependency`.
- ☑ Clean up `Select` and `Ident` handling with the `IC` values.
  - ☑ When evaluating a field in `Select` or `Ident`, pass the complete `IC` of the current object.
- ☑ Add class initializers to the method queue.
- ☑ Pass state implicitly with `given`/`using`, and store the current global object in the `ScanState` roots.
- ☑ Investigate moving the mutable-read warning check from `Call` into `Select`, or removing it from `Call`. Getter calls should contain a `Select` that is already checked.
  - ☑ Confirm the tree traversal order because `Call` may be visited first.
- ☑ Build out `evalType` based on its concrete uses:
  - ☑ For `this.x`, `this` is implicit and the tree may be an `Ident`; type information is needed to recover its context.
  - ☑ `Ident` and `Select` may require similar logic.
  - ☑ For an import such as `import G.*`, the qualifier is implicit and must be recovered.
  - ☑ Compare these cases with the old checker's uses of `evalType`.

#### Additional progress

- ☑ Removed return-value type tracking from the `Call` case.

#### Topics and open questions

- ☑ Decide whether the mutable-read check belongs in `Call` or `Select`; confirm the visitation order.
- ☑ Determine how an imported or static member reference is represented:
  - Example:

    ```scala
    object G:
      val x = ...

    object A:
      import G.*
      val y = x
    ```

  - Representation question: for a typed `Ident(x)`, does the `TermRef` prefix reliably refer to `G`, or can it be `TermRef(NoPrefix, x)` even though `x.symbol.owner` is `G`?
  - The old safe-initialization checker includes this fallback:

    ```scala
    case tmref: TermRef if tmref.prefix == NoPrefix && tmref.symbol.owner.isClass =>
      evalType(tmref.symbol.termRef)
    ```

  - Decide whether the checker should recover the qualifier through `evalType(prefix)` or also fall back to `symbol.owner` or `symbol.termRef`.

- ☑ Should a type reference such as `val x: G.T` inside `object A` add a dependency?
- ☑ Inside `evalType`, should nested global objects add dependencies, and does `isStatic` rule this out?
- ☑ Should `warnIfMutableRead` consider nested classes or derived classes?
- ☑ Clarify handling of `this` in a global object.
  - A normal use of `this` should not create a meaningful self-dependency. For example, `object G: val x = this.y` should not be interpreted as `G` depending on itself.
  - `addObjectDependency` already ignores `target == source`, so this is harmless but conceptually noisy.
  - The `ThisType(tref)` case mirrors the old checker's structure:

    ```scala
    case tp @ ThisType(tref) =>
      val sym = tref.symbol
      if sym.isStaticObject && sym != klass then
        accessObject(sym.moduleClass.asClass)
      else
        resolveThis(...)
    ```

### July 11, 2026 – July 20, 2026

#### Goals

- ☑ Run the full test suite.
- ☑ Classify the failures as precision issues, bugs, or work deferred to another pass, and debug them.
- ☑ Review the class check in `warnIfMutableRead`.
  - ☑ Do not check only the direct owner. Inspect the base classes to determine whether one contains the field, using an approach similar to the regular checker.
- ☑ Add scanned class initializers to `summary.RD` as reachable definitions.
- ☑ Verify and, if needed, fix `NewExpr` handling:

  ```scala
  // Example: new Box(arg)
  case NewExpr(tref, _, ctor, argss) =>
    evalArgs(argss)
    // A class created while initializing root is owned by root.
    evalType(tref.tpe.prefix)
  ```

  Confirm whether `tref` is a type or a `TypeRef` and make sure this compiles correctly.

#### Additional progress

- ☑ Implemented lazy values with `evaluatedLazies` and `forcingLazies`.
- ☑ Added `ReachableDefinition`.
- ☑ Changed `TypeTree` to `TypTree` so that cases such as `AppliedTypeTree`, rather than only `TypeTree`, are included:

  ```text
  TypTree
  ├── TypeTree
  ├── AppliedTypeTree
  ├── SingletonTypeTree
  ├── RefinedTypeTree
  ├── TypeBoundsTree
  ├── MatchTypeTree
  └── ...
  ```

- ☑ Added warnings for mutable writes.
- ☑ For a source-defined `unapplySeq`, also scan the protocol operations that pattern matching may invoke implicitly: `length`, `apply`, `toSeq`, and `drop`. These methods are added to the reachable-method worklist and restricted to source objects already known to the checker.
- ☑ Allowed the flow-sensitive `Semantic.scala` checker to inspect global objects conditionally.
- ☑ Added array and closure checking to match the test suite.

#### Topics and open questions

- Initial audit: 47 of 48 tests failed.

| Result                                      | Count | Meaning                                                                                       |
| ------------------------------------------- | ----: | --------------------------------------------------------------------------------------------- |
| Exact pass                                  |     1 | `cyclic-object.scala`                                                                         |
| Correct warning, different message text     |     7 | Semantically detected, but missing the old checker's traces or details                        |
| Detected something, fewer expected warnings |     6 | Usually one cycle warning instead of warnings at both expected positions                      |
| No warnings at all                          |    24 | Genuine false negatives                                                                       |
| Wrong-position `Unhandled tree` warnings    |     3 | The intended problem was missed and an internal unsupported-tree warning was reported instead |
| Extra warnings                              |     6 | A combination of deliberate over-approximation and unsupported-tree warnings                  |
| Compiler crash                              |     1 | Unsafe cast in `defDefOf`                                                                     |

- ☑ Are mutable reads expected in `tests/init-global/warn/global-irrelevance2.scala`?
- ☑ How should lazy values be handled?
  - Should they be evaluated eagerly?
  - Is pointer analysis needed?
- ☑ How should aliasing be handled, and is it appropriate for the current scope?

  ```scala
  object Test:
    class Box(value: => Int)

    def f(a: => Int): Box =
      lazy val b = a
      Box(b)

    val box = f(n)
    val n = 10
  ```

- ☑ Determine whether self-aliasing through a lazy value requires special handling:

  ```scala
  object O:
    lazy val self = this
    val f2 = 5
    val f3 = self.f2 + self.f3
  ```

- ☑ Compare the lazy case with a strict self-alias:

  ```scala
  object O:
    val self = this
    val f2 = 5
    val f3 = self.f2 + self.f3
  ```

- ☑ Determine how to handle closures and anonymous functions, which are harder than lazy values because they are anonymous:

  ```scala
  (a: Int) => a + B.n

  Block(
    List(
      DefDef($anonfun, ..., a + B.n)
    ),
    Closure(..., $anonfun, ...)
  )
  ```

- Follow-up audit after several fixes:

| Result                           | Old | New | Interpretation                                   |
| -------------------------------- | --: | --: | ------------------------------------------------ |
| Exact pass                       |   1 |   1 | Fully matches the old checker                    |
| Correct warning, message differs |   7 |   8 | Semantic detection, but without a detailed trace |
| Some expected warnings detected  |   6 |   7 | Usually one warning instead of two               |
| No warning                       |  24 |  29 | Intended warning genuinely missing               |
| Extra warnings                   |   6 |   3 | Intended warning plus false positives            |
| Wrong-position `Unhandled tree`  |   3 |   0 | Fixed                                            |
| Compiler crash                   |   1 |   0 | Fixed                                            |

- The audit identified two main groups:
  - 22 cases are semantic variants of `MethodObjectCycle.scala`: initialization eventually reads the currently initializing object's own not-yet-initialized state. These can reasonably be delegated to the flow-sensitive class-initialization checker.
  - Seven cases cannot be deferred: one global object reads or mutates mutable state owned by another global object. `ObjectsSimple` should warn for all seven.

- Recommended implementation order for the seven `ObjectsSimple` bugs:
  1. Foreign writes, covering `global-irrelevance2`.
  2. Owned mutable arrays, covering `global-irrelevance5`, `global-irrelevance6`, and `global-irrelevance7`.
  3. Owned mutable closure environments, covering `global-irrelevance3` and `global-irrelevance4`.
  4. Pattern-protocol reachability plus array ownership, covering `patmat-unapplySeq`.

- `unapplySeq` protocol handling:
  - The explicit tree for `patmat-unapplySeq.scala` exposes the call to `unapplySeq`, but sequence matching may implicitly invoke `length` or `lengthCompare`, `apply`, `toSeq`, and `drop`.
  - The old global checker models these protocol calls using the result type of `unapplySeq`.
  - The simple checker originally scanned only ordinary source-backed calls, so it missed these compiler-implied calls.
  - A conservative prototype enqueues `length`, `apply`, `toSeq`, and `drop` from the object defining `unapplySeq`.
    - This covers the existing test because `unapplySeq` returns `A.type` and all four methods are defined directly in `A`.
  - The prototype is not general:
    - An extractor could return a separate result class containing the protocol methods.
    - Some methods may come from external code without available source.
  - Remaining options:
    1. Model the protocol from the final result type of `unapplySeq`, scanning methods whose source is available.
    2. Keep the narrow conservative implementation for source-defined object extractors.
    3. Treat compiler-implied pattern-protocol calls as outside this checker's scope and defer them elsewhere.
  - Preferred direction: use the result type when source is available. This matches Scala's pattern semantics without requiring the old checker's complete abstract interpretation.

### July 21, 2026 – July 24, 2026

#### Goals

- ☑ Model lazy values owned by static objects as objects, subject to the same rules and restrictions.
- ☑ Represent closures as `IC` values, filtering for `IC` values that contain an `apply` method.
- ☑ Handle dynamic dispatch in `Call` by also adding reachable methods with the same name from matching `IC` values.
- ☑ Remove `val isClosureCall = method.name == nme.apply && defn.isFunctionClass(method.owner)` after confirming that it is unnecessary.
- ☑ Confirm that `!sel.symbol.isSetter` is unnecessary.

#### Topics and open questions

- ☑ Do not remove the mutability check entirely. A write to a `val` can reach `Assign`: the typer permits assignments to non-method fields from their primary constructor, including immutable fields. See `Typer.scala` around line 1449.

### July 25, 2026 – August 7, 2026

#### Goals

- ☑ Run the complete test suite and manually review all 48 warning cases.
- ☑ Classify the issues.
- ☑ Treat class-owned lazy values as methods invoked from `evalType`. This must happen in `evalType`, not `initClass`, because lazy values are skipped there.
- ☑ Evaluate by-name parameters immediately.
  - ☑ This is sound but imprecise. After a longer discussion, Ondrej and I concluded that this is the simpler approach and appropriate for this checker.
- ☑ Replace `OwnedClass` with a representation closer to `GlobalInitNode`, making the types more explicit and reducing room for bugs.
- ☑ Confirm that `resolve` works as intended and locate its source.

#### Additional progress

- ☑ Removed the AI checker.

#### Topics and open questions

- ☑ Preserve the following distinction when following prefixes:
  - If the symbol is an ordinary field or method, continue following its prefix.
  - If the symbol is already represented by an object or lazy node, do not turn its lexical namespace into another dependency.
  - Still record the dependency on the nested node itself.

### August 8, 2026 – August 14, 2026

#### Goals

- ☑ Finalize the code and clean up comments.
- ☑ Clean up this README, including future tasks.
- ☑ Make sure the code is shareable.
- ☑ Freeze the results and add them to the README.
- ☑ Record lessons learned.
- ☑ Squash commits.
- ☑ Investigate the one genuine additional warning.
- ☑ Investigate why the flow-sensitive failure cases are not caught by the instance checker.
- ☑ Investigate `global-cycle4.scala`:

  ```scala
  val a = callThatReads(a) // unsafe
  ```

  Determine whether the initialization checker should detect this case.

## Frozen State:

| Classification               |  Count |
| ---------------------------- | -----: |
| No remaining issue           |      2 |
| Reporting/checkfile mismatch |     16 |
| Genuine additional warning   |      1 |
| False-positive extras        |      7 |
| Missing expected warnings    |     20 |
| **Total**                    | **46** |

Final rerun results:

- Warn: **46 total, 2 passed, 44 failed**
- Positive: **62 total, 60 passed, 2 failed**
- `warn_final_log` records the exact 44-test warning failure set.

Logs:

- [warn_final_log](warn_final_log)
- [pos_final.log](pos_final.log)

## Warn suite classification

### Passing — 2

`cyclic-object`, `lazy-field`

### Reporting/checkfile differences — 16

The unsafe initialization path is reported, but the diagnostic contract differs
from the old checkfile.

- Correct violation, simpler wording/no trace: `global-cycle5`,
  `global-irrelevance1/2`, `mutable-read3/5/6`, `partial-ordering`.
- One aggregate cycle warning instead of the old cycle warning plus a secondary
  field/before-super warning: `global-cycle1/7/14`, `i11262`, `i9176`, `t9261`.
- Same diagnostic at a different cycle anchor or source span:
  `unapply-implicit-arg`.
- Correct diagnostic at the wrong source location: `i15883`. `AccessNonInit`
  anchors the warning at `b`'s declaration on line 2 instead of the read on
  line 1.
- Earlier conservative diagnostic instead of the later causal read:
  `inner-extends-outer`. `Semantic` warns when partial `O` is passed into
  `new Outer(this)`; the old checkfile warns later when dispatch reaches
  uninitialized `O.f2`. This is more than a row-number error, but the unsafe
  path is not silently accepted.

Future: decide diagnostic policy, then update either the reporter or tests/checkfiles.

### Genuine additional warning — 1

- `lazy-local-val`

The second warning is real: the strict `Box` constructor forces the lazy local and reads the later field. Future: add the missing `// warn`.

### False-positive extras — 7 tests, 8 warnings

- Receiver conflation: `mutable-read1/4`. A read from `B.boxB` is confused
  with a read from `A.box` because both values have class `Box`.
- Nested-initialization context conflation: `mutable-read2`,
  `global-irrelevance5/6/7`, `patmat-unapplySeq`. Work performed while
  initializing `A` itself is imported into `B`'s summary and then described as
  foreign access "during initialization of B." `global-irrelevance7` contains
  two such extra warnings; each other test contains one.

Both subgroups come from collapsing receiver, owner, and execution-context
provenance into one root-level class set. Fixing them requires keeping the
concrete receiver/owner and preserving which nested initializer is executing.

### Missing expected warnings — 20

These tests expect a warning but do not currently receive one. Some contain a
real runtime early read, while others exercise conservative behavior required
by the old checker. The simplest example of the main problem is:

```scala
object O:
  val a = B.readA()

object B:
  def readA() = O.a // O.a is read before its initialization finishes
```

While initializing `O.a`, the checker follows a call into `B.readA()`, which
returns to the same field before it is ready. The two passes each understand
only part of this path:

- `Semantic` tracks individual fields and initially knows that `O.a` is
  uninitialized. It can lose that field-level fact after the access crosses a
  method call or the receiver is treated as fully initialized (`Hot`).
- `ObjectsSimple` follows the method call back to `O`, but tracks global objects
  rather than individual fields. It sees an `O → O` dependency, ignores it as
  an ordinary self-dependency, and therefore misses the read of `O.a`.

In short, `Semantic` has the field state and `ObjectsSimple` has the
interprocedural call path, but neither pass currently transfers enough
information to the other.

| Root cause                                  | Count | Tests                                                                                                 |
| ------------------------------------------- | ----: | ----------------------------------------------------------------------------------------------------- |
| Active global-field state lost across calls |    14 | Dynamic receivers (4), extractors (2), direct calls (3), local/outer paths (3), returned closures (2) |
| Captured mutable cells absent               |     2 | `global-irrelevance3/4`                                                                               |
| Construction phases absent                  |     4 | `call-before-super*`, `global-cycle10`                                                                |

#### 1. Active global-field state is lost across calls — 14

- Dynamic receivers: `global-cycle4`, `context-sensitivity`, and
  `global-region1` are conservative expectations—the concrete executions take
  safe branches, but another reachable class reads the current field.
  `unsoundness` is the genuine case: the selected `B.update` reads `O.x` before
  initialization. Receiver provenance improves precision, but all four still
  require current-field state at the final access.
- Extractors: `unapply-implicit-arg2/3` conservatively consider the `m2` branch,
  which reads current `Bar.i2`; the concrete selector takes `m1`. Parameter
  binding is imprecise, and the final `Bar.i2` field state is absent.
- Direct calls: `global-cycle2`, `global-cycle3`, and `global-local-var` are
  genuine early reads. The path crosses a static call or newly hot instance and
  returns to the currently initializing field. `global-cycle2` is the minimal
  boundary example:

```scala
object A:
  val a = B.foo()

object B:
  def foo() = A.a
```

- Local/outer paths: `local-class`, `resolve-parent-this`, and
  `resolve-outer-of-parent` are genuine early reads hidden behind captured
  values, inherited `this`, or path-dependent outer pointers.
- Returned closures: `return` and `return2` are genuine reads of current
  `B.n`. The analyses do not jointly preserve returned-closure control flow and
  the active `B.n` state; `return2` also emits the unrelated nonlocal-return
  language warning.

#### 2. Captured mutable cells are absent from the domain — 2

`global-irrelevance3/4` read and write a local `var` captured by closures owned
by `A` while `B` initializes. This violates initialization-time irrelevance,
not field order.

`ObjectsSimple` represents a closure with only its abstract owner, generated
closure class, and `apply` method:

```text
OwnedClass.Closure(owner = A, cls = <closure class>, apply = <apply method>)
```

This is enough to follow `B → A.p.g() → g.apply`, but it does not record the
closure's captured environment. When `g.apply` reads `x`, `x` is still a local
variable captured from `foo`, not a field with a class owner. The current
mutable-access check handles class-owned state, so it cannot connect `x` back
to `A` and does not report the foreign read.

The write closure is not executed in `global-irrelevance3`; only `g` is called,
so the expected warning is at the read. `global-irrelevance4` calls `f` instead
and expects the corresponding warning at the write. Supporting both cases
requires an abstract captured-cell value that records `A` as the owner of `x`.

#### 3. Construction phases are not represented — 4

An object exists before it is fully initialized. The old checker distinguishes
three broad construction phases:

```text
BeforeSuper → Constructing → Initialized
```

The new checkers do not record this phase. For example:

```scala
object X extends C(j = X.foo()):
  def foo() = 5
```

The superclass argument `X.foo()` is evaluated after `X` is allocated but
before `C`'s constructor runs. The receiver `X` therefore exists, but it is
still in the `BeforeSuper` phase. The old checker rejects the call based on
that phase, even though `foo()` happens to return a constant.

- `call-before-super` makes this call directly in a superclass argument.
- `call-before-super2` reaches `X.k` indirectly through `A.foo`.
- `call-before-super3` calls `X.foo()` from the superclass constructor body.

`global-cycle10` exercises the same rule through virtual dispatch:

- `O` begins construction by running the `Base` constructor.
- `Base` initializes `msg`, then calls the abstract method `foo()`.
- Dynamic dispatch selects `O.foo()` before the `Base` constructor has
  finished.
- `O.foo()` creates `Inner`, which receives the still-partial outer object `O`.

The concrete read of `Base.msg` is safe because that field has already been
initialized. The expected warning is conservative: the old policy rejects
calling methods on, or creating an inner object from, a receiver whose
superclass construction has not finished.

- `Semantic` checks the called body and sees only a constant or an already
  initialized field, so it does not reject the receiver's construction phase.
- `ObjectsSimple` follows the calls and class initialization, but does not
  attach a construction phase to the receiver.

Supporting these warnings requires tracking `BeforeSuper`, `Constructing`, and
`Initialized` for abstract objects and preserving that state through indirect
calls, virtual dispatch, and inner-object creation.

#### Takeaway and priority

The main issue is not simply "insufficient receiver flow." Fourteen tests need
the interprocedural reachability of `ObjectsSimple` and the field-order state of
`Semantic` at the same time. Neither checker currently transfers that state to
the other.

If development continued, use `unsoundness`, `global-cycle2`, and
`global-cycle3` as soundness targets, then decide which checker owns the active
static field state across calls. Captured cells and construction-phase policy
are separate scope decisions.

## Positive suite classification

### Passing — 60

No action.

The low positive-suite failure count does not mean that `ObjectsSimple` is
uniformly precise. It over-approximates some dimensions but under-approximates
others.

It over-approximates after facts have been discovered: both branches are
scanned, reachable classes and methods are unioned, and those sets are retained
flow-insensitively for the whole global root. This can conflate distinct
instances and produce extra warnings.

However, fact discovery is under-approximating:

- there is no explicit unknown/top abstract value;
- there is no environment mapping locals, parameters, fields, or return values
  to abstract values;
- captured mutable cells are absent;
- ordinary dependencies from a global object back to itself are ignored;
- only suitable source/current-run definitions are followed;
- several compiler-implied or nonlocal control-flow relationships need special
  modeling.

In other words, the checker conservatively unions the facts it knows, but it
does not conservatively account for everything it does not know. A missing fact
does not expand to "all possible values/methods/owners"; it usually disappears.
That makes the checker quiet on many positive tests, but it also explains the
missing warnings above. Over-approximation and under-approximation occur at
different stages and are not contradictory.

The positive suite is also a regression corpus, not a statistical sample for
estimating a production false-positive rate. In this run, both positive-suite
failures are diagnostics from `Semantic`; `ObjectsSimple` itself causes no
positive-suite test failure. That is useful attribution, but it should not be
interpreted as proof that the simple checker is both sound and highly precise.

### False positives — 2

- `global-this`: the semantic checker rejects temporarily publishing partially initialized `this` into state owned by the same global object. Needs same-region/ownership awareness.
- `lazy-local-val`: the semantic checker loses the fact that the value remains suspended through a lazy local and by-name constructor argument. Needs correct by-name/lazy suspension tracking.

## Future todos

- **Correctness and precision**
  - ☐ Define the abstract domain and lattice for the precise checker.
    - The simple checker currently collapses value information into a root-wide `IC` set; a precise domain should distinguish locals, fields, return values, and closure environments.
  - ☐ Decide which checker owns active global-field state across calls.
    - `ObjectsSimple` follows interprocedural calls, while `Semantic` tracks field order, but neither currently transfers enough state to the other.
  - ☐ Add an environment for locals, parameters, fields, and return values.
    - This is needed to preserve values through argument binding, field access, and method returns.
  - ☐ Preserve receiver and owner provenance instead of collapsing values by class.
    - This should address receiver conflation in tests such as `mutable-read1/4` and nested-initialization context errors.
  - ☐ Analyze methods with different argument sets or contexts when their summaries may change.
    - Reachable definitions are currently scanned once per root without parameter-specific summaries.
  - ☐ Add captured mutable cells to closure summaries if initialization-time irrelevance remains in scope.
    - `OwnedClass.Closure` records the owner, closure class, and `apply` method, but not the captured environment. As a result, `global-irrelevance3/4` cannot associate the captured `var` with `A`.
  - ☐ Decide whether construction phases and before-super calls remain part of the simplified checker's policy.
    - This determines whether `call-before-super*` and `global-cycle10` should remain expected warnings.
  - ☐ Revisit the old checker's `filterType()` approach for narrowing possible receiver classes.
  - ☐ Generalize `unapplySeq` protocol discovery from the extractor result type when source is available.
    - The current implementation only checks methods on source objects already known to the analysis.
  - ☐ Explore the semi-flow-sensitive approach for cases such as `MethodObjectCycle.scala`.
    - The goal is to retain active field state across calls without rebuilding the complete precise checker.

- **Performance**
  - ☐ Make dependency and fixed-point traversal incremental so only affected roots and call sites are revisited.
    - The current implementation reruns every root whenever any summary changes.
  - ☐ Avoid rescanning methods when their possible arguments or receiver classes have not changed.
    - A dependency or observer structure could requeue only call sites affected by new information.
  - ☐ Consider a double-ended queue for drain ordering only if profiling shows it is useful.
  - ☐ Force termination after a certain amount of cycles

- **Tests and diagnostics**
  - ☐ Use `unsoundness`, `global-cycle2`, and `global-cycle3` as the primary soundness targets.
    - These are genuine runtime early reads rather than only conservative expectations.
  - ☐ Add edge traces and useful logging back to the simple checker.
    - Traces were removed during simplification, which is why many correct warnings do not match the old diagnostics.
  - ☐ Filter the global-initialization tests around the responsibilities of the new checker.
    - Separate `ObjectsSimple` failures from cases intentionally delegated to `Semantic`.
  - ☐ Fix the two positive-suite false positives.
    - The remaining cases are `global-this` and `lazy-local-val`.
  - ☐ Settle diagnostic wording, locations, and warning-count policy.
    - Eighteen warning tests currently detect the unsafe path but differ from their checkfiles.
  - ☐ Update test annotations and checkfiles last, after the checker behavior is stable.

## Lessons learned

- Treat static lazy vals as global initialization nodes; they can be forced independently and form cycles.
- Union is conservative only for discovered facts. Missing information still causes false negatives.
- Reachability and field initialization state must be shared between the two checkers.
- Summaries must preserve execution context when information is imported from another object.
- Object-level self-dependencies can hide field-level re-entry.
- Receiver, owner, and initialization root are different concepts and should remain separate.
- Closures need their captured environment, not just their class and `apply` method.
- By-name arguments and lazy vals require tracking when evaluation happens.
- Typed trees can hide operations such as array access and the `unapplySeq` protocol.
- Raw pass counts are less useful than classifying genuine misses, false positives, and reporting differences.
