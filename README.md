[![Github Action](https://github.com/KorewaLidesu/TestBridge/actions/workflows/main_build.yml/badge.svg)](https://github.com/KorewaLidesu/TestBridge/actions/workflows/main_build.yml)
## Features
- Logistics Pipe:
    + Result pipe
    + Crafting manager pipe  
    + Buffer Upgrade  

[//]: # (    + New upgrade for CM pipe: WIP)

[//]: # (- Applied Energistics 2:  WIP)

[//]: # (- Refined Storage:  WIP  )

### How to use *this repo*:
1. Clone this repository.
2. In the local repository, run the command `gradlew setupDecompWorkspace idea`.
3. Open the `.ipr` file in IDEA.
4. Right-click in IDEA `build.gradle` of your project, and select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
5. Instead of running `genIntellijRuns` and using generated IntelliJ tasks, just run `gradlew runClient` and `gradlew runServer`.

## Current stage & version: PoC
Alpha 0.0.3

## Proof of concept
![Concept](/Stuff/concept.gif)
