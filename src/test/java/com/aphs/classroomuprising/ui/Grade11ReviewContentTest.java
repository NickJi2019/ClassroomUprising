package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

/** Backend lesson resource contract, checked with the existing frontend dialogue parser. */
class Grade11ReviewContentTest {
    @ParameterizedTest @ValueSource(ints={1,2})
    void reviewLecturesParseAsShortMrRewardLinesWithoutGameplayHints(int day) throws Exception {
        var lesson=DemoProfile.load().content().lesson(day);
        var lines=DialogueScript.parse(lesson.text(),"Unexpected fallback");
        assertTrue(lines.size()>=8,"A review lecture needs more than a pair of gameplay hints.");
        assertEquals(lesson.text().lines().count(),lines.size());
        var hints=Pattern.compile("(?i)\\b(hub|xp|san|hp|penalt\\w*|tutorial|warning\\w*|save|saving|schedule)\\b|after school|late night|half.slot|choose once");
        for(var line:lines) {
            assertEquals("Mr. Reward",line.speaker());assertNull(line.expression());
            assertFalse(line.text().isBlank());assertTrue(line.text().length()<=200,"Keep each dialogue page compact.");
            assertFalse(hints.matcher(line.text()).find(),line.text());
            assertFalse(line.text().contains("\\n"),"Resource escapes must decode into line breaks.");
        }
        assertEquals(ContentCatalog.Interaction.ASK_TEACHER,lesson.interaction());
        assertEquals(Set.of("ask","quiet"),lesson.choices().keySet());
        assertEquals(StatChange.learning(1,4,2,-2),lesson.choices().get("ask"));
        assertEquals(StatChange.none(),lesson.choices().get("quiet"));
    }
    @Test void theTwoDaysCoverDifferentReviewTopics() throws Exception {
        var profile=DemoProfile.load();String first=profile.content().lesson(1).text();String second=profile.content().lesson(2).text();
        for(String topic:List.of("Grade 11","variable","int ","double ","String ","boolean ","if (","else","&&",".equals","System.out.println"))
            assertTrue(first.contains(topic),topic);
        for(String topic:List.of("Grade 11","for (","while (","int[]","values.length","method","Parameters","return "))
            assertTrue(second.contains(topic),topic);
        assertNotEquals(first,second);
    }
    private String excerpt(String lesson,String source) {
        assertTrue(lesson.contains(source),"Executable example must actually appear in the lecture: "+source);
        return source;
    }
    private JavaAssessmentRunner.Report run(String body,String methods,String output) throws Exception {
        var runner=new JavaAssessmentRunner(Path.of(System.getProperty("java.home")),
                new JavaAssessmentRunner.Limits(Duration.ofSeconds(10),Duration.ofSeconds(3),8192));
        return runner.evaluate("Grade11Example","public class Grade11Example { "+methods+
                " public static void main(String[] args) { "+body+" } }",
                List.of(new JavaAssessmentRunner.TestCase("lecture-trace","",output)),List.of());
    }
    private void passed(JavaAssessmentRunner.Report report) {
        assertEquals(0,report.compilation().exitCode(),report.compilation().stderr());
        assertEquals(JavaAssessmentRunner.Status.PASSED,report.tests().getFirst().status(),report.tests().toString());
    }
    @Test void dayOneJavaExamplesProduceTheTaughtValues() throws Exception {
        String text=DemoProfile.load().content().lesson(1).text();var source=new StringBuilder();
        for(String code:List.of("int tickets = 7;","double price = 2.5;","String name = \"Ada\";",
                "boolean hasPass = true;","tickets = tickets + 1;","int wholeShare = 7 / 2;",
                "double decimalShare = 7 / 2;","double properShare = 7 / 2.0;","int age = 16;"))
            source.append(excerpt(text,code));
        source.append("System.out.print(tickets+\"|\"+price+\"|\"+name+\"|\"+hasPass+\"|\"+wholeShare+\"|\"+decimalShare+\"|\"+properShare+\"|\");");
        source.append("System.out.print((").append(excerpt(text,"2 + 3 * 4")).append(")+\"|\"+(")
                .append(excerpt(text,"(2 + 3) * 4")).append(")+\"|\");");
        String branch=excerpt(text,"if (age >= 16 && hasPass) { System.out.print(\"Enter\"); } else { System.out.print(\"Wait\"); }");
        source.append(branch).append("System.out.print(\"|\");age=15;").append(branch)
                .append("System.out.print(\"|\"+").append(excerpt(text,"\"Ada\".equals(name)")).append(");");
        passed(run(source.toString(),"","8|2.5|Ada|true|3|3.0|3.5|14|20|Enter|Wait|true"));
    }
    @Test void dayTwoJavaExamplesProduceTheTaughtValues() throws Exception {
        String text=DemoProfile.load().content().lesson(2).text();var source=new StringBuilder();
        source.append(excerpt(text,"for (int i = 0; i < 3; i++) { System.out.print(i); }"));
        source.append(excerpt(text,"int total = 0;")).append(excerpt(text,"for (int n = 1; n <= 3; n++) { total += n; }"));
        source.append(excerpt(text,"int remaining = 3;")).append(excerpt(text,"while (remaining > 0) { remaining--; }"));
        source.append(excerpt(text,"int[] values = {4, 7, 2};"));
        source.append(excerpt(text,"int sum = 0;")).append(excerpt(text,"for (int i = 0; i < values.length; i++) { sum += values[i]; }"));
        source.append(excerpt(text,"int doubled = doubleValue(4);"));
        source.append("System.out.print(\"|\"+total+\"|\"+remaining+\"|\"+values[1]+\"|\"+values.length+\"|\"+sum+\"|\"+doubled);");
        passed(run(source.toString(),excerpt(text,"static int doubleValue(int value) { return value * 2; }"),"012|6|0|7|3|13|8"));
    }
    @Test void theArrayBoundaryExampleReallyIsOutOfBounds() throws Exception {
        String text=DemoProfile.load().content().lesson(2).text();
        var report=run(excerpt(text,"int[] values = {4, 7, 2};")+"System.out.print("+excerpt(text,"values[values.length]")+");","","");
        assertEquals(0,report.compilation().exitCode());
        var result=report.tests().getFirst();assertEquals(JavaAssessmentRunner.Status.RUNTIME_ERROR,result.status());
        assertTrue(result.process().stderr().contains("ArrayIndexOutOfBoundsException"));
    }
    @Test void lecturesStillLeadToOneChoiceWithTheSameTutorialEffects() throws Exception {
        var game=DemoProfile.load().newGame("Review student",new Random(42));game.completeOpening();
        for(int day=1;day<=2;day++) {
            if(game.snapshot().period()==DayPeriod.MORNING_BONUS) game.hubActivity("rest");
            var before=game.snapshot().student();var lesson=game.showLesson();
            assertTrue(lesson.text().contains("Grade 11"));
            var result=game.lessonChoice("ask");var after=game.snapshot().student();
            assertEquals(before.hp(),after.hp());assertEquals(before.san(),after.san());
            assertEquals(before.codingVibe()+4,after.codingVibe());
            assertEquals(before.unitKnowledge().getFirst()+2,after.unitKnowledge().getFirst());
            assertFalse(result.warnings().isEmpty());assertEquals(DayPeriod.OTHER_CLASSES,game.snapshot().period());
            assertThrows(IllegalStateException.class,()->game.lessonChoice("quiet"));
            game.otherClasses(false);game.sleepImmediatelyAfterSchool();game.beginNextDay();
        }
        assertEquals(3,game.snapshot().day());
    }
}
