# Frontend handoff: authored classroom responses

Status: **backend implemented; frontend work remains with Claude**, explicitly confirmed by the user on 2026-11-10. This request changes neither production UI nor frontend-owned tests. The current flat screen is not the requested final two-level interaction. Do not restore `ask -> automatic reward` to keep old tests green.

## Confirmed behavior

- Day 1: Mr. Reward asks an int-operator question. The player may raise their hand, then select one of three answers, or stay quiet.
- Day 2: the player may ask a question, then select one of three authored questions, or stay quiet.
- Days 3–4 also have three authored question options. Each lesson uses only one interaction kind.
- Only appropriate/correct responses grant learning bonuses. Wrong/inappropriate options grant no XP; their only configured penalty is SAN. **All negatives on Days 1–2 are waived, never deferred. Wrong tutorial choices mean no bonus plus a warning, no SAN loss.** Quiet does nothing.
- Menu opening/navigation has no effect, spends no extra time and is not a second rewarded classroom action. Only the final response/quiet settles once and moves to OTHER_CLASSES.
- Existing correct-response values are retained. Wrong SAN=-2 is explicitly Demo-only. Display effects from data/snapshots; do not hard-code these numbers. Production balance is T03/T04.
- Both review lecture texts, all 31 lines, slide cues and assets are unchanged. Save schema remains version 1 and day-end-only. Do not add CLI flags.

## Data ownership and APIs

`SchoolCalendar` and `DayPlan`: the single forty-day schedule. `ContentCatalog.lessonDays()`: configured content availability only. No alternate calendar or automatic fill of missing days.

`ContentCatalog.Lesson`:
- `title()`: backend-authored course title.
- `text()`: unchanged speaker lines and slide markers.
- `interaction()`: RAISE_HAND for Day 1, ASK_TEACHER for Day 2 (and the Day 3–4 Demo).
- `decision()`: an immutable `LessonDecision` for every shipped Demo lesson.
- `choices()`: compatibility effect lookup for final IDs; **not a first-level menu**. The three-argument legacy constructor remains for explicit old fixture catalogs with `decision() == null`.

`LessonDecision`:
- `prompt()`: the second-level question/prompt.
- `responses()`: immutable response options in authored order (three in each Demo lesson).
- `skip()`: the single neutral option.
- `options()`: responses plus skip.

Each `LessonDecision.Option` provides `id`, `label`, `outcome`, `effect`, `feedback`. Render `label`, not ID; do not alphabetize or reveal outcome/correctness in the choice menu. `feedback` belongs after settlement. A local Java app is not an anti-cheat boundary; outcome metadata is content, not a hidden server answer key.

`GameController.lessonChoice(int lessonDay, String optionId)`:
1. Requires the captured current lesson day, ICS_CLASS, lesson already shown and no replacement makeup.
2. Resolves an actual final ID. `ask`, `raise`, blank/null, another day's response and unknown IDs reject without changes.
3. Applies the configured effect once on a copy; tutorial negatives warn only.
4. Advances to OTHER_CLASSES and returns authored feedback in `Result.message()` plus `Result.warnings()`.
5. A repeated submission rejects without extra rewards. Day mismatch also rejects quiet, whose ID is reused.

The existing one-argument method delegates to the same settlement using the current day. **Add a thin two-argument wrapper to GameSession**, so callbacks carry their captured day instead of silently targeting a later lesson. Do not add `beginParticipation` state to the core or persist menu state: there are no mid-class saves.

## Minimal frontend change

1. Keep lecture playback/projector lifecycle as-is. Capture `lessonDay` when opening that lesson.
2. After playback, show a participation button based on interaction (`RAISE YOUR HAND` or `ASK A QUESTION`) and `decision.skip().label()`.
3. Participation calls **no backend operation**; replace the menu with `decision.prompt()` and the three `responses()` labels. A back button, if used, is UI-only and returns to participation/quiet.
4. A specific response or quiet calls the thin `GameSession.lessonChoice(lessonDay, optionId)` exactly once. Existing click-locking still applies.
5. On Done, display authored feedback and warnings, refresh HUD and offer Continue. On rejected input, show the existing diagnostic and unlock appropriately; do not pretend class ended.
6. Use backend title where suitable without changing the visual design. No FXML/CSS/asset rewrite is requested.

