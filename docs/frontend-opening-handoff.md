# Opening exam handoff — continuous 180-second exam clock

Updated 2026-11-11, backend request 25, implementing the user's decision forwarded in [frontend request 36](backend-request-opening-continuous-clock.md). This is the current opening contract. It supersedes request 23's clock-after-reveal and request 24's active-answering-only budget. Normal semester combat is unchanged.

## This increment

- `startOpeningBattle()` starts the shared **180 seconds** on the backend monotonic clock. SELECT_QUESTION, QUESTION_REVEAL, QUESTION, menus/log, bullet flight, ripple, verdict, folding and counter-shot all consume time. The intro before that call does not. Only a settled ending freezes time.
- `hitOpeningQuestion(id)` still charges 1 HP on impact; `startOpeningQuestion(id)` still permits answers only after reveal. Neither starts/resumes/resets a clock. Answer submission costs no HP and does not pause time.
- Any phase at or beyond the deadline settles TIMEOUT / 67 / DEFEAT. `advanceOpeningBattle()` remains the pulse settlement API; keep calling it through the whole battle.
- Hit, start, answer and duck calls settle the clock **before validating or applying the requested action**. If expired, they commit only the timeout (and any earlier due recovery), without hit damage, new answer feedback, item consumption or reveal transition. Stale IDs/options cannot roll back timeout by throwing. Repeated callbacks after settled TIMEOUT are no-ops. Other settled endings retain their existing rejected-action behavior.
- The budget remains 180 seconds. The request's approximately 30 seconds of animation overhead is an unmeasured frontend estimate, not a newly approved allowance.

## Public contract — no signature change

| Operation | Contract |
| --- | --- |
| `openingIntro()` | Authored story only; no clock before the battle starts. |
| `startOpeningBattle()` | Creates isolated exam state and starts the single continuous deadline. |
| `hitOpeningQuestion(id)` | Tick first. If still live, validate unused ID/current phase, deduct 1 HP once and enter QUESTION_REVEAL; zero HP immediately gives PASS_OUT / 67. An expired hit does not deduct HP or replace TIMEOUT with PASS_OUT. |
| `startOpeningQuestion(id)` | Tick first. If live, require QUESTION_REVEAL and the impacted ID, then enter QUESTION. No clock mutation or HP cost. Call from actual reveal completion as before. |
| `answerOpeningQuestion(id, optionId)` | Tick first. If live, require QUESTION and a valid option, then reveal correctness immediately. No HP cost and no pause. Wrong loses; ten correct wins; otherwise next turn. |
| `advanceOpeningBattle()` | Settle timeout/recovery in every live phase. Timeout wins a tie with duck recovery. Later terminal pulses are harmless. |
| `openingBattle()` | Pure immutable view: `timeLimit()` is 180 seconds; `remaining()` falls in every live phase and clamps to zero, freezing only at an ending; `clockRunning()` equals `!finished()`. Reading a zero does not itself settle TIMEOUT—pulse before deciding what to display. |
| `useOpeningDuck()` | Tick first; if live, existing eligibility applies: one use between questions or during QUESTION, not during QUESTION_REVEAL. Consumes the item immediately; +3 HP after 5 seconds, no SAN/XP. The exam clock now runs alongside it in both eligible phases. |
| `completeOpening()` | After ending presentation, acknowledge once; both branches enter fresh Day 1. |

```text
startOpeningBattle: start the continuous 180-second deadline
SELECT_QUESTION -- hit: HP -1 --> QUESTION_REVEAL
QUESTION_REVEAL -- start: permit answers --> QUESTION
QUESTION -- correct answer, no HP charge --> SELECT_QUESTION or VICTORY / 89
QUESTION -- wrong answer --> DEFEAT / WRONG_ANSWER / 67
live phase -- deadline reached --> DEFEAT / TIMEOUT / 67
live impact -- HP reaches zero before deadline --> DEFEAT / PASS_OUT / 67
terminal ending -- completeOpening --> fresh Day 1
```

Pre-deadline stale/invalid callbacks still reject transactionally. In particular, answers, duplicate hits and duck input during reveal are not permitted; a repeated reveal-completion call does not reset time. Detached battle copies retain the same start instant and frozen ending elapsed time without sharing mutable actions. There is no client-side timer/accumulator, new save field, CLI option or dependency.

## Frontend handoff

