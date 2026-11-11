# Opening exam — backend contract for Claude

Date: 2026-11-10. Backend/content delivered; frontend and animation integration are still assigned to Claude. No UI, Main, FXML, CSS, asset or frontend-owned test was changed. This replaces the old predetermined-defeat opening, not the semester combat system.

## Confirmed behavior

- Previous-semester ICS4U Final, before summer-school Day 1. The player took ICS4U in Grade 11.
- Ten difficult multiple-choice questions selected from the two supplied AP CSA PDFs. Each has five authored options. Selection is a reasoned difficulty judgment, not an official ranking; see `opening-question-sources.md`.
- Initial HP **9**, **no HP upper cap**. The superseded caps 9/100 and unlimited-duck interpretation are not used.
- One consumable **Ms Tang's rubber duck**. Separate Demo settings: +3 HP after 5 seconds, no SAN/XP change; the active question clock continues. Inventory is reserved immediately and never replenished during this opening.
- Opening-only Demo SAN=1, Coding Vibe=0, six Knowledge values=0. These are centralized provisional values, not semester balancing. The ordinary Day 1 profile still starts at HP 50/SAN 60; display the opening view, not that future state, during the prologue.
- Each valid submitted answer costs exactly 1 HP and reveals correctness immediately. Wrong answer ends the battle. HP reaching zero produces PASS_OUT, even when that answer was correct. If both occur together, PASS_OUT selects the animation and feedback still reports the wrong answer.
- Hard question deadlines: `20 × 0.9^(ordinal−1)` seconds, where ordinal is answer order, not source number. Limits: 20, 18, 16.2, 14.58, 13.122, 11.8098, 10.62882, 9.565938, 8.6093442, 7.74840978. At the exact deadline, timeout wins. A timed-out non-submission does not consume answer HP.
- Preserve the existing choice of unanswered-question order. One question runs at a time; switching/restarting it is rejected. The timer starts on `startOpeningQuestion`, stops when that answer settles, and the next timer starts only when the next question is explicitly selected. Selection/feedback screens have no question timer. There is no pause operation for an active question.
- A duck may be used between questions or during one. Answer/start controls are disabled while its five-second action is pending. Timer polling settles healing and timeout in chronological order, including after a missed UI pulse; if healing and timeout coincide, timeout wins and no healing occurs.
- Defeat: previous-semester grade **67**, Mr. Reward mocks the player, revenge motivates summer school. Victory: all ten correct while HP remains positive, grade **89**, Reward is astonished, the player feels it is still not good enough.
- **Both** endings enter Day 1. Opening HP/SAN/XP, inventory and 67/89 never affect this summer's GradeBook, attendance, rewards or student attributes. No opening save or new save schema. Day 1–2 classroom penalties remain warning-only; that waiver does not cancel the expressly requested previous-semester defeat rules.
- The normal 30-second soft timeout, weight curve, Dasher, Combo and pending grading/fatigue rules are unchanged and are not applied to this special 67/89 opening.

## Minimal adapter API

`DemoProfile.load()` loads `demo/demo.properties`, whose `opening.resource` selects `/demo/opening.properties`. `DemoProfile.newGame(...)` installs the opening content. No CLI flags, debug routes, frontend numeric defaults or new dependencies are needed.

| Call | Result / use |
| --- | --- |
| `game.openingIntro()` | Immutable `List<String>` in existing `Speaker: line` convention. Show before combat. |
| `game.startOpeningBattle()` | Creates the isolated battle once, after intro. No question timer yet. |
| `game.openingBattle()` | Pure immutable `OpeningBattle.View`. It does not settle time-dependent events. |
| `game.startOpeningQuestion(questionId)` | Starts one unanswered question and its backend monotonic timer. |
| `game.answerOpeningQuestion(questionId, optionId)` | Validates ID and A–E option, checks deadline, settles HP/correctness/ending atomically. Read the view afterwards. |
| `game.useOpeningDuck()` | Consumes the sole item and starts recovery delay, or settles an already-expired question. Read the view. |
| `game.advanceOpeningBattle()` | Call on each UI timer pulse and when re-entering the screen, then read the view. Resolves timeout even with no answer click. Terminal calls are idempotent. |
| `game.completeOpening()` | Only after battle is terminal and its ending presentation has finished. Clears ephemeral battle data and opens the unchanged Day 1 state. Early or repeated acknowledgement is rejected. |

`View` exposes: `phase`, `student`, `ducksRemaining`, `usingDuck`, `duckRemaining`, `availableQuestions`, `activeQuestionId`, `questionOrdinal`, `timeLimit`, `remaining`, immutable `answers` feedback, nullable `ending`, and `finished()`.

Question views carry `id`, `prompt`, separate multiline Java `code` and ordered `(id,text)` options. No answer key is in the pre-answer view. Some options contain multiple lines (recursive output / 5×5 grids): use a monospaced, scrollable layout and preserve newlines. 2014 Q22 includes the shared Book/AudioBook definitions; 2015 Q37 includes the shared binary-search method. Do not omit these context blocks.

