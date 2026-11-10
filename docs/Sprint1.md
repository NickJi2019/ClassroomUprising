# **Software Dev. Sprint 1**

TeamPike

assignment: [Software Development Project](/docs/assignment.md)

## **Software Scope document**

### **Problem Statement (jerry)**

Dual problem: Students struggle with abstract OOP(Object Oriented Programming) concepts (recursion, arrays, File I/O) due to dry lessons and exam stress, while standard turn-based games lack meaningful long term progression and strategic variety.

Core solution: *Classroom Uprising: Defeat Mr. Reward* *(if this sounds familiar, you have mistaken\! Any resemblance is purely coincidental)*. It merges JRPG game design with Computer Science education, turning game failure into a learning loop where players earn permanent resources (EXP, Knowledge Shards) to upgrade stats and skills.

Technical integration: Directly incorporates course requirements, such as dynamic arrays, File I/O persistence, and recursive skills into core gameplay mechanics.  
Stakeholders: Target end users are CS students needing engaging practice. Secondary stakeholders include educators seeking higher engagement and the development team building a practical OOP system.

Risks and assumptions: Primary risks involve scope creep from adding RPG mechanics, game balancing issues, and potential savefile corruption via File I/O, assuming development proceeds on a standard Java compatible desktop setup over an 18-week timeline.

In Computer Science education, students often struggle to maintain engagement and retain abstract OOP concepts, such as recursion, dynamic data structures, and file persistence, when taught strictly through traditional lessons and static assignments, a difficulty further compounded by severe exam anxiety and academic stress. Concurrently, traditional turn-based educational games often suffer from a severe lack of long term progression quickly becoming repetitive and predictable once players grasp the basic combat loop because failure provides no lasting reward, meaningful feedback, or incentive to adapt strategy. *Classroom Uprising: Defeat Mr. Reward* directly resolves both challenges by seamlessly integrating Roguelite growth mechanics with core Computer Science principles into a unified, highly engaging progression loop.

By replacing punitive Game Over screens with persistent rewards, such as experience points (EXP) for character attributes and Knowledge Shards for skill trees, the application transforms failure from a demoralizing setback into an essential learning mechanism. Players naturally progress through repeated attempts, employing abstract Computer Science concepts as functional gameplay mechanics. For instance, dynamic ArrayLists manage inventory and question banks, recursive logic powers, advanced combat abilities like Stack Overflow and File I/O handles long term savestate persistence.

Designed primarily for high school and introductory college Computer Science students seeking an interactive study aid, as well as educators aiming to improve concept retention, the project clearly defines its stakeholders and functional scope. While risks such as scope creep from feature-heavy RPG systems, delicate combat balancing, and potential save-file corruption exist, they are systematically mitigated through structured software engineering practices. Ultimately, by executing a phased 18 week development timeline on a standard Java desktop environment, the project delivers a balanced, educationally valuable software system that turns a daunting academic curriculum into an empowering interactive experience.

### **Vision of Solution (jorlanda)**

Our proposed solution is the game called *Classroom Uprising*, a Java-based and turn-based strategy game that combines classroom-themed combat with character progression. We aim to create an engaging environment in which computer science concepts are represented and delivered to players throughout the game narrative. It would not take the form of isolated, topic-based programming exercises (not like POTDs). Moreover, the game will make failure and defeat part of the progression process, aiming to avoid the repetitiveness of simple turn-based combat.  We also do not expect players to defeat the boss immediately or early in the game; rather, we set resources, character improvement, learnt attack patterns and new strategies to characters in each failed attempt.

Players will begin as Level 1 students whose ultimate objective is to defeat Mr. Reward *(if this sounds familiar, you have mistaken\! Any resemblance is purely coincidental)*, the teacher boss of the room. During each battle, the player must make strategic decisions between attacking, moving, studying, using items, and activating special abilities. Battles take place on a classroom map, where the player’s positions affect how they respond to attacks. Mr. Reward progresses through three increasingly difficult phases: Classroom, Midterm, and Final Exam. He will have different attacks and combinations of previously introduced mechanics.

There will be a progression loop in which both the character’s abilities and the player’s understanding of the boss improve over time.

Our major game features will include a turn-based combat system, a classroom battlefield, boss attack patterns, three progressive boss phases, academic programming question challenges, character levels, experience points, skill progression, equipment management, and a leaderboard. Standard interfaces will include a main menu, Student Center, character selection, battle interfaces, skill menus, instructions, and result panels (specific details can be altered later).

