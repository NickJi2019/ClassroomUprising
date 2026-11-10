# Backend request: starting HP and SAN, and the SAN range

Date: 2026-11-10. From: frontend (request 31). To: backend owner of the core and of `src/main/resources/demo/demo.properties`.

The player asked for the stats to be shown differently. The frontend part is done: HP stays a number, SAN is a bar with no number, and Coding Vibe and the six units' knowledge are shown together as one XP total that opens to show its parts. Three things in that request are game values or rules, so they are yours:

## 1. Starting values (asked for directly)

- `initial.hp=100` (now 50)
- `initial.san=80` (now 60)

Leave `initial.codingVibe` and `initial.knowledge` as they are. Core tests that assume 50/60 will need their expected values (for example `DemoProfileTest`, `CombatRulesTest`, `CoreRulesTest`, `NormalSleepTest`, `SaveManagerTest`, `Fixtures.student()`), and the README's "Initial Demo values" sentence. The frontend tests read values from the profile and do not need changes.

## 2. SAN range (needed for the bar)

The bar shows SAN as a share of a full bar. The core has no SAN maximum or minimum today: `StudentState` adds changes without a limit, so sleep recovery could take SAN above any bar. Until you decide, the frontend uses a display scale of 100 (`ui/Stats.SAN_FULL`): 80 SAN is an 80% bar, anything above 100 shows a full bar, and changes are written as a share of the bar ("SAN −2%"). Please decide and, if SAN is meant to be 0–100:

- clamp SAN to 0..100 when changes are applied, and
- expose the maximum (for example `RuleValues.sanMax()` or a `rules.sanMax=100` profile value), so the frontend reads it instead of its own constant.

If the maximum is not 100, the bar still works: the frontend only needs the number.

## 3. XP total (no change needed unless it becomes a rule)

The frontend shows Coding Vibe plus the six knowledge values as one XP total, for display only (`ui/Stats.xp`). The core keeps them separate and nothing in the game uses the total. If the total should ever count in a rule (levels, thresholds), the core should own it (for example `StudentState.totalXp()`), and the frontend will switch to it.
