classDiagram
    class MainMenuController {
        +newGame(String name) void
        +continueGame() boolean
        +showInstructions() void
        +exitGame() void
    }
    class GameController {
        -GameState state
        +newGame(String name) void
        +loadGame() boolean
        +beginDay() void
        +routeCurrentPeriod() void
        +endPeriod() boolean
        +finishAssessment(AssessmentResult result) void
        +saveGame() boolean
    }
    class GameUI {
        +showMainMenu() void
        +showInstructions() void
        +showPeriod(GameState state) void
        +showStory(StoryEvent event, int sceneIndex) void
        +showAssessment(AssessmentSession session) void
        +showFeedback(String message) void
        +showResult(AssessmentResult result) void
    }
    class GameState {
        -StudentState student
        -SchoolCalendar calendar
        -ActionBudget actionBudget
        -ContentProgress contentProgress
        -ArrayList~AssessmentResult~ assessmentHistory
        +recordAssessment(AssessmentResult result) boolean
    }
    class StudentState {
        -String name
        -int mentalState
        -int icsKnowledge
        -int otherKnowledge
        -int gradeHp
        +apply(StatChange change) void
        +isGradeDepleted() boolean
    }
    class SchoolCalendar {
        -int currentDay
        -DayPeriod currentPeriod
        -ArrayList~ScheduledAssessment~ schedule
        +advancePeriod() void
        +advanceDay() void
        +assessmentDue() ScheduledAssessment
    }
    class DayPeriod {
        <<enumeration>>
        ICS_CLASS
        OTHER_CLASSES
        STUDENT_HUB
    }
    class ActionBudget {
        -int initializedDay
        -int[] remainingByPeriod
        +initializeDay(int day) void
        +canSpend(DayPeriod period, int cost) boolean
        +spend(DayPeriod period, int cost) void
        +closePeriod(DayPeriod period) void
        +remaining(DayPeriod period) int
    }
    class ContentProgress {
        -boolean introductionCompleted
        -ArrayList~String~ unlockedTopicIds
        -ArrayList~String~ availableStoryIds
        -ArrayList~String~ completedStoryIds
        -ArrayList~String~ resolvedAssessmentIds
        +isTopicUnlocked(String id) boolean
        +isStoryCompleted(String id) boolean
        +isAssessmentResolved(String id) boolean
    }
    class StoryController {
        -StoryEvent activeEvent
        -int sceneIndex
        +refreshUnlocks(GameState state) void
        +playIntroduction(GameState state) void
        +playDueStories(GameState state) void
        +advanceScene(GameState state) void
        +hasPendingRequiredStory(GameState state) boolean
    }
    class StoryEvent {
        -String id
        -int unlockDay
        -DayPeriod triggerPeriod
        -boolean required
        -ArrayList~String~ prerequisiteIds
        -ArrayList~String~ scenes
        -StatChange completionEffect
        +isEligible(GameState state) boolean
    }
    class KnowledgeTopic {
        -String id
        -String subject
        -int unlockDay
        -ArrayList~String~ prerequisiteIds
        +isEligible(GameState state) boolean
    }
    class ContentCatalog {
        -ArrayList~StoryEvent~ stories
        -ArrayList~KnowledgeTopic~ topics
        -ArrayList~Question~ questions
        -ArrayList~Activity~ activities
        +findActivity(String id) Activity
        +selectQuestion(String topicId) Question
    }
    class SaveManager {
        -String savePath
        +save(GameState state) boolean
        +load() GameState
        -validate(GameState state) boolean
    }

    MainMenuController --> GameController : starts or restores session
    MainMenuController --> GameUI : menu and instructions
    GameController --> GameUI : presents state
    GameController "1" --> "0..1" GameState : active session
    GameController --> StoryController : story gates and unlocks
    GameController --> ActivityController : daily activities
    GameController --> StudentHubController : Hub navigation
    GameController --> AssessmentController : scheduled encounter
    GameController --> SaveManager : persistence
    GameState "1" *-- "1" StudentState
    GameState "1" *-- "1" SchoolCalendar
    GameState "1" *-- "1" ActionBudget
    GameState "1" *-- "1" ContentProgress
    GameState "1" *-- "0..*" AssessmentResult : history
    SchoolCalendar --> DayPeriod
    SchoolCalendar "1" --> "0..*" ScheduledAssessment : preset schedule
    ActionBudget --> DayPeriod : indexed allowance
    StoryController --> ContentCatalog : story and topic definitions
    StoryController --> GameState : updates content flags
    StoryController --> GameUI : scenes
    ContentCatalog "1" *-- "1..*" StoryEvent
    ContentCatalog "1" *-- "1..*" KnowledgeTopic
    ContentCatalog "1" *-- "1..*" Question
    ContentCatalog "1" *-- "1..*" Activity
    StoryEvent --> DayPeriod : trigger point
    StoryEvent --> StatChange : one-time effect
    SaveManager ..> GameState : saves and restores
%% graph:positions {"MainMenuController":{"x":330,"y":-50},"GameController":{"x":782,"y":-84},"GameUI":{"x":252,"y":238},"GameState":{"x":762,"y":432},"StudentState":{"x":120,"y":693},"SchoolCalendar":{"x":796,"y":710},"DayPeriod":{"x":754,"y":1039},"ActionBudget":{"x":412,"y":701},"ContentProgress":{"x":1164,"y":693},"StoryController":{"x":1704,"y":238},"StoryEvent":{"x":1503,"y":854},"KnowledgeTopic":{"x":1872,"y":868},"ContentCatalog":{"x":1734,"y":636},"SaveManager":{"x":1548,"y":-50},"ActivityController":{"x":696,"y":238},"StudentHubController":{"x":1093,"y":178},"AssessmentController":{"x":984,"y":270},"AssessmentResult":{"x":1238,"y":636},"ScheduledAssessment":{"x":984,"y":919},"Question":{"x":2184,"y":710},"Activity":{"x":2184,"y":661},"StatChange":{"x":1548,"y":1110}}
%% graph:waypoints {"GameState>StudentState":[[369,520]],"SaveManager>GameState":[[1445,37]]}
%% graph:edges {"SaveManager>GameState":{"label":[-1,-192]}}