After a valid answer, `answers().getLast()` contains selected/correct option IDs, correctness and explanation. Keep the prior question view if showing feedback beside its original code. A late answer instead produces an ending without appending feedback; do not assume every button click creates an answer.

`Ending` exposes `grade`, nullable `reason`, `animation` and authored `dialogue`. Defeat reasons: WRONG_ANSWER, TIMEOUT, PASS_OUT. Animation cues: DEFEAT, PASS_OUT, VICTORY. A terminal view is stable, not a stream of new animation events: play the ending once per frontend session, not on every pulse. Stop input and timer callbacks before calling `completeOpening`; cached late callbacks must not start a new opening.

All mutations run on the existing GameController copy/commit boundary. Serialize them on the JavaFX thread and use existing adapter exception reporting/double-click protection. The frontend submits IDs, never elapsed time, correctness, HP costs or final grades. A pure getter showing zero remaining is not a settled timeout: call `advanceOpeningBattle()` first.

### Integration sketch (event-driven, not a CLI)

```java
// After showing game.openingIntro():
game.startOpeningBattle();
// On the player's selection:
game.startOpeningQuestion(questionId);
// On each JavaFX timer pulse (also during the duck animation):
game.advanceOpeningBattle();
OpeningBattle.View view = game.openingBattle();
// On answer click, if the current view permits input:
game.answerOpeningQuestion(questionId, selectedOptionId);
view = game.openingBattle();
// If view.finished(), play view.ending().animation() and dialogue once.
// Only in that presentation's completion callback:
game.completeOpening();
// Then use the existing Day 1 route. Do not run this entire sketch sequentially.
```

## Frontend migration and checks

The current `OpeningScreen` still contains the old static failure SCRIPT and only calls `completeOpening()`. That bypass is intentionally rejected by the shipped Demo now. Replace the script/flow using the backend content, add thin GameSession wrappers, and retain the existing visual style. This backend delivery is not a playable/visually verified opening screen.

The six-argument `ContentCatalog` constructor still serves explicit legacy fixtures without an opening scenario; those retain the old acknowledgement-only fixture behavior. Do not rebuild the shipped Demo through that constructor to bypass the exam. A custom catalog preserving Demo content must pass its `opening()` to the new seven-argument constructor.

Frontend tests that directly call `completeOpening` on a freshly loaded Demo must perform a real opening defeat/victory first, using a fake monotonic clock where necessary. Existing `GameSessionCombatTest` copied catalogs omit opening content and still hit the prior classroom `ask`-success mismatch; they also need the already-delivered classroom contract migration. No test has been removed/disabled here.

Human/UI acceptance:
1. Intro → selected question shows full source code and all five options. HUD reads opening HP 9/SAN 1, not Day 1 stats.
2. Wrong answer immediately disables input and plays defeat/67/mockery; correct answers show immediate feedback.
3. Nine correct without the duck produces HP 0 and pass-out/67. One duck followed by ten correct leaves HP 2 and gives victory/89/"not good enough".
4. No click until the deadline triggers defeat without a submit action. Each later selected question has the smaller limit. Opening a menu must not pause it.
5. Duck inventory becomes zero on use, recovers only at five seconds, does not stop the question timer, and cannot heal after timeout. HP can rise above its starting value; no capped bar may imply a maximum.
6. Each ending plays once; acknowledgement leads to the normal Day 1 review and existing day-end save. Continue from a saved day does not replay opening.
7. Verify long questions/grids at the actual window size and with keyboard/mouse; no such rendering/input test is claimed by the backend tests.

## Reusable Claude prompt

```text
Implement only the opening frontend and thin GameSession adapter described in
 docs/frontend-opening-handoff.md. Preserve all backend classes, story/question
resources, normal combat logic, Main argument-free launch, existing FXML/CSS/art
style and day-end saving contract. Replace OpeningScreen's hardcoded defeat with
GameController.openingIntro, the OpeningBattle public API, and Ending cues/dialogue.
Opening HP starts at 9 with NO cap; there is exactly ONE duck. Read every amount,
clock value, answer result and grade from the backend. Do not treat normal combat
or tutorial penalty waiver as this opening's rules. Poll the backend on timer
pulses, serialize actions, reject double clicks/stale callbacks, show complete
multiline code and A–E options, play defeat/pass-out/victory once, and acknowledge
only after the ending presentation. Both branches go to Day 1. Migrate frontend
tests via real backend transitions; also address the earlier classroom option
handoff. Do not add a CLI, answer bypass or core mutation. Run tests and visually
verify both endings, timers, duck and long-question layout; report actual results.
```

### Observed backend verification

- Baseline full suite: 348 tests, 11 existing frontend failures.
- Modified full suite: 400 tests, 380 passed, 20 frontend failures (9 GameSessionCombatTest, 8 GameSessionTest, 2 TutorialTipsTest, 1 ClockTimesTest).
- Backend/core + public API contracts + unchanged entry + backend-owned lecture checks: 294 passed, including 52 new opening cases. Ten question snippets are exercised against their keys on JDK 21.
- `installDist` succeeds; new classes and `/demo/opening.properties` are present in the packaged JAR. Both design diagrams render.
- These results do not establish frontend playability or animation/layout/input correctness; that remains the integration task above.
