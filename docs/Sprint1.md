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

### **IPO diagrams**

| Core function | Input | Process | Output |
| :---- | :---- | :---- | :---- |
| **1\. Navigate the main menu** | New Game, Continue, Instructions, or Exit selection; saved-game availability | Route the selection. For New Game, initialize student state, calendar, and content flags. For Continue, validate and restore the complete saved state. | Opening story for a new game; restored current screen for a saved game; instructions; exit; or a load-error message. |
| **2\. Play the opening story** | New-game state; scripted scene in which an ICS4U newcomer receives a harsh introduction from Mr. Reward; player dialogue-advance input | Present scenes in order. Record completion after the final scene. Apply any configured opening effects once. Prevent normal daily gameplay until the introduction is complete. | Completed introduction; established student state; entry into Day 1\. |
| **3\. Begin a school day** | Current day; school schedule; student state; story/knowledge flags | Set the period to ICS class. Initialize the day's fixed action allowances. Check date-based story and knowledge unlocks, then check scheduled events. Do not reset accumulated knowledge, mental state, or HP/grade. | Day and schedule display; remaining-action counters; available topics and story notifications; normal ICS class or scheduled Lab/Test entry. |
| **4\. Perform an ICS-class activity** | POTD, answer a question, ask the teacher, or slack off; submitted answer where needed; remaining ICS actions; unlocked topics; current student state | Validate availability and action cost. Select content from unlocked ICS topics. Evaluate an answer/POTD or apply the chosen activity's configured effects. Deduct the action cost once and update affected state values. | Activity result and explanation; changes to mental state and/or ICS4U knowledge; configured HP/grade effects, if any; updated action counter. |
| **5\. Perform an other-class activity** | Answer a subject question, ask the teacher, study, or slack off; answer where needed; remaining other-class actions; available subject content; current student state | Validate the activity. Evaluate subject answers or apply the activity's effects. Consume the configured number of actions and update the relevant state. | Subject feedback; changes to other-course knowledge and/or mental state; configured HP/grade effects, if any; remaining actions. |
| **6\. Perform a Student Hub activity** | Choice to practise POTD, review ICS, study other subjects, rest/slack off, or use an available help interaction; answers where needed; remaining Hub actions; current state | Show current stats and the schedule. Validate the selected activity, evaluate practice where applicable, apply its effects, and consume the action cost. Offer teacher/help interactions only when their availability conditions are met. | Updated Hub display; activity feedback; changes to the corresponding knowledge or mental state; any explicitly configured grade recovery; remaining Hub actions. |
| **7\. Advance the daily routine** | Current period; remaining actions; End Period/End Day selection; event-completion flags | Advance when the allowance is exhausted or the player chooses to finish. Enforce ICS class → other classes → Student Hub. Discard unused actions for the finished period. Block advancement while a required story or assessment remains incomplete. After the Hub, advance the calendar by one day. | Next-period screen or next-day state; updated calendar; completed daily activity record. |
| **8\. Unlock story and knowledge content** | Current day; story-event dates; topic-release dates; prerequisite flags; completed-event IDs | Check date requirements and any configured prerequisites. Mark eligible topics available. Queue newly eligible required scenes at their configured points in the day. Play queued scenes and record their completion without replaying their effects. | Newly accessible knowledge topics; story scenes and unlock messages; updated content/completion flags. |
| **9\. Start a scheduled Lab/Test encounter** | Current date and ICS period; event schedule; required-story completion; mental state, knowledge, and HP/grade; unlocked assessment content | Confirm that the encounter is due and unfinished. Complete prerequisite scenes. Initialize the Lab/Test challenge and its turn state using the current student state. Pause the ordinary daily routine until the assessment ends. | Lab/Test battle screen; assessment objectives, challenge state, current HP/grade, and available actions. |

### **Class diagram**
mermaid file: [class diagram.md](/docs/class%20diagram.mmd)

### **Sequence Diagram**
mermaid file: [sequence diagram.mmd](/docs/sequence%20diagram.mmd)