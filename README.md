# Global Initialization Checker Rewrite Progress

### Written by Ryan Maxin

[Link](https://ryanmaxin.github.io/ura-progress/)

[Code here](https://github.com/Ryanmaxin/scala3)

## Weeks

### June 8, 2026 - June 15, 2026

#### Progress

_Goals:_

- [x] Add cycle detection, simple dependency analysis for conservative checker.
  - At first I did all dependencies at once, then check for cycles but this is less realistic
- [x] Decide on structure for the progress document (this)
- [x] Formalize the abstract domain. Add it to the top of the code file.

_Additional:_

- Built small testing harness so that I can switch back and forth between the old and new initialization checker (and later the precise one)
- Started building up my own small test suite, I can merge this in later
- Learned more about lattices, solidifed the Kleene's least fixed point theorem that I missed from class
- Uploaded this progress file to a github pages that can be shared and updated

#### Topics

- [x] How should I share my code changes? Should I fork or branch from the official scala compiler. Whats the process to get it merged? https://github.com/scala/scala3/branches/all From branches, we see that they don't seem to allow random branches
- [x] How is my abstract domain?
  - [x] The whole point of the domain is to build a lattice, which proves that we have a fixed point (IE the program will terminate).
  - [x] The fixed point could still be massive in practice?
  - [x] What should be the top exactly?
  - [x] When should I add in "corrupting" informaiton (IE unknown info, incomputable info, when to "give up" on the computation?), corruption spreads with join?
  - [x] Should the RM, dep be defined as part of the abstract domain, should they be lattices aswell?
  - [x] This is the idea here?
        <br>
        ![alt text](image.png)
  - [x] I was weak on order theory, LUB, GLB, Kleene's least fixed point theorem.
    - [x] Kleene's least point formula is really simple in practice but it is written complicated. It seems trivially true in my case.
    - [x] The example I started with here is super simple to create our lattice, the hard part is making sure its a lattice, how difficult is it?

### June 16, 2026 - June 22, 2026

#### Progress

_Goals:_

- [x] Implement eval (get as far as possible)
  - [x] One of those cases is if its a read, check if its unowned mutable state.
  - [x] One (or maybe a few) will be method calls which will have to interact with RM
    - NOTE: Will have to think about how to deal with potentially analyzing methods multiple times with different arguments
  - [x] A few will interact with IC, need to fill those out as we keep iterating
  - [x] Look into the repeated iteration/finding the fixed point

- [ ] Examine tree structure for basic cases
  - USe vprint, scala -V <- This will print "scala" like code
  - Look for the argument that lets you see the node names directly
    - Probably one of the -x options (-Xprint-types?)
    - -Vprint:typer <- LOOK UP THE PASS JUST BEFORE GLOBAL INIT CHECKER
    - -Vprint:typer -Yplain-printer <- Try this one for examining trees. Compare it to just the Vprint:typer one. This one has the actual type names of nodes (useful for eval() implementations

_Additional:_

- [x] [Fork code here](https://github.com/Ryanmaxin/scala3)
  - Scala staging allows branches, may need special access
    - Main benefit of staging, is that others can push to my pr
    - This isn't really a benefit for me though
  - I chose to just create a fork
- [x] Finished complete abstract domain
  - _NOTE:_
    - L1 x L2 => Still a lattice
    - f -> L => Still a lattice

#### Topics

- [ ] Do we know our full lattice actually has a fixed height? It seems intuitive to me but you told me that in the past with nested classes we had some issues.
- [ ] Too many details for the abstract domain? What do you prefer?
- [ ] How much complexity/creativty in the data structures?
- Cool scala feature: extension
- [ ] Do I want to replicate the current code?
- [ ] eval is way overkill for our needs, is it still good to keep it?
- [ ] Thoughts on "Scan State". What is abstract domain, what is machinery?
- [ ] AI recommends passing it everywhere to avoid implicit state. My mind says to just make it a member of each global object, like summaries.
- [ ] Should eval() return our Set[OwnedClass]?
- [ ] What exactly is Apply?

### June 23, 2026 - June 29, 2026

#### Progress

_Goals:_

- [ ] Add local environment to the abstract domain

## Future todos (Add and remove as needed)

- Update test checks (only when the checker is in a better place)
- Filter the global init checker tests to be properly focused on the features of the new checker

- Optimize stack traversal for dependency finding
- Optimize calling methods as few times as possible (collect all possible arguments first maybe? Use an observer to rerun only when possible args changes?)
- Add back edge traces, logging
- Define abstract domain, lattice, for the precise checker
- Implement mutable read detection on simple domain
- Look into optimizations for the init checker

## Notes:
