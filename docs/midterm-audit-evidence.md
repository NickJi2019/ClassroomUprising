# Midterm AI Feasibility Audit — evidence notes

Date: 2026-11-10. These notes collect facts for the Sprint 2 **Midterm AI Feasibility Audit Report** ([assignment](assignment.md), 30 marks). They are evidence, not the report itself. They do not change the current rule register, now maintained in [Sprint2.md](Sprint2.md).

> These notes were drafted by the same AI tool that wrote the JavaFX frontend. That makes them an AI self-review, not an independent check. The team must verify each row before using it. Cells marked **Team** need facts the repository does not contain.

Sources compared:

- **Sprint 1 Scope**: the Scope Document and Feasibility Study in Sprint1.md. This is the original JRPG version, with levels, skills, equipment, a leaderboard, a classroom battle map and three boss phases.
- **Design brief**: the team's design notes ("Official version of plan" plus the Chinese planning notes on the calendar, Lesson, Lab/Test, Student Hub, SAN and Rewards).
- **Rule register at the time of this audit**: Sprint1.md "Design basis and status — 2026-11-09", containing the confirmed calendar, rules R01–R15 and pending rules T01–T14. The current register has since moved to [Sprint2.md](Sprint2.md); this reference update does not revise the dated audit observations below.
- **Code**: this repository on 2026-11-10 (working tree), together with [implementation-status.md](implementation-status.md).

## 1. Sprint 1 constraints (criterion worth 5 marks)

| Sprint 1 constraint | What the prototype shows | Status |
| --- | --- | --- |
| Runs on standard computers with only a keyboard and mouse | The game uses keyboard and mouse only (no gamepad). Its fixed 1280×720 canvas scales to the window. | Met on the development Mac. School computers not tried. |
| Lightweight | The installed app has 30.6 MB of libraries plus a JDK 21. It ran at about 120 fps on the development Mac (Apple Silicon). | Not measured on school hardware. |
| Minimise third-party dependencies | build.gradle bundles FXGL 21.1, libGDX 1.14.2 and LWJGL3. No game code uses them; only `FrameworkDependencyTest` touches them. They make up about 20 MB of the 30.6 MB. | **Not met.** Removing them is a team decision and a Gradle change. |
| Integration: file paths and saving | Paths are built with `java.nio.file.Path`. The save goes to `~/.classroom-uprising/` and is written atomically through a temp file and a move. | Checked on macOS only. |
| Research: GUI, file storage, game state, unit testing, balancing | Done: JavaFX GUI, versioned day-end saves, 317 JUnit tests. Not started: balancing. Every number is a provisional Demo value. | Partly met. |
| Time: 18 weeks (Sprint1.md also shows a 16-week schedule and estimates about 85 hours) | The repository holds no hour records. | **Team** |

Course concepts required by the assignment:

| Concept | Found in code | Status |
| --- | --- | --- |
| Multiple classes | 85 main source files: 22 core and 63 frontend | Yes |
| Interface or abstract class | `StyleRule` (interface), `Screen` (abstract class), sealed interfaces `Outcome` and `CombatEvaluations.Result` | Yes |
| 2D arrays | None found by search | **Missing** |
| ArrayLists | Used in 24 files | Yes |
| Recursive algorithm | None found by a search for self-calling methods (the search is not exhaustive) | **Missing** |
| Sorting algorithm | Library sorting only (`sorted()` and `Comparator`); no hand-written sort | Library only |
| File I/O | `SaveManager` (day-end save), `DemoProfile`, `AudioSettings`, the asset manifest | Yes |
| Automated testing | 317 JUnit tests, 0 failures (macOS, 2026-11-10) | Yes |

## 2. Operating systems and libraries (criterion worth 5 marks)

| Platform | Checked | Not checked |
| --- | --- | --- |
| macOS, Apple Silicon, JDK 21 | Build; 317 tests; `installDist`; installed app launched; scripted play-throughs of Days 1–5; combat screen; audio playback | Manual play session |
| Windows | Nothing | Never built or run. School computers are probably Windows (**Team**: confirm). |
| Linux | Nothing | Never built or run. |

Known integration facts:

