# Foundation implementation status

Date: 2026-11-09. Latest action: add the JavaFX frontend for the approved Demo (Days 1–4) and make Main open it, without changing the rule core, save format or build configuration. The previous increment provided the Demo profile, normal sleep and read-only frontend accessors.

This record separates implemented behavior from the full design target. No production balancing configuration or missing game rule is approved by this implementation.

| Area | Current implementation | Boundary |
| --- | --- | --- |
| Calendar | Exact 40-day schedule; six units; Midterm day 20 and Final day 40 | Authored content is external |
| Daily flow | Opening acknowledgement, one Lesson choice, other classes, Hub slots, normal/early sleep, half-slot and closed-day transition | JavaFX screens for Days 1–4 of the Demo; no actual opening battle |
| Effects | Explicit numeric deltas; tutorial negative effects waived; dated next-day effects | Amounts and composition are not production policy defaults |
| Attendance | Two free absences, then percentage-point deductions; leave exempt; no major-exam skip | Tutorial accounting must be supplied; skipped-class free activities not implemented |
| Questions | Fixed ordinary banks; sampled major banks with supplied size/RNG | Test fixtures only; no production bank or timer/answer policy |
| Assessments | Submitted versus official marks; ordinary/makeup return after three days; majors immediate | Marks come from an evaluator, not an invented answer/HP formula |
| Estimates | Separate value and source-labelled provisional effect; removal on official return | Accuracy and repeat policy are not invented |
| Grade feedback | Supplied effect applied exactly once after official return | Automatic average/95 calculation remains T02/T03 |
| Makeups | Caller selects first/second later Lesson; collisions/no-Lesson rejected; same questions, no Lesson reward, configured SAN cost | Detection is a condition flag, not an invented probability |
| Cheating | Configured SAN loss; serious zero survives submission; tutorial exemption | Detection/reward content not implemented |
| Grade arithmetic | 65/10/25 with supplied Midterm relative weight and Participation percent, only after all marks become official | No missing-score zero, floor/cap or HP mapping |
| Run transitions | Two-opportunity counter and selected-owned-reward carryover helper | No complete reset/new-run launch; Midterm Continue/Retake reports T10 |
| Saving | Atomic versioned closed-day snapshot and explicit next-day operation; validation | Drop/Retake/Final policy and migrations remain T12 |
| Java Lab | Actual JDK compilation, child-JVM cases, bounded output/time, independent style findings | Trusted local runner, not a security sandbox or automatic course grader |
| Relationships/rewards | State representation and persistence | No authored interaction/item gameplay |

No production damage, ending, SAN, XP, grade or detection defaults are installed. The separately approved Demo profile contains explicitly provisional initial attributes and activity/rule values; tests retain independent fixtures. PendingRuleException identifies missing gameplay/content T-numbers.

## Checks

Run the Gradle test and jar tasks. The JUnit suite includes calendar and rule tests, invalid-input/non-mutation checks, save round trips/corruption/failure checks, and real trusted-Java process tests.

The previous Swing GUI, its demo content/rubric, GUI-specific tests and launcher remain removed. Main now only launches the JavaFX window; it does not read its arguments and has no debug hooks or alternate routes. Framework dependency tests cover FXGL/JavaFX API availability, a single JavaFX native platform variant, libGDX native pixel operations and a GLFW native query without opening a window. Entry-point tests check that Main only launches the window, never reads its arguments, and that no former command route or application parameter is read by production code.

Command output, hashes and rollback evidence accompany this change as verification artifacts, not fabricated gameplay results.

## Remaining design work

The T01–T14 register in Sprint1 remains authoritative: Final combat, automatic grading/feedback, Retake reset/ending policy, production activity/recovery values, full lesson/question/rubric content and social/reward mechanics remain open. Normal sleep in EVENING/LATE_NIGHT is now confirmed; its recovery amount must be supplied.

The class diagram shows the core classes, the JavaFX frontend layer and Main as its launcher. It does not present the planned StoryController/AssessmentController objects as working code. The sequence diagram traces callable prototype operations and labels separate planned boundaries. The IPO remains the target, not a claim that every row has been delivered.

## Framework infrastructure

Gradle supplies FXGL 21.1, JavaFX 21.0.11, libGDX 1.14.2, the LWJGL3 desktop backend and native libraries. The Java toolchain and compilation target are 21. OpenJFX's Gradle plugin selects host-specific JavaFX binaries, and FXGL's transitive OpenJFX entries are excluded to prevent mixed architecture jars.

The frontend uses plain JavaFX. FXGL and libGDX are not started, and no integration between their event loops is implemented. The framework-only change preserved the rule, persistence and Java-assessment source files. The subsequent backend change adds normal sleep and accessors; persistence and the Java runner remain unchanged. InstallDist packages runtime dependencies; a normal application JAR alone is not a self-contained framework distribution.

## Read-only frontend integration

ContentCatalog.hubActivityIds() returns an immutable, naturally sorted list of configured activity IDs. An empty catalog returns an empty list; the list is not a statement that the current phase permits an activity. GameController still validates availability and consumes time.