In order to maintain a realistic development scope, the game would not include:

- Online multiplayer versions

  The game will not include online multiplayer, matchmaking, or player vs. player battles. These features require networking, server management, synchronization, and additional testing that are implausible for the scope of this project. The game will instead focus on a complete single-player experience.

- User customed characters

  Players would not be able to create their own characters, abilities, questions, maps, or boss attacks. Supporting user-generated content would require additional editors, validation systems, file management, and extensive testing, which is also implausible for this project. Players will instead choose from/be assigned to predefined characters and progression options.

- Real-time combat

  Combat will remain turn-based rather than real-time movements and attacks. Real time combat needs substantially more complex input handling, collision detection, timing, and animation systems, while turn-based better supports the game’s emphasis on strategy and academic challenges.

- Multiple major bosses

  We focus on Mr. Reward as the single major boss rather than multiple teachers or boss characters. Developing additional bosses will require new attack patterns, animations, and progression systems. Instead, our variety will come from Mr. Reward’s three increasingly difficult phases.

- Open-World Exploration

  Players will not be able to freely explore a larger school environment with classrooms, hallways, or other locations. This would require additional map designs, geographical features, interactions, and navigation systems without significantly contributing to the core turn-based game design.

## **Feasibility study:**

### **Problem definition (jerry)**

*Classroom Uprising: Defeat Mr. Reward* is a Java based, turn based Roguelite role playing game designed to address the persistent lack of long term engagement and concept retention in Computer Science undergraduates, and educators seeking supplementary interactive teaching tools, the application transforms daunting software engineering principles. Such as recursion, dynamic arrays, sorting algorithms, and File I/O persistence into interactive, core gameplay mechanics. Players assume the role of a student attempting to defeat their formidable professor, Mr. Reward, in a challenging classroom battle system where initial defeat is not a penalty, but rather an essential stepping stone for long term progression. By earning persistent experience points and knowledge shards through repeated attempts, users systematically unlock new skill trees, manage customizable equipment, and refine their tactical strategies over time. The inferable purpose of this software is to reduce academic exam anxiety, foster intrinsic learning motivation, and provide a practical, highly contextualized platform where students master essential Object Oriented Programming (OOP) concepts through an iterative, rewarding growth loop that bridges theoretical Computer Science fundamentals with engaging gameplay dynamics.

### **Problem analysis (constraints) (jorlanda)**

In order to ensure the project being successful within the schedule and available school resources, this application will therefore be designed as a relatively lightweight Java game that can run on standard school and personal computers without requiring specialized hardware (only require a keyboard and a mouse). Because it is developed in Java, compatibility across the OS used by team members will not be tested, especially for graphical interfaces, file pathnames, saving files, and external libraries. We will minimize unnecessary third-party dependencies to reduce integration problems.

The intended users are primarily high-school students and other users familiar with basic computer interaction. Time is the most significant constraint. We must implement, document, test, and maintain the application within 18 weeks while also completing the other deliverables. Features such as multiplayer and additional bosses are therefore outside the initial scope.

Research will be required for Java GUI development, persistent file storage, game-state, unit testing, and balanced progression systems. Additional testing will be necessary to ensure that player growth is noticeable without making later encounters trivial and that the first encounters are difficult without making progression feel excessively slow.

### **End user requirement (niki)**

- Core Gameplay: Players should be able to repeatedly challenge Mr. Reward, earn rewards from each attempt, upgrade their level, and eventually defeat the Boss.
    - Progression: The game must include EXP, level, skills, equipment, and persistent progress.
    - Replayability: Losing a battle will not end the game.
    - User Interface: Menus should clearly display HP, EXP, Knowledge, and most importantly, rewards.
    - Difficulty: Mr. Reward should be difficult to defeat at Level 1 but be defeatable through progression.
    - Saving: Players should be able to save game processes manually and also automatically, including their current skills, equipment, and rewards.
    - Feedback: The game should clearly communicate damage, reward, unlocked skills, new phases, and victory.
    - Accessibility: Controls and instructions should be simple enough that a player could easily understand the basic gameplay without outside assistance.

### **Software project plan (niki)**

