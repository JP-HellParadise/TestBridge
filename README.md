## Test Addon for certain pipe mods
## Features
Have some same features as Logistics Bridge (old and rework version) with some changed:
- Logistics Pipe:
    + Result pipe
      Using Satellite pipe as template, now using LP upgrade for simplification.
    + Crafting manager pipe
      Should fix some issues with LB rework version.
    + New upgrade for CM pipe: WIP

[//]: # (- Applied Energistics 2:  WIP)

[//]: # (- Refined Storage:  WIP  )

### How to use *this repo*:
1. Clone this repository.
2. In the local repository, run the command `gradlew setupDecompWorkspace idea`.
3. Open the `.ipr` file in IDEA.
4. Right-click in IDEA `build.gradle` of your project, and select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
5. Instead of running `genIntellijRuns` and using generated IntelliJ tasks, just run `gradlew runClient` and `gradlew runServer`.

## Current stage/version: (PoC)
Alpha 0.0.1

## PoC (Proof of Concept)
![Concept](/Stuff/concept.gif)
