# Opening exam handoff — shared 180-second answering budget

Updated 2026-11-10 for the user's shared three-minute budget amendment after the impact/reveal split. This is the current opening contract; it supersedes the earlier per-question decreasing limits and answer-cost/picker contract. Normal semester combat is unchanged.

## Changes confirmed for this increment

1. **HP is lost on the question's impact**, not on answer submission. Each impact costs 1 HP exactly once. HP starts at 9 with no upper cap. If impact reduces it to 0, the battle immediately ends with PASS_OUT / 67, before reading or answering. The ninth unhealed hit therefore leaves **eight**, not nine, answer feedback records.
2. **All ten questions share 180 seconds of active answering time.** There is no per-question reset or 10% decrease. Impact and timer resume remain separate calls: only QUESTION, after the paper fully opens, spends the shared budget. Intro, turns/items used between questions, log, feedback, attack/reveal/fold/counter animations and endings do not spend it. A duck used during QUESTION still consumes five seconds of budget. Backend code waits for the actual reveal-completion callback, not a hardcoded animation duration.
3. **Mr. Reward fires in authored order.** The frontend uses `availableQuestions().getFirst()`. The backend still accepts explicit unanswered IDs for compatibility/test consumers; this is not a requirement to expose a player question picker.
4. Between attacks, the existing turn actions remain FACE THE NEXT QUESTION, ITEM and LOG. Reading the log has no core mutation. Defend, hints, skips and further items are not introduced.

## State and callback contract

```text
SELECT_QUESTION
  -- hitOpeningQuestion(id): HP -1 --> QUESTION_REVEAL
  -- same hit when HP becomes 0 --> DEFEAT / PASS_OUT / 67
QUESTION_REVEAL
  -- startOpeningQuestion(sameId), resume leftover shared budget --> QUESTION
QUESTION
  -- answerOpeningQuestion(id, option): no HP change --> next turn / defeat / victory
  -- cumulative active time reaches 180 seconds --> DEFEAT / TIMEOUT / 67
```

| Operation | Contract |
| --- | --- |
| `openingIntro()` / `startOpeningBattle()` | Existing story and initial isolated state; no timer. |
| **`hitOpeningQuestion(id)` — new** | Call once at bullet impact. Validate unused ID/current phase before charging 1 HP. Returns with QUESTION_REVEAL or terminal PASS_OUT. No question timer yet. |
| **`startOpeningQuestion(id)` — changed** | Call once when that impacted question's paper is fully open. Requires QUESTION_REVEAL and matching ID. Resumes the unspent shared budget on the backend monotonic clock; costs no HP and never refills time. It no longer selects/impacts a question. |
| `answerOpeningQuestion(id, optionId)` | Only in QUESTION and with a valid option. Settles budget exhaustion, records this question's elapsed answering time once, then pauses the budget and reveals correctness; does not charge HP. |
| `advanceOpeningBattle()` | Existing pulse. Settles deadlines/recovery, but QUESTION_REVEAL has no running question clock. |
| `openingBattle()` | Pure immutable view. `timeLimit()` is the constant total 180 seconds. `remaining()` is the shared time left in **every phase**, initially 180, frozen outside QUESTION and after an ending. `clockRunning()` is true only in QUESTION; do not infer clock activity from a positive remaining value. |
| `useOpeningDuck()` | Existing one-use action between questions or during QUESTION. Rejected while the impact paper is still revealing; no item input belongs to that presentation stage. |
| `completeOpening()` | Existing terminal-only acknowledgement after ending presentation; both branches enter fresh Day 1. |

During QUESTION_REVEAL, `activeQuestionId`, `questionOrdinal`, total `timeLimit`, unspent `remaining`, full question data and post-hit HP are already available. No answer feedback is invented. Answering, a second hit, selecting another question, and a mismatched clock-start callback reject without mutation. Repeating clock start during QUESTION also rejects and **does not reset** the shared budget. Completed-question time is copied with the battle snapshot, not recomputed by the UI. A stale callback after PASS_OUT/other terminal result rejects. Detached snapshots copy the pending-reveal state correctly.

No new CLI, save field, dependency or frontend implementation is part of this backend increment. Saves remain day-end only; opening state is ephemeral.

## Claude's precise frontend edits

The request-33 screen currently calls `startOpeningQuestion` inside `impact()`. It must migrate these two callbacks; the old one-call flow is deliberately not silently supported because it would consume animation time again.

1. Add a thin `GameSession.hitOpeningQuestion(String)` wrapper using the existing adapter exception mapping.
2. In `OpeningScreen.impact`, call **hit**, read the view and update the HUD immediately. If terminal, begin the existing pass-out ending and return without opening paper or starting a timer.
3. Otherwise render the impacted question and run `Ripple.open`. Keep answer/item inputs disabled during reveal. At animation completion, check the screen/session generation, active question ID and phase, then call **start** and refresh the view; enable input only when phase is QUESTION.
4. Show one **EXAM TIME LEFT** counter throughout the battle from `view.remaining()`, with a total of `view.timeLimit()` (180 seconds). Use `view.clockRunning()` to indicate running/paused. During reveal/turns it keeps the same value; starting another question does not refill it. Change the turn preview from “this question gets timeLimit seconds” to the same shared remaining time. Example: 30 seconds spent on Q1 leaves 150 seconds during feedback, the next reveal and at the start of Q2.
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
view = game.openingBattle(); // QUESTION, leftover exam time resumes, unchanged HP

