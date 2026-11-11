# Backend request: opening exam as a turn-based battle (HP on the hit)

Date: 2026-11-10. From: frontend (request 33). To: backend owner of `OpeningBattle` / `GameController`.

The player asked for the opening exam to play like a turn-based bullet battle. The frontend part is built: the ten questions fan out of Mr. Reward as bullets. Each round has two halves. In **the player's turn** they can face the next question, use an item or read the log. In **his attack** the next question flies at the player, opens into the question paper where it lands, and is answered against its clock. The answer then flies back at him as the player's bullet: it lands when right and fizzles when wrong. The core API already supports all of this, but one rule now disagrees with what the player asked for, and two handoff statements are superseded.

## 1. HP is lost when the question hits, not when it is answered (rule change, needed)

What the player asked: "命中角色时扣除hp并播放受击特效", meaning the HP is deducted when the question hits the character, with the hit effect. Today `OpeningBattle.answer` takes 1 HP when an answer is submitted, and a timed-out question costs nothing.

The frontend calls `startOpeningQuestion(id)` at the moment the bullet lands. Please move the cost there:

- `select(id, now)` (the start) applies −1 HP. `answer` no longer changes HP.
- A question that times out has therefore already cost its HP. The ending is still TIMEOUT / 67.
- **HP reaching 0 on the hit:** please decide. The frontend suggests the battle ends at once with PASS_OUT / 67, since the player collapses before reading the question. The alternative is that the question can still be answered and PASS_OUT follows the answer. Either works for the frontend; it plays whatever ending the view reports.
- Outcomes the player can reach stay the same. Without the duck the 9th hit brings HP 9 to 0 (pass out). With the duck, ten hits leave HP 2, and ten right answers still win 89.
- The duck rule is unchanged: if healing and the deadline coincide, timeout wins.

The frontend needs no API change for this. It already plays the HUD's hit effects (red screen flash, HP jolt, the character shaking and flashing red) whenever the view's HP drops. Today that happens when an answer is submitted. After this change it happens when the question lands, which is what the player wants. The bullet's own impact (character jolt and burst) already plays on landing.

**Clock fairness (recommended).** The question clock starts in `startOpeningQuestion`, i.e. at the hit. The paper then takes about 0.76 s to ripple open (the player asked for a wider, slower ripple), so that much of each clock is spent opening: about 10% of the last question's 7.7 s. If you prefer, split the call: `hitOpeningQuestion(id)` takes the HP, and `startOpeningQuestion(id)` starts the clock once the paper is open. The frontend would call the two in turn. Otherwise it stays as it is.

## 2. Question order: Mr. Reward fires them in authored order (no change needed)

The handoff said to "preserve the existing choice of unanswered-question order" (the player picks). The player now wants Mr. Reward to fire one question per round. The frontend always starts `availableQuestions().getFirst()`, the first unanswered one in authored order. The API still allows any order, so nothing needs to change. Please update the handoff/IPO docs so they no longer require player-chosen order.

## 3. The player's turn: more actions would be rules (no change needed now)

Between his attacks the player's turn offers: face the next question, use an item (the one duck), read the log. Reading the log changes nothing. Facing and the duck are the existing calls. Any further action that affects the battle (defending, focusing, a hint, skipping, more items) is a game rule and would need backend data and calls. The turn menu can take new entries once they exist.

## Unchanged

The duck: one use, +3 HP after 5 s, the clock keeps running, no SAN/XP. The 20 × 0.9^(n−1) s limits. Wrong answer or timeout → 67. Ten right with HP left → 89. Both endings lead to Day 1. No save.
