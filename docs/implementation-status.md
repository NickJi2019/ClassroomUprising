# Foundation implementation status

Date: 2026-11-10. Current action: backend-only previous-semester opening exam and story; HP 9 initially with no upper cap, one duck, immediate judging, hard decreasing deadlines, and 67/89 branches. Frontend two-level menu wiring and frontend-owned tests remain with Claude; no production UI, Main, assets or build dependency change.

This record separates implemented behavior from the full design target. No production balancing configuration or missing game rule is approved by this implementation.

| Area | Current implementation | Boundary |
| --- | --- | --- |
| Calendar | Exact 40-day schedule; six units; Midterm day 20 and Final day 40 | Authored content is external |
| Daily flow | Terminal opening acknowledgement, one Lesson choice, other classes, Hub slots, normal/early sleep, half-slot and closed-day transition | New two-level classroom menus await Claude; existing flat UI is not migrated; opening screen/animations not migrated |
| Opening exam | Isolated OpeningBattle + OpeningContent; ten reviewed questions; 1 HP per answer; one consumable duck; backend timing and both ending cues/dialogue | UI/animation and associated frontend tests await Claude; no current-term grade pollution |
| Effects | Explicit numeric deltas; tutorial negative effects waived; dated next-day effects | Amounts and composition are not production policy defaults |
| Attendance | Two free absences, then percentage-point deductions; leave exempt; no major-exam skip | Tutorial accounting must be supplied; skipped-class free activities not implemented |
| Questions | Fixed ordinary banks; sampled major banks; optional authored difficulty ranks for combat | Full question content, answer keys and approved timing choices remain explicit |
| Assessments | Combat state/tickets and configured settlement, plus submitted versus official marks; ordinary/makeup return after three days; majors immediate | Marks come from an evaluator, not an invented answer/HP formula |
| Estimates | Separate value and source-labelled provisional effect; removal on official return | Accuracy and repeat policy are not invented |
| Grade feedback | Supplied effect applied exactly once after official return | Automatic average/95 calculation remains T02/T03 |
| Makeups | Caller selects first/second later Lesson; collisions/no-Lesson rejected; same questions, no Lesson reward, configured SAN cost | Detection is a condition flag, not an invented probability |
| Cheating | Configured SAN loss; serious zero survives submission; tutorial exemption | Detection/reward content not implemented |
| Grade arithmetic | 65/10/25 with supplied Midterm relative weight and Participation percent, only after all marks become official | No missing-score zero, floor/cap or HP mapping |
| Run transitions | Two-opportunity counter and selected-owned-reward carryover helper | No complete reset/new-run launch; Midterm Continue/Retake reports T10 |
| Saving | Atomic versioned closed-day snapshot and explicit next-day operation; validation | Drop/Retake/Final policy and migrations remain T12 |
| Java Lab | Actual JDK compilation, child-JVM cases, bounded output/time, independent style findings | Trusted local runner, not a security sandbox or automatic course grader |
| Relationships/rewards | State representation and persistence | No authored interaction/item gameplay |