GameController.snapshot() exposes GameState.lessonShown() and GameState.hasActiveAssessment(). The lesson flag remains true after a shown lesson until beginNextDay() resets it. The active-assessment flag becomes true only after a successful start and false after successful submission. Snapshots remain detached: read a fresh snapshot after each state-changing operation. No save fields or schema changed.

The frontend no longer needs its own authoritative copy of the lesson/assessment flags. Accessors do not themselves select initial values, grading weights or combat rules.

## Approved Demo and normal sleep increment

`DemoProfile.load()` reads the UTF-8 classpath resource `demo/demo.properties`; `newGame(name, random)` creates an independent Day 1 run with the opening still unacknowledged. Initial HP/SAN/Coding Vibe/six Knowledge values, RuleValues, four lessons and two Hub activities are centralized in that resource. Its label must remain visible in the frontend. No Main or UI launch change accompanies this API.

The Demo covers Days 1–4 on the original 40-day calendar. Day 3 chooses ASK_TEACHER for the Demo only. Later content, quiz answers, per-Lab runner inputs and assessment grading remain pending, including Day 5 T07. Demo values are provisional and do not resolve production T03/T04, Participation conflicts or T10 continuation/Retake.

`sleepNormally()` accepts EVENING/LATE_NIGHT, requires explicitly configured non-negative `normalSleepSanRecovery`, applies it once when closing the day, and reaches DAY_END without an early-sleep entitlement or a new late-night effect. It preserves existing effects. Other phases, repeated calls, checkpoints, missing policy and arithmetic overflow reject transactionally. The old six-argument RuleValues constructor leaves this new field null. Save format version 1 and early-sleep behavior are unchanged; normal recovery is already in the saved StudentState and is not granted again.

Integration tests cover the four-day flow, tutorial suppression, half-slot scaling, next-day-only late-night effects, normal sleep, real day-end save/load, independent new games, resource validation, and read-only snapshots. Rendering a UI and grading a Lab are separate acceptance checks, not claims of these tests.

## JavaFX frontend increment

Package `com.aphs.classroomuprising.ui` with FXML layouts and `theme.css` under `src/main/resources/com/aphs/classroomuprising/ui/`. No core, save-format, Demo-resource or Gradle change.

| Area | Implemented | Boundary |
| --- | --- | --- |
| Launch | Main → `ClassroomApp`; fixed 1280×720 canvas scaled to the window | No arguments read; FXGL/libGDX not started |
| Navigation | `AppContext` picks the screen from the snapshot's period/status; `Router` blocks input during transitions. A time change within a day plays the clock (`ClockShift`); a new day plays the date flip and calendar strip on the day card; other changes use the mask reveal (old UI steps out, new screen opens from where the player chose, ghost edge, new UI steps in) or an artist frame sequence when one is delivered. The Esc menu sits on a captured, blurred, rippling copy of the paused screen | Midterm/Final checkpoints show a T10 notice only; clock times are display-only |
| Actions | `GameSession` makes one `GameController` call per player action and returns Done/Pending/Rejected; `ChoiceList` locks after one activation | Pending rules are shown with their T-number; nothing is bypassed |
| Screens | Title (new/continue/quit), opening scene, day card, classroom Lesson with one choice, other classes, Hub with configured previews, early/normal sleep, morning bonus, day end, Esc menu (status, calendar, grades) | Opening battle not built; opening text is labelled placeholder |
| Saving | Day-end screen writes `~/.classroom-uprising/day-end-save.properties` through SaveManager; Continue reopens that closed day | Single slot; no other save point |
| Java Lab | `LabScreen` and `LabRunService`: background compile/run, one run at a time, stale results after edits, submit refused for stale or missing results | Not reachable with the Demo (Day 5 reports T07); Lab problem data not supplied; evidence-to-mark is T06 |
| Scene art | Every screen draws its background and characters from image files named in `assets/manifest.properties` (`SceneView`): drifting, time-of-day colour grades, optional foreground plane, ambient grain/motes/vignette, character expressions and speaker focus. Undelivered files show as labelled "MISSING ART" plates | No final art delivered yet; the two textures are generated placeholders. Dialogue expression tags (`Speaker[expression]:`) need agreement with content authors |
| Menu motion | `ChoiceList` uses one travelling cursor (stepped overshoot path, idle breathing and twitch, trailing accent shard), dark text only where the cursor covers it, neighbours stepping aside, stepped shimmer; timing comes from `MotionTokens` | Keyboard, mouse; no gamepad or sound yet |
| Not built | Chapter-test screen, absence/makeup flow, Midterm/Final decisions, combat, items, endings | Need backend content/rules first |

Checks for this increment: 220 JUnit tests pass (`./gradlew test`), including frontend tests for `GameSession` against the real core and Demo, and for `LabRunService` with a real JDK. A scratch frame-rate probe measured about 120 fps (the display rate) in every scenario on the development Mac, worst frame under 17 ms. `installDist` builds; the installed launcher was started on macOS (Apple Silicon) and stayed running without errors, also when given old flags. A separate scratch harness (not in the project) drove the real window from New Game through Day 5 and Continue and captured each screen; it reported no exceptions. Manual play-testing and other operating systems have not been checked.
