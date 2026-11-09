package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SaveManagerTest {
    @TempDir Path dir;
    @Test void dayEndRoundTripPreservesPendingResultsAndEffects() throws Exception {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);g.startAssessment();g.recordSelfEstimate(92,2,4);
        g.recordEvaluatedSubmission(70,"fixture evidence");g.otherClasses(false);g.hubActivity("rest");g.hubActivity("rest");g.hubActivity("rest");
        var s=g.snapshot();s.run=new RunProgress(1);s.rewards.add("shirt");s.relationships.put("peer",2.5);
        var save=new SaveManager();Path file=dir.resolve("保存.properties");save.saveAtDayEnd(s,file);
        var restored=save.load(file);assertEquals(Fixtures.signature(s),Fixtures.signature(restored));
        assertEquals(s.run(),restored.run());assertEquals(s.rewards(),restored.rewards());assertEquals(s.relationships(),restored.relationships());
        assertEquals(2,restored.effects().size());assertTrue(restored.grades().records().getFirst().officialScore().isEmpty());
        assertEquals(1,restored.run().retakesUsed());
        var resumed=Fixtures.game(restored);resumed.beginNextDay();assertEquals(6,resumed.snapshot().day());
        assertEquals(.5,resumed.snapshot().xpMultiplier());
        assertThrows(IllegalStateException.class,resumed::beginNextDay);
    }
    @Test void dayEndSaveResumesWithoutRegrantingEarlySleepBonus() throws Exception {
        var g=Fixtures.game(3,DayPeriod.AFTER_SCHOOL);g.sleepImmediatelyAfterSchool();
        var save=new SaveManager();var file=dir.resolve("day.properties");save.saveAtDayEnd(g.snapshot(),file);
        var restored=save.load(file);assertEquals(3,restored.day());assertEquals(DayPeriod.DAY_END,restored.period());
        assertEquals(60,restored.student().san());
        var next=Fixtures.game(restored);next.beginNextDay();assertEquals(63,next.snapshot().student().san());
        assertEquals(DayPeriod.MORNING_BONUS,next.snapshot().period());
        assertThrows(IllegalStateException.class,next::beginNextDay);
    }
    @Test void makeupAssignmentSurvivesSave() throws Exception {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);g.skipClass(false);g.scheduleMakeup(5,6);g.otherClasses(false);g.sleepImmediatelyAfterSchool();
        var file=dir.resolve("makeup.properties");var saves=new SaveManager();saves.saveAtDayEnd(g.snapshot(),file);
        var state=saves.load(file);assertEquals(Map.of(6,5),state.makeups());assertEquals(Set.of(5),state.missedAssessments());
    }
    @Test void midActivitySaveDoesNotOverwriteExistingFile() throws Exception {
        Path file=dir.resolve("existing");Files.writeString(file,"original bytes");
        assertThrows(IllegalStateException.class,()->new SaveManager().saveAtDayEnd(Fixtures.state(5,DayPeriod.ICS_CLASS),file));
        assertEquals("original bytes",Files.readString(file));
    }
    @Test void endingBoundaryIsNotInvented() {
        var s=Fixtures.state(20,DayPeriod.DAY_END);s.status=GameState.Status.DROPPED;
        assertThrows(IllegalStateException.class,()->new SaveManager().saveAtDayEnd(s,dir.resolve("drop")));
    }
    @ParameterizedTest @CsvSource({
        "version,2","day,41","period,ICS_CLASS","opening,false","morningBonus,maybe",
        "student.hp,NaN","student.san,Infinity","retakes,3","absences,-1","effects.count,-1","grades.count,100000"})
    void malformedSnapshotRejected(String field,String value) throws Exception {
        var save=new SaveManager();Path file=dir.resolve("bad.properties");save.saveAtDayEnd(Fixtures.state(3,DayPeriod.DAY_END),file);
        Properties p=new Properties();try(var in=Files.newBufferedReader(file)){p.load(in);}p.setProperty(field,value);
        try(var out=Files.newBufferedWriter(file)){p.store(out,"corrupt fixture");}
        assertThrows(IOException.class,()->save.load(file));
    }
    @Test void missingAndUnknownFieldsRejected() throws Exception {
        var save=new SaveManager();Path file=dir.resolve("bad.properties");save.saveAtDayEnd(Fixtures.state(3,DayPeriod.DAY_END),file);
        String original=Files.readString(file);
        Files.writeString(file,original+"\ninvented.rule=1\n");assertThrows(IOException.class,()->save.load(file));
        Files.writeString(file,original.replaceAll("(?m)^student.hp=.*\\R",""));assertThrows(IOException.class,()->save.load(file));
    }
    @Test void fileFailureLeavesExistingDestinationIntactAndCleansTemporary() throws Exception {
        Path target=Files.createDirectory(dir.resolve("destination"));Files.writeString(target.resolve("keep"),"original");
        assertThrows(IOException.class,()->new SaveManager().saveAtDayEnd(Fixtures.state(3,DayPeriod.DAY_END),target));
        assertEquals("original",Files.readString(target.resolve("keep")));
        try(var files=Files.list(dir)){assertEquals(List.of(target),files.toList());}
    }
    @Test void returnedMarksDoNotReturnAgainAfterLoad() throws Exception {
        var g=Fixtures.game(5,DayPeriod.ICS_CLASS);var v=g.startAssessment();g.recordEvaluatedSubmission(75,"fixture");
        var s=g.snapshot();s.day=7;s.period=DayPeriod.DAY_END;g=Fixtures.game(s);g.beginNextDay();
        g.applyOfficialFeedback(v.id(),new StatChange(0,2,0,Collections.nCopies(6,0.0)));
        var end=g.snapshot();end.period=DayPeriod.DAY_END;
        var saves=new SaveManager();Path path=dir.resolve("official");saves.saveAtDayEnd(end,path);
        var loaded=Fixtures.game(saves.load(path));loaded.beginNextDay();
        assertEquals(62,loaded.snapshot().student().san());
        assertThrows(IllegalStateException.class,()->loaded.applyOfficialFeedback(v.id(),StatChange.none()));
    }
}
