# Opening exam handoff — impact, reveal, answer

Updated 2026-11-10 for frontend request 33 and the user's subsequent timing decision. This is the current opening contract; it supersedes the earlier answer-cost/picker contract. Normal semester combat is unchanged.

## Changes confirmed for this increment

1. **HP is lost on the question's impact**, not on answer submission. Each impact costs 1 HP exactly once. HP starts at 9 with no upper cap. If impact reduces it to 0, the battle immediately ends with PASS_OUT / 67, before reading or answering. The ninth unhealed hit therefore leaves **eight**, not nine, answer feedback records.
2. **Impact and timer start are separate calls.** The user chose to start the full question limit only after the paper has completely opened. The 0.76-second ripple is not charged to the answer clock. Backend code does not hardcode animation duration: it waits for the real presentation-completion callback.
3. **Mr. Reward fires in authored order.** The frontend uses `availableQuestions().getFirst()`. The backend still accepts explicit unanswered IDs for compatibility/test consumers; this is not a requirement to expose a player question picker.
4. Between attacks, the existing turn actions remain FACE THE NEXT QUESTION, ITEM and LOG. Reading the log has no core mutation. Defend, hints, skips and further items are not introduced.

## State and callback contract

```text
SELECT_QUESTION
  -- hitOpeningQuestion(id): HP -1 --> QUESTION_REVEAL
  -- same hit when HP becomes 0 --> DEFEAT / PASS_OUT / 67
QUESTION_REVEAL
  -- startOpeningQuestion(sameId), after full reveal --> QUESTION
QUESTION
  -- answerOpeningQuestion(id, option): no HP change --> next turn / defeat / victory
  -- deadline reached --> DEFEAT / TIMEOUT / 67
```

| Operation | Contract |
| --- | --- |
| `openingIntro()` / `startOpeningBattle()` | Existing story and initial isolated state; no timer. |
| **`hitOpeningQuestion(id)` — new** | Call once at bullet impact. Validate unused ID/current phase before charging 1 HP. Returns with QUESTION_REVEAL or terminal PASS_OUT. No question timer yet. |
| **`startOpeningQuestion(id)` — changed** | Call once when that impacted question's paper is fully open. Requires QUESTION_REVEAL and matching ID. Starts the backend monotonic clock with the full limit; costs no HP. It no longer selects/impacts a question. |
| `answerOpeningQuestion(id, optionId)` | Only in QUESTION and with a valid option. Settles deadline, then reveals correctness; does not charge HP. |
| `advanceOpeningBattle()` | Existing pulse. Settles deadlines/recovery, but QUESTION_REVEAL has no running question clock. |
| `openingBattle()` | Pure immutable view. `remaining()` is ZERO during QUESTION_REVEAL, meaning **not started**, not expired. Read `timeLimit()` for the upcoming budget. |
| `useOpeningDuck()` | Existing one-use action between questions or during QUESTION. Rejected while the impact paper is still revealing; no item input belongs to that presentation stage. |
| `completeOpening()` | Existing terminal-only acknowledgement after ending presentation; both branches enter fresh Day 1. |

During QUESTION_REVEAL, `activeQuestionId`, `questionOrdinal`, `timeLimit`, full question data and post-hit HP are already available. No answer feedback is invented. Answering, a second hit, selecting another question, and a mismatched clock-start callback reject without mutation. Repeating clock start during QUESTION also rejects and **does not restart** its deadline. A stale callback after PASS_OUT/other terminal result rejects. Detached snapshots copy the pending-reveal state correctly.

No new CLI, save field, dependency or frontend implementation is part of this backend increment. Saves remain day-end only; opening state is ephemeral.

## Claude's precise frontend edits

The request-33 screen currently calls `startOpeningQuestion` inside `impact()`. It must migrate these two callbacks; the old one-call flow is deliberately not silently supported because it would consume animation time again.

1. Add a thin `GameSession.hitOpeningQuestion(String)` wrapper using the existing adapter exception mapping.
2. In `OpeningScreen.impact`, call **hit**, read the view and update the HUD immediately. If terminal, begin the existing pass-out ending and return without opening paper or starting a timer.
3. Otherwise render the impacted question and run `Ripple.open`. Keep answer/item inputs disabled during reveal. At animation completion, check the screen/session generation, active question ID and phase, then call **start** and refresh the view; enable input only when phase is QUESTION.
4. Continue polling during reveal, but show “opening”/the full limit rather than zero as an expired clock. A question's `remaining` becomes its complete limit at the start call.
5. Preserve request 33's ripple cleanup/status-listener fix. Attach the completion callback without changing its cleanup or the 760 ms open / 360 ms close visuals.
6. Resuming QUESTION_REVEAL must not call hit again: complete/replay presentation and then start. Resuming QUESTION must not call start again: preserve the running backend timer. Ignore callbacks after leaving the screen or entering an ending.
7. Correct/wrong submission continues to produce immediate feedback, but no second HP flash/drop. Timeout retains the impact's already-spent HP. Existing HUD change detection now gets the impact delta.