- **The build only runs where it was built.** JavaFX native jars are picked for the machine that runs the build. The current `build/install` folder holds `-mac-aarch64` JavaFX jars, so it runs only on Apple Silicon Macs. A Windows copy has to be built on Windows or packaged separately. This matters for the "runnable program in Google Drive" mark.
- **Labs need a full JDK.** The Java Lab feature compiles student code with `javac` from the Java installation running the game. On a JRE-only install the game says "Java Lab needs a full JDK".
- **Audio formats.** Music and sound use `javafx.media`, which plays mp3, m4a, wav and aiff, but not ogg.
- **Single JavaFX version.** FXGL's own OpenJFX jars are excluded, so only one JavaFX native variant ships (`FrameworkDependencyTest` checks this).
- **Feasibility wording.** The Sprint 1 Feasibility Study says compatibility across the team's operating systems "will not be tested". That was probably meant to say "will be tested". As written, it contradicts this criterion.

## 3. Progress against the schedule (criterion worth 10 marks)

No Gantt chart is stored in the repository. The only schedule here is the text in Sprint1.md, a 16-week Waterfall plan:

- Weeks 1–2: requirements
- Weeks 3–4: design
- Weeks 5–12: development
- Weeks 13–14: testing
- Week 15: integration
- Week 16: acceptance

Compare against the real Gantt chart (**Team**: current week and chart).

Development list for Weeks 5–12 compared with the code:

| Scheduled item | Status | Note |
| --- | --- | --- |
| Core classes | Built | Calendar, daily flow, effects, attendance, grades, makeups, combat rules |
| Student Hub | Built (Demo) | Two Demo activities: rest and study |
| Battle system | Built, but not reachable | Combat rules and the combat screen exist. No question bank or answer evaluator ships, so Day 5 reports T07. |
| Boss phases | Not built | Original JRPG item; the new design replaces it with Midterm and Final |
| Questions | Model built, no content | No question bank ships |
| Rewards / inventory | Data only | Waiting on T09 |
| EXP | Built | As Coding Vibe plus unit Knowledge |
| Skills, equipment, leaderboard, Practice Mode | Not built | Not part of the new design (**Team**: confirm they were dropped) |
| File I/O save and load | Built | Saves at day end only |
| Testing (scheduled for Weeks 13–14) | Started early | 317 automated tests exist. Human and UX testing have not started. |

Playable content covers Days 1–4 of 40 (the Demo). Days 5–40 still need content and rules.

**Commit history.** The repository has 5 commits from 2026-11-06 to 2026-11-08, all by one author. Everything since then is uncommitted. The rubric gives 10 marks for edit history from all members, and the current history does not show the team's work.

## 4. Scope match and feature creep (criterion worth 10 marks)

**Main finding.** The prototype follows the rule register (a campus-life semester). It does not follow the Sprint 1 Scope Document (a JRPG). The report should say the scope changed, when the team approved the change, and why (**Team**). Without that explanation, the whole game reads as feature creep.

Sprint 1 Scope features that were not built:

- Turn-based combat on a 2D classroom map, including movement
- Three boss phases
- Character levels
- Skill progression and Knowledge Shards
- Equipment
- Leaderboard
- Character selection
- Practice Mode

The Sprint 1 list of features that will **not** be developed, compared with now:

| Excluded in Sprint 1 | Now |
| --- | --- |
| Online multiplayer | Not built. |
| User-created content | Not built. |
| Real-time combat | Questions run on a 30-second soft clock, but the player still picks the order and answers one question at a time. Explain this as timed rather than real-time. |
| Multiple major bosses | The design brief adds a "Mr. Glad" round after a second cheating detection (not built). If the team keeps it, the report should explain it. |
| Open-world exploration | Places (classroom, hallway, hub, dorm, lab) are reached through menus; there is no free movement. |
| "Not like POTDs" (Vision of Solution) | The design brief makes Labs POTD-style Java coding, and a Java Lab runner is built. This **conflicts directly**; explain the change. |

Built features named in no version (feature-creep candidates):

| Feature | Link to a requirement |
| --- | --- |
| Lecture slide projector | Supports "intro course material" in the Lesson template |
| Music, interface sounds, volume settings | None. Sprint 1 lists only "game assets and UI elements". |
| Animated transitions, clock and calendar animation, menu motion | The clock and calendar show the R01 time slots; the animation itself is polish. |
| Esc menu with status, calendar and grades | End-user requirement: "Menus should clearly display HP, EXP, Knowledge, and … rewards" |
| Tutorial tip cards | R05 and T11 |
| Asset manifest with "missing art" plates | Art pipeline; no final art has been delivered yet |
| Bundled but unused FXGL and libGDX | None (see section 1) |