1. Statement of Work  
   Project: Classroom Uprising: Reward from Reward

   The team will design and develop a turn-based progression game in which the player controls a student attempting to defeat Mr. Reward, also known as the final Boss in this game. The player begins at Level 1  with limited abilities and gradually becomes stronger by completing battles, answering questions, earning rewards and Knowledge Shards, unlocking skills, and obtaining equipment.



2. Requirements

   The project will require several resources for development, testing, and documentation. Java will be used as the main programming language, while an IDE will be used for programming and debugging. GitHub or Google Drive will be used for version control and file sharing, and computers will be required for development and testing. Java ArrayLists will be used to organize inventories, skills, questions, and battle history, while 2D arrays will be used for battle or classroom map. File I/O will be used to save and load player progression, and sorting and searching algorithms will support features such as the leaderboard, equipment, skills, and player data. Automated testing will be used to test game mechanics and calculations. The project will also require game assets and UI elements, including menus, characters, the Boss, icons, and other interface components. Project documentation will be maintained throughout development to record requirements, schedules, testing and design decisions. Team responsibilities will be divided between a Project Manager, Lead Programmer, Gameplay Programmer, UI/Design Lead, Testing Lead and Documentation Lead.

3. Timing

   The project will be completed through a series of development stages with an estimated total workload of approximately 85 hours. The first stage will focus on gathering requirements and developing the game concept, followed by designing battle mechanics and creating the class structure and architecture. Development will then move into the character and class system, Student Hub and menus, and the main battle system. After the basic combat system is functional, the team will develop Mr. Reward's Boss phases and adaptive behaviour, followed by the EXP and leveling system, skills and equipment. The save/load system will then be implemented	 using File I/O, followed by the leaderboard, sorting, searching, and automated testing. The final stages will focus on bug fixing, integrating all systems, and completing the final polish and documentation.

**Schedule:**

The project will follow a 16-week Waterfall development model, with each phase completed before moving on to the next. The project will begin with requirements and planning, followed by system and game design, implementation, testing, and final deployment.

- Week 1-2: Gather user requirements, define the game concept, features, target users, and project scope.
    - Week 3-4: Finalize gameplay, UML diagrams, class structure, inheritance, interfaces, and system relationships.
    - Week 5-12: Develop the core classes, Student Hub, battle system, Boss phases, questions, rewards, EXP, skills, equipment, inventory, Practice Mode, File I/O, saving/loading, and leaderboard.
    - Week 13-14: Conduct black-box, white-box, automated, and manual testing; identify and fix bugs and verify requirements
    - Week 15: Conduct system integration, improve the UI, balance gameplay, and resolve remaining issues.
    - Week 16: Perform final acceptance testing, confirm all requirements are met, and prepare the final game and documentation for submission.

Several risks may affect the project’s development. The battle system could become too complicated, so the team should build basic combat first before adding Boss phases and special mechanics. Falling behind schedule can be reduced by breaking large features into smaller tasks and prioritizing the core gameplay. Code conflicts between team members can be managed through clear class responsibilities and regular integration. Save/load problems should be addressed by testing File I/O separately and validating saved data. The game’s difficulty should be adjusted through playtesting so that it does not become too easy or too difficult. Since bugs may occur when different systems interact, features should be integrated gradually rather than combining everything at the end. If development time becomes limited, the core battle and progression system should be prioritized while Practice Mode and additional polish remain secondary. Other risks, such as artificial uses of sorting and searching, reward farming, incomplete File I/O, and players not understanding the mechanics, can be addressed through natural uses of these algorithms, balanced rewards, early save/load testing, and clear instructions or a tutorial battle.

## **Design Documents**

### Design basis and status — 2026-11-09

This section records the confirmed campus-life design target. A first Java foundation now exists; implemented and pending responsibilities are separated in [implementation status](implementation-status.md). The IPO is not a claim of complete gameplay. The class and sequence diagrams now show the implemented foundation and its explicit pending-policy boundaries. The earlier Scope and Feasibility sections are preserved for a separate revision; where they describe a different game, use the confirmed rules below for these three design artifacts, not an implied combination of both versions.

HP is **not** the course percentage and is **not** defined as physical energy. Assessments may contain fantasy combat; the Final definitely uses turn-based question combat. HP-to-grade mapping, detailed combat presentation, HP cost/depletion and other undecided mechanics remain explicitly TBD. An unknown rule is not a default rule.

### Confirmed calendar

