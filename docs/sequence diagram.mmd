sequenceDiagram
    actor Player
    participant UI as GameUI
    participant Game as GameController
    participant State as GameState
    participant Story as StoryController
    participant Exam as AssessmentController
    participant Reward as MrReward
    participant Activity as ActivityController
    participant Hub as StudentHubController
    participant Save as SaveManager

    Game->>Game: beginDay()
    Game->>State: Set ICS period, initialize new-day allowances once
    Game->>Story: refreshUnlocks(state)
    Story->>State: Unlock eligible topics and story events
    Game->>Story: playDueStories(state)
    loop Each required scene before the assessment
        Story->>UI: showStory(event, sceneIndex)
        Player->>UI: Advance dialogue
        UI->>Story: advanceScene(state)
    end
    Story->>State: Record story completion and one-time effects
    Game->>State: calendar.assessmentDue()
    State-->>Game: Scheduled Lab/Test
    Game->>Exam: start(state, event)
    Exam->>State: Validate date, unresolved event and story prerequisites
    Exam-->>Game: Encounter initialized
    Game->>UI: showAssessment(session)

    loop Until assessment finishes
        Player->>UI: Choose action and submit answer/solution
        UI->>Exam: resolveTurn(state, action, answer)
        alt Invalid action
            Exam-->>UI: Rejection, retain current turn
        else Valid action
            Exam->>Exam: Evaluate answer using knowledge and mental-state rules
            Exam->>State: Update challenge-related student effects
            opt Assessment remains active
                Exam->>Reward: selectChallenge(session)
                Reward-->>Exam: Challenge pattern
                Exam->>Reward: calculateResponse(student, correct)
                Reward-->>Exam: StatChange
                Exam->>State: student.apply(change)
            end
            Exam->>Exam: Update turn state, check progress and grade
            Exam-->>UI: Turn feedback and updated state
        end
    end

    Game->>Exam: result()
    Exam-->>Game: AssessmentResult
    Game->>Game: finishAssessment(result)
    Game->>State: Reject duplicate ID, record result, mark event resolved
    Game->>State: Apply completion effects once
    Game->>Story: refreshUnlocks(state)
    Game->>UI: showResult(result)

    alt Continuation allowed
        Player->>UI: Continue after result and any required scene
        UI->>Game: endPeriod()
        Game->>State: Advance to OTHER_CLASSES
        Game->>Story: playDueStories(state)
        Note over Story,UI: Complete any required scenes before activities
        Game->>UI: showPeriod(state)
        loop While actions remain and player stays in period
            Player->>UI: Select other-class activity
            UI->>Activity: perform(state, activityId, answer)
            Activity->>State: Check period, topic availability and action budget
            alt Activity accepted
                Activity->>Activity: Evaluate activity
                Activity->>State: Spend cost once, apply StatChange
                Activity-->>UI: Learning feedback and remaining actions
            else Activity rejected
                Activity-->>UI: Reason, no state or budget change
            end
        end
        Player->>UI: End period, or acknowledge exhausted allowance
        UI->>Game: endPeriod()
        Game->>State: Close period, advance to STUDENT_HUB
        Game->>Story: playDueStories(state)
        Note over Story,UI: Complete any required Hub story first
        Game->>Hub: openHub(state)
        Hub->>UI: showPeriod(state)
        loop While Hub actions remain and player stays
            Player->>UI: Choose practice, study, help or rest
            UI->>Hub: chooseActivity(state, id, answer)
            Hub->>Activity: perform(state, id, answer)
            Activity->>State: Validate, spend and apply only if accepted
            Activity-->>Hub: ActivityResult
            Hub-->>UI: Feedback, four stats and remaining actions
        end
        Player->>UI: End day
        UI->>Hub: endDay()
        Hub->>Game: endPeriod()
        Game->>Story: hasPendingRequiredStory(state)
        Story-->>Game: false
        Game->>State: Close Hub budget, advance calendar to next day
        Game->>Game: beginDay()
        Game->>Save: save(state)
        Save-->>Game: Success or failure
        Game->>UI: Next-day screen, save status or retry message
    else Continuation blocked
        Game->>UI: Show configured failure/recovery screen
        Note over Game,UI: Do not advance day or reset the grade automatically
    end
