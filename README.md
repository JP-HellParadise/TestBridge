## TemplateDevEnv

### Instructions:

1. Click `use this template` at the top.
2. Clone the repository you have created with this template.
3. In the local repository, run the command `gradlew setupDecompWorkspace idea`
4. Open the `.ipr` file in IDEA.
5. Right-click in IDEA `build.gradle` of your project, and select `Link Gradle Project`, after completion, hit `Refresh All` in the gradle tab on the right.
6. Instead of running `genIntellijRuns` and using generated IntelliJ tasks, just run `gradlew runClient` and `gradlew runServer`.

*A checkstyle is coming soon, meaning Cleanroom's projects will use a specific coding style that has to be abided at time of PR/building.*

### If you want to add dependency, please follow this:

#### The dependency format is as follows: `curse.maven:<descriptor>-<projectid>:<fileids>`

1. curse.maven -> Required. Marks the dependency to be resolved by the curse maven website.
2. `<descriptor>` -> Can be anything you want. This file downloaded will have this in it's name, so it's good to use this to show which files are what. A good practice would be to have this as the project slug.
3. `<projectid>` -> The project id of the file you want to add as a dependency.
4. `<fileids>` -> The file ids of the file you want to add as a dependency, plus the any optional classifiers.

For more infomation please take look at [this](https://cursemaven.com/)