No unspecified production damage, ending, SAN, XP, grade or detection defaults are installed. The explicit opening exception has fixed 1-HP cost, 67/89 story results and configured Demo attributes/item values. The separately approved Demo profile contains explicitly provisional initial attributes and activity/rule values; tests retain independent fixtures. PendingRuleException identifies missing gameplay/content T-numbers.

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
| Navigation | `AppContext` picks the screen from the snapshot's period/status; `Router` blocks input during transitions. A time change within a day plays the clock (`ClockShift`: smear, final ticks, 24-hour ring, scene re-graded behind it); a new day plays the date flip and calendar strip on the day card (finished day struck out, night-to-day grade, next assessment from the fixed calendar); other changes use the mask reveal (old UI steps out, new screen opens from where the player chose, ghost edge, new UI steps in) or an artist frame sequence when one is delivered. The Esc menu sits on a captured, blurred, rippling copy of the paused screen | Midterm/Final checkpoints show a T10 notice only; clock times are display-only |
| Actions | `GameSession` makes one `GameController` call per player action and returns Done/Pending/Rejected; `ChoiceList` locks after one activation | Pending rules are shown with their T-number; nothing is bypassed |
| Screens | Title (new/continue/sound/quit), opening scene, day card, classroom Lesson with one choice, other classes, Hub with configured previews, early/normal sleep, morning bonus, day end, Esc menu (status, calendar, grades, sound) | Opening battle not built; opening text is labelled placeholder |
| Saving | Day-end screen writes `~/.classroom-uprising/day-end-save.properties` through SaveManager; Continue reopens that closed day | Single slot; no other save point |
| Java Lab | `LabScreen` and `LabRunService`: background compile/run, one run at a time, stale results after edits, submit refused for stale or missing results | Not reachable with the Demo (Day 5 reports T07); Lab problem data not supplied; evidence-to-mark is T06 |
| Scene art | Every screen draws its background and characters from image files named in `assets/manifest.properties` (`SceneView`): drifting, time-of-day colour grades, optional foreground plane, ambient grain/motes/vignette, character expressions and speaker focus. Undelivered files show as labelled "MISSING ART" plates | No final art delivered yet; the two textures are generated placeholders. Dialogue expression tags (`Speaker[expression]:`) need agreement with content authors |
| Menu motion | `ChoiceList` uses one travelling cursor (stepped overshoot path, idle breathing and twitch, trailing accent shard), dark text only where the cursor covers it, neighbours stepping aside, stepped shimmer; timing comes from `MotionTokens` | Keyboard, mouse; no gamepad |
| Tutorial tips | Days 1–2 (the Grade 11 review, labelled `GR11 REVIEW`) show how-to-play tip cards from `TutorialTips` via `TipLayer` at the moment each system first matters; the dialogue box is only used for characters, and Review/assessment-day schedule lines are no longer dialogue | Tip text names approved rules and controls only, no amounts. The Demo's Day 1–2 backend text now contains Mr. Reward's Grade 11 lectures: Java files/main, types, variables, operators and decisions, then loops, methods/pythag, self-directed topics and automated testing. Gameplay hints are separate tip-card content. Lecture text and slide cues are unchanged. New backend titles/decisions replace direct ask bonuses; frontend labels/menu integration remains pending |
| Assessment combat | `CombatScreen` over `GameSession`'s combat methods: targets in difficulty order, backend clock and 30 s soft limit, answer capture and lock, evaluating/reward busy states, Dasher/Combo display, call-out banners, HP hit, results and settlement. `CombatEvaluations` sends answers to a `CombatEvaluator` off the UI thread, one at a time, and drops results after the player leaves | Not reachable with the shipped Demo: Day 5 reports T07 (no bank) and combat policy is pending. No evaluator is shipped; submission reports T06 (Lab) or T07 and creates no ticket. Answers are free text pending question options. Reward delays are timing only (items T09). No pause while a question's clock runs |
| Lecture slides | `SlideProjector` in the classroom: a lecture line that starts with `[slide:name]` (parsed by `DialogueScript`, not shown) turns the projector to that slide; the dialogue box shows the spoken explanation; stepped wipe on each new slide; the panel leaves when the lecture ends; Z or a click opens the slide large while the lecture waits. 12 useful pages of the class deck *Review Overview* are bundled (`slides/gr11-review/`, listed as `slide.<name>`); title, joke, divider and policy pages (1, 3–6, 17) are left out | The bundled Day 1–2 lecture text now matches the frontend request verbatim: 16 and 15 spoken lines cue seven and five slides respectively. Which slide a line shows is now part of the lecture text, so reordering lines cannot put a slide out of step; a test checks every cued slide is listed and delivered |
| Music and sounds | `AudioDirector`: each screen names a music cue (title, opening, school, after-school, night, combat); the same cue continues across screens, a change cross-fades; interface sounds requested by controls through `Sfx` events. Volume (music, effects, 0–10) on the title screen and in the Esc menu, kept in `settings.properties` beside the save | No music files delivered (silent until added). The 11 interface sounds are synthesized placeholders. Missing or unplayable files are silent and listed as missing. Sound never changes game state |
| Not built | Absence/makeup flow, Midterm/Final decisions, items, endings, opening battle | Need backend content/rules first |