## 5. Conflicts to settle before more content is built

| # | Topic | Design brief | Rule register / code | Needs |
| --- | --- | --- | --- | --- |
| 1 | Length | Official plan: 30 days, Midterm Day 15, Final Day 30. The Chinese summary line also says 30, but its day-by-day list runs to 40 (Midterm 20, Final 40). | 40 days, Midterm 20, Final 40 (implemented) | One version |
| 2 | Day 0 | An undefeatable boss fight on Day 0 | Opening scene before Day 1; the opening battle is not built (T11) | Opening battle design |
| 3 | HP | "Represents the physical state of the player" | HP is "not defined as physical energy" | One definition |
| 4 | Participation | Feeds the final boss fight; skipping class lowers it | 10% of the final grade; two free absences, then −1 point each (R06, R07) | Which one, or both |
| 5 | Lesson branches | LOCK IN / DISTRACTED daydream chain (three steps); chess.com slacking choice | Exactly one choice node per Lesson (R03) | Allow multi-step choices, or merge into one node |
| 6 | Lesson interaction | Chapter 10 Days 1–2 list both "ask Mr. Reward" and "Mr. Reward asks the class" | One or the other per Lesson (R03) | Pick one per lesson |
| 7 | Hand-raise answer | Options A–D; correct gives "+CP, +Participation", wrong gives −SAN | Lesson choices are ids with fixed effects; they have no options and no correct answer. "CP" is undefined. | Content model change (backend) and a definition of CP |
| 8 | Cheating | 1st time caught: large loss (Chinese notes: HP, XP, SAN and Participation). 2nd: Mr. Glad round. 3rd: game failure ("R U Glad?") | Large SAN loss; serious cheating scores zero; no HP, XP or Participation loss assumed (R12, T09) | Penalties and the repeat rule |
| 9 | Reward timing | R 10 s, SR 15 s, SSR 20 s; Safe 5 s | Safe 5 s; Risky at most 20 s, exact value pending (T09) | If approved, this settles part of T09 |
| 10 | Endings | 12 named endings. "Healthy/unhealthy" and grades A/B/C are undefined. Failing with normal SAN has no ending, and Retake is not mentioned. | Drop or Retake at Midterm, two Retakes, no speculative endings (R13, T10) | Thresholds, priority, the fail case |
| 11 | Test questions | Multiple choice, A–D | Combat answers are free text; question banks have no options | Options in the question bank (backend) and a choice UI (frontend) |
| 12 | SAN and time | SAN shortens the question time limit ("numbers not decided yet") | 30 s soft limit (T07); no SAN link | Rule and values |
| 13 | Weight step | "(n−1)%" vs the example 1.0, 1.1, … 1.4 | T07 | Pick one |
| 14 | Chapter template | "Day 7 Chapter test" | A chapter has five days | Typo: should be Day 5 |
| 15 | Chapter 9 test | Q10 is cut off. Q4's options use `Example` but the array is `example`, and Java is case-sensitive. "Row 3, column 4" does not say whether counting starts at 0 or 1. | — | Content fix |

## 6. Notes for the AI Prompt & Response Journal

- **AI tools used.** The JavaFX frontend (`ui` package) was generated with Claude Code (Anthropic). The course policy names Gemini, so the journal should record the tools actually used. The repository does not say which tool produced the backend (**Team**). Claude Code keeps raw session transcripts (JSONL) under `~/.claude/projects/` on the machine where it ran; they can serve as the raw-output record.
- **Flaws found in AI output this week** (each was fixed and rechecked; all are in the frontend):
  - The broken-music check relied on a JavaFX error that never arrives for some bad mp3 files. Fixed by adding a 5-second readiness limit.
  - Stopping the old music track raced with releasing it, so the player ended as STOPPED instead of DISPOSED.
  - The HUD's top shading dimmed the slide label.
  - The slide's large view collided with the dialogue name tag.
  - The volume meters on the title screen were misaligned because the row labels had no fixed width.