Frontend request 36 already provides the pulse, always-visible clock and stage-specific timeout presentation. **No frontend implementation or API rewiring is requested.** The backend increment only migrates the old timing assertions in `OpeningFlowTest` identified in section 4 of the request; FXML/CSS/assets, Main, GameSession and screen implementations are unchanged.

Continue using these callbacks, checking the fresh view after each:

```java
// Intro has finished:
game.startOpeningBattle(); // starts the deadline now
// Each pulse, including turn menus and animations:
game.advanceOpeningBattle();
OpeningBattle.View view = game.openingBattle();
// Bullet-impact callback, if the screen/session is still current:
game.hitOpeningQuestion(questionId);
view = game.openingBattle(); // may now be TIMEOUT, without any hit damage
// Reveal-completion callback, if still live and revealing this question:
game.startOpeningQuestion(questionId);
view = game.openingBattle(); // QUESTION permits answers; the existing clock keeps running
```

These are separate event callbacks, not a sequential startup script. Guard screen generation, active ID and stage as before; never hit twice to resume presentation. In a pending visual ending, retain the terminal view rather than reading a later clock or processing another answer.

## Unchanged gameplay

Ten sourced multiple-choice questions, five options each, authored content and code in `/demo/opening.properties`; keys/provenance remain in `opening-question-sources.md`. Mr. Reward fires in authored order using the first available question; the backend's explicit-ID compatibility remains, not a player-picker requirement. `timeLimit(int ordinal)` remains a validated compatibility accessor to the same total, not a per-question grant. No decreasing per-question limits return.

HP starts at 9, with no cap (neither 9 nor 100); the ninth unhealed impact ends before an answer, leaving eight feedback records. Exactly one duck; no extra turn actions. If a missed pulse observes both recovery and timeout overdue, apply the recovery only if it was due strictly before the deadline; a tie or later recovery gives no HP. Opening Demo SAN=1, Coding Vibe/six Knowledge values=0.

Wrong answer, timeout or pass-out gives 67 with mockery; ten correct while alive gives 89 with astonishment and the player's dissatisfaction. Both enter Day 1 without carrying opening stats/items/grade into the current term. Day 1–2 penalty waiver does not waive opening damage. Normal CombatSession/CombatRules and day-end-only saving remain unchanged.

## Verification and frontend acceptance

Backend/headless evidence: baseline **443/443**, modified **462/462**, no failures or skipped cases. Nineteen added cases cover all live phases, pure reads versus pulse settlement, valid/invalid/repeated late callbacks, timeout rather than lethal ninth impact, between-turn recovery/deadline ordering, and victory/pass-out freezing. Existing cases now assert continuous menu/reveal/feedback time, snapshot start preservation, nanosecond boundaries, signed nanoTime wrap, unchanged damage and both Day 1 branches. The public consumer completes all ten questions with a duck and ten 0.76-second reveals: 112.6 seconds used, 67.4 remaining at victory.

Real-window acceptance was run by frontend request 37 (all passed; see implementation-status.md). It was asked to test timeout during the initial turn/fan-out, bullet flight, reveal, verdict and counter-shot, alongside ordinary QUESTION timeout. Confirm TIME RAN OUT / 67, no late HP flash/drop, no re-opened question or duplicate ending, and fresh Day 1. Confirm the timer runs from battle start and never displays PAUSED while live. Backend tests do not establish visual timing, sound or keyboard/mouse acceptance.

## Prompt to Claude

```text
Merge backend request 25 (continuous opening clock), then verify request 36's
existing frontend paths. No API or UI redesign is needed. Keep the 180-second
value from the backend; do not add a frontend timer, CLI flags, or core changes.
The clock starts at startOpeningBattle, runs through every menu and animation,
and freezes only at an ending. Keep hit at impact and start at reveal completion;
start now only enables answers. Refresh the view after each call: a late callback
can settle TIMEOUT without damage, feedback or item consumption.
Exercise timeout in the turn/fan-out, flight, reveal, QUESTION, verdict and
counter-shot in the real window. Check 67 / TIME RAN OUT, no late damage or extra
answers, no duplicate ending, both ending branches reaching fresh Day 1, and the
single duck's existing +3 after 5 s with timeout winning ties. Preserve visuals,
assets, firing order and normal combat. Report actual observed results separately
from headless tests; do not claim untested animation paths have passed.
```
