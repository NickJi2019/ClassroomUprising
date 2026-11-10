package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomChoiceTest {
    private GameController game(int day) throws Exception {
        var demo = DemoProfile.load();
        return new GameController(Fixtures.state(day, DayPeriod.ICS_CLASS), demo.rules(), demo.content(), new Random(42));
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4})
    void readingParticipationAndResponseMenusGrantsNothing(int day) throws Exception {
        var game = game(day); var lesson = game.showLesson(); String before = Fixtures.signature(game.snapshot());
        for (int n=0;n<3;n++) {
            assertEquals(3,lesson.decision().responses().size());
            assertEquals(lesson, game.showLesson());
            assertThrows(IllegalArgumentException.class, () -> game.lessonChoice(day,"ask"));
            assertThrows(IllegalArgumentException.class, () -> game.lessonChoice(day,"raise"));
        }
        assertEquals(before,Fixtures.signature(game.snapshot()));
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4})
    void onlyTheSelectedAppropriateOptionGrantsTheConfiguredBonusOnce(int day) throws Exception {
        var game=game(day); var lesson=game.showLesson(); var before=game.snapshot().student();
        var correct=lesson.decision().responses().stream().filter(o->o.outcome()==LessonDecision.Outcome.APPROPRIATE).findFirst().orElseThrow();
        var result=game.lessonChoice(day,correct.id());
        assertEquals(before.apply(correct.effect(),1,1,day<=2),game.snapshot().student());
        assertEquals(correct.feedback(),result.message()); assertEquals(DayPeriod.OTHER_CLASSES,game.snapshot().period());
        var settled=Fixtures.signature(game.snapshot());
        assertThrows(IllegalStateException.class,()->game.lessonChoice(day,correct.id()));
        assertThrows(IllegalStateException.class,()->game.lessonChoice(day,"quiet"));
        assertEquals(settled,Fixtures.signature(game.snapshot()));
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4})
    void allInappropriateChoicesGiveNoXpAndTutorialOnlyWarns(int day) throws Exception {
        var lesson=DemoProfile.load().content().lesson(day);
        for(var option:lesson.decision().responses()) {
            if(option.outcome()!=LessonDecision.Outcome.INAPPROPRIATE) continue;
            var game=game(day); game.showLesson(); var before=game.snapshot().student();
            var result=game.lessonChoice(day,option.id()); var after=game.snapshot().student();
            assertEquals(before.codingVibe(),after.codingVibe()); assertEquals(before.unitKnowledge(),after.unitKnowledge());
            assertEquals(before.hp(),after.hp());
            assertEquals(before.san()+(day<=2?0:option.effect().san()),after.san());
            assertEquals(day<=2,!result.warnings().isEmpty()); assertTrue(game.snapshot().effects().isEmpty());
            assertEquals(option.feedback(),result.message()); assertEquals(DayPeriod.OTHER_CLASSES,game.snapshot().period());
        }
    }
    @ParameterizedTest @ValueSource(ints={1,2,3,4})
    void stayingQuietIsOneNeutralSettlement(int day) throws Exception {
        var game=game(day);var skip=game.showLesson().decision().skip();var before=game.snapshot().student();
        assertTrue(game.lessonChoice(day,skip.id()).warnings().isEmpty());
        assertEquals(before,game.snapshot().student());assertEquals(DayPeriod.OTHER_CLASSES,game.snapshot().period());
    }
    @Test void invalidOldAndStaleInputLeaveEverythingUnchanged() throws Exception {
        var game=game(2);String notShown=Fixtures.signature(game.snapshot());
        assertThrows(IllegalStateException.class,()->game.lessonChoice(2,"quiet"));
        assertEquals(notShown,Fixtures.signature(game.snapshot()));game.showLesson();String shown=Fixtures.signature(game.snapshot());
        assertThrows(IllegalStateException.class,()->game.lessonChoice(1,"quiet"));
        for(String id:Arrays.asList("d1-equality","unknown","",null))
            assertThrows(IllegalArgumentException.class,()->game.lessonChoice(2,id));
        assertEquals(shown,Fixtures.signature(game.snapshot()));
    }
    @Test void anAssessmentMakeupCannotReceiveAReplacedLessonsReward() throws Exception {
        var demo=DemoProfile.load();var s=Fixtures.state(6,DayPeriod.ICS_CLASS);s.lessonShown=true;s.makeups.put(6,5);
        var game=new GameController(s,demo.rules(),demo.content(),new Random(42));String before=Fixtures.signature(game.snapshot());
        assertThrows(IllegalStateException.class,()->game.lessonChoice(6,"quiet"));
        assertEquals(before,Fixtures.signature(game.snapshot()));
    }
    @Test void overflowRejectsTheWholeSettlementRatherThanConsumingClass() {
        var huge=StatChange.learning(1,Double.MAX_VALUE,0,-2);
        var d=new LessonDecision("?",List.of(new LessonDecision.Option("correct","Correct",LessonDecision.Outcome.APPROPRIATE,huge,"Yes"),
                new LessonDecision.Option("wrong","Wrong",LessonDecision.Outcome.INAPPROPRIATE,StatChange.learning(1,0,0,-2),"No"),
                new LessonDecision.Option("quiet","Quiet",LessonDecision.Outcome.SKIP,StatChange.none(),"Quiet")));
        var content=new ContentCatalog(Map.of(3,new ContentCatalog.Lesson("Title","Text",ContentCatalog.Interaction.RAISE_HAND,d)),Map.of(),Map.of(),Map.of(),Map.of(),null);
        var s=Fixtures.state(3,DayPeriod.ICS_CLASS);s.student=new StudentState(50,60,Double.MAX_VALUE,Collections.nCopies(6,0.0));
        var game=new GameController(s,Fixtures.rules(),content,new Random(42));game.showLesson();String before=Fixtures.signature(game.snapshot());
        assertThrows(IllegalArgumentException.class,()->game.lessonChoice(3,"correct"));assertEquals(before,Fixtures.signature(game.snapshot()));
    }
}
