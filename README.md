# Classroom Uprising

Java **foundation prototype** for the confirmed campus-life design. This is not a finished playable story or GUI. Production lesson content, numeric balancing and pending T01–T14 decisions have not been invented.

## Requirements and commands

Use a JDK 21 or newer. The existing Gradle wrapper and JUnit 6 dependencies are retained; Java output targets release 21. No production dependency was added.

From the project directory:

~~~sh
./gradlew test jar
java -jar build/libs/ClassroomUprising-1.0-SNAPSHOT.jar --calendar
java -jar build/libs/ClassroomUprising-1.0-SNAPSHOT.jar --demo
java -jar build/libs/ClassroomUprising-1.0-SNAPSHOT.jar --lab-demo
~~~

Use **--offline** with Gradle when dependencies are already cached. **--demo /absolute/path/day-end.properties** chooses a save destination; otherwise the demo uses a temporary directory.

**The demo is explicitly a test fixture.** Its initial HP/SAN, activity effects, penalties, XP multiplier, lesson text and rubric limit are sample inputs, not approved game rules. Normal construction with RuleValues.pending() and ContentCatalog.pending() does not silently install them.

## What is executable

- Exact forty-day schedule, ICS, OTHER_CLASSES and three named Hub periods.
- Single post-content Lesson choice, normal activity consumption and half-value morning activities.
- Tutorial penalty suppression, early-sleep entitlement and next-day late-night effects.
- Two free unexcused absences, then one overall percentage point per absence; mandatory Midterm/Final attendance.
- Fixed ordinary question banks and explicitly configured random sampling for majors.
- Ordinary three-day result release versus immediate Midterm/Final results.
- Separate provisional estimates; result release removes only estimate-related effects.
- Explicitly chosen first/second-Lesson makeup placement, original questions and makeup conditions.
- Detected-cheating SAN reduction and serious-cheating zero, without HP/XP penalties.
- A bounded Retake counter and selected-reward carryover helper.
- Versioned, atomic day-end save/load, preserving pending results, effects and scheduling.
- A real **local** Java compilation/test runner, plus independent mechanical style-rule evidence.

The controller commits a copied state only after a successful operation. Invalid choices or missing policy values raise a descriptive exception and do not spend time or partially mutate gameplay state.

## What is intentionally not playable yet

See [implementation status](docs/implementation-status.md) and [pending rules](docs/Sprint1.md#pending-rules--no-defaults-are-authorized).

There is no authored opening battle, final-combat formula, automatic 95-based SAN calculation, complete Retake reset, ending selector, GUI, reward-use system or social-event content. Midterm continuation/Retake remains an explicit T10 boundary rather than granting continuation using a guessed threshold. The opening operation acknowledges a tutorial supplied by a future presentation layer; it does not claim to render that battle.

The recordEvaluatedSubmission operation accepts a normalized percentage **from a configured evaluator**, not a player-entered answer or an HP conversion. Tests use a fixture evaluator. The Java runner produces evidence; it is not automatically connected to a course-mark rubric.

## Java Lab execution contract

JavaAssessmentRunner compiles a complete public Java class, including an optional package, using the selected JDK. Each supplied case starts a fresh child JVM with supplied stdin; stdout is compared exactly with supplied expected text. Compilation, wrong output, runtime failure, timeout and output-limit failures are separate results. Style checks return separate findings; no point weights are guessed.

The current helper runs **trusted local code only**. Time and output limits are process controls, not an OS/filesystem/network sandbox. A hosted or shared submission service needs a separately isolated runner before accepting untrusted submissions. The JDK, test cases, limits and rubric are explicit inputs. **--lab-demo** runs only source embedded in this repository.

## Persistence contract for this prototype

SaveManager accepts an active, closed day only. It writes a version-1 UTF-8 snapshot to a temporary file and atomically replaces the destination. Unsupported atomic replacement or other failures are reported; the old file is not deliberately deleted first.

Loading returns that same closed-day snapshot. It does not advance the calendar, apply the wake-up bonus or re-release marks. Calling beginNextDay is separate. This is an API/storage convention, not an invented autosave policy for Drop, Retake or Final; those exceptional boundaries remain T12.

The caller supplies compatible content and policy configuration when resuming. Production content-version migration is not implemented. Saved percentages and estimates are not cryptographically protected.

## Source map

| Responsibility | Source |
| --- | --- |
| Schedule and time periods | SchoolCalendar, DayPlan, CourseType, DayPeriod |
| Student state and explicit effects | StudentState, StatChange, TimedEffect, RuleValues |
| Content and question selection | ContentCatalog |
| Transactional operations | GameController, GameState |
| Marks, absence accounting, carryover | GradeBook, AssessmentRecord, RunProgress |
| Day-end files | SaveManager |
| Compilation, tests and style evidence | JavaAssessmentRunner, StyleRule |
| Console demonstration | Main |

Source is in src/main/java/com/aphs/classroomuprising. Tests are in the matching test package. The class and sequence diagrams describe this implemented foundation; the IPO remains the approved target with explicit TBD boundaries. Generated code and automated tests are not evidence of independent human review or student code ownership.