The playable semester has 40 days. The opening shows the previous semester's difficult Final battle against Mr. Reward and its scripted defeat, introduces combat, and establishes the current retake. It precedes the Day 1–40 schedule.

| Day(s) | Scheduled ICS4U content |
| :---- | :---- |
| 1–2 | Grade 11 review lessons alongside classroom and Student Hub tutorials. Mr. Reward teaches the review; separate in-game tip cards explain systems and tutorial warnings. All penalties become warnings only. |
| 3, 4, 5, 6, 7 | Chapter 1: Lesson, Lesson, Lab, Lesson, Unit Test. |
| 8, 9, 10, 11, 12 | Chapter 2: Lesson, Lesson, Lab, Lesson, Unit Test. |
| 13, 14, 15, 16, 17 | Chapter 3: Lesson, Lesson, Lab, Lesson, Unit Test. |
| 18–19 | Midterm Review. |
| 20 | Midterm: random questions, immediate official result, no skipping. |
| 21, 22, 23, 24, 25 | Chapter 4: Lesson, Lesson, Lab, Lesson, Unit Test. |
| 26, 27, 28, 29, 30 | Chapter 5: Lesson, Lesson, Lab, Lesson, Unit Test. |
| 31, 32, 33, 34, 35 | Chapter 6: Lesson, Lesson, Lab, Lesson, Unit Test. |
| 36–39 | Final Review. |
| 40 | Final: random questions, turn-based question combat, immediate official result, no skipping. |

### Confirmed shared rules

| Rule ID | Confirmed behavior |
| :---- | :---- |
| R01 — Time | Each normal day has one ICS4U slot, one OTHER_CLASSES slot, and three Hub slots: AFTER_SCHOOL, EVENING and LATE_NIGHT. A normal activity costs one time unit. Sleeping immediately after school gives a SAN bonus and an extra half-unit on waking the next day; numerical effects of activities in that half-unit are halved. Normal sleep in EVENING or LATE_NIGHT closes the day without the early-sleep half-unit or a late-night-activity penalty; recovery amount is explicitly configured. |
| R02 — Late night | Using the late-night slot lowers SAN and the XP gain multiplier for the following day. This is a next-day effect, not deletion of accumulated XP. Amounts and how the SAN reduction interacts with other effects are TBD. |
| R03 — Learning and participation | XP contains total Coding Vibe and separate unit Knowledge. XP helps assessments through hints/confidence; SAN also affects assessments. Each Lesson presents its content and then one settled interaction: optionally participate, then select a concrete response, or stay quiet. Opening the response menu grants nothing and is not a second rewarded node. A given Lesson offers either asking or hand-raising, not both. Day 1 offers answers to Mr. Reward; Day 2 offers three questions to ask. Appropriate responses alone earn learning bonuses; inappropriate responses have no bonus and reduce SAN outside tutorial days. Quiet has no effect. |
| R04 — Other classes | OTHER_CLASSES remains. Focusing on the other class produces no stat change; revising ICS produces +XP and −SAN. There is no other-subject Knowledge stat. |
| R05 — Tutorial | The opening introduces the systems. Days 1–2 combine Grade 11 review lectures with classroom/Hub tutorials. Character dialogue carries teaching content and its inline `[slide:name]` cues select the accompanying review slides; separate in-game tip cards carry system instructions and tutorial-warning explanations. All tutorial penalties are warnings only, never deferred. In particular, a wrong/inappropriate classroom response on Days 1–2 gives no bonus and only a warning, with no actual SAN deduction. Opening combat details and tutorial attendance bookkeeping remain TBD. |
| R06 — Grade components | The final percentage uses 65% Term, 10% Participation and 25% Final. Midterm belongs to Term. Unit Labs and Unit Tests have equal weight; Midterm's relative weight within Term is not yet specified. Do not infer thirteen equally weighted Term items. |
| R07 — Absence | The first two unexcused absences do not reduce the grade; each subsequent unexcused absence subtracts one percentage point from the overall grade. Excused leave does not count as an absence. Participation bounds and other participation scoring are TBD. Midterm and Final cannot be skipped. |
| R08 — Assessment forms | Unit Labs and Unit Tests use fixed questions. Midterm and Final randomly select questions. Labs accept complete Java source, run built-in tests, and check a coding-style rubric. The combat soft threshold is 30 seconds; timing lifecycle, pools, execution rules and rubrics still have explicit pending decisions. Final turn-based question combat is confirmed. Labs/Tests now also have the confirmed question-order, soft-timeout, fatigue, Dasher and Combo mechanics below; detailed presentation, grading and HP depletion remain TBD. |
| R09 — Results and estimates | Ordinary Lab/Unit Test results, including ordinary makeups, return three in-game days after submission. Midterm and Final are exceptions: official results are immediate. During an applicable assessment, the player may spend SAN to self-estimate the mark; the estimate can be wrong and produces provisional effects. Official return resets the estimate-related effects. Exact estimate/reset calculations are TBD. |
| R10 — SAN and grades | Current average and changes in that average affect SAN. The stated target is a buff above 95 and a debuff below 95; the exact 95 boundary, average calculation, magnitudes and combinations are TBD. HP is distinct from these grade records. |
| R11 — Makeups | A missed ordinary assessment is replaced by a makeup in the first or second subsequent Lesson. The replaced Lesson grants no normal lesson bonus. The makeup uses the same questions, has a SAN debuff, forbids looking at a classmate's answers, and makes cheating easier to detect. Selection between those Lessons, collisions and end-of-term handling are TBD. |
| R12 — Cheating | Being caught causes a large SAN decrease. Serious cheating sets the current assessment's mark to zero. Tutorial penalty suppression applies. Detection rates, seriousness and remaining item/event rules are TBD; do not retain the earlier blanket claim that cheating affects grades only. |
| R13 — Midterm and endings | At the Midterm checkpoint, the player may Drop or Retake; low grades together with low SAN prohibit continuing. Retake has two opportunities, returns to Day 1, preserves XP and starts with a SAN debuff. Very low SAN unlocks a bad ending. Other ending branches are academic/support outcomes; thresholds and branch priority are TBD. |
| R14 — Persistence and carryover | Saves occur only at day end. An in-semester Retake is different from a new playthrough: only selected rewards carry into a new playthrough; other progress does not. The two Retake opportunities remain bounded across resets. The full Retake reset list and exceptional save boundaries are TBD. |
| R15 — Social play | Classmate interactions have persistent but shallow relationships. Concrete relationship values, events and benefits are TBD. |

