# Global Initialization Checker Rewrite Progress

## Written by Ryan Maxin

[Link](https://ryanmaxin.github.io/ura-progress/)

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

- [ ] How should I share my code changes? Should I fork or branch from the official scala compiler. Whats the process to get it merged? https://github.com/scala/scala3/branches/all From branches, we see that they don't seem to allow random branches
- [ ] How is my abstract domain?
  - [ ] The whole point of the domain is to build a lattice, which proves that we have a fixed point (IE the program will terminate).
  - [ ] The fixed point could still be massive in practice?
  - [ ] What should be the top exactly?
  - [ ] When should I add in "corrupting" informaiton (IE unknown info, incomputable info, when to "give up" on the computation?), corruption spreads with join?
  - [ ] Should the RM, dep be defined as part of the abstract domain, should they be lattices aswell?
  - [ ] This is the idea here?
        <br>
        ![alt text](image.png)
  - [ ] I was week on order theory, LUB, GLB, Kleene's least fixed point theorem.
    - [ ] Kleen's least point formula is really simple in practice but it is written complicated. It seems trivially true in my case.
    - [ ] The example I started with here is super simple to create our lattice, the hard part is making sure its a lattice, how difficult is it?

### June 8, 2026 - June 15, 2026

#### Progress

_Goals:_

- [ ] Add read of mutable state check
- [ ] Examine tree structure for basic cases

## Future todos (Add and remove as needed)

- Update test checks (only when the checker is in a better place)
- Filter the global init checker tests to be properly focused on the features of the new checker

- Optimize stack traversal for dependency finding
- Optimize calling methods as few times as possible (collect all possible arguments first maybe? Use an observer to rerun only when possible args changes?)
- Add back edge traces, logging
- Define abstract domain, lattice, for the precise checker
- Implement mutable read detection on simple domain
