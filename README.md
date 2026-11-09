# Classroom Uprising

Java campus-life game: a **rule core** plus a **JavaFX frontend** that plays the approved Demo (Days 1–4). The earlier Swing UI, its demonstration content, launcher and Main command-line routes were removed and are not used by the new frontend.

## Build

Install a full **JDK 21**. Gradle uses the Java 21 toolchain for compilation and tests; the existing wrapper and JUnit 6 setup are retained.

```sh
./gradlew test installDist
```

The first build downloads dependencies from Maven Central and the Gradle plugin portal. Once cached, `./gradlew --offline test installDist` works without a new dependency download.

## Run the game

```sh
./gradlew installDist
build/install/ClassroomUprising/bin/ClassroomUprising
```

On Windows use `build\install\ClassroomUprising\bin\ClassroomUprising.bat`. Run it with a full JDK 21 (`JAVA_HOME`), not a JRE: the Java Lab compiles code with the same JDK's `javac`.

Main only opens the game window (`Application.launch(ClassroomApp.class)`). It ignores command-line arguments: there are no modes, debug commands or day jumps. Use the IDE/JVM debugger for debugging, not an in-game command system.

| Input | Action |
| --- | --- |
| Up / Down (or W / S), mouse hover | Move between options |
| Enter / Space, mouse click | Choose; advance dialogue |
| Esc | Open or close the menu (status, calendar, grades) |

The single save slot is `~/.classroom-uprising/day-end-save.properties`. It is written only at a legal day end; **Continue** reopens that closed day without advancing or repeating rewards.

The launch script and window were checked on macOS (Apple Silicon). JavaFX binaries are host-specific, so build and check the distribution on each target OS before relying on it there.

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

The frontend uses **JavaFX** (FXML layouts and CSS). FXGL and libGDX remain build dependencies only: neither engine is started, and there is no framework-switching abstraction. Having both dependencies does not integrate JavaFX and LWJGL event loops.

For a future libGDX/LWJGL3 entry point on macOS, configure its JVM launch with `-XstartOnFirstThread`. This is a JVM setting, not a Main argument. Do not apply it globally to the JavaFX launch. The current JavaFX Main needs neither setting.

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

JavaAssessmentRunner compiles complete Java programs and runs supplied input/output cases in child JVMs. Its time/output limits are not an OS/filesystem/network sandbox; use trusted local submissions. It returns test/style evidence, not an automatic production grading formula. The frontend runs it on a background thread (`LabRunService`), never on the JavaFX thread.

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

## Frontend (JavaFX)

Code is in `com.aphs.classroomuprising.ui`; layouts and the stylesheet are in `src/main/resources/com/aphs/classroomuprising/ui/`. The UI never edits `GameState`: every action goes through `GameController`, and every screen reads a fresh snapshot afterwards.

| Responsibility | Classes / files |
| --- | --- |
| Window, fixed 1280×720 canvas scaled to the window | `ClassroomApp`, `Theme` |
| Screen switching, slanted wipe, input blocking during transitions, Esc menu overlay | `Router`, `PauseMenu`, `PausePanels` |
| Time-of-day change: clock hands sweep from the old period's time to the new one (display times only, not rules) | `ClockShift`, `ClockTimes` |
| Day change: split-flap date flip and a calendar strip that scrolls to the new day | `FlipNumber`, `CalendarStrip` (on `DayIntroScreen`) |
| Shared context; picks the screen from the core's current period | `AppContext` |
| Frontend controller: one core call per player action, refusals turned into results, day-end save path | `GameSession`, `Outcome`, `Fetch` |
| Screens (one per scene; abstract base `Screen`) | `TitleScreen`, `OpeningScreen`, `DayIntroScreen`, `ClassroomScreen`, `OtherClassesScreen`, `HubScreen`, `DayEndScreen`, `LabScreen`, `BlockedScreen` |
| Menus: one selection cursor that travels between options (overshoot and settle, idle breathing and twitch, a trailing accent shard); only the text under the cursor turns dark; neighbours step aside; unselected options shimmer in steps; one activation then the list locks | `ChoiceList`, `CursorGeometry` (pure shape/path maths) |
| Shared motion timing (durations, curves, stepped rate) | `MotionTokens`, `Motion` |
| Reusable controls | `DialogueBox`, `Hud`, `Slant`, `Backdrop` (large background typography), `Toasts`, `Diagnostic` |
| Scene art behind each screen (package `ui.scene`): background art that drifts and is colour-graded per time of day, characters with expressions and speaker focus, foreground plane, ambient grain/motes/vignette | `SceneView`, `BackgroundLayer`, `CharacterView`, `AmbientLayer`, `Grade` |
| Art loading by key from the asset manifest; labelled "missing art" plates for anything not delivered | `AssetManifest`, `AssetStore`, `Placeholders`; files in `src/main/resources/com/aphs/classroomuprising/assets/` |
| Java Lab runs off the UI thread; one run at a time; stale results after edits | `LabRunService` |
| Layout and style | `*.fxml`, `theme.css` (all colours, sizes and states) |

Visual language: one slant angle (about 14°, from the `//` comment mark), ink-indigo background, caret-yellow focus, cyan for code and information, green for HP and passes, violet for SAN, red for failures and late-night risk. Fonts are picked at runtime from installed families (Impact/Menlo/Avenir Next on macOS, with Windows fallbacks).

Backgrounds and characters come only from image files listed in `assets/manifest.properties`; nothing is drawn as stand-in figures in code. Until art is delivered, each listed file appears as a labelled "MISSING ART" plate. Delivery rules (sizes, anchors, expressions, time-of-day variants, colour grades) are in [docs/art-asset-spec.md](docs/art-asset-spec.md).

Connected now: title (new game, continue, quit) → opening scene (text only; the opening battle is not built) → day card → Lesson with its single choice → other classes → Student Hub activities, early or normal sleep, morning bonus slot → day-end save → next day; Esc menu with status, 40-day calendar and grades. Pending rules appear as `warning[Txx]` notices and change nothing. With the Demo profile, Day 5 shows `warning[T07]` and offers a return to the title.

Not connected yet: the Lab screen and its runner service are built and tested, but the backend does not supply Lab problems (statement, class name, tests, rubric, limits), and turning evidence into a mark is T06. Chapter tests (no answer options yet), Midterm/Final decisions (T10), absences and makeups, combat, items and endings have no screens.

## Verification and remaining work

The core tests cover calendar/rules, save round trips and invalid files, and actual Java compilation/runtime cases. Infrastructure tests compile against FXGL/JavaFX, execute libGDX and GLFW native calls without a window and detect duplicate JavaFX native variants. Entry-point tests check that Main only launches the window, never reads its arguments, and that no former command route (`--gui`, `--demo`, `--help`, …) or application parameter is read anywhere in production code. Frontend tests drive `GameSession` against the real core and Demo profile (single settlement, refused repeats, day-end-only saves, load without re-applied rewards, pending T07 without changes) and `LabRunService` (one run at a time, stale detection, failures delivered, real JDK compile on a background thread).

Automated tests do not render the window. Rendering and the Day 1–5 path were checked separately on macOS; full manual play-testing has not been recorded.

[Implementation status](docs/implementation-status.md) records current boundaries. [Sprint1](docs/Sprint1.md) remains the authority for approved rules and T01–T14 pending decisions. The class diagram includes the frontend layer; the sequence diagram traces core operations. Final combat, production grading/SAN formulas, full Retake/reset, endings, activities and authored content remain separate work.
