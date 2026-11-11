# Classroom Uprising

Java campus-life game: a **rule core** plus a **JavaFX frontend** for the approved Demo (Days 1–4). The latest backend opening exam and authored classroom menus await Claude frontend integration; the current old opening acknowledgement is rejected until the battle is played. The earlier Swing UI, its demonstration content, launcher and Main command-line routes were removed and are not used by the new frontend.

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
| Hold Enter, Space or the left mouse button (anywhere in the window) | Fast-forward whatever is playing until released: dialogue, screen changes, the clock and the Esc menu opening (3× speed). A FAST badge shows while it works. It never makes a choice: a held key's repeats are swallowed, and a held click that fast-forwarded something does not count as a click. Not in text fields or during a combat question |
| H, scroll up over the dialogue | Open the log: read every earlier line of the scene again, with the slide that was up (Z or Enter: that slide large) |
| Z, click on the slide | During a lecture: open the current slide large, and close it again (Enter or Space also close it) |
| Left / Right (or A / D) | In SOUND (title or Esc menu): change music or effects volume |
| Click the XP total (HUD) | Show what it is made of: Coding Vibe and each unit's knowledge (click again or Esc to close) |
| Opening exam, your turn: ↑↓ and Enter | FACE THE NEXT QUESTION (Mr. Reward fires it at you), ITEM, LOG. The ten questions share 180 s of exam time, shown at the top all through the battle. It runs continuously from battle start through menus and animations, and freezes only at an ending |
| A–E (or ↑↓), then Enter; or click an option | Opening exam: answer the question on the clock (the answer counts at once); then Enter or a click fires it back at him |
| R, ITEM in your turn, or click the duck | Opening exam: use Ms Tang's rubber duck (in your turn or during a question; answering and facing the next question wait until it has worked) |
| Esc | Back one level, anywhere: the open XP panel, a large slide, the log, the response menu (back to the first menu), the title's name or sound panel, the Esc menu's SOUND page, then the Esc menu itself. With nothing to go back from, it opens the Esc menu (status, calendar, grades, sound) where the game allows it |

The bottom-right corner of every screen shows the keys that work there, as yellow key caps on a dark plate.

The single save slot is `~/.classroom-uprising/day-end-save.properties`. It is written only at a legal day end; **Continue** reopens that closed day without advancing or repeating rewards. Volume levels are window preferences in `~/.classroom-uprising/settings.properties`, written when you change them; they are not part of the save.

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

The team approved a separate, labelled Demo configuration to unblock frontend work. This does **not** approve its numbers as production balancing. Day 1–4 Demo initial attributes, rule values, lesson text/choices and Hub effects are in `src/main/resources/demo/demo.properties` (UTF-8). `DemoProfile` loads and validates them; malformed/missing values fail explicitly rather than falling back.

```java
DemoProfile demo = DemoProfile.load();
GameController game = demo.newGame(playerName, java.util.random.RandomGenerator.getDefault());
// Display demo.label() visibly. Opening playback belongs to the frontend.
// Play the opening via startOpeningBattle/hitOpeningQuestion/startOpeningQuestion/answerOpeningQuestion.
// See docs/frontend-opening-handoff.md for timer, duck and ending callbacks.
// Only after a terminal result AND its ending playback:
game.completeOpening();
ContentCatalog.Lesson lesson = game.showLesson();
// Render lesson.text(), then show participation/quiet followed by decision.responses().
// Opening the response menu makes no core call. Submit only the selected final option ID.
int lessonDay = game.snapshot().day();
game.lessonChoice(lessonDay, selectedOptionId); // or lesson.decision().skip().id()
```

The profile supplies Days 1–4 only. Day 1 uses RAISE_HAND (answer Mr. Reward); Day 2 uses ASK_TEACHER (choose a question). Every shipped lesson has three authored response options plus a neutral quiet option. One final response settles the class; opening the response menu grants nothing. Only an appropriate response grants its configured XP bonus. An inappropriate response has no XP bonus and a Demo-only -2 SAN effect, waived with a warning on Days 1–2. Quiet has zero immediate effects. Day 3 temporarily uses the ask-teacher variant, not both variants; this is Demo content, not final authoring approval. The calendar stays 40 days/six teaching units. Starting Day 5 Lab still reports T07: there is no supplied assessment bank, Lab rubric or grading policy. Later lesson/review content remains pending. The separate opening exam is implemented in the backend, with ten sourced MC questions and 67/89 story branches; frontend request 36 supplies its screens/animations, with continuous-clock visual acceptance still to run. See [opening handoff](docs/frontend-opening-handoff.md) and [question sources](docs/opening-question-sources.md).

