## Test Addon for certain pipe mods  
## Current stage: 
brainded, will have to implement Logistics somehow...
## Features
- Logistics addon:
  + Result pipe
  + Crafting manager pipe
  + New upgrade for CM pipe  
- Applied Energistics 2:  WIP  
- Refined Storage:  WIP  




### How to use *this repo*:  
1. Clone this repository.
2. In the local repository, run the command `gradlew setupDecompWorkspace idea`.
3. Open the `.ipr` file in IDEA.
4. Right-click in IDEA `build.gradle` of your project, and select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
5. Instead of running `genIntellijRuns` and using generated IntelliJ tasks, just run `gradlew runClient` and `gradlew runServer`.
