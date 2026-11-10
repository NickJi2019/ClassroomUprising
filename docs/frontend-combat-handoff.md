# Combat backend handoff to the frontend

Date: 2026-11-09. Recipient: Claude / frontend owner.

Implement the combat presentation and thin frontend controller using the backend APIs below. Preserve the existing JavaFX scenes, core rules, 40-day calendar and day-end save flow. Keep Main free of command-line options. This increment provides combat state and timing, not authored questions, an approved HP formula, a grading rubric or an inventory system.

## Ownership

- Backend: question weights and completion order, monotonic time, submission tickets, HP application, Dasher, Combo, reward-delay completion checks, and conversion/settlement into existing grade records.
- Frontend: views, question selection, answer/source capture, animations, disabling duplicate actions, background evaluation dispatch and stale-result filtering. Marshal all GameController access onto one controller/UI thread; the controller is not a concurrent service.
- A configured trusted evaluator supplies an earned fraction in [0,1] and evidence. UI controls never supply their own percentage, HP cost, elapsed time or Combo result.
- `GameSession`, `LabScreen`, `LabRunService`, Main, CSS/FXML/assets and existing frontend tests were not edited by this backend increment. Add frontend wrappers rather than modifying core classes.

## Startup and configuration

`GameController(initial, rules, content, random, combatRules)` uses the backend monotonic clock. Its six-argument overload accepts a fake clock for tests only. The existing four-argument constructor is compatible and uses `CombatRules.pending()`.

`DemoProfile.newGame(name, random, combatRules)` is also available. Loading a day-end save should supply the same approved combat policy using the five-argument GameController constructor. The shipped Demo remains Days 1–4: it has no Day 5 assessment bank, and adding a combat screen does not create that content.

CombatRules has these explicit fields:

| Field | Meaning / pending behavior |
| --- | --- |
| weightStep | Positive increment: the discussion text implies .01, its example implies .1. No default; T07 if absent. |
| timeoutDasher | KEEP_DOUBLE or CANCEL_DOUBLE. UNDECIDED reports T07 only when the last answer exceeds 30s. |
| answerTiming | PLAYER_SUBMISSION freezes time before asynchronous evaluation; EVALUATION_FINISH includes it. UNDECIDED reports T07 when starting a question. |
| rewardTimeCounts | Whether actual reward-delay duration counts toward the question time. Null reports T09. |
| riskyRewardDuration | Explicit positive duration at most 20s. Required only for Risky timing; absent reports T09. Safe is fixed at 5s. |
| hpCost | Pure, deterministic function of elapsed Duration. It must implement the approved faster-answer/higher-fatigue curve. Missing formula reports T01. Fixtures are not approved balancing. |
| scoreConverter | Pure function from CombatSession.Summary to a normalized 0..100 grade. Missing conversion reports T06; negative/non-finite/over-100 output rejects. |

Keep these decisions in backend configuration. Do not copy the test fixture's .1 / HP curve / constant grade into production UI. HP depletion remains T01: a non-tutorial answer that would leave HP <= 0 is rejected without applying the answer or damage.

## Content contract

`ContentCatalog.Question(id, prompt, difficultyRank)` supplies a positive, distinct authored difficulty rank. The old two-argument constructor still works for legacy assessments, but reports missing rank on combat start. Ranks order the selected question set by difficulty; their positions 1..n determine weights `1 + weightStep * (position - 1)`. Picking questions in a different play order does not move their initial weights. Major exams still sample through the existing catalog first, then the combat view orders the sampled set by rank.

Use `combatSnapshot().questions()` to render weighted combat cards, not the potentially shuffled order in AssessmentView. Questions still have no multiple-choice options/answer key. Per-Lab source class, cases, rubric, limits and fraction evaluator remain content/T06 work. A Java test case is not automatically a player question.

## Required frontend operations

| UI action | Backend operation | Result / display |
| --- | --- | --- |
| Enter an assessment combat | `startCombatAssessment()` | AssessmentView retains makeup/no-peeking/detection conditions. Calling twice rejects. |
| Refresh the combat screen | `combatSnapshot()` | Immutable View: questions/statuses, current question, elapsed Duration, timedOut, dasherReady, Combo state, answer history and pending tickets. |
| Start a chosen unanswered question | `startCombatQuestion(questionId)` | Starts the backend clock. Reopening the same unanswered question never resets it. |
| Submit captured work | `submitCombatAnswer()` | Locks the submission and returns AnswerTicket. Disable editing/submitting while it is pending. |
| Trusted evaluation returns | `recordCombatEvaluation(ticket, earnedFraction, evidence)` | Validates ticket, computes weight and HP, records one answer. Stale/duplicate tickets reject. |
| Display final combat breakdown | `combatSummary()` | Requires all answers. Shows per-answer order/time/weights/HP, weightedCredits, Combo status and separate comboBonus. |
| Settle the whole assessment | `finishCombatAssessment()` | Uses the configured converter, applies serious-cheating zero if present, then follows the original result-return flow. |

Example call order (each step belongs to its respective event, not one startup script):