Day 1 initial Demo values are HP 50, SAN 60, Coding Vibe 0 and six zero Knowledge values. Normal sleep restores 6 SAN, early sleep grants 3 SAN on waking, and using late night schedules -4 effective SAN and 0.5 XP gain for the following day. These are adjustable **Demo values only**. Hub IDs are `rest` and `study`; the resource describes each nine-number effect vector. Do not duplicate these values in UI controllers.

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
| Screen changes: the old UI steps out, the new screen opens out of the old one through a growing slanted mask that starts where the player chose, a cool "ghost" of the new screen runs just ahead of the edge, the new UI steps in. Input is blocked while any transition runs | `Router`, `RevealMask` (pure geometry), `RevealOriginEvent` |
| Artist-drawn transitions: when a `sequence.screen` or `sequence.menu-open` frame sequence is delivered, it plays instead of the built-in one | `FrameSequence`, `AssetStore.sequence` |
| Esc menu over a captured, blurred, rippling copy of the paused screen; opens and closes through a stepped mask | `PauseMenu`, `MenuBackdrop`, `PausePanels` |
| Time-of-day change: translucent clock overlay; hands sweep (minute-hand smear) then tick the last three minutes; a 24-hour ring shows the day's periods with the destination lit; the scene behind is re-graded to the new time of day. Clock times are display only, not rules. Face and hands can be artist images (`ui.clock-*`) | `ClockShift`, `ClockTimes` |
| Day change: split-flap date flip with flap shading, cast shadow and landing bounce; the finished day is struck with "//"; the strip overshoots and settles; the scene brightens from night to the new day's grade; the next assessment is shown | `FlipNumber`, `CalendarStrip`, `DayIntroScreen` |
| Shared context; picks the screen from the core's current period | `AppContext` |
| Frontend controller: one core call per player action, refusals turned into results, day-end save path | `GameSession`, `Outcome`, `Fetch` |
| Screens (one per scene; abstract base `Screen`) | `TitleScreen`, `OpeningScreen`, `DayIntroScreen`, `ClassroomScreen`, `CombatScreen`, `OtherClassesScreen`, `HubScreen`, `DayEndScreen`, `LabScreen`, `BlockedScreen` |
| Assessment combat: targets in difficulty order with their weights, the backend's question clock against the 30 s soft limit, answer capture and lock, evaluating/reward busy states, Dasher and Combo status, call-out banners (overtime, Dasher ready, all clear), HP hit, results breakdown and settlement. Everything shown comes from `combatView()`; the screen never times, weighs, damages or marks | `CombatScreen`, `TimeGauge`, `CutIn`, `CombatText` |
| Combat answers go to a trusted evaluator off the UI thread, one at a time; results return with their ticket; results that arrive after leaving the combat are dropped; a failed result keeps the ticket and work for a retry | `CombatEvaluator` (no implementation shipped), `CombatEvaluations` |
| Menus: one selection cursor that travels between options (overshoot and settle, idle breathing and twitch, a trailing accent shard); only the text under the cursor turns dark; neighbours step aside; unselected options shimmer in steps; one activation then the list locks | `ChoiceList`, `CursorGeometry` (pure shape/path maths) |
| Shared motion timing (durations, curves, stepped rate) | `MotionTokens`, `Motion` |
| Reusable controls | `DialogueBox`, `Hud`, `KeyHints` (the bottom-right key caps), `Slant`, `Backdrop` (large background typography), `Toasts`, `Diagnostic` |
| Stats as shown: HP a number; SAN a ten-segment bar with no number (changes as a share of the bar, "SAN −2%"); Coding Vibe and the six units' knowledge as one XP total, whose parts open on a click (HUD chip, Esc menu STATUS; Esc closes the HUD panel). Display only; the core keeps the separate values. Status effects: an HP drop flashes the screen red and jolts the HP chip; below HP 10 the HP chip turns red and its number blinks, on every screen; below SAN 10 the SAN bar twitches; only in a battle (the opening exam, an assessment) the screen's edges also pulse red, under the panels | `Stats`, `SanBar`, `XpBreakdown`, `Hud`, `StatDelta` |
| Lecture slides: a lecture line that starts with `[slide:name]` turns a projector panel beside Mr. Reward to that slide (the cue is not shown); the dialogue box carries what he says about it, and lines without a cue keep the slide that is up. New slides wipe in over the old one in drawn steps; the panel leaves when the lecture ends. Z or a click opens the slide large (the lecture waits). Images are listed as `slide.<name>` in the asset manifest; the Grade 11 review uses 12 pages of the class deck *Review Overview* | `SlideProjector`, `DialogueScript` (cue parsing), `ClassroomScreen` |
| Music and interface sounds: each screen names a music cue (`title`, `opening`, `school`, `after-school`, `night`, `combat`); the same cue carries on across screens, a new one cross-fades. Controls ask for interface sounds (cursor, confirm, menu, dialogue, slide, tip, transition, clock, cut-in, hit, shatter) through an event, so they know nothing about audio. Missing or unplayable files are silent and listed as missing | `AudioDirector`, `Sfx`, `Screen.music()`; files via `AssetStore.bgm`/`sfx` |
| Volume: music and effects levels (0–10) on the title screen and in the Esc menu (SOUND); kept in a settings file next to the save, never in the save | `SoundPanel`, `AudioSettings` |
| Hold to fast-forward, for the whole window: holding Enter, Space or the mouse button speeds up dialogue and transitions (a held key's repeats and the click that ends a useful hold are swallowed, so it cannot press a menu that appears); Esc goes back one level (`Screen.back()`) before it opens the menu; the log lists the scene's lines with the slide each was said over, also from the choice menus | `FastForward`, `Router` (rate, FAST badge, Esc chain), `DialogueBox`, `DialogueHistory`, `DialogueLog` |
| Lesson decision after the lecture: take part or stay quiet, then one of the authored responses (full sentences, wrapped); only the final choice settles the class, for the captured lesson day; authored feedback, tutorial warnings and stat changes follow. If you stay quiet, a classmate raises a hand (or asks) with the first appropriate response and Mr. Reward answers with its authored feedback; this is only shown, the class is already settled as quiet | `LessonMenu` (menus from `Lesson.decision()`, `classmateAnswer`), `ClassroomScreen`, `GameSession.lessonChoice(lessonDay, optionId)` |
| Tutorial tips: during the first two days (the Grade 11 review), how-to-play notices appear as a tip card at the top of the screen when a system first matters (lesson choice, other classes, Hub, evening sleep, bonus morning, day-end save). They are the game's voice, never a character's dialogue, and state no amounts | `TutorialTips` (text), `TipLayer` (card; shown after a transition, removed on screen change) |
| Scene art behind each screen (package `ui.scene`): background art that drifts and is colour-graded per time of day, characters with expressions and speaker focus, foreground plane, ambient grain/motes/vignette | `SceneView`, `BackgroundLayer`, `CharacterView`, `AmbientLayer`, `Grade` |
| Art loading by key from the asset manifest; labelled "missing art" plates for anything not delivered | `AssetManifest`, `AssetStore`, `Placeholders`; files in `src/main/resources/com/aphs/classroomuprising/assets/` |
| Java Lab runs off the UI thread; one run at a time; stale results after edits | `LabRunService` |
| Layout and style | `*.fxml`, `theme.css` (all colours, sizes and states) |

Visual language: one slant angle (about 14°, from the `//` comment mark), ink-indigo background, caret-yellow focus, cyan for code and information, green for HP and passes, violet for SAN, red for failures and late-night risk. Fonts are picked at runtime from installed families (Impact/Menlo/Avenir Next on macOS, with Windows fallbacks).

Backgrounds and characters come only from image files listed in `assets/manifest.properties`; nothing is drawn as stand-in figures in code. Until art is delivered, each listed file appears as a labelled "MISSING ART" plate (slides as "MISSING SLIDE"). Music files are not delivered yet, so the game has no music until they are added; the interface sounds are simple synthesized placeholders. Delivery rules (sizes, anchors, expressions, time-of-day variants, colour grades, slides, music and sound formats) are in [docs/art-asset-spec.md](docs/art-asset-spec.md).

Connected now: title (new game, continue, sound, quit) → opening exam (story, then a turn-based battle: ten question bullets fired by Mr. Reward one per round, each answered against the shared continuous exam clock and fired back, one rubber duck, then a 67 or 89 ending; both lead to Day 1) → day card → Lesson (slides when its lecture cues them) with its decision: take part or stay quiet, then one authored response → other classes → Student Hub activities, early or normal sleep, morning bonus slot → day-end save → next day; Esc menu with status, 40-day calendar, grades and sound volume. Assessment days start a combat through `startCombatAssessment()` and open `CombatScreen`; settling returns to the normal day (or the T10 checkpoint notice after Midterm/Final). The dialogue box is only for characters: tutorial explanations are tip cards, and schedule notices on Review and assessment days are a "TODAY · …" line, not dialogue. Pending rules appear as `warning[Txx]` notices and change nothing. With the shipped Demo, Day 5 still shows `warning[T07]` (no question bank, no combat policy) and offers a return to the title.

The combat screen has been driven end to end only with test fixtures (a fixture bank, fixture `CombatRules` and a fixture evaluator, in tests and a scratch harness). To play it in the game the backend must supply: approved `CombatRules` (T01/T06/T07/T09), a trusted evaluator for answers (`CombatEvaluator`: T06 for Labs, T07 for questions), and assessment banks with difficulty ranks (T07). Until then submission reports that rule ID and creates no ticket. Answers are captured as free text (a code editor for Labs) because questions have no options or answer keys yet; that capture format still needs agreement.

Not connected yet: the standalone Lab screen and its runner service are built and tested, but the backend does not supply Lab problems (statement, class name, tests, rubric, limits). Midterm/Final decisions (T10), absences and makeups, items (inventory, use, effects: T09) and endings have no screens.

## Combat backend integration

The combat backend is available through `CombatRules`, `CombatSession` views and the new GameController operations. The frontend implementation requirements and copyable Claude prompt are in [frontend combat handoff](docs/frontend-combat-handoff.md).

It supports authored difficulty ordering, player-selected answer order, a 30-second soft threshold, last-answer Dasher, timely-completion Combo, configured HP costs, backend-clock reward delays, async evaluation tickets and settlement through the existing grade flow. The new path rejects direct legacy score submission. Existing constructors, the Day 1–4 Demo, Main and the current UI remain compatible.

The .01-versus-.1 weight conflict, HP formula, timeout/Dasher priority, evaluation timing, reward-clock inclusion and score conversion are explicit policy inputs. No production defaults or new assessment content were invented. Reward-delay APIs are timing only, not inventory/effect/cheating operations. The frontend side is `CombatScreen` and the combat methods of `GameSession` (see Frontend above).

## Verification and remaining work

The core tests cover calendar/rules, save round trips and invalid files, and actual Java compilation/runtime cases. Infrastructure tests compile against FXGL/JavaFX, execute libGDX and GLFW native calls without a window and detect duplicate JavaFX native variants. Entry-point tests check that Main only launches the window, never reads its arguments, and that no former command route (`--gui`, `--demo`, `--help`, …) or application parameter is read anywhere in production code. Frontend tests drive `GameSession` against the real core and Demo profile (single settlement, refused repeats, day-end-only saves, load without re-applied rewards, pending T07 without changes) and `LabRunService` (one run at a time, stale detection, failures delivered, real JDK compile on a background thread).

Automated tests do not render the window. Rendering and the Day 1–5 path were checked separately on macOS; full manual play-testing has not been recorded.

[Implementation status](docs/implementation-status.md) records current boundaries. [Sprint1](docs/Sprint1.md) remains the authority for approved rules and T01–T14 pending decisions. The class diagram includes the frontend layer; the sequence diagram traces core operations. Final combat, production grading/SAN formulas, full Retake/reset, endings, activities and authored content remain separate work.

## Authored daily lessons and classroom choices (backend increment, 2026-11-10)

`SchoolCalendar`/`DayPlan` still own the 40-day course schedule. `ContentCatalog.Lesson` now supplies the title, unchanged lecture text/slide cues, one interaction kind, and a `LessonDecision` with its prompt and ordered options. Each option contains a stable ID, display label, outcome, explicit effect and feedback. No generic scripting engine or duplicate calendar is introduced.

Content authors can copy [the populated lesson template](docs/templates/lesson.properties), then update the registered lesson day, IDs, labels and explicit Demo effects. The loader rejects missing/unknown fields, duplicate IDs and contradictory effects. `lessonDays()` lists authored lesson availability, not the full semester.

Use `GameController.lessonChoice(lessonDay, optionId)` to guard stale-day callbacks; it resolves the authored ID transactionally and consumes the ICS slot once. Literal `ask`/`raise` actions no longer earn anything for the shipped Demo. The older method/three-argument Lesson constructor remain for explicit legacy fixture catalogs, not as automatic correct-answer aliases. Day-end save format, Main, CLI policy, combat and the 31 review lecture lines are unchanged.

**Frontend integration (frontend request 22).** After the lecture, `ClassroomScreen` offers two menus built by `LessonMenu` from `Lesson.decision()`: take part (RAISE YOUR HAND on Day 1, ASK A QUESTION from Day 2) or the authored quiet option, then the three authored responses in order, with BACK (or Backspace) returning to the first menu. Opening or leaving these menus calls nothing in the core. The final choice goes once through `GameSession.lessonChoice(lessonDay, optionId)` with the day captured when the lecture opened. Afterwards Mr. Reward's authored feedback appears in the dialogue box (staying quiet shows its narration line instead), tutorial warnings as a card, and stat changes as toasts. When a decision prompt is a spoken line (Day 1), Mr. Reward asks it in the dialogue box before the menu. The lesson title replaces "LESSON" in the HUD course line. The obsolete `ask` fixture steps in the frontend tests now use authored option IDs; `LessonDecisionFlowTest` covers the menus and every settlement case. See [the frontend classroom handoff](docs/frontend-classroom-handoff.md).

## Opening backend — continuous exam clock (request 25, 2026-11-11)

Opening HP starts at **9 with no upper cap**. Each question impact costs 1 HP; zero HP passes out immediately before reveal. Answering costs no HP. Exactly one rubber duck recovers 3 HP after 5 seconds; no SAN/XP change. Opening Demo SAN is 1, Coding Vibe and all six Knowledge values are 0, separate from the Day 1 profile.

**All ten questions share 180 seconds of continuous time starting at `startOpeningBattle()`.** Menus/log, flight, ripple, answering, verdict, folding and counter-shot all spend time. Only a settled ending freezes it. `startOpeningQuestion(id)` still marks reveal completion and permits answers, but does not start/resume/reset time. `View.clockRunning()` equals `!finished()`; `remaining()` decreases in every live phase. The pulse and late action callbacks settle TIMEOUT / 67 before applying an action. An expired impact cannot deduct HP or replace TIMEOUT with PASS_OUT. Duck recovery strictly before the deadline still settles; timeout wins a tie.

Wrong answer, timeout or HP loss gives 67; ten correct with positive HP gives 89. Both endings enter fresh summer-school Day 1 with no current-term grade/stat/item carryover. Question content, authored firing order, normal semester CombatSession/CombatRules, Main, dependencies, assets and save schema are unchanged.

Frontend requests 34/36 already connected hit/reveal callbacks, the global clock and stage-specific timeout handling. This backend increment changes no frontend implementation; it migrates the four old timing expectations named in `OpeningFlowTest` by request 36. Baseline **443/443** and modified **462/462** headless tests pass. Real-window checks for the newly reachable animation-timeout paths remain with Claude. See [current API, acceptance checks and Claude prompt](docs/frontend-opening-handoff.md).

Historical timing decisions: backend request 23 split impact damage from post-reveal clock start; request 24 introduced one active-answering-only budget. The user's 2026-11-11 continuous-clock decision supersedes those pause/start rules, not the hit/reveal lifecycle. The 180-second amount is unchanged; the animation-time estimate is not a balancing change.
