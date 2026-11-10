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
        String text=DemoProfile.load().content().lesson(1).text();
        String source=excerpt(text,"int count;")+excerpt(text,"double price;")+"int x;"+
                excerpt(text,"x = 22;")+excerpt(text,"System.out.println(\"'sup?\");");
        passed(run(source,"","'sup?"+System.lineSeparator()));
    }
    @Test void dayTwoJavaExamplesProduceTheTaughtValues() throws Exception {
        String text=DemoProfile.load().content().lesson(2).text();
        // The loop initializes y to zero as in the supplied slide/example contract.
        String loop=excerpt(text,"for (int i = 0; i < 100; i += 2)");
        String add=excerpt(text,"y += i");
        String source="int y=0;int last=-1;"+loop+" { "+add+";last=i;System.out.println(i+\" \"+y); }"+
                "System.out.println(\"last=\"+last+\", y=\"+y);"+
                "double aSide=3.0;double bSide=4.0;"+
                excerpt(text,"double cSide = pythag(aSide, bSide);")+
                excerpt(text,"System.out.println(cSide);")+
                excerpt(text,"int[] values = {4, 7, 2};")+"System.out.println(values.length);";
        String method="static double pythag(double a,double b) { return "+
                excerpt(text,"Math.sqrt(a * a + b * b)")+"; }";
        String nl=System.lineSeparator();var output=new StringBuilder();
        // Independent closed-form expectation for the sum of even integers through 2*n.
        for(int n=0;n<50;n++) output.append(2*n).append(' ').append(n*(n+1)).append(nl);
        output.append("last=98, y=2450").append(nl).append("5.0").append(nl).append("3").append(nl);
        passed(run(source,method,output.toString()));
    }
    @ParameterizedTest @ValueSource(ints={1,2})
    void everyReviewLineKeepsTheSlideSpecifiedByTheNarration(int day) throws Exception {
        var lines=DialogueScript.parse(DemoProfile.load().content().lesson(day).text(),"Unexpected fallback");
        var slides=day==1?List.of("review-purpose","file-structure","example-java","data-types",
                "variables","math-and-symbols","decisions"):
                List.of("loops","methods","method-example","the-rest","automated-testing");
        var counts=day==1?List.of(3,2,2,2,2,3,2):List.of(5,2,3,3,2);
        assertEquals(day==1?16:15,lines.size());
        assertEquals(slides,SlideProjector.cues(lines));
        // This is an assertion about the current supplied draft, not a runtime line-number map.
        String current=null;int offset=0;
        for(int page=0;page<slides.size();page++) {
            for(int i=0;i<counts.get(page);i++) {
                var line=lines.get(offset++);
                if(line.slide()!=null) current=line.slide();
                assertEquals(slides.get(page),current);
                if(i==0) assertEquals(current,line.slide()); else assertNull(line.slide());
                assertFalse(line.text().contains("[slide:"));
                assertTrue(line.text().length()<=180,"Narration should retain the validated compact page size.");
            }
        }
        assertEquals(lines.size(),offset);
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
