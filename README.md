[![Github Action](https://github.com/KorewaLidesu/TestBridge/actions/workflows/main_build.yml/badge.svg)](https://github.com/KorewaLidesu/TestBridge/actions/workflows/main_build.yml)
## Anime
![image](https://user-images.githubusercontent.com/24401452/207646011-61a84639-8123-4bba-809b-20b69b7ae007.png)
## Features
- Logistics Pipe:
  + Result Pipe
  + Crafting Manager Pipe
  + Buffer Upgrade
- Additional / Soft dependencies:  
  + Applied Energistics 2:
      + Satellite Bus
      + Crafting Manager
      + Package

[//]: # (  + Refined Storage:  WIP  )

### How to use *this repo*:
1. Clone this repository.
2. In the local repository, run the command `gradlew setupDecompWorkspace idea`.
3. Open the `.ipr` file in IDEA.
4. Right-click in IDEA `build.gradle` of your project, and select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
5. Instead of running `genIntellijRuns` and using generated IntelliJ tasks, just run `gradlew runClient` and `gradlew runServer`.

## Current stage & version: PoC
Beta 0.2.4

## Demo
![Concept](/Stuff/concept.gif)