```java
GameController.AssessmentView assessment = game.startCombatAssessment();
CombatSession.View view = game.combatSnapshot();
game.startCombatQuestion(selectedQuestionId);

// Capture and lock the source/answer before starting asynchronous evaluation.
CombatSession.AnswerTicket ticket = game.submitCombatAnswer();
// Dispatch the captured work to the trusted evaluator off the UI thread.
// On the controller thread, only for this session and unchanged captured work:
game.recordCombatEvaluation(ticket, evaluatedFraction, evidence);

// Repeat selection/submission until view.complete(), then:
CombatSession.Summary summary = game.combatSummary();
game.finishCombatAssessment();
```

The intermediate `evaluatedFraction` is not a value the player enters. Existing JavaAssessmentRunner.Report is still evidence, not that fraction. Keep graded submission unavailable until a configured evaluator can produce it.

Before acquiring a submission ticket, ensure the worker is idle and input is valid. Once a ticket exists, retain the captured work and ticket if infrastructure evaluation fails. Surface the error; do not invent zero credit, clear the backend pending submission, change the answer, or start another question. A technical retry can evaluate the identical captured work for the same ticket. Editing/re-answering policy remains T06/T07. Frontend request generations must additionally discard results after abandoning a run or changing screens; backend ticket validation is a second guard, not a substitute for that lifecycle check.

## Time and scoring presentation

- Show 30 seconds as a soft threshold: elapsed > 30s loses the initial weight bonus; exactly 30s is still on time. The answer button remains available after timeout.
- Show the count of evaluated questions and `dasherReady`. The final completed answer receives double weight. The one-question edge is also Dasher-ready from the start. A timed-out last answer follows the explicit timeoutDasher policy.
- Combo follows the current wording: all evaluated answers must have elapsed <= 30s; correctness is not an additional condition. Consequently an all-wrong but timely run still earns the separate 5-point Combo award. Weighted credits and those five points are not yet a course percentage.
- Within the <=30s band, the current confirmed scoring rule gives no additional continuous speed bonus. HP cost is separate; do not animate fabricated extra marks for every second saved.
- Backend time starts on the explicit start-question action and runs continuously until the configured stopping point. No pause API exists. Opening a menu/animation does not establish paused time. Switching unfinished questions, editing submitted answers and general retry rules remain pending; present the backend T-number without changing state.
- Show submitted/evaluating separately from answered. HP and completion count change only after accepted evaluation, once.

## Reward delays

`beginCombatRewardDelay(SAFE/RISKY)` returns RewardTicket with the duration and locks other submissions/reward delays. Animate a busy state using `pendingReward` and `rewardRemaining` from the view. On completion, call `finishCombatRewardDelay(ticket)`; an early callback is rejected. Old tickets cannot complete another delay.

These durations elapse on the real monotonic clock. Do not also add 5/20 seconds in the frontend. If rewardTimeCounts is false, the backend excludes exactly the configured duration, not extra time from a late UI callback.

These APIs reserve and finish time only. They do not check ownership, consume an item, apply a reward benefit or determine cheating. Keep usable inventory/item effects marked T09 until the actual item operation exists; a finished animation is not evidence that an item was consumed.

## Settlement and saves

- Combat uses `finishCombatAssessment()`, never direct `recordEvaluatedSubmission(...)`; the legacy entry explicitly rejects combat runs.
- Ordinary Lab/Unit Test, including makeup: official result still returns three game days after submission.
- Midterm/Final: immediate official result and existing checkpoint status. Continue/Retake/ending policy remains T10.
- Serious cheating still overrides the converted assessment grade to zero. It does not undo HP already spent.
- No mid-combat save. After ordinary settlement, reuse OTHER_CLASSES/Hub and the existing day-end SaveManager. The save schema stays version 1. The grade record retains the combat breakdown as evidence; an active combat session is not serialized.

## Acceptance checks for frontend work

1. Arbitrary order changes the Dasher recipient, not the cards' initial weights.
2. A redraw/repeated start click does not restart the timer. Passing 30s does not disable answers.
3. Submission disables repeats/edits; old session tickets and late worker results never change the current run.
4. Displayed HP matches a new GameState snapshot; no extra UI-side deduction.
5. Safe duration is 5s; Risky uses the supplied value <=20s. Early completion and overlapping actions reject.
6. Missing configuration/content surfaces warning[T01/T06/T07/T09]; no fallback marks or unapproved HP values.
7. Settlement follows existing return dates and checkpoints, and saving remains day-end-only.
8. Existing Day 1–4 Demo flow, current UI assets and Main's no-argument behavior remain intact.

## Prompt for Claude

```text
接入 docs/frontend-combat-handoff.md 中的战斗后端接口。只修改 JavaFX 前端、GameSession 薄适配层及前端测试；保留核心类、现有视觉组件和日常流程，不添加命令行参数。
以 GameController.combatSnapshot() 为显示依据，按文档串联选题、提交票据、异步评估结果和整场结算。计时、HP、权重、Dasher、Combo 与成绩计算归后端；UI 不重复结算。
明确区分未答、作答中、等待评估、已答和道具延时状态，保留防重复点击、旧会话回调丢弃及日末存档。
当前 Demo 题库只覆盖 Day 1–4；HP 公式、战斗分到百分制的转换、选项/答案和 Lab 评分内容仍待提供。测试夹具不是正式配置。待定项显示 T 编号，不用自编数值绕过。
道具延时接口只负责时间，背包、消耗、效果和作弊逻辑仍是 T09。请按文档验收清单完成前端接入并报告实际运行结果。
```
