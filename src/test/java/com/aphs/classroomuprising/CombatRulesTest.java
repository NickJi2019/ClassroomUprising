package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** Synthetic rules exercise boundaries. None of these fixture formulas are production balancing. */
class CombatRulesTest {
    @TempDir Path temp;
    private final AtomicLong clock = new AtomicLong();
    private static CombatRules policy() {
        return new CombatRules(.1, CombatRules.TimeoutDasher.KEEP_DOUBLE,
                CombatRules.AnswerTiming.PLAYER_SUBMISSION, true, Duration.ofSeconds(15),
                elapsed -> 2 + Math.max(0, 30 - elapsed.toNanos()/1e9)/30, summary -> 80);
    }
    private static CombatRules with(CombatRules p, Double step, CombatRules.TimeoutDasher dasher,
                                   CombatRules.AnswerTiming timing, Boolean rewardCounts, Duration risky,
                                   CombatRules.HpCost hp, CombatRules.ScoreConverter scoring) {
        return new CombatRules(step, dasher, timing, rewardCounts, risky, hp, scoring);
    }
    private ContentCatalog catalog(int count, boolean ranks) {
        var questions = new ArrayList<ContentCatalog.Question>();
        for (int i=count;i>=1;i--) questions.add(new ContentCatalog.Question("q"+i,"Question "+i,ranks?i:null));
        var banks = new LinkedHashMap<String,List<ContentCatalog.Question>>();
        for (var day:SchoolCalendar.days()) if (day.course().isAssessment()) banks.put(day.assessmentId(),questions);
        return new ContentCatalog(Map.of(),Map.of(),Map.of(),banks,
                Map.of(CourseType.MIDTERM,count,CourseType.FINAL,count),StatChange.none());
    }
    private GameController game(int day,int count,CombatRules policy) {
        return new GameController(Fixtures.state(day,DayPeriod.ICS_CLASS),Fixtures.rules(),catalog(count,true),
                new Random(42),policy,clock::get);
    }
    private void seconds(double n) { clock.addAndGet((long)(n*1e9)); }
    private CombatSession.Answer answer(GameController game,String id,double time,double fraction) {
        game.startCombatQuestion(id); seconds(time);
        var ticket=game.submitCombatAnswer(); game.recordCombatEvaluation(ticket,fraction,"fixture evaluated answer");
        return game.combatSnapshot().answers().getLast();
    }
    private String signature(GameController game) { return Fixtures.signature(game.snapshot())+game.combatSnapshot(); }