### Event sketch (not a sequential program)

```java
// Bullet-impact callback:
game.hitOpeningQuestion(questionId);
OpeningBattle.View view = game.openingBattle();
// Update HUD with view.student(). If finished: play ending, do not reveal/start.
// Otherwise show the paper without answering yet, and run Ripple.open.

// Paper-reveal completion callback, after checking the active screen/question:
game.startOpeningQuestion(questionId);
view = game.openingBattle(); // QUESTION, full remaining time, unchanged HP

// Answer callback, only while QUESTION:
game.answerOpeningQuestion(questionId, optionId);
view = game.openingBattle(); // immediate feedback, unchanged HP
```

## Unchanged rules/data

Ten sourced multiple-choice questions with five options each; full code and multiline options are in `/demo/opening.properties`. Keys/provenance are documented in `opening-question-sources.md`. Pre-answer views contain no keys. Deadlines remain `20 × 0.9^(attack ordinal−1)` seconds; exact-deadline submissions time out. The tenth question gets the full 7.74840978 seconds **after** its reveal.

One Ms Tang rubber duck, Demo +3 HP after five seconds, no SAN/XP change; if a question clock is active it continues. Timeout wins if recovery and deadline coincide. No HP cap, including 100. Opening Demo SAN=1 and Coding Vibe/six Knowledge values=0. These do not change Day 1 profile values.

Wrong answer/timeout/pass-out gives previous-semester grade67 with mockery and revenge motive. Ten correct while alive gives grade89 with astonishment and the player's dissatisfaction. Both proceed to Day1, with no opening grade/HP/XP/item carryover into current-term state. Normal CombatSession's soft timeout, fatigue/weights/Dasher/Combo policies remain independent. Day1–2 tutorial warning-only rules do not waive opening impact damage.

## Tests and acceptance

Backend tests exercise both callbacks, impact-only damage, invalid/repeated/stale calls, no reveal deadline, full limits after 760 ms and longer reveals, lethal ninth/tenth hits, timeout retaining spent HP, one duck, unchanged endings and day-end persistence. The public consumer test uses authored-order impacts → reveal completion → answers, without frontend timing arithmetic.

Frontend-owned fixtures need the same hit → start → answer sequence. In particular, `OpeningPlay.enterDayOne`, its key probes, and `OpeningFlowTest.right` currently call start without hit. They should check terminal state after hit before starting/answering. The pass-out test must expect eight correct records then a ninth lethal hit. Do not weaken the backend state guards or add a skip to keep old fixtures green.

Frontend runtime acceptance: verify red HP effects exactly on impact, no second drop on answer, no visible countdown during ripple, a full first/last limit after ripple, repeated completion callbacks never reset time, immediate lethal-hit shatter, duck and both Day1 branches. Earlier request-33 frame measurements/screenshots are frontend-reported evidence for that revision, not verification of this new split callback wiring.

## Prompt to Claude

```text
Integrate docs/frontend-opening-handoff.md's latest split lifecycle only.
Preserve backend, normal combat, visuals, ripple cleanup, assets and no-CLI launch.
Add GameSession.hitOpeningQuestion; call it at bullet impact. If it returns a
terminal view, play PASS_OUT immediately. Otherwise reveal the paper, then call
startOpeningQuestion from the real reveal-completion callback. Keep answers/items
locked during QUESTION_REVEAL. Show full timeLimit without treating remaining=0
as a timeout. Guard generation/question/phase so late or duplicate callbacks never
restart clocks or hit again. Answering now costs no HP. Keep authored firing order.
Migrate frontend-owned tests through real hit/start/answer transitions; expect eight
answers before an unhealed ninth-hit pass-out. Test and visually inspect red effects,
full timing after ripple, repeated callbacks, duck, shatter and both Day1 branches.
Do not change core rules or add CLI/debug/skip routes. Report observed results.
```

Observed verification for this increment: baseline request33 **427/427 passed**. The modified full run reports **430 cases / 410 passed / 20 failures** in unchanged frontend tests: GameSessionTest10, LessonDecisionFlowTest6, TutorialTipsTest2, ClockTimesTest1, and OpeningFlowTest initializationError1. OpeningFlowTest's failed shared setup replaces its six test cases with one initializationError in the JUnit report; this explains the total instead of 435 (427 + 8 new backend cases). No test was removed or disabled. Backend/core/public-contract/entry/backend-owned lecture subset: **302 passed**; installDist succeeds. Full-suite green and runtime callback acceptance require Claude's frontend migration.