### **IPO diagrams**

| Core function | Input | Process | Output |
| :---- | :---- | :---- | :---- |
| **1. Start or resume a game** | New Game / Continue / Instructions / Exit; an existing day-end save | Start the opening tutorial for a new game, or validate and restore saved progress without reapplying recorded effects. Distinguish loading from Retake and from a new playthrough. | Opening or restored progress; instructions; exit; or an explicit load error. Save format/resume boundary: T12. |
| **2. Play the opening and first-two-day tutorials** | Previous-semester Final scene; battle tutorial actions; Day 1–2 lesson/Hub choices | Present the difficult fight and scripted defeat, establish the retake, and teach Grade 11 review through Mr. Reward's Day 1–2 lectures. Explain classroom/Hub operations and tutorial warnings through separate in-game tip cards, not character dialogue. Replace every tutorial penalty with a warning rather than applying or postponing it. | Tutorial progress and explanations; entry into normal semester play after Day 2. Detailed content and tutorial bookkeeping: T11. |
| **3. Begin a day and return due marks** | Day/phase; fixed schedule plus makeup replacements; pending records; next-day effects; early-sleep entitlement | Apply the effects due for this day once. Offer the earned half-unit after waking, with half numerical activity effects and the early-sleep SAN bonus. Return due ordinary marks, replace their estimates and trigger the corresponding grade/SAN feedback. Show today's plan. | Current schedule, effective SAN/XP conditions, optional half-unit, and due results. No repeated rewards on reopening the display. Effect calculations: T03–T04, T08. |
| **4. Record attendance and route ICS4U** | Today's scheduled course; attend/skip input where allowed; excused-leave status | Reject skipping Midterm or Final. For a permitted ordinary absence, record attendance under R07 and route a missed assessment to makeup planning. Otherwise enter the scheduled Lesson, Review or assessment. Do not substitute a different course at will. | Attendance record; permitted course/absence route; absence deduction if applicable; or rejected major-exam skip with unchanged progress. Leave/skip details: T05, T11. |
| **5. Complete a Lesson or Review** | Scheduled lesson content and title; participation/quiet menu; concrete response option ID; XP and SAN | Present Lesson content, then offer participation or quiet. Participation opens the authored response options without a state change; resolve only the selected final option (or neutral quiet) once. Day 1 uses RAISE_HAND and Day 2 uses ASK_TEACHER, with three responses each. Appropriate options earn the configured bonus; inappropriate options grant no bonus and only reduce SAN outside the fully exempt tutorial. Finish the class slot once. Review content and interaction details are not assumed identical to Lessons. | Learning and participation feedback; updated progress; completed ICS4U slot. Values, choices and Review behavior: T03–T04, T14. |
| **6. Complete OTHER_CLASSES** | Focus on the other class / revise ICS | Spend the one scheduled class slot. Focus produces no stat change; ICS revision increases XP and reduces SAN, subject to tutorial penalty suppression and active next-day conditions. | Unchanged stats or +XP/−SAN; finished class slot. No other-subject Knowledge value is created. Magnitudes and XP split: T03–T04. |
| **7. Perform a Hub or bonus-time activity** | Current available slot; activity/interaction; time entitlement; item/relationship availability | Validate the choice, consume its stated time once, and apply the corresponding learning, rest, entertainment or relationship effect. Ordinary activities cost one unit; an earned morning half-unit scales numerical activity effects to half. Apply tutorial suppression before penalties reach state. | Updated time, states and relationship/event records, or a rejection without resource changes. Specific activity/item effects and half-scaling details: T04, T09, T13. |
| **8. Choose sleep or use late night** | Sleep/activity decision; current Hub slot | If sleeping immediately after school, end the remaining day's activities and record next morning's half-unit and SAN bonus. If using late night, record next-day lower SAN and XP gain multiplier unless tutorial suppression applies. Normal sleep in EVENING/LATE_NIGHT closes the day, grants the explicitly configured recovery once, and grants neither the early-sleep half-unit nor a late-night-activity penalty. Recovery is recorded in the closed-day state, not regranted after loading. | Sleep decision and dated next-day effects/entitlement; progression to day end. Production values and remaining half-slot details: T03–T04; normal-sleep operation is confirmed. |
| **9. Attempt and submit an assessment** | Scheduled original/makeup assessment; full Java source or answers; Coding Vibe, unit Knowledge, SAN and HP | Load fixed ordinary questions or sampled Midterm/Final questions. For a Lab, run complete Java code against tests and check the style rubric. For combat, track player-chosen order, single-question time, trusted evaluated answers, configured fatigue costs, timeout weights, last-answer Dasher and timely-completion Combo. Convert the resulting credits and separate bonus with an explicit grade policy. Apply the agreed makeup restrictions where relevant. Keep HP, submitted work and assessed marks separate. | Submitted assessment record and feedback; evidence from tests/style checks; battle state where defined. No guessed HP-to-score formula. Remaining execution, timing, HP and conversion decisions: T01, T06–T07. |
| **10. Self-estimate, release and record results** | Player's estimate request; SAN cost; submitted work; return day or immediate major-exam result | Record an uncertain estimate and its provisional effects separately. Ordinary marks become official after three days; Midterm/Final marks are official immediately. Replace estimate-related effects on official return and calculate SAN feedback using the current average/change under the eventual formula. Do not require an ordinary mark to return before leaving that day's class. | Clearly labelled estimated/official mark; one-time grade feedback; current grade record. Grading and replacement rules: T02–T03, T08. |
| **11. Use an assessment reward or resolve cheating** | Selected owned reward; applicable assessment; caught/severity outcome | Apply the eventual item's defined effect. If caught, apply the large SAN penalty; if serious, set this assessment's mark to zero. Use the higher detection risk and no-classmate-peeking rule during makeups. Replace all tutorial penalties with warnings. | Item/assessment outcome and explanation; updated SAN/grade where applicable. Item details, severity and repeated-detection events: T09. |
| **12. Schedule and complete a makeup** | Missed ordinary assessment; original question set; next two Lesson candidates; existing replacements | Record the makeup requirement and replace the selected eligible Lesson, without its normal learning bonus. Retain the original questions and attach the confirmed makeup conditions. Do not silently choose between competing dates or invent a post-term exam. | Makeup replacement and later submission, or a visibly unresolved scheduling case. Selection, conflicts and final-week cases: T05; ordinary makeup marks return after three days. |
| **13. Resolve Midterm, Retake or semester ending** | Immediate official Midterm/Final result; grades, SAN, XP; remaining Retake opportunities; reward records | At Midterm, allow the confirmed Drop/Retake choices, and prevent continuation when both grade and SAN satisfy the eventual low thresholds. A permitted Retake consumes one of two opportunities, preserves XP, returns to Day 1 and applies its initial SAN debuff. At semester end, derive a percentage and an ending from the agreed grades/XP/SAN rules. | Continue, Drop, Retake, or an academic/support/bad ending; selected-reward-only carryover for a new playthrough. Exact thresholds, reset list, priority and HP mapping: T01–T03, T10. |
| **14. End the day and save** | Completed day/sleep decision; student and assessment records; effects, schedule changes, relationships and run progress | Produce the day summary without repeating rewards. Save only at the day-end boundary, including pending marks, estimates, future effects and Retake count. Report failure explicitly. Advance or end the run according to its resolved status; never advance Final to an invented playable Day 41. | Day-end save status and next-day/ending transition. No mid-activity save operation. Exact snapshot timing and Retake/ending failure handling: T12. |

