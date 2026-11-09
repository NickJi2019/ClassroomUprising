# Classroom Uprising

Pure-Java **Swing demo** over the existing campus-life rule core. Play Days 1–8, or jump to a labelled Day 5 Lab fixture. Production numeric balancing and pending T01–T14 decisions remain separate from demo fixtures.

## Requirements and commands

Use a JDK 21 or newer. The existing Gradle wrapper and JUnit 6 dependencies are retained; Java output targets release 21. No production dependency was added.

From the project directory:

~~~sh
./gradlew test jar
java -jar build/libs/ClassroomUprising-1.0-SNAPSHOT.jar
# The no-argument launch opens the GUI. Existing CLI modes remain:
java -jar build/libs/ClassroomUprising-1.0-SNAPSHOT.jar --calendar
java -jar build/libs/ClassroomUprising-1.0-SNAPSHOT.jar --demo
java -jar build/libs/ClassroomUprising-1.0-SNAPSHOT.jar --lab-demo
~~~

Use **--offline** with Gradle when dependencies are already cached. **--demo /absolute/path/day-end.properties** chooses a save destination; otherwise the demo uses a temporary directory.

**The demo is explicitly a test fixture.** Its initial HP/SAN, activity effects, penalties, XP multiplier, lesson text and rubric limit are sample inputs, not approved game rules. Normal construction with RuleValues.pending() and ContentCatalog.pending() does not silently install them.

## A short classroom presentation

1. Start the JAR with a full JDK 21+. No JavaFX, browser, engine or runtime library download is needed.
2. Start the semester, read the lesson, then choose one interaction. Use the right-hand record to show the changed XP and unchanged tutorial SAN.
3. Choose an OTHER_CLASSES action. In Student Hub, sleep immediately, save at day end, and advance to the half-slot morning. Try Load to return to the saved closed day.
4. For the technical highlight, use **Lab shortcut · D5**. This resets to a labelled fixture; it does not pretend the earlier days were played.
5. Run the starter: its `a - b` is a deliberate bug. Change it to `a + b`, run again, then submit. The real compiler, three child-JVM cases and a line-length check run off the UI thread.
6. Play Day 6, the Day 7 multiple-choice test, and advance to Day 8. The official Lab mark appears after three days. The slice stops at Day 8; the 40-day calendar is still displayed.

The UI is in English for the ICS4U presentation. A text flashback stands in for the opening battle. There is no free-roaming map or playable Final battle in this slice.

GUI saves default to **demo-save.properties in the launch directory** (ignored by Git). Use `--gui /absolute/path/demo-save.properties` to select another file. Save is enabled only at DAY_END; there is no mid-day save or close-time autosave. Use this GUI's own saves; production content-version migration remains pending.

All lesson/activity values are labelled fixtures. In this demo, participating gives +4 Vibe/+2 unit Knowledge/-2 SAN, listening +2/+1, Unit 1 study +4/+2/-2, relaxing +4 SAN, early sleep +3 SAN and a half-slot, and late night -4 next-day SAN/×0.5 XP. Tutorial negative effects are waived. The demo Lab uses 80% exact-output cases + 20% line length (compile failure = 0); the one-question quiz uses 100 or 0. These are not approved course rules. Other sleep timings remain TBD rather than being silently added.

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

There is no authored opening battle, final-combat formula, automatic 95-based SAN calculation, complete Retake reset, ending selector, reward-use system or social-event content. Midterm continuation/Retake remains an explicit T10 boundary rather than granting continuation using a guessed threshold. The opening operation acknowledges a tutorial supplied by a future presentation layer; it does not claim to render that battle.

The recordEvaluatedSubmission operation accepts a normalized percentage **from a configured evaluator**, not a player-entered answer or an HP conversion. Tests and the GUI use explicitly labelled fixture evaluators. The Java runner produces evidence; DemoScenario connects it to a demo-only rubric, not the final course-mark rubric.

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
| Entry point / CLI checks | Main |
| Swing view and Java2D scene | GameWindow |
| Demo actions, explicit fixture content and rubric | DemoSession, DemoScenario |

Source is in src/main/java/com/aphs/classroomuprising. Tests are in the matching test package. The class and sequence diagrams describe this implemented foundation; the IPO remains the approved target with explicit TBD boundaries. Generated code and automated tests are not evidence of independent human review or student code ownership.

## GUI verification

`./gradlew test jar` runs headless JUnit checks. The executable integration check additionally drives real Swing buttons and the background compiler, verifies invalidated results after editing, and renders five PNGs:

~~~sh
java -Djava.awt.headless=true -cp build/classes/java/main:build/classes/java/test \
  com.aphs.classroomuprising.GuiSmoke /tmp/classroom-gui-check
~~~

On a machine with a desktop, use `-Djava.awt.headless=false` and append `native` to show real JFrames during this check. The PNGs are component renders, not desktop screenshots. On Windows use `;` instead of `:` in the classpath. Automated checks do not establish an independent human presentation rehearsal.
