# Foundation implementation status

Date: 2026-11-09. Current action: provide the approved opt-in Demo profile, normal sleep and read-only frontend accessors; keep UI/Main/framework setup unchanged.

This record separates implemented behavior from the full design target. No production balancing configuration or missing game rule is approved by this implementation.

| Area | Current implementation | Boundary |
| --- | --- | --- |
| Calendar | Exact 40-day schedule; six units; Midterm day 20 and Final day 40 | Authored content is external |
| Daily flow | Opening acknowledgement, one Lesson choice, other classes, Hub slots, normal/early sleep, half-slot and closed-day transition | Core API only; no frontend or actual opening battle |
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

The previous GUI, its demo content/rubric, GUI-specific tests and launcher have been removed. Main is an empty entry point without argument parsing, output, debug hooks or engine startup. Framework dependency tests cover FXGL/JavaFX API availability, a single JavaFX native platform variant, libGDX native pixel operations and a GLFW native query without opening a window. Entry-point tests check that old argument strings have no behavior.

Command output, hashes and rollback evidence accompany this change as verification artifacts, not fabricated gameplay results.

## Remaining design work

The T01–T14 register in Sprint1 remains authoritative: Final combat, automatic grading/feedback, Retake reset/ending policy, production activity/recovery values, full lesson/question/rubric content and social/reward mechanics remain open. Normal sleep in EVENING/LATE_NIGHT is now confirmed; its recovery amount must be supplied.

The class diagram shows the remaining core classes and the empty Main entry point. It does not present the planned StoryController/AssessmentController objects as working code. The sequence diagram traces callable prototype operations and labels separate planned boundaries. The IPO remains the target, not a claim that every row has been delivered.

## Framework infrastructure

Gradle supplies FXGL 21.1, JavaFX 21.0.11, libGDX 1.14.2, the LWJGL3 desktop backend and native libraries. The Java toolchain and compilation target are 21. OpenJFX's Gradle plugin selects host-specific JavaFX binaries, and FXGL's transitive OpenJFX entries are excluded to prevent mixed architecture jars.

Both framework APIs are available to future frontend code. Neither is started by Main, and no integration between their event loops is implemented. The framework-only change preserved the rule, persistence and Java-assessment source files. The subsequent backend change adds normal sleep and accessors; persistence and the Java runner remain unchanged. InstallDist packages runtime dependencies; a normal application JAR alone is not a self-contained framework distribution.

## Read-only frontend integration

ContentCatalog.hubActivityIds() returns an immutable, naturally sorted list of configured activity IDs. An empty catalog returns an empty list; the list is not a statement that the current phase permits an activity. GameController still validates availability and consumes time.

GameController.snapshot() exposes GameState.lessonShown() and GameState.hasActiveAssessment(). The lesson flag remains true after a shown lesson until beginNextDay() resets it. The active-assessment flag becomes true only after a successful start and false after successful submission. Snapshots remain detached: read a fresh snapshot after each state-changing operation. No save fields or schema changed.

The frontend no longer needs its own authoritative copy of the lesson/assessment flags. Accessors do not themselves select initial values, grading weights or combat rules.

## Approved Demo and normal sleep increment

`DemoProfile.load()` reads the UTF-8 classpath resource `demo/demo.properties`; `newGame(name, random)` creates an independent Day 1 run with the opening still unacknowledged. Initial HP/SAN/Coding Vibe/six Knowledge values, RuleValues, four lessons and two Hub activities are centralized in that resource. Its label must remain visible in the frontend. No Main or UI launch change accompanies this API.

The Demo covers Days 1–4 on the original 40-day calendar. Day 3 chooses ASK_TEACHER for the Demo only. Later content, quiz answers, per-Lab runner inputs and assessment grading remain pending, including Day 5 T07. Demo values are provisional and do not resolve production T03/T04, Participation conflicts or T10 continuation/Retake.

`sleepNormally()` accepts EVENING/LATE_NIGHT, requires explicitly configured non-negative `normalSleepSanRecovery`, applies it once when closing the day, and reaches DAY_END without an early-sleep entitlement or a new late-night effect. It preserves existing effects. Other phases, repeated calls, checkpoints, missing policy and arithmetic overflow reject transactionally. The old six-argument RuleValues constructor leaves this new field null. Save format version 1 and early-sleep behavior are unchanged; normal recovery is already in the saved StudentState and is not granted again.

Integration tests cover the four-day flow, tutorial suppression, half-slot scaling, next-day-only late-night effects, normal sleep, real day-end save/load, independent new games, resource validation, and read-only snapshots. Rendering a UI and grading a Lab are separate acceptance checks, not claims of these tests.