Checks for the review slides: the backend integration replaces both bundled property lines verbatim from backend-request-review-lectures.md. The two old example-running tests now check the supplied narration (println, the even-number loop, pythag and array length); the existing out-of-bounds test is retained. A per-day content contract checks all 31 lines against the expected seven/five slide sequence, hidden cues and the 180-character text bound. Existing slide tests check the manifest and delivered assets. The frontend previously reported that this exact draft fitted the real classroom screen and the panel left after each lecture; that visual result is separate from this backend content/integration check.

Checks for slides and sound: 317 JUnit tests passed at the time (15 new: manifest parsing of slides/music/sounds and malformed entries, slide-per-line selection (since replaced by cues), every `Sfx` listed and delivered, every music cue listed, volume clamping, settings round trip without touching the save). A scratch harness stepped through the Day 1 lecture in the real window: all 13 lines showed the expected slide, Esc and Enter only closed the large view (no menu, no skipped line), the panel left after the lecture, and the SOUND rows changed and saved the levels. A second harness played a generated tone through `AudioDirector` with JavaFX media: fade-in to the set level, same cue not restarted, old track released after its fade, volume change applied at once, an unplayable file dropped and listed after its 5 s ready limit, and sounds and music read from the installed jar. The full Day 1–5 harness and the combat harness reported no exceptions. Nobody has listened to the sounds in a play session, and Windows has not been checked.