// Answer callback, only while QUESTION:
game.answerOpeningQuestion(questionId, optionId);
view = game.openingBattle(); // immediate feedback, unchanged HP
```

## Unchanged rules/data

Ten sourced multiple-choice questions with five options each; full code and multiline options are in `/demo/opening.properties`. Keys/provenance are documented in `opening-question-sources.md`. Pre-answer views contain no keys. The total is `OpeningBattle.totalTimeLimit()` = 180 seconds. `timeLimit(int ordinal)` is retained only as a validated compatibility accessor for the same total, never a per-question grant. Exact cumulative-budget exhaustion wins over submission. For example, 170 seconds used by earlier questions leaves 10 seconds for the last question. No per-question 20-second/decreasing limit remains.

One Ms Tang rubber duck, Demo +3 HP after five seconds, no SAN/XP change; if a question clock is active it continues. Timeout wins if recovery and deadline coincide. No HP cap, including 100. Opening Demo SAN=1 and Coding Vibe/six Knowledge values=0. These do not change Day 1 profile values.

Wrong answer/timeout/pass-out gives previous-semester grade67 with mockery and revenge motive. Ten correct while alive gives grade89 with astonishment and the player's dissatisfaction. Both proceed to Day1, with no opening grade/HP/XP/item carryover into current-term state. Normal CombatSession's soft timeout, fatigue/weights/Dasher/Combo policies remain independent. Day1–2 tutorial warning-only rules do not waive opening impact damage.

## Tests and acceptance

Backend tests exercise both callbacks, impact-only damage, invalid/repeated/stale calls, no reveal deadline, unchanged leftover budgets after 760 ms and longer reveals, accumulated time across questions, snapshot preservation and no double charging across timer pulses, lethal ninth/tenth hits, timeout retaining spent HP, one duck, unchanged endings and day-end persistence. The public consumer test uses authored-order impacts → reveal completion → answers, without frontend timing arithmetic.

Frontend-owned fixtures need the same hit → start → answer sequence. In particular, `OpeningPlay.enterDayOne`, its key probes, and `OpeningFlowTest.right` currently call start without hit. They should check terminal state after hit before starting/answering. The pass-out test must expect eight correct records then a ninth lethal hit. Do not weaken the backend state guards or add a skip to keep old fixtures green.

Frontend runtime acceptance: verify red HP effects exactly on impact, no second drop on answer, a paused shared counter during ripple, the same leftover budget after the first/last reveal, repeated completion callbacks never reset time, immediate lethal-hit shatter, duck and both Day1 branches. Earlier request-33 frame measurements/screenshots are frontend-reported evidence for that revision, not verification of this new split callback wiring.

## Prompt to Claude

```text
Integrate docs/frontend-opening-handoff.md's latest split lifecycle only.
Preserve backend, normal combat, visuals, ripple cleanup, assets and no-CLI launch.
Add GameSession.hitOpeningQuestion; call it at bullet impact. If it returns a
terminal view, play PASS_OUT immediately. Otherwise reveal the paper, then call
startOpeningQuestion from the real reveal-completion callback. Keep answers/items
locked during QUESTION_REVEAL. All ten questions share 180 seconds of active answer
time: display remaining across all phases, use clockRunning for paused/running,
and use timeLimit only as the total denominator. Never reset time for a new question. Guard generation/question/phase so late or
duplicate callbacks never
restart clocks or hit again. Answering now costs no HP. Keep authored firing order.
Migrate frontend-owned tests through real hit/start/answer transitions; expect eight
answers before an unhealed ninth-hit pass-out. Test and visually inspect red effects,
leftover timing after ripple, repeated callbacks, duck, shatter and both Day1 branches.
Do not change core rules or add CLI/debug/skip routes. Report observed results.
```

Prior request23 verification (before the shared-budget amendment): baseline request33 **427/427 passed**. The modified full run reports **430 cases / 410 passed / 20 failures** in unchanged frontend tests: GameSessionTest10, LessonDecisionFlowTest6, TutorialTipsTest2, ClockTimesTest1, and OpeningFlowTest initializationError1. OpeningFlowTest's failed shared setup replaces its six test cases with one initializationError in the JUnit report; this explains the total instead of 435 (427 + 8 new backend cases). No test was removed or disabled. Backend/core/public-contract/entry/backend-owned lecture subset: **302 passed**; installDist succeeds. Full-suite green and runtime callback acceptance require Claude's frontend migration.


### Additional test migration for the shared budget

Update old frontend timer assertions as well as hit/reveal callbacks. The first and tenth ordinal's total is 180, not 20/7.7; a later question's available time is `remaining`, not a fresh allowance. In `OpeningFlowTest`, initial SELECT_QUESTION has 180 remaining and clockRunning=false, not zero. Move fake time to the remaining shared deadline for timeout checks. Confirm the duck spends budget during an active question but not between questions. Do not invent a client-side accumulator, wall-clock deadline across menus, or per-question reset.


Current request24 verification: baseline **430 cases / 410 passed / 20 frontend failures**. Modified full run **437 / 417 / 20**, with exactly the same failed test identities. Backend/core/public-contract/entry/backend-owned lecture subset: **309 passed**; seven new cases cover shared time accumulation, paused phases, repeated polling, between-question duck use, a later-question recovery/deadline tie, copied budget state, stable ending time and stale callbacks. Existing timing cases and the public consumer were updated to assert the shared budget instead of per-question limits. `installDist` succeeds and both diagrams render. The frontend source and its pending migration failures are preserved; this is not runtime UI verification.
