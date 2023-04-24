[![GitHub release (latest by date)](https://img.shields.io/github/v/release/KorewaLidesu/TestBridge?label=github%20release&logo=github&style=for-the-badge)](https://github.com/KorewaLidesu/TestBridge/releases/latest)
[![CurseForge](https://azusahideout.ml/badge/testbridge)](https://www.curseforge.com/minecraft/mc-mods/testbridge/files)
[![GitHub Workflow Status (with branch)](https://img.shields.io/github/actions/workflow/status/KorewaLidesu/TestBridge/main_build.yml?label=test%20build&logo=github&style=for-the-badge)](https://github.com/KorewaLidesu/TestBridge/actions/workflows/main_build.yml)
[![GitHub](https://img.shields.io/github/license/KorewaLidesu/TestBridge?style=for-the-badge)](https://github.com/KorewaLidesu/TestBridge/blob/master/LICENSE)

This mod currently utilizies **Gradle 8.1.1** + **RetroFuturaGradle 1.3.3** + **Forge 14.23.5.2847**.

## Features
- Logistics Pipe:
  + Result Pipe
  + Crafting Manager Pipe
  + Buffer Upgrade
- Additional content (Soft dependencies):  
  + Applied Energistics 2:
    + Satellite Bus
    + Crafting Manager
    + Package
  + Refined Storage:  WIP

## How to use *this repo*:
1. Clone this repository.
2. In the local repository, run the command `gradlew setupDecompWorkspace`
3. Open the project folder in IDEA.
4. Right-click in IDEA `build.gradle` of your project, and select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
5. Run `gradlew runClient` and `gradlew runServer`, or use the auto-imported run configurations in IntelliJ like `1. Run Client`.

## Concept demo
![Concept](/Stuff/concept.gif)