### Pending rules — no defaults are authorized

| ID | TBD decision |
| :---- | :---- |
| **T01 — HP and combat** | Answering now consumes HP, with faster answers causing more fatigue. The cost curve, HP range/recovery/depletion and battle presentation remain open. Dasher and Combo are confirmed below; HP is still separate from a grade. |
| **T02 — Grade calculations** | Midterm's share within Term; within-category aggregation; current-average calculation while marks are pending; participation's other scoring rules and deduction floor/cap; final percentage conversion. Unit Labs and Unit Tests have equal weight, but no 13-item equal-weight formula is approved. |
| **T03 — SAN, XP and thresholds** | Starting values, limits, numerical changes, grade-related SAN calculation and exactly 95; assessment influence of SAN and Coding Vibe/unit Knowledge; next-day SAN stacking/recovery; low-grade/low-SAN thresholds. The late-night XP reduction lasts the following day, but its multiplier is TBD. |
| **T04 — Time and activity values** | Exact activity rewards, XP split, sleep recovery and early-sleep SAN bonus; how the half-unit handles rounding, non-numeric choices and discrete item effects. Normal sleep in EVENING/LATE_NIGHT is confirmed; other unused-slot choices and detailed ordinary-skip activity handling are not fixed. |
| **T05 — Absence and makeups** | Who selects the first versus second subsequent Lesson; date collisions, repeated absence, absence from a makeup, and an exam with no later Lesson (including the final chapter). Clarify exceptional approved leave for mandatory major exams and whether a missed assessment has any temporary grade. Do not invent a zero for mere absence. |
| **T06 — Java Lab evaluation** | Java version, execution environment, input/output contract, built-in tests, rubric items and scoring, compile/runtime failure treatment, resource limits, submission/retry rules and how evidence produces marks. The Java runner exists; its evidence-to-credit rubric and combat-credit/Combo-to-percentage conversion remain unapproved. |
| **T07 — Questions and timing** | The 30-second soft threshold and continued answering after timeout are confirmed. Remaining: .01 text versus .1 example weight increment, authored difficulty ordering, timeout/Dasher composition, whether evaluation time counts, switching/revising/retrying questions, pause behavior and Lab question granularity. Fixed ordinary and sampled major banks remain. |
| **T08 — Estimates** | Estimate availability (especially for immediate-result exams), SAN cost, error generation, repeat-estimate rules and the exact replacement of provisional SAN/average effects when official marks return. Unrelated effects must remain identifiable; do not reset an entire state value by guesswork. |
| **T09 — Rewards and cheating** | Safe timing is 5 seconds; Risky timing is at most 20 seconds. Exact Risky duration, inclusion in question time, item effects, ownership/consumption, detection rates, definition of serious cheating, and unresolved repeated-detection scenes from the brainstorm. Latest confirmed penalties are large SAN loss and a zero on this assessment for serious cheating; no additional XP, HP or participation punishment is assumed. |
| **T10 — Run transitions and endings** | Thresholds and priority among forced Drop/Retake and low-SAN bad endings; behavior after both Retakes are used; reset/retention of non-XP data on Retake; tutorial replay and its SAN-debuff interaction; which rewards carry to a new playthrough and its opening. No speculative ending is added. |
| **T11 — Tutorial details** | Confirmed: Days 1–2 are Grade 11 review lessons plus tutorials; system instructions and tutorial-warning explanations use in-game tip cards, not character dialogue. Day 1 has 16 spoken lines covering review purpose, Java files/main, data types, variables, operators and decisions across seven slides. Day 2 has 15 lines covering loops, methods/pythag, self-directed topics including arrays, and automated testing across five slides. Confirmed classroom increment: Day 1 answers and Day 2 asks a chosen question; wrong tutorial choices grant no bonus and only warn. Remaining: opening combat interactions, later content refinement and whether tutorial absence records consume the two future free absences. All tutorial penalties are waived, not deferred; the scripted previous-semester defeat is story context. |
| **T12 — Day-end persistence** | Snapshot just before/after day advancement; exact resume position; save failure/retry; and persistence at Midterm Drop/Retake or Final ending while retaining the day-end-only rule. No extra autosave point is assumed. |
| **T13 — Relationships** | Which classmates, relationship values, available interactions, conditions, benefits and event effects. Keep the system persistent but shallow; no deep relationship tree is specified. |
| **T14 — Content** | Day 1–2 Demo review lectures in lesson.1.text and lesson.2.text now match the narrated Review Overview slide text in backend-request-review-lectures.md, including inline slide cues; their lecture text and slide cues remain unchanged. The new LessonDecision template defines title, prompt, three Demo response options, neutral quiet, feedback and explicit effects for Days 1–4. Day 1 is RAISE_HAND and Day 2 is ASK_TEACHER. Correct-option bonus values are retained; incorrect SAN costs are provisional Demo configuration, never tutorial penalties. Remaining content includes later lessons, Review-day behavior, opening/scene dialogue and release conditions for story and knowledge. A Lesson still settles once; the participation menu is presentation-only and grants nothing. Frontend wiring belongs to Claude; see frontend-classroom-handoff.md. |