Checks for the combat frontend: 292 JUnit tests pass (the backend's 273 plus 19 frontend tests). `GameSessionCombatTest` drives the real core with a fixture bank, fixture `CombatRules`, fixture evaluator results and a fake clock, covering handoff acceptance checks 1–7: card weights unchanged by answer order and the Dasher on the last answer; a repeated start not restarting the clock, exactly 30 s on time and answering after it; submission lock and the T07 refusal to switch; duplicate and other-run tickets refused; HP equal to the core's value; Safe 5 s and the fixture Risky duration, early and overlapping reward use refused; T07/T09/T01/T06 reported with nothing changed; three-day return and day-end-only saving. `CombatEvaluationsTest` covers one-at-a-time dispatch, dropped results after abandoning, failures kept for retry and UI-thread delivery. A scratch harness drove `CombatScreen` in the real window with fixtures through select, answer, overtime, a Safe reward delay, Dasher, all clear, results and settlement, and separately without an evaluator (warning[T06]); it reported no exceptions, and the frame rate stayed at about 120 fps (worst frame 17.4 ms, when an evaluation is accepted). Check 8 rests on the existing Day 1–4 tests and launch check. Manual play-testing and other operating systems have not been checked.

Checks for the first frontend increment: 224 JUnit tests passed (`./gradlew test`), including frontend tests for `GameSession` against the real core and Demo, and for `LabRunService` with a real JDK. A scratch frame-rate probe measured about 120 fps (the display rate) in every scenario on the development Mac, worst frame under 17 ms. `installDist` builds; the installed launcher was started on macOS (Apple Silicon) and stayed running without errors, also when given old flags. A separate scratch harness (not in the project) drove the real window from New Game through Day 5 and Continue and captured each screen; it reported no exceptions. Manual play-testing and other operating systems have not been checked.

## Combat backend increment

CombatRules and CombatSession now implement the confirmed mechanics behind GameController: difficulty-ranked weights (explicit step), arbitrary unanswered-question selection, a 30-second soft threshold, final-answer Dasher, timely-completion Combo, configured fatigue damage, reward timing reservations, and trusted asynchronous evaluation with session/submission tickets. Combat state is copied transactionally and exposed as immutable views. Legacy constructors and non-combat evaluation remain available.

The weighted-credit total and separate five-point Combo award go to a configured percentage converter; direct legacy score submission rejects an active combat. HP depletion reports T01 rather than guessing defeat behavior. Serious-cheating zero, ordinary three-day return, immediate major results, makeup conditions and version-1 day-end saves retain their behavior.

This is backend logic, not an installed combat screen or a complete item system. The shipped Demo remains Days 1–4 and has no assessment bank. The weight text/example conflict, HP curve, timeout/Dasher priority, evaluation timing, reward-clock inclusion and score normalization are explicit pending policy, not hidden demo constants. Item ownership, consumption, effects and detection remain T09; reward delay completion alone performs none of them.

The frontend task is specified in [frontend combat handoff](frontend-combat-handoff.md), including API signatures, event ordering, worker tickets, UI states, remaining content needs and a Claude prompt. Existing frontend files, assets, Main and build dependencies were preserved.

## Authored classroom decisions — backend only

The backend now exposes `Lesson.title()` and `Lesson.decision()`. `LessonDecision` owns the prompt and immutable ordered `Option` values (id, label, outcome, effect, feedback). The populated `docs/templates/lesson.properties` is a reusable authoring example; the Demo loader reads the same schema from the selected classpath profile. `DayPlan`/`SchoolCalendar` remain the sole schedule and no new runtime/save state is required for menu navigation.

Days 1–4 each have three response options and one neutral quiet option. Day 1 uses RAISE_HAND; Day 2 uses ASK_TEACHER. Correct responses retain the existing Demo bonuses/costs. Inappropriate responses grant no XP and have a provisional -2 SAN effect; Days 1–2 waive it entirely and warn, with no deferred effect. Opening participation, reading choices and backing out are presentation-only operations with no core settlement. The backend never maps `ask` or `raise` to an automatic correct response.

`lessonChoice(int lessonDay, String optionId)` validates day, phase, shown lesson, no makeup replacement and authored final ID. It returns authored feedback, applies the configured effect transactionally and advances once to OTHER_CLASSES. Repeats, invalid/stale IDs and overflow leave state unchanged. The existing one-argument method delegates to the same validation; legacy explicit fixture catalogs remain supported.

The user explicitly kept the frontend with Claude. `ClassroomScreen`, `GameSession`, frontend-owned tests, FXML, CSS, assets and Main remain unchanged. The old flat screen does not yet show the two-level menu or authored labels. Full-suite failures from old frontend `ask`-success expectations are a known contract migration, not suppressed tests or approved reward aliases. Backend-owned Grade11ReviewContentTest is updated for the new interaction contract while retaining all lecture/example/slide checks. Frontend work and test migration are specified in [frontend-classroom-handoff.md](frontend-classroom-handoff.md).

## Opening exam — backend only

`OpeningContent` loads validated UTF-8 story, question, option, answer, provenance and provisional SAN/XP/duck data from `/demo/opening.properties`. `OpeningBattle` owns the isolated previous-semester state. GameController exposes intro/start/select/answer/duck/tick/view and validates terminal acknowledgement before Day 1. Snapshots copy the battle; public views are immutable and omit unsubmitted answer keys. No frontend code, assets, dependencies, Main arguments or save fields changed.

The latest correction removes the HP upper cap entirely (not 100); initial opening HP is 9. The previous unlimited-duck proposal is replaced by one consumed item. Failed HP, wrong-answer and timeout branches produce 67; successful ten-question completion produces 89. Ending data requests frontend animation, not a claim that an animation exists. Both acknowledgements clear opening data and keep the original Day 1 initial student/empty gradebook. Tutorials continue to waive classroom penalties, not these explicit prologue rules.

The normal combat policy remains independent: its soft timeout, grading, fatigue and item framework have not been filled in by opening constants. Explicit legacy catalogs without an opening scenario retain fixture acknowledgement compatibility. The shipped Demo always installs this scenario, and the old frontend skip is rejected.

See [frontend-opening-handoff.md](frontend-opening-handoff.md) for the callable lifecycle and Claude prompt, and [opening-question-sources.md](opening-question-sources.md) for PDF pages/keys/fingerprints and executable question verification. Full-suite/frontend migration status and exact test evidence accompany this delivery; backend completion is not UI completion.

Observed checks for this increment: full baseline **348 tests / 337 passed / 11 failed**; modified full suite **400 / 380 / 20**, all failures in preserved frontend tests (GameSessionCombatTest 9, GameSessionTest 8, TutorialTipsTest 2, ClockTimesTest 1). Backend/core, public contracts, unchanged Main entry checks and backend-owned review-content tests: **294 passed**, including **52 new opening/content/contract cases**. `installDist` succeeds and its JAR includes both opening classes and the authored resource. Class/sequence Mermaid sources render successfully. This is build/headless evidence, not a rendered opening or animation acceptance.