    @Test void ranksBindWeightsWhilePlayerChoosesOrderAndLastAnswerDoubles() {
        var game=game(5,5,policy()); var old=game.snapshot(); game.startCombatAssessment();
        assertFalse(old.hasActiveCombat()); assertTrue(game.snapshot().hasActiveCombat());
        var view=game.combatSnapshot();
        assertEquals(List.of("q1","q2","q3","q4","q5"),view.questions().stream().map(CombatSession.QuestionView::id).toList());
        assertEquals(List.of(1.0,1.1,1.2,1.3,1.4),view.questions().stream().map(CombatSession.QuestionView::initialWeight).toList());
        for(String id:List.of("q5","q2","q4","q3")) assertFalse(answer(game,id,10,1).dasher());
        assertTrue(game.combatSnapshot().dasherReady());
        var last=answer(game,"q1",10,1);
        assertTrue(last.dasher()); assertEquals(2,last.effectiveWeight());
        var summary=game.combatSummary(); assertEquals(7,summary.weightedCredits(),1e-9);
        assertEquals(5,summary.comboBonus()); assertEquals(CombatSession.ComboStatus.EARNED,summary.combo());
        assertEquals(50-summary.answers().stream().mapToDouble(CombatSession.Answer::appliedHpCost).sum(),game.snapshot().student().hp(),1e-9);
        assertEquals(60,game.snapshot().student().san()); assertEquals(0,game.snapshot().student().codingVibe());
        assertFalse(game.combatSnapshot().dasherReady());
    }
    @ParameterizedTest @ValueSource(doubles={0,29.999,30,30.000000001,45})
    void thirtySecondsIsAnInclusiveSoftDeadline(double time) {
        var game=game(5,2,policy()); game.startCombatAssessment();
        var result=answer(game,"q2",time,.5);
        boolean late=time>30; assertEquals(late,result.timedOut());
        assertEquals(late?1:1.1,result.effectiveWeight()); assertEquals((late?1:1.1)*.5,result.weightedCredit());
        assertEquals(late?CombatSession.ComboStatus.LOST:CombatSession.ComboStatus.IN_PROGRESS,game.combatSnapshot().combo());
    }
    @Test void allWrongButTimelyStillSatisfiesTheLiteralComboCondition() {
        var game=game(5,2,policy()); game.startCombatAssessment();
        answer(game,"q1",10,0); answer(game,"q2",10,0);
        assertEquals(0,game.combatSummary().weightedCredits()); assertEquals(5,game.combatSummary().comboBonus());
    }
    @Test void oneQuestionAssessmentIsImmediatelyDasherReady() {
        var game=game(5,1,policy()); game.startCombatAssessment();
        assertTrue(game.combatSnapshot().dasherReady()); assertEquals(2,answer(game,"q1",10,1).effectiveWeight());
    }
    @Test void explicitTimeoutDasherPoliciesProduceDifferentOutcomes() {
        var p=policy();
        for(var mode:List.of(CombatRules.TimeoutDasher.KEEP_DOUBLE,CombatRules.TimeoutDasher.CANCEL_DOUBLE)) {
            var game=game(5,1,with(p,p.weightStep(),mode,p.answerTiming(),true,p.riskyRewardDuration(),p.hpCost(),p.scoreConverter()));
            game.startCombatAssessment();
            assertEquals(mode==CombatRules.TimeoutDasher.KEEP_DOUBLE?2:1,answer(game,"q1",31,1).effectiveWeight());
            assertEquals(0,game.combatSummary().comboBonus());
        }
    }
    @Test void unresolvedTimeoutOrderRejectsWithoutApplyingHpOrRecordingAnswer() {
        var p=policy(); var game=game(5,1,with(p,.1,CombatRules.TimeoutDasher.UNDECIDED,p.answerTiming(),true,p.riskyRewardDuration(),p.hpCost(),p.scoreConverter()));
        game.startCombatAssessment(); game.startCombatQuestion("q1"); seconds(31); var ticket=game.submitCombatAnswer();
        String before=signature(game);
        assertEquals("T07",assertThrows(PendingRuleException.class,()->game.recordCombatEvaluation(ticket,1,"checked")).ruleId());
        assertEquals(before,signature(game));
    }
    @Test void submissionVsEvaluationTimeIsAnExplicitPolicy() {
        var p=policy();
        for(var mode:List.of(CombatRules.AnswerTiming.PLAYER_SUBMISSION,CombatRules.AnswerTiming.EVALUATION_FINISH)) {
            var game=game(5,2,with(p,.1,p.timeoutDasher(),mode,true,p.riskyRewardDuration(),p.hpCost(),p.scoreConverter()));
            game.startCombatAssessment(); game.startCombatQuestion("q2"); seconds(20); var ticket=game.submitCombatAnswer();
            seconds(15); game.recordCombatEvaluation(ticket,1,"evaluated");
            var answer=game.combatSnapshot().answers().getFirst();
            assertEquals(mode==CombatRules.AnswerTiming.PLAYER_SUBMISSION?20:35,answer.elapsed().toSeconds());
            assertEquals(mode==CombatRules.AnswerTiming.EVALUATION_FINISH,answer.timedOut());
        }
    }
    @Test void repeatedQuestionDisplayNeverResetsClockAndSwitchingIsPending() {
        var game=game(5,2,policy()); game.startCombatAssessment(); game.startCombatQuestion("q1"); seconds(10);
        game.startCombatQuestion("q1"); assertEquals(Duration.ofSeconds(10),game.combatSnapshot().elapsed());
        String before=signature(game);
        assertEquals("T07",assertThrows(PendingRuleException.class,()->game.startCombatQuestion("q2")).ruleId());
        assertEquals(before,signature(game));
        var ticket=game.submitCombatAnswer();
        assertThrows(IllegalStateException.class,game::submitCombatAnswer);
        game.recordCombatEvaluation(ticket,1,"graded");
        assertThrows(IllegalStateException.class,()->game.startCombatQuestion("q1"));
    }
    @Test void ticketsRejectDuplicatesAndResultsFromDifferentRuns() {
        var a=game(5,2,policy()); var b=game(5,2,policy()); a.startCombatAssessment(); b.startCombatAssessment();
        a.startCombatQuestion("q1"); b.startCombatQuestion("q1");
        var old=a.submitCombatAnswer(); var current=b.submitCombatAnswer(); String before=signature(b);
        assertThrows(IllegalStateException.class,()->b.recordCombatEvaluation(old,1,"stale")); assertEquals(before,signature(b));
        b.recordCombatEvaluation(current,1,"accepted"); String after=signature(b);
        assertThrows(IllegalStateException.class,()->b.recordCombatEvaluation(current,1,"duplicate")); assertEquals(after,signature(b));
    }
    @ParameterizedTest @ValueSource(doubles={-1,1.1,Double.NaN,Double.POSITIVE_INFINITY})
    void invalidEvaluatorResultDoesNotConsumeSubmission(double fraction) {
        var game=game(5,2,policy()); game.startCombatAssessment(); game.startCombatQuestion("q1"); var ticket=game.submitCombatAnswer();
        String before=signature(game);
        assertThrows(IllegalArgumentException.class,()->game.recordCombatEvaluation(ticket,fraction,"bad")); assertEquals(before,signature(game));
        game.recordCombatEvaluation(ticket,1,"corrected evaluator result");
    }
    @Test void unknownQuestionAndPrematureSettlementPreserveState() {
        var game=game(5,2,policy()); game.startCombatAssessment(); String before=signature(game);
        assertThrows(IllegalArgumentException.class,()->game.startCombatQuestion("missing"));
        assertThrows(IllegalStateException.class,game::submitCombatAnswer);
        assertThrows(IllegalStateException.class,game::finishCombatAssessment);
        assertThrows(IllegalStateException.class,()->game.recordEvaluatedSubmission(100,"bypass"));
        assertEquals(before,signature(game));
    }
    @Test void snapshotsAreImmutableAndStateCopiesDoNotShareCombatMutations() {
        var game=game(5,2,policy()); game.startCombatAssessment(); var view=game.combatSnapshot();
        assertThrows(UnsupportedOperationException.class,()->view.questions().clear());
        assertThrows(UnsupportedOperationException.class,()->view.answers().clear());
        var copy=new GameController(game.snapshot(),Fixtures.rules(),catalog(2,true),new Random(42),policy(),clock::get);
        answer(copy,"q1",1,1);
        assertEquals(0,game.combatSnapshot().answers().size()); assertEquals(50,game.snapshot().student().hp());
        assertEquals(CombatSession.QuestionStatus.AVAILABLE,view.questions().getFirst().status());
    }
    @Test void safeRewardRequiresFiveRealSecondsAndDoesNotDoubleChargeClock() {
        var game=game(5,2,policy()); game.startCombatAssessment(); game.startCombatQuestion("q1"); seconds(10);
        var ticket=game.beginCombatRewardDelay(CombatSession.RewardKind.SAFE);
        assertEquals(Duration.ofSeconds(5),ticket.duration());
        assertThrows(IllegalStateException.class,()->game.finishCombatRewardDelay(ticket));
        assertThrows(IllegalStateException.class,game::submitCombatAnswer);
        assertThrows(IllegalStateException.class,()->game.beginCombatRewardDelay(CombatSession.RewardKind.SAFE));
        seconds(5); game.finishCombatRewardDelay(ticket);
        assertEquals(Duration.ofSeconds(15),game.combatSnapshot().elapsed());
        assertThrows(IllegalStateException.class,()->game.finishCombatRewardDelay(ticket));
        assertTrue(game.snapshot().rewards().isEmpty()); // timing alone does not invent/consume an item
    }
    @Test void excludingRewardTimeExcludesOnlyItsDurationNotLateUiCallbackTime() {
        var p=policy();var game=game(5,2,with(p,.1,p.timeoutDasher(),p.answerTiming(),false,p.riskyRewardDuration(),p.hpCost(),p.scoreConverter()));
        game.startCombatAssessment();game.startCombatQuestion("q1");seconds(10);
        var ticket=game.beginCombatRewardDelay(CombatSession.RewardKind.SAFE);seconds(3);
        assertEquals(Duration.ofSeconds(10),game.combatSnapshot().elapsed());
        assertEquals(Duration.ofSeconds(2),game.combatSnapshot().rewardRemaining());
        seconds(4);game.finishCombatRewardDelay(ticket);
        assertEquals(Duration.ofSeconds(12),game.combatSnapshot().elapsed());
    }
    @Test void riskyRewardUsesConfiguredDurationAtMostTwentySeconds() {
        var game=game(5,2,policy());game.startCombatAssessment();game.startCombatQuestion("q1");
        var ticket=game.beginCombatRewardDelay(CombatSession.RewardKind.RISKY);
        assertEquals(Duration.ofSeconds(15),ticket.duration()); seconds(14);
        assertThrows(IllegalStateException.class,()->game.finishCombatRewardDelay(ticket));
        seconds(1);game.finishCombatRewardDelay(ticket); assertEquals(Duration.ofSeconds(15),game.combatSnapshot().elapsed());
    }
    @Test void rewardPolicyGapsArePendingAndAtomic() {
        var p=policy();
        for(boolean absentTiming:List.of(true,false)) {
            var game=game(5,2,with(p,.1,p.timeoutDasher(),p.answerTiming(),absentTiming?null:true,null,p.hpCost(),p.scoreConverter()));
            game.startCombatAssessment();game.startCombatQuestion("q1");String before=signature(game);
            assertEquals("T09",assertThrows(PendingRuleException.class,()->game.beginCombatRewardDelay(CombatSession.RewardKind.RISKY)).ruleId());
            assertEquals(before,signature(game));
        }
    }
    @Test void unresolvedHpAndDepletionNeverInventDamageOrDefeat() {
        var p=policy();
        for(CombatRules.HpCost cost:Arrays.<CombatRules.HpCost>asList(null,elapsed->50.0)) {
            var game=game(5,2,with(p,.1,p.timeoutDasher(),p.answerTiming(),true,p.riskyRewardDuration(),cost,p.scoreConverter()));
            game.startCombatAssessment();game.startCombatQuestion("q1");var ticket=game.submitCombatAnswer();String before=signature(game);
            assertEquals("T01",assertThrows(PendingRuleException.class,()->game.recordCombatEvaluation(ticket,1,"checked")).ruleId());
            assertEquals(before,signature(game));
        }
    }
    @ParameterizedTest @ValueSource(doubles={0,-1,Double.NaN,Double.POSITIVE_INFINITY})
    void invalidHpFormulaCannotCommit(double cost) {
        var p=policy();var game=game(5,2,with(p,.1,p.timeoutDasher(),p.answerTiming(),true,p.riskyRewardDuration(),e->cost,p.scoreConverter()));
        game.startCombatAssessment();game.startCombatQuestion("q1");var ticket=game.submitCombatAnswer();String before=signature(game);
        assertThrows(IllegalArgumentException.class,()->game.recordCombatEvaluation(ticket,1,"checked")); assertEquals(before,signature(game));
    }
    @Test void fixtureFatigueCostsMoreForFastAnswers() {
        var game=game(5,2,policy());game.startCombatAssessment();
        var fast=answer(game,"q1",1,1);var slower=answer(game,"q2",29,1);
        assertTrue(fast.appliedHpCost()>slower.appliedHpCost());
    }
    @Test void tutorialHpApplicationWaivesAllDamageEvenIfItWouldDepleteHp() {
        var state=Fixtures.state(1,DayPeriod.ICS_CLASS);
        var p=policy();var costPolicy=with(p,.1,p.timeoutDasher(),p.answerTiming(),true,p.riskyRewardDuration(),e->1000,p.scoreConverter());
        var session=new CombatSession(List.of(new ContentCatalog.Question("q","Tutorial",1)),costPolicy);
        session.startQuestion("q",0,costPolicy);var ticket=session.submit(0,costPolicy);
        var result=session.evaluate(ticket,1,"tutorial",0,costPolicy,state);
        assertEquals(1000,result.requestedHpCost());assertEquals(0,result.appliedHpCost());
        // Tutorial opening is not a scheduled assessment; verify the shared core application separately.
        assertEquals(50,state.student.apply(new StatChange(-result.requestedHpCost(),0,0,Collections.nCopies(6,0.0)),1,1,true).hp());
    }
    @Test void finalPercentageIsSeparateFromWeightsAndComboAndPendingWhenUnconfigured() {
        var p=policy();var game=game(5,1,with(p,.1,p.timeoutDasher(),p.answerTiming(),true,p.riskyRewardDuration(),p.hpCost(),null));
        game.startCombatAssessment();answer(game,"q1",10,1);String before=signature(game);
        assertEquals(2,game.combatSummary().weightedCredits());assertEquals(5,game.combatSummary().comboBonus());
        assertEquals("T06",assertThrows(PendingRuleException.class,game::finishCombatAssessment).ruleId());assertEquals(before,signature(game));
    }
    @ParameterizedTest @ValueSource(doubles={-1,105,Double.NaN,Double.POSITIVE_INFINITY})
    void invalidScoreConversionDoesNotCloseCombat(double score) {
        var p=policy();var game=game(5,1,with(p,.1,p.timeoutDasher(),p.answerTiming(),true,p.riskyRewardDuration(),p.hpCost(),s->score));
        game.startCombatAssessment();answer(game,"q1",10,1);String before=signature(game);
        assertThrows(IllegalArgumentException.class,game::finishCombatAssessment);assertEquals(before,signature(game));
    }
    @Test void regularResultStillWaitsThreeDaysAndDayEndSaveRoundTrips() throws Exception {
        var game=game(5,2,policy());game.startCombatAssessment();
        assertThrows(IllegalStateException.class,()->new SaveManager().saveAtDayEnd(game.snapshot(),temp.resolve("mid.properties")));
        answer(game,"q2",10,1);answer(game,"q1",10,1);game.finishCombatAssessment();
        assertFalse(game.snapshot().hasActiveAssessment());assertFalse(game.snapshot().hasActiveCombat());
        assertEquals(DayPeriod.OTHER_CLASSES,game.snapshot().period());
        var record=game.snapshot().grades().records().getFirst();assertEquals(8,record.returnDay());assertTrue(record.officialScore().isEmpty());
        assertTrue(record.evidence.contains("weightedCredits"));
        assertThrows(IllegalStateException.class,game::finishCombatAssessment);
        game.otherClasses(false);game.sleepImmediatelyAfterSchool();var saves=new SaveManager();var file=temp.resolve("day.properties");
        saves.saveAtDayEnd(game.snapshot(),file);var loaded=saves.load(file);
        assertFalse(loaded.hasActiveCombat());assertEquals(Fixtures.signature(game.snapshot()),Fixtures.signature(loaded));
    }
    @ParameterizedTest @ValueSource(ints={20,40})
    void majorResultsRemainImmediateAndUseExistingCheckpoints(int day) {
        var game=game(day,2,policy());game.startCombatAssessment();answer(game,"q1",1,1);answer(game,"q2",1,1);game.finishCombatAssessment();
        assertEquals(80,game.snapshot().grades().records().getFirst().officialScore().orElseThrow());
        assertEquals(day==20?GameState.Status.MIDTERM_DECISION:GameState.Status.FINAL_DECISION,game.snapshot().status());
        assertFalse(game.snapshot().hasActiveCombat());
    }
    @Test void seriousCheatingStillOverridesConvertedCombatMark() {
        var game=game(20,2,policy());game.startCombatAssessment();game.caughtCheating(true);
        answer(game,"q1",1,1);answer(game,"q2",1,1);game.finishCombatAssessment();
        assertEquals(0,game.snapshot().grades().records().getFirst().officialScore().orElseThrow());assertEquals(48,game.snapshot().student().san());
    }
    @Test void combatMakeupRetainsOriginalBankAndMakeupConditions() {
        var state=Fixtures.state(6,DayPeriod.ICS_CLASS);state.makeups.put(6,5);state.missedAssessments.add(5);
        var game=new GameController(state,Fixtures.rules(),catalog(2,true),new Random(42),policy(),clock::get);
        var view=game.startCombatAssessment();assertTrue(view.makeup());assertFalse(view.peekingAllowed());assertTrue(view.increasedDetectionRisk());
        assertEquals(53,game.snapshot().student().san());answer(game,"q1",1,1);answer(game,"q2",1,1);game.finishCombatAssessment();
        assertEquals(9,game.snapshot().grades().records().getFirst().returnDay());assertTrue(game.snapshot().missedAssessments().isEmpty());
    }
    @Test void legacyQuestionAndControllerConstructorsRemainUsable() {
        var question=new ContentCatalog.Question("old","Old content");assertNull(question.difficultyRank());
        var game=Fixtures.game(5,DayPeriod.ICS_CLASS);game.startAssessment();game.recordEvaluatedSubmission(80,"legacy evaluator");
        assertEquals(DayPeriod.OTHER_CLASSES,game.snapshot().period());
    }
    @Test void incompleteCombatConfigurationNeverStartsAnAssessment() {
        var state=Fixtures.state(5,DayPeriod.ICS_CLASS);
        var noRanks=new GameController(state,Fixtures.rules(),catalog(2,false),new Random(42),policy(),clock::get);
        var noPolicy=game(5,2,CombatRules.pending());
        for(var game:List.of(noRanks,noPolicy)) {
            String before=Fixtures.signature(game.snapshot());
            assertEquals("T07",assertThrows(PendingRuleException.class,game::startCombatAssessment).ruleId());
            assertEquals(before,Fixtures.signature(game.snapshot()));assertFalse(game.snapshot().hasActiveCombat());
        }
    }
    @Test void unresolvedTimingCannotStartQuestion() {
        var p=policy();var game=game(5,2,with(p,.1,p.timeoutDasher(),CombatRules.AnswerTiming.UNDECIDED,true,p.riskyRewardDuration(),p.hpCost(),p.scoreConverter()));
        game.startCombatAssessment();String before=signature(game);
        assertEquals("T07",assertThrows(PendingRuleException.class,()->game.startCombatQuestion("q1")).ruleId());assertEquals(before,signature(game));
    }
    @Test void explicitOnePercentStepRemainsDifferentFromExampleStep() {
        var p=policy();var game=game(5,2,with(p,.01,p.timeoutDasher(),p.answerTiming(),true,p.riskyRewardDuration(),p.hpCost(),p.scoreConverter()));
        game.startCombatAssessment();assertEquals(1.01,game.combatSnapshot().questions().getLast().initialWeight());
    }
    @Test void rejectsInvalidRankDuplicateRanksAndInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class,()->new ContentCatalog.Question("q","prompt",0));
        assertThrows(IllegalArgumentException.class,()->new CombatSession(List.of(new ContentCatalog.Question("a","A",1),new ContentCatalog.Question("b","B",1)),policy()));
        var p=policy();
        for(double bad:List.of(0.0,-.1,Double.NaN,Double.POSITIVE_INFINITY))
            assertThrows(IllegalArgumentException.class,()->with(p,bad,p.timeoutDasher(),p.answerTiming(),true,p.riskyRewardDuration(),p.hpCost(),p.scoreConverter()));
        for(Duration bad:List.of(Duration.ZERO,Duration.ofSeconds(-1),Duration.ofSeconds(21)))
            assertThrows(IllegalArgumentException.class,()->with(p,.1,p.timeoutDasher(),p.answerTiming(),true,bad,p.hpCost(),p.scoreConverter()));
    }
    @Test void oldRewardTicketsDoNotCompleteAnotherSessionsDelay() {
        var a=game(5,2,policy());var b=game(5,2,policy());a.startCombatAssessment();b.startCombatAssessment();a.startCombatQuestion("q1");b.startCombatQuestion("q1");
        var old=a.beginCombatRewardDelay(CombatSession.RewardKind.SAFE);b.beginCombatRewardDelay(CombatSession.RewardKind.SAFE);seconds(5);
        String before=signature(b);assertThrows(IllegalStateException.class,()->b.finishCombatRewardDelay(old));assertEquals(before,signature(b));
    }
    @Test void productionOverloadsKeepDemoPolicyExplicit() throws Exception {
        var profile=DemoProfile.load();
        var game=profile.newGame("Configured",new Random(42),policy());
        assertEquals(1,game.snapshot().day());assertFalse(game.snapshot().hasActiveCombat());
        var old=profile.newGame("Legacy",new Random(42));
        assertEquals(game.snapshot().student(),old.snapshot().student());
    }
    @Test void badClockAndMissingEvidencePreserveSubmittedWork() {
        var p=policy();var game=game(5,2,with(p,.1,p.timeoutDasher(),CombatRules.AnswerTiming.EVALUATION_FINISH,true,p.riskyRewardDuration(),p.hpCost(),p.scoreConverter()));
        clock.set(100);game.startCombatAssessment();game.startCombatQuestion("q1");
        var ticket=game.submitCombatAnswer();String before=signature(game);
        assertThrows(IllegalArgumentException.class,()->game.recordCombatEvaluation(ticket,1,""));assertEquals(before,signature(game));
        clock.set(50);assertThrows(IllegalStateException.class,()->game.recordCombatEvaluation(ticket,1,"checked"));
        clock.set(100);assertEquals(before,signature(game));
    }

}