### **Class diagram**

Mermaid file: [class diagram.mmd](class%20diagram.mmd). It shows implemented foundation classes, the JavaFX frontend layer and explicit policy boundaries. Methods accepting evaluated scores or effect inputs do not imply an automatic grader or approved numerical policy. See implementation-status.md for undelivered features.

### **Sequence diagram**

Mermaid file: [sequence diagram.mmd](sequence%20diagram.mmd). It traces callable prototype operations, including rejection, result timing and day-end persistence. Battle, grading and ending policies remain explicit TBD boundaries. Diagram rendering alone does not establish working gameplay; executed tests are recorded separately.

### Approved Demo configuration exception (2026-11-09)

The team approved a separate, clearly labelled Demo profile to unblock the frontend, with centralized adjustable initial attributes and classroom/Hub values. These values are **not** production balancing decisions and do not close T03/T04. The shipped profile covers Days 1–4 only; the 40-day calendar and remaining content/policy gaps are preserved. The Demo uses ASK_TEACHER for Day 3, while final content authors still choose one interaction for each lesson. Normal sleep in EVENING/LATE_NIGHT is separately confirmed; recovery must be explicitly supplied and does not earn early-sleep time or count as late-night activity.

### Confirmed combat increment

Labs and Tests present questions of increasing difficulty and let the player choose answer order. Initial weights increase with difficulty position. The source text says `(n-1)%`, while its example is `1.0, 1.1, 1.2, 1.3, 1.4`; the increment remains an explicit T07 choice, not silently resolved.

Each question has a 30-second soft threshold. More than 30 seconds reduces its initial weight to 1.0, but does not prevent answering. Answering costs HP; faster answers cause more fatigue, with the formula still T01. After n-1 completed answers, Dasher doubles the last answer's mark. Its interaction with timeout remains T07. Completing all questions within time grants Combo and five bonus points; the current wording adds no correctness requirement. The unit of those points and conversion/capping into a course percentage remain T06.

Safe Reward duration is five seconds. Risky Reward duration is at most twenty seconds. Exact Risky timing, whether reward time counts against a question, ownership, effects and detection remain T09. Existing day-end saving, attendance, ordinary result delays, major immediate results, serious-cheating zero and tutorial penalty exemptions remain in force.

The backend separates explicit start-question, submit-work and trusted evaluation operations. It supports configurable stop-at-submission or stop-at-evaluation timing without picking a production default. Unfinished-question switching, revisions and pause behavior remain pending. Backend API details and frontend acceptance checks are in [frontend combat handoff](frontend-combat-handoff.md); they do not approve fixture values or replace this rule register.
