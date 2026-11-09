# Classroom Uprising

Java campus-life game **infrastructure and rule core**. The previous Swing UI, demonstration content, launcher and Main command-line routes have been removed. Frontend work is separate.

## Build

Install a full **JDK 21**. Gradle uses the Java 21 toolchain for compilation and tests; the existing wrapper and JUnit 6 setup are retained.

```sh
./gradlew test installDist
```

The first build downloads dependencies from Maven Central and the Gradle plugin portal. Once cached, `./gradlew --offline test installDist` works without a new dependency download.

Main is deliberately empty: starting it exits quietly. It does not open a window, parse game/debug arguments, launch a demonstration or start either framework. Use the IDE/JVM debugger for future debugging, not an application-level command system.

## Prepared libraries

| Library | Pinned version / purpose |
| --- | --- |
| FXGL | 21.1 — Java game framework |
| JavaFX | 21.0.11 — controls, FXML and media; includes base/graphics dependencies |
| OpenJFX Gradle plugin | 0.1.0 — selects host-specific JavaFX artifacts |
| libGDX | 1.14.2 — core game API |
| libGDX LWJGL3 backend | 1.14.2 — desktop backend and upstream LWJGL dependencies |
| libGDX desktop natives | 1.14.2 — native runtime support |

All configuration is in `build.gradle`; the project stays a single Gradle project. Both frameworks are on the main compile/runtime classpath. FXGL's transitive OpenJFX entries are excluded because the OpenJFX plugin owns the JavaFX version and platform selection, avoiding mixed Intel/ARM JavaFX jars.

No frontend, renderer, game loop, UI design or framework-switching abstraction is supplied. Choose and implement the actual application startup during frontend work. Merely having both dependencies does not integrate JavaFX and LWJGL event loops.

For a future libGDX/LWJGL3 entry point on macOS, configure its JVM launch with `-XstartOnFirstThread`. This is a JVM setting, not a Main argument. Do not apply it globally to a future FXGL/JavaFX launch. The current empty Main needs neither setting.

`installDist` creates `build/install/ClassroomUprising/` with launch scripts and runtime libraries. The ordinary JAR is not a fat JAR; future framework code should use the Gradle/runtime distribution classpath rather than assume that JAR contains its dependencies. JavaFX binaries are selected for the build host; build and verify the distribution on each target OS/architecture.

Official references: [FXGL / Java compatibility](https://github.com/AlmasB/FXGL/wiki), [OpenJFX Gradle plugin](https://github.com/openjfx/javafx-gradle-plugin), [libGDX versions](https://libgdx.com/dev/versions/).

## Existing core to reuse

| Responsibility | Classes |
| --- | --- |
| Schedule and time periods | SchoolCalendar, DayPlan, CourseType, DayPeriod |
| Student state and explicit effects | StudentState, StatChange, TimedEffect, RuleValues |
| Content and question selection | ContentCatalog |
| Transactional operations | GameController, GameState |
| Marks, absence accounting, carryover | GradeBook, AssessmentRecord, RunProgress |
| Day-end files | SaveManager |
| Java compilation, tests and style findings | JavaAssessmentRunner, StyleRule |

These core source files were retained, not rewritten for a framework. Missing production rules still raise PendingRuleException; `RuleValues.pending()` and `ContentCatalog.pending()` do not invent balancing values or lesson content.

SaveManager accepts active closed-day snapshots only. Loading returns the same closed day; advancing to the next day is a separate controller operation. The frontend should reuse this behavior rather than install a competing save system.

JavaAssessmentRunner compiles complete Java programs and runs supplied input/output cases in child JVMs. Its time/output limits are not an OS/filesystem/network sandbox; use trusted local submissions. It returns test/style evidence, not an automatic production grading formula. A future frontend must keep these blocking operations off its UI/render thread.

## Frontend integration: opt-in Demo profile

The team approved a separate, labelled Demo configuration to unblock frontend work. This does **not** approve its numbers as production balancing. All Demo initial attributes, rule values, lesson text/choices and Hub effects are in `src/main/resources/demo/demo.properties` (UTF-8). `DemoProfile` loads and validates them; malformed/missing values fail explicitly rather than falling back.

```java
DemoProfile demo = DemoProfile.load();
GameController game = demo.newGame(playerName, java.util.random.RandomGenerator.getDefault());
// Display demo.label() visibly. Opening playback belongs to the frontend.
// Only after that playback/acknowledgement:
game.completeOpening();
ContentCatalog.Lesson lesson = game.showLesson();
// Render lesson.text() and its single interaction, then submit a configured choice ID.
game.lessonChoice("ask"); // or "quiet" for this Demo
```

The profile supplies Days 1–4 only, with one ASK_TEACHER node per lesson and zero immediate effects for quiet. Day 3 temporarily uses the ask-teacher variant, not both variants; this is Demo content, not final authoring approval. The calendar stays 40 days/six teaching units. Starting Day 5 Lab still reports T07: there is no supplied assessment bank, Lab rubric or grading policy. Later lesson/review content remains pending. The opening acknowledgement is not a combat implementation.

Initial Demo values are HP 50, SAN 60, Coding Vibe 0 and six zero Knowledge values. Normal sleep restores 6 SAN, early sleep grants 3 SAN on waking, and using late night schedules -4 effective SAN and 0.5 XP gain for the following day. These are adjustable **Demo values only**. Hub IDs are `rest` and `study`; the resource describes each nine-number effect vector. Do not duplicate these values in UI controllers.

- `demo.content().hubActivityIds()` returns immutable sorted IDs, not a phase-eligibility decision.
- Read a fresh `game.snapshot()` after each mutation. Its `lessonShown()` and `hasActiveAssessment()` are authoritative read-only flags; old snapshots do not update themselves.
- `game.sleepNormally()` is accepted only in EVENING or LATE_NIGHT, applies configured SAN recovery once as the day closes, and enters DAY_END. It grants no morning half-slot, adds no late-night penalty, and does not remove existing effects. There is no second recovery on load or next day.
- `sleepImmediatelyAfterSchool()` remains the distinct AFTER_SCHOOL operation; its existing next-morning bonus is unchanged.
- `rules.normalSleepSanRecovery()` is nullable; unconfigured normal sleep reports T04 without changing state. The existing six-argument RuleValues constructor remains source-compatible and leaves this new value pending. Zero is an explicitly permitted recovery, not a default.

Resume through the existing version-1 save format, with the same Demo policy/content:

```java
GameState saved = new SaveManager().load(savePath);
GameController resumed = new GameController(saved, demo.rules(), demo.content(),
        java.util.random.RandomGenerator.getDefault());
// The loaded state is still DAY_END. Advance once when the player continues.
resumed.beginNextDay();
```

The frontend chooses savePath and owns scenes, launch, display parsing, click protection and async Lab UI. No Main arguments, alternate launch route, automatic engine startup, battle rules or scoring bypass were added. IOExceptions from profile loading are configuration errors, not pending gameplay decisions.

## Verification and remaining work

The core tests cover calendar/rules, save round trips and invalid files, and actual Java compilation/runtime cases. Infrastructure tests compile against FXGL/JavaFX, execute libGDX and GLFW native calls without a window, detect duplicate JavaFX native variants, and confirm that Main has no former command behavior. They do not constitute a rendered game or integrated dual-engine application.

[Implementation status](docs/implementation-status.md) records current boundaries. [Sprint1](docs/Sprint1.md) remains the authority for approved rules and T01–T14 pending decisions. The class/sequence diagrams show the core API, not a working frontend. Final combat, production grading/SAN formulas, full Retake/reset, endings, activities and authored content remain separate work.
