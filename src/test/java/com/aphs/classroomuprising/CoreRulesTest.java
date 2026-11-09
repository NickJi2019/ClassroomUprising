package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CoreRulesTest {
    @ParameterizedTest @CsvSource({
        "1,LESSON,0","2,LESSON,0","3,LESSON,1","4,LESSON,1","5,LAB,1","6,LESSON,1","7,UNIT_TEST,1",
        "8,LESSON,2","9,LESSON,2","10,LAB,2","11,LESSON,2","12,UNIT_TEST,2",
        "13,LESSON,3","14,LESSON,3","15,LAB,3","16,LESSON,3","17,UNIT_TEST,3",
        "18,REVIEW,0","19,REVIEW,0","20,MIDTERM,0",
        "21,LESSON,4","22,LESSON,4","23,LAB,4","24,LESSON,4","25,UNIT_TEST,4",
        "26,LESSON,5","27,LESSON,5","28,LAB,5","29,LESSON,5","30,UNIT_TEST,5",
        "31,LESSON,6","32,LESSON,6","33,LAB,6","34,LESSON,6","35,UNIT_TEST,6",
        "36,REVIEW,0","37,REVIEW,0","38,REVIEW,0","39,REVIEW,0","40,FINAL,0"})
    void exactFortyDayCalendar(int day,CourseType type,int unit) {
        assertEquals(new DayPlan(day,type,unit,day<=2),SchoolCalendar.day(day));
    }
    @ParameterizedTest @ValueSource(ints={-1,0,41,Integer.MAX_VALUE})
    void calendarBounds(int day) { assertThrows(IllegalArgumentException.class,()->SchoolCalendar.day(day)); }
    @Test void calendarImmutable() { assertThrows(UnsupportedOperationException.class,()->SchoolCalendar.days().clear()); }
    @Test void openingMustBeAcknowledgedOnce() {
        var g=new GameController(new GameState("A",Fixtures.student()),Fixtures.rules(),Fixtures.catalog(),new Random(1));
        assertThrows(IllegalStateException.class,g::showLesson);g.completeOpening();
        assertThrows(IllegalStateException.class,g::completeOpening);assertNotNull(g.showLesson());
    }
    @Test void lessonContentThenExactlyOneChoice() {
        var g=Fixtures.game(3,DayPeriod.ICS_CLASS);
        assertThrows(IllegalStateException.class,()->g.lessonChoice("learn"));
        var lesson=g.showLesson();assertEquals(ContentCatalog.Interaction.ASK_TEACHER,lesson.interaction());
        String before=Fixtures.signature(g.snapshot());
        assertThrows(IllegalArgumentException.class,()->g.lessonChoice("both"));
        assertEquals(before,Fixtures.signature(g.snapshot()));
        g.lessonChoice("learn");assertEquals(8,g.snapshot().student().codingVibe());
        assertEquals(58,g.snapshot().student().san());assertEquals(50,g.snapshot().student().hp());
        assertEquals(DayPeriod.OTHER_CLASSES,g.snapshot().period());
        assertThrows(IllegalStateException.class,()->g.lessonChoice("learn"));
    }
    @Test void allNegativeTutorialDeltasAreWaived() {
        var negative=new StatChange(-10,-5,-3,Collections.nCopies(6,-2.0));
        var catalog=new ContentCatalog(Map.of(1,new ContentCatalog.Lesson("Fixture",ContentCatalog.Interaction.RAISE_HAND,
                Map.of("bad",negative))),Map.of(),Map.of(),Map.of(),Map.of(),null);
        var g=new GameController(Fixtures.state(1,DayPeriod.ICS_CLASS),RuleValues.pending(),catalog,new Random(1));
        g.showLesson();var result=g.lessonChoice("bad");
        assertEquals(Fixtures.student(),g.snapshot().student());assertFalse(result.warnings().isEmpty());
    }
    @Test void otherClassesHasNoOtherKnowledgeStat() {
        var a=Fixtures.game(3,DayPeriod.OTHER_CLASSES);a.otherClasses(false);
        assertEquals(Fixtures.student(),a.snapshot().student());
        var b=Fixtures.game(3,DayPeriod.OTHER_CLASSES);b.otherClasses(true);
        assertEquals(8,b.snapshot().student().codingVibe());assertEquals(58,b.snapshot().student().san());
        assertEquals(6,b.snapshot().student().unitKnowledge().size());
    }
    @Test void earlySleepAndMorningHalfActivity() {
        var g=Fixtures.game(3,DayPeriod.AFTER_SCHOOL);g.sleepImmediatelyAfterSchool();
        assertEquals(DayPeriod.DAY_END,g.snapshot().period());g.beginNextDay();
        assertEquals(DayPeriod.MORNING_BONUS,g.snapshot().period());assertEquals(63,g.snapshot().student().san());
        g.hubActivity("study");
        assertEquals(4,g.snapshot().student().codingVibe());assertEquals(2,g.snapshot().student().unitKnowledge().getFirst());
        assertEquals(62,g.snapshot().student().san());assertEquals(DayPeriod.ICS_CLASS,g.snapshot().period());
        assertThrows(IllegalStateException.class,()->g.hubActivity("study"));
    }
    @Test void lateNightEffectLastsOnlyFollowingDay() {
        var g=Fixtures.game(3,DayPeriod.LATE_NIGHT);g.hubActivity("study");
        assertEquals(1,g.snapshot().xpMultiplier());assertEquals(58,g.snapshot().effectiveSan());
        g.beginNextDay();assertEquals(.5,g.snapshot().xpMultiplier());assertEquals(54,g.snapshot().effectiveSan());
        g.showLesson();g.lessonChoice("learn");assertEquals(12,g.snapshot().student().codingVibe());
        var end=g.snapshot();end.period=DayPeriod.DAY_END; // isolated fixture with no second late night
        var next=Fixtures.game(end);next.beginNextDay();
        assertEquals(1,next.snapshot().xpMultiplier());assertEquals(next.snapshot().student().san(),next.snapshot().effectiveSan());
    }
    @Test void tutorialLateNightNeverQueuesPenalty() {
        var g=new GameController(Fixtures.state(2,DayPeriod.LATE_NIGHT),RuleValues.pending(),Fixtures.catalog(),new Random(1));
        assertFalse(g.hubActivity("study").warnings().isEmpty());assertTrue(g.snapshot().effects().isEmpty());
        g.beginNextDay();assertEquals(1,g.snapshot().xpMultiplier());assertEquals(60,g.snapshot().effectiveSan());
    }
    @Test void missingLateNightRuleRejectsWithoutMutation() {
        var g=new GameController(Fixtures.state(3,DayPeriod.LATE_NIGHT),RuleValues.pending(),Fixtures.catalog(),new Random(1));
        String before=Fixtures.signature(g.snapshot());
        var e=assertThrows(PendingRuleException.class,()->g.hubActivity("study"));assertEquals("T03",e.ruleId());
        assertEquals(before,Fixtures.signature(g.snapshot()));
    }
    @Test void unknownHubChoiceDoesNotSpendSlot() {
        var g=Fixtures.game(3,DayPeriod.AFTER_SCHOOL);String before=Fixtures.signature(g.snapshot());
        assertThrows(PendingRuleException.class,()->g.hubActivity("invented"));
        assertEquals(before,Fixtures.signature(g.snapshot()));
    }
    @Test void noDayFortyOne() {
        var g=Fixtures.game(40,DayPeriod.DAY_END);
        assertThrows(IllegalStateException.class,g::beginNextDay);assertEquals(40,g.snapshot().day());
    }
    @ParameterizedTest @ValueSource(ints={20,40})
    void majorExamsCannotBeSkippedEvenWithLeaveFlag(int day) {
        var g=Fixtures.game(day,DayPeriod.ICS_CLASS);String before=Fixtures.signature(g.snapshot());
        assertThrows(IllegalStateException.class,()->g.skipClass(false));
        assertThrows(IllegalStateException.class,()->g.skipClass(true));
        assertEquals(before,Fixtures.signature(g.snapshot()));
    }
    @Test void thirdAbsenceCostsOneOverallPoint() {
        var s=Fixtures.state(8,DayPeriod.ICS_CLASS);s.grades.unexcusedAbsences=2;
        var g=Fixtures.game(s);g.skipClass(false);
        assertEquals(3,g.snapshot().grades().unexcusedAbsences());assertEquals(1,g.snapshot().grades().absenceDeductionPoints());
    }
    @Test void firstTwoAbsencesAreFreeAndLeaveExempt() {
        for(int previous=0;previous<2;previous++) {
            var s=Fixtures.state(3,DayPeriod.ICS_CLASS);s.grades.unexcusedAbsences=previous;
            var g=Fixtures.game(s);g.skipClass(false);assertEquals(0,g.snapshot().grades().absenceDeductionPoints());
        }
        var g=Fixtures.game(3,DayPeriod.ICS_CLASS);g.skipClass(true);assertEquals(0,g.snapshot().grades().unexcusedAbsences());
    }
    @Test void undecidedTutorialAbsenceAccountingIsExplicit() {
        var g=new GameController(Fixtures.state(1,DayPeriod.ICS_CLASS),RuleValues.pending(),Fixtures.catalog(),new Random(1));
        assertEquals("T11",assertThrows(PendingRuleException.class,()->g.skipClass(false)).ruleId());
        assertEquals(DayPeriod.ICS_CLASS,g.snapshot().period());
    }
    @Test void makeupUsesOriginalQuestionsAndReplacesLesson() {
        var original=Fixtures.game(5,DayPeriod.ICS_CLASS).startAssessment();
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);g.skipClass(false);
        assertEquals(List.of(6,8),SchoolCalendar.makeupCandidates(5));g.scheduleMakeup(5,6);
        var later=g.snapshot();later.day=6;later.period=DayPeriod.ICS_CLASS;
        var makeup=Fixtures.game(later);assertThrows(IllegalStateException.class,makeup::showLesson);
        var view=makeup.startAssessment();
        assertEquals(original.questions(),view.questions());assertTrue(view.makeup());assertFalse(view.peekingAllowed());
        assertTrue(view.increasedDetectionRisk());assertEquals(53,makeup.snapshot().student().san());
        makeup.recordEvaluatedSubmission(80,"fixture evaluator");
        assertEquals(9,makeup.snapshot().grades().record(view.id()).returnDay());
        assertEquals(0,makeup.snapshot().student().codingVibe());assertTrue(makeup.snapshot().missedAssessments().isEmpty());
    }
    @Test void lastUnitTestHasNoInventedMakeupDate() {
        var g=Fixtures.game(35,DayPeriod.ICS_CLASS);g.skipClass(false);
        String before=Fixtures.signature(g.snapshot());
        assertEquals("T05",assertThrows(PendingRuleException.class,()->g.scheduleMakeup(35,36)).ruleId());
        assertEquals(before,Fixtures.signature(g.snapshot()));
    }
    @Test void invalidOrRepeatedMakeupAssignmentsDoNotMutate() {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);g.skipClass(false);
        assertThrows(IllegalArgumentException.class,()->g.scheduleMakeup(5,9));
        g.scheduleMakeup(5,8);String before=Fixtures.signature(g.snapshot());
        assertThrows(IllegalStateException.class,()->g.scheduleMakeup(5,6));
        assertEquals(before,Fixtures.signature(g.snapshot()));
    }
    @ParameterizedTest @CsvSource({"5,8,false","7,10,false","20,20,true","40,40,true"})
    void ordinaryVersusImmediateResults(int day,int due,boolean official) {
        var g=Fixtures.game(day,DayPeriod.ICS_CLASS);var active=g.startAssessment();
        g.recordEvaluatedSubmission(84,"fixture evaluator");var r=g.snapshot().grades().record(active.id());
        assertEquals(due,r.returnDay());assertEquals(official,r.officialScore().isPresent());
        assertEquals(50,g.snapshot().student().hp());
    }
    @Test void duplicateSubmissionDoesNotProduceAnotherMark() {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);g.startAssessment();g.recordEvaluatedSubmission(80,"fixture");
        assertThrows(IllegalStateException.class,()->g.recordEvaluatedSubmission(90,"duplicate"));
        assertEquals(1,g.snapshot().grades().records().size());
    }
    @Test void invalidMarkDoesNotCloseAssessment() {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);g.startAssessment();String before=Fixtures.signature(g.snapshot());
        assertThrows(IllegalArgumentException.class,()->g.recordEvaluatedSubmission(Double.NaN,"fixture"));
        assertEquals(before,Fixtures.signature(g.snapshot()));g.recordEvaluatedSubmission(50,"fixture");
    }
    @Test void estimatesExpireWithoutUndoingOtherSanChanges() {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);var view=g.startAssessment();g.recordSelfEstimate(99,3,5);
        assertEquals(57,g.snapshot().student().san());assertEquals(62,g.snapshot().effectiveSan());
        g.recordEvaluatedSubmission(70,"fixture");
        var s=g.snapshot();s.day=7;s.period=DayPeriod.DAY_END;
        s.student=new StudentState(50,80,0,Collections.nCopies(6,0.0)); // unrelated persisted SAN changes
        s.effects.add(new TimedEffect("unrelated",8,9,2,1));
        var next=Fixtures.game(s);next.beginNextDay();
        assertEquals(70,next.snapshot().grades().record(view.id()).officialScore().orElseThrow());
        assertEquals(80,next.snapshot().student().san());assertEquals(82,next.snapshot().effectiveSan());
        assertTrue(next.snapshot().estimates().isEmpty());assertEquals("unrelated",next.snapshot().effects().getFirst().id());
    }
    @Test void estimationRetryAndMajorExamEstimateRemainPending() {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);g.startAssessment();g.recordSelfEstimate(80,1,2);
        String before=Fixtures.signature(g.snapshot());
        assertEquals("T08",assertThrows(PendingRuleException.class,()->g.recordSelfEstimate(90,1,2)).ruleId());
        assertEquals(before,Fixtures.signature(g.snapshot()));
        var major=Fixtures.game(20,DayPeriod.ICS_CLASS);major.startAssessment();
        assertThrows(PendingRuleException.class,()->major.recordSelfEstimate(80,1,2));
    }
    @Test void seriousCheatingZeroSurvivesSubmissionAndKeepsHpXp() {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);var v=g.startAssessment();g.caughtCheating(true);
        assertEquals(48,g.snapshot().student().san());assertEquals(50,g.snapshot().student().hp());assertEquals(0,g.snapshot().student().codingVibe());
        g.recordEvaluatedSubmission(100,"correct but serious cheating");
        assertEquals(0,g.snapshot().grades.record(v.id()).score);
    }
    @Test void ordinaryCaughtCheatingDoesNotInventZeroMark() {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);var v=g.startAssessment();g.caughtCheating(false);g.recordEvaluatedSubmission(80,"fixture");
        assertEquals(80,g.snapshot().grades.record(v.id()).score);
    }
    @Test void tutorialCheatingGuardWaivesEveryPenalty() {
        var s=Fixtures.state(1,DayPeriod.ICS_CLASS);
        s.active=new GameState.ActiveAssessment(SchoolCalendar.day(5),List.of(new ContentCatalog.Question("fixture","fixture")),false);
        var g=new GameController(s,RuleValues.pending(),Fixtures.catalog(),new Random(1));
        assertFalse(g.caughtCheating(true).warnings().isEmpty());assertEquals(Fixtures.student(),g.snapshot().student());
        assertTrue(g.snapshot().seriousCheating.isEmpty());
    }
    @Test void officialFeedbackIsExplicitAndExactlyOnce() {
        var g=Fixtures.game(20,DayPeriod.ICS_CLASS);var v=g.startAssessment();g.recordEvaluatedSubmission(80,"fixture");
        g.applyOfficialFeedback(v.id(),new StatChange(0,-4,0,Collections.nCopies(6,0.0)));
        assertEquals(56,g.snapshot().student().san());
        assertThrows(IllegalStateException.class,()->g.applyOfficialFeedback(v.id(),StatChange.none()));
    }
    @Test void midtermDecisionsDoNotInventThresholdsOrResetRules() {
        var g=Fixtures.game(20,DayPeriod.ICS_CLASS);g.startAssessment();g.recordEvaluatedSubmission(90,"fixture");
        assertEquals(GameState.Status.MIDTERM_DECISION,g.snapshot().status());
        String before=Fixtures.signature(g.snapshot());
        assertThrows(PendingRuleException.class,()->g.resolveMidterm("continue"));
        assertThrows(PendingRuleException.class,()->g.resolveMidterm("retake"));
        assertEquals(before,Fixtures.signature(g.snapshot()));g.resolveMidterm("drop");
        assertEquals(GameState.Status.DROPPED,g.snapshot().status());
    }
    @Test void retakeCounterCannotResetItselfAndCarryoverIsSelectedOnly() {
        var run=new RunProgress(0).consumeRetake().consumeRetake();
        assertEquals(0,run.remainingRetakes());assertThrows(IllegalStateException.class,run::consumeRetake);
        assertEquals(Set.of("shirt"),RunProgress.rewardsForNewPlaythrough(Set.of("shirt","duck"),Set.of("shirt")));
        assertThrows(IllegalArgumentException.class,()->RunProgress.rewardsForNewPlaythrough(Set.of("duck"),Set.of("shirt")));
    }
    @Test void fullGradeFormulaNeedsExplicitMidtermWeightAndAllOfficialMarks() {
        var b=new GradeBook();assertThrows(PendingRuleException.class,()->b.finalPercentage(2,100));
        for(var d:SchoolCalendar.days()) if(d.course().isAssessment()) {
            double score=d.course()==CourseType.MIDTERM?100:d.course()==CourseType.FINAL?90:80;
            b.submit(new AssessmentRecord(d.assessmentId(),d.course(),d.day(),d.day()+(d.course().isMajor()?0:3),
                    List.of("fixture"),false,score,true,"fixture"));
        }
        double expected=.65*((12*80+2*100)/14.0)+10+.25*90;
        assertEquals(expected,b.finalPercentage(2,100),1e-9);
        b.unexcusedAbsences=3;assertEquals(expected-1,b.finalPercentage(2,100),1e-9);
    }
    @Test void questionBanksAreFixedForOrdinaryAndSampledForMajors() {
        var catalog=Fixtures.catalog();
        assertEquals(catalog.questions(SchoolCalendar.day(5),new Random(1)),catalog.questions(SchoolCalendar.day(5),new Random(999)));
        var q=catalog.questions(SchoolCalendar.day(20),new Random(1));assertEquals(2,q.size());
        assertEquals(2,q.stream().distinct().count());
        assertEquals(q,catalog.questions(SchoolCalendar.day(20),new Random(1)));
    }
    @Test void missingContentAndNumericOverflowDoNotCommit() {
        var g=new GameController(Fixtures.state(3,DayPeriod.ICS_CLASS),RuleValues.pending(),ContentCatalog.pending(),new Random(1));
        assertEquals("T14",assertThrows(PendingRuleException.class,g::showLesson).ruleId());assertFalse(g.snapshot().lessonShown);
        var s=Fixtures.state(3,DayPeriod.AFTER_SCHOOL);
        s.student=new StudentState(0,Double.MAX_VALUE,0,Collections.nCopies(6,0.0));
        var c=new ContentCatalog(Map.of(),Map.of("overflow",new StatChange(0,Double.MAX_VALUE,0,Collections.nCopies(6,0.0))),
                Map.of(),Map.of(),Map.of(),null);
        var overflow=new GameController(s,Fixtures.rules(),c,new Random(1));String before=Fixtures.signature(overflow.snapshot());
        assertThrows(IllegalArgumentException.class,()->overflow.hubActivity("overflow"));assertEquals(before,Fixtures.signature(overflow.snapshot()));
    }
    @Test void snapshotsAreDefensive() {
        var g=Fixtures.game(3,DayPeriod.ICS_CLASS);var copy=g.snapshot();copy.day=30;
        assertEquals(3,g.snapshot().day());
        assertThrows(UnsupportedOperationException.class,()->g.snapshot().student().unitKnowledge().clear());
        var b=g.snapshot().grades();b.unexcusedAbsences=10;assertEquals(0,g.snapshot().grades().unexcusedAbsences());
    }
}
