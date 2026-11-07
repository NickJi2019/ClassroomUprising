# Classroom Uprising: Defeat Mr. Reward

A single-player, turn-based Java game by TeamPike. You play an ICS4U student who works through the school
days, builds up knowledge, and takes on Mr. Reward in scheduled Labs and Tests. Losing doesn't end the game:
progress carries over between attempts.

> **Status: skeleton only. The game is not playable yet.** The classes and method signatures exist, but almost
> every method throws `UnsupportedOperationException` until it is implemented.

## Getting started

You need a JDK (17 or newer, which Gradle 9.6 requires). The build has only been run on JDK 25 on macOS so far.
You don't need to install Gradle, because the wrapper downloads it. On Windows, use `gradlew.bat` instead of
`./gradlew`.

Run the game:

```bash
./gradlew run
```

Expected output:

```text
=== Classroom Uprising ===
[skeleton] Main menu: New Game / Continue / Instructions / Exit
```

Run the tests:

```bash
./gradlew test
```

## What is implemented

| Implemented | Still a stub |
| :---- | :---- |
| Constructors and getters | All game rules and other methods in the diagrams |
| Controller wiring in `Main.createMainMenu` | All `GameUI` screens except the main menu |
| `DayPeriod` enum and `GameUI.showMainMenu` (console placeholder) | Save file reading and writing |
| 4 JUnit tests in `SkeletonTest` (wiring and initial state) | Game content (`ContentCatalog` starts empty) |

## Code map

All classes are in `src/main/java/com/aphs/classroomuprising/`. Class comments name the
[IPO rows](docs/Sprint1.md#ipo-diagrams) they cover.

| Area | Classes |
| :---- | :---- |
| Entry point | `Main` |
| Controllers | `MainMenuController`, `GameController`, `StoryController`, `ActivityController`, `StudentHubController`, `AssessmentController` |
| User interface | `GameUI` |
| Game state | `GameState`, `StudentState`, `SchoolCalendar`, `DayPeriod`, `ActionBudget`, `ContentProgress` |
| Content | `ContentCatalog`, `StoryEvent`, `KnowledgeTopic`, `Question`, `Activity` |
| Assessments | `MrReward`, `ScheduledAssessment`, `AssessmentSession`, `AssessmentResult` |
| Shared values | `StatChange`, `ActivityResult` |
| Saving | `SaveManager` |

## Where the skeleton goes beyond the diagrams

The [class diagram](docs/class%20diagram.mmd) and [sequence diagram](docs/sequence%20diagram.mmd) are the source
for this structure. They don't specify everything, so these parts were inferred and **should be reviewed, then
added to the diagrams if accepted**:

- **Classes with no members in the class diagram:** methods for `ActivityController`, `StudentHubController` and
  `AssessmentController` come from the sequence diagram. Fields of `AssessmentResult`, `ScheduledAssessment`,
  `Question`, `Activity` and `StatChange` are inferred.
- **Classes only in the sequence diagram:** `MrReward`, `AssessmentSession` and `ActivityResult`. The fields of
  `AssessmentSession` and `ActivityResult` are inferred.
- **Placeholder types:** the assessment action, turn feedback and challenge pattern are plain `String`s.
- **Additions:** getters on `GameState`, `StudentState` and `SchoolCalendar`, and constructors throughout.

## Design gaps

The team needs to decide these. The skeleton does not settle them.

**Required course topics missing from the design.** The [assignment](docs/assignment.md) requires all of these
unless the teacher excludes one:

| Topic | Status in the design |
| :---- | :---- |
| Multiple classes | Present |
| ArrayLists | Present in the diagrams |
| File I/O | `SaveManager`, not implemented yet |
| Automated testing | JUnit set up, with 4 tests |
| Interface or abstract class | **Not in the diagrams** |
| 2D arrays | **Not in the diagrams** (the scope document plans a classroom map) |
| Recursive algorithm | **Not in the diagrams** (the scope document plans recursive skills) |
| Sorting algorithm | **Not in the diagrams** (the scope document plans a leaderboard) |

**Scope and design documents disagree.** The scope document describes EXP, levels, Knowledge Shards, skills,
equipment, a classroom map, a leaderboard and three boss phases. The IPO tables and diagrams describe a school-day
routine with four stats and don't include those features. The skeleton follows the diagrams.

**Links the diagrams don't agree on.** The sequence diagram has `GameUI` calling the controllers, but the class
diagram has no such associations. So `GameUI` can't send menu choices to `MainMenuController` yet. `ContentProgress`
also has no methods for recording unlocks or completions.

**Smaller inconsistencies in `Sprint1.md`:**

- The title is also given as "Reward from Reward".
- The schedule says 16 weeks, while other sections say 18.
- The Hub is called both "Student Center" and "Student Hub".

## Documents

- [Sprint 1 documents](docs/Sprint1.md): scope, feasibility study, plan and IPO tables
- [Class diagram](docs/class%20diagram.mmd)
- [Sequence diagram](docs/sequence%20diagram.mmd)
- [Assignment](docs/assignment.md)
