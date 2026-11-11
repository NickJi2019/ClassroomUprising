# Backend request: one exam clock that runs through the whole opening battle

Date: 2026-11-11. From: frontend (request 36). To: backend owner of `OpeningBattle` / `GameController`.

**Status: requested, not applied.** The frontend side is built (request 36) and works with today's rule and with the new one.

## What the player asked

During request 36 the player reported: "计时在播放动画时仍会暂停，并且计时器只在问题界面显示，而不是战斗中全局显示". In English: the time still pauses while animations play, and the timer only shows on the question paper instead of across the whole battle.

Asked which rule they wanted, they chose **"整场连续走，不暂停"**: the exam time runs continuously through the whole battle and never pauses.

This replaces their earlier request to pause the time during transitions, and their clarification recorded with backend request 24 ("not continuous wall time across the whole battle"). That earlier request was the basis of two current rules:
- backend request 23 (`a033f7f`): the clock starts only after the reveal.
- backend request 24 (`1404eed`): the time is spent only while a question is open.

## 1. The rule change (needed)

**When the time runs**
- The shared 180 s budget starts when the battle starts (`startOpeningBattle`). It then runs in every phase:
  - `SELECT_QUESTION`: the player's turn, and his attack while it flies.
  - `QUESTION_REVEAL`: the paper rippling open.
  - `QUESTION`.
  - Between rounds: the verdict, the paper folding, the counter-shot.
- It stops only at an ending, where it stays frozen as today.

**Running out**
- When the time reaches zero, in any phase, the exam ends as TIMEOUT / 67 with the DEFEAT cue. `advanceOpeningBattle` settles it as it does today.

**The view**
- `View.clockRunning()` is true whenever the exam is not finished.
- `remaining()` goes down in every phase.

**`startOpeningQuestion(id)`**
- It keeps its job of opening the question for answers (`QUESTION_REVEAL` → `QUESTION`), but it no longer starts or resumes the time.
- The frontend still calls it once the paper is fully open, so please keep the call.

**Calls that arrive after the time ran out**
- A call that arrives after the time has run out should settle the timeout and change nothing else. This applies to the bullet landing (`hitOpeningQuestion`), `startOpeningQuestion`, an answer and the duck.
- One case to watch: today `hit` settles time first but then goes on to take the HP. After this change, that first settle can already end the exam, so `hit` must then stop without taking HP. Otherwise a pass-out could replace the timeout.

**The duck**
- Unchanged: one use, +3 HP after 5 s, no SAN/XP. It now always runs alongside the exam time.
- Today's tie rule stays: when the heal and the end of the time fall due together, the timeout wins.

## 2. An open decision for the team (the frontend does not decide it)

Animations and the player's turn now spend exam time. The figures below are estimates from the animation lengths in the frontend code, not measurements.

| Part of a round | Approximate time |
|---|---|
| His attack (draw back, hold, flight) | 0.65 s |
| The paper opening | 0.76 s |
| The paper folding | 0.36 s |
| The counter-shot and its settle | about 1 s |
| **Total per round** | **about 3 s** |

Over ten rounds that is roughly 30 s of the 180 s, before the time the player spends in the turn menu and reading verdicts.

Whether 180 s stays is the team's call. The frontend reads `timeLimit()` and shows whatever it is.

## 3. What the frontend already does (request 36, no API change needed)

**The clock is always on screen.** It stands at the top of the screen through the whole battle: the turn, the attack, the reveal, the question, the verdict and the counter-shot. It reads `remaining()`, `timeLimit()` and `clockRunning()` from the view. Until this change lands it shows PAUSED whenever `clockRunning()` is false; after it, it never will. The paper and the turn menu no longer repeat the time.

**A timeout is handled in every phase.**

| Where the time runs out | What the frontend does |
|---|---|
| The turn or the fan-out | The exam ends at once. |
| A bullet in flight | It lands without a hit, then the exam ends. |
| A paper still opening | It finishes opening, shows TIME UP and folds away. |
| A verdict being read | The paper folds away. |
| A counter-shot | It finishes first. |

Each of these ends on the DEFEATED card with "TIME RAN OUT". Only the question case can happen with today's rule; the others are ready for the new one.

## 4. Tests that assert today's rule

These assert that the time does not run outside `QUESTION`, and will need updating with the rule:

**`src/test/java/com/aphs/classroomuprising/ui/OpeningFlowTest.java`**
- `theExamComesBeforeDayOneAndStartsFromItsOwnStats`: "no clock between questions".
- `aWrongAnswerIsADefeatAndDayOneStartsFresh`: "no clock while the paper opens".
- `theDuckHealsOnTheClockAndTenRightAnswersWin`: "a duck between questions spends no exam time".
- `theTenQuestionsShareOneExamTimeThatOnlyRunsWhileAQuestionIsOpen`.

**Other test files**
- `src/test/java/com/aphs/classroomuprising/OpeningBattleTest.java` and `src/test/java/com/aphs/classroomuprising/contract/OpeningFrontendContractTest.java` read `clockRunning()` / `remaining()` and may assert the same rule.

## Unchanged

- HP is taken on the hit.
- An answer costs no HP.
- Questions come in authored order.
- A wrong answer → 67.
- HP 0 → pass out, 67.
- Ten right answers with HP left → 89.
- Both endings lead to Day 1.
- No save.
