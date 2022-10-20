## Test Addon for certain pipe mods

### Instructions:

1. In the local repository, run the command `gradlew setupDecompWorkspace idea`
2. Open the `.ipr` file in IDEA.
3. Right-click in IDEA `build.gradle` of your project, and select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
4. Instead of running `genIntellijRuns` and using generated IntelliJ tasks, just run `gradlew runClient` and `gradlew runServer`.