## Test migration and acceptance

The backend tests now cover all authored options, correct-only rewards, tutorial exemption, ordinary SAN loss, neutral quiet, immutable data, malformed templates, invalid/stale/repeated operations, arithmetic rollback, existing day-end saves and unchanged lecture examples/cues.

The existing frontend `GameSessionTest` and `GameSessionCombatTest` still use the obsolete `lessonChoice("ask")` success assumption. Migrate their fixture steps to a real authored final option ID (or neutral quiet where learning is irrelevant). Keep the assertions about phase changes, repeat rejection and all combat behavior. Do not delete/disable these tests or alias `ask` to the correct answer. Backend-owned `ui/Grade11ReviewContentTest` is already migrated; its Java examples and cue checks remain.

Required frontend checks:
- Day 1 first menu offers answering, Day 2 offers asking; each second menu has the three correct labels in authored order.
- Opening/backing out of a menu does not alter HP, SAN, XP, time or lesson progress. Lecture/slides are not replayed by changing choice menus.
- Correct selection grants only the configured bonus once. All wrong selections on Days 1–2 leave SAN and XP unchanged, display feedback plus a tutorial warning and finish class once.
- Wrong selection on Day 3/4 reduces configured SAN but grants no XP. Quiet changes nothing.
- Repeated clicks, stale-day callbacks and malformed IDs never settle twice or reward another class.
- Each selected option's feedback fits the current UI; the original 31 lecture lines/slides still fit and finish normally.
- Run the entire suite after migrating the obsolete frontend fixture calls; a backend-only passing subset is not a claim that frontend integration is finished.

## Copyable prompt for Claude

```text
Implement only the frontend/controller wiring for docs/frontend-classroom-handoff.md.
The user assigned frontend work to you. Keep all core classes, the new Demo data,
lecture text/slide cues, save rules, Main and CLI policy unchanged. Do not redesign
FXML, CSS or assets. Read Lesson.title()/decision() rather than hard-coding labels.

The first menu chooses participation or quiet; participation only opens the three
response labels and must make no core call. The final option/quiet calls a thin
GameSession wrapper around GameController.lessonChoice(capturedLessonDay, optionId).
Show the returned feedback/warnings after success and retain click-locking.
Day 1 answers Mr. Reward; Day 2 asks a selected question. Wrong tutorial options
have no bonus and warning only, no SAN loss. Only correct/appropriate options earn
bonuses. Never alias ask/raise to a correct answer or choose an answer for the player.

Migrate obsolete ask-success fixture calls in GameSessionTest/GameSessionCombatTest
without deleting their behavioral checks. Add menu-stage/no-effect, correct/wrong,
quiet, stale-day and double-click checks. Run the full suite and inspect both real
classroom menus, feedback and unchanged slide playback. Report actual test and
visual results separately. Make one local commit for this request; do not push.
```

## Observed backend-only verification

Full suite on the backend-only copy: **348 tests, 337 passing, 11 failing**, exit 1. All 11 failures are unchanged frontend tests hitting `lessonChoice("ask")`: nine `GameSessionCombatTest` cases stop at their shared Day 1–4 fixture setup (line 218), and two `GameSessionTest` cases stop at the old direct-ask assertions (lines 57/162). No test is disabled and no production frontend/test file is modified. This full suite is not green; it must be rerun after your integration and fixture migration. The original baseline had 320 passing tests. A separate backend/core plus unchanged entry-point check runs 242 tests and passes (exit 0); it does not replace the full-suite result.
