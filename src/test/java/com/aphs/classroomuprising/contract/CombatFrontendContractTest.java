package com.aphs.classroomuprising.contract;

import com.aphs.classroomuprising.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** External-package consumer of the handoff API. All grading/combat numbers here are test fixtures. */
class CombatFrontendContractTest {
    @TempDir Path temp;
    @Test void frontendCanCompleteCombatUsingOnlyPublicOperations() throws Exception {
        var demo=DemoProfile.load();
        var lessons=new LinkedHashMap<Integer,ContentCatalog.Lesson>();
        for(int day=1;day<=4;day++) lessons.put(day,demo.content().lesson(day));
        var hub=new LinkedHashMap<String,StatChange>();
        for(String id:demo.content().hubActivityIds()) hub.put(id,demo.content().hub(id));
        var content=new ContentCatalog(lessons,hub,Map.of(),Map.of(SchoolCalendar.day(5).assessmentId(),
                List.of(new ContentCatalog.Question("easy","Fixture easy",1),new ContentCatalog.Question("hard","Fixture hard",2))),
                Map.of(),demo.content().otherRevision());
        var combatRules=new CombatRules(.1,CombatRules.TimeoutDasher.KEEP_DOUBLE,
                CombatRules.AnswerTiming.PLAYER_SUBMISSION,true,Duration.ofSeconds(15),
                elapsed -> 2+1.0/(elapsed.toSeconds()+1),summary -> {
                    assertEquals(3.1,summary.weightedCredits(),1e-9); assertEquals(5,summary.comboBonus()); return 80;
                });
        var clock=new AtomicLong();
        var game=new GameController(new GameState("Contract student",demo.initialStudent()),demo.rules(),content,
                new Random(42),combatRules,clock::get);
        game.completeOpening();
        for(int day=1;day<=4;day++) {
            if(game.snapshot().period()==DayPeriod.MORNING_BONUS) game.hubActivity("rest");
            game.showLesson();game.lessonChoice("quiet");game.otherClasses(false);
            game.sleepImmediatelyAfterSchool();game.beginNextDay();
        }
        game.hubActivity("rest");
        assertEquals(CourseType.LAB,game.startCombatAssessment().type());
        game.startCombatQuestion("hard");var reward=game.beginCombatRewardDelay(CombatSession.RewardKind.SAFE);
        clock.addAndGet(Duration.ofSeconds(5).toNanos());game.finishCombatRewardDelay(reward);
        var first=game.submitCombatAnswer();game.recordCombatEvaluation(first,1,"trusted fixture result");
        assertTrue(game.combatSnapshot().dasherReady());
        game.startCombatQuestion("easy");clock.addAndGet(Duration.ofSeconds(10).toNanos());
        var last=game.submitCombatAnswer();game.recordCombatEvaluation(last,1,"trusted fixture result");
        assertTrue(game.combatSnapshot().complete());assertEquals(CombatSession.ComboStatus.EARNED,game.combatSummary().combo());
        game.finishCombatAssessment();assertFalse(game.snapshot().hasActiveCombat());
        assertEquals(DayPeriod.OTHER_CLASSES,game.snapshot().period());
        game.otherClasses(false);game.sleepImmediatelyAfterSchool();
        var saves=new SaveManager();Path file=temp.resolve("combat-day-end.properties");saves.saveAtDayEnd(game.snapshot(),file);
        var loaded=saves.load(file);assertFalse(loaded.hasActiveCombat());
        assertEquals(8,loaded.grades().records().getFirst().returnDay());
        assertTrue(loaded.grades().records().getFirst().officialScore().isEmpty());
    }
}
