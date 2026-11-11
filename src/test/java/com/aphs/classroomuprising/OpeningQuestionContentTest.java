package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Execute the shipped question snippets, not separately retyped solutions. Keys also checked against PDF images. */
class OpeningQuestionContentTest {
    String source() throws Exception {
        try (var in = getClass().getResourceAsStream("/demo/opening.properties")) {
            assertNotNull(in); return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    OpeningContent content() throws Exception { return OpeningContent.read(new StringReader(source())); }
    JavaAssessmentRunner.Report run(String members, String body, String expected) throws Exception {
        var runner = new JavaAssessmentRunner(Path.of(System.getProperty("java.home")),
                new JavaAssessmentRunner.Limits(Duration.ofSeconds(10), Duration.ofSeconds(3), 16384));
        return runner.evaluate("OpeningExample", "import java.util.*; public class OpeningExample { " + members
                + " public static void main(String[] args) { " + body + "\n } }",
                List.of(new JavaAssessmentRunner.TestCase("pdf-question-trace", "", expected)), List.of());
    }
    void passed(String members, String body, String output) throws Exception {
        var r = run(members, body, output);
        assertEquals(0, r.compilation().exitCode(), r.compilation().stderr());
        assertEquals(JavaAssessmentRunner.Status.PASSED, r.tests().getFirst().status(), r.tests().toString());
    }
    String segment(String code, String from, String to) {
        int start = code.indexOf("// " + from) + from.length() + 3;
        int end = to == null ? code.length() : code.indexOf("// " + to, start);
        return code.substring(start, end).strip();
    }
    @Test void tenSourceKeysMatchBothPdfAnswerKeyPagesAndNoKeyLeaksIntoQuestionViews() throws Exception {
        var c = content(); var keys = Map.of("csa2014-q15","A","csa2014-q22","B","csa2014-q23","B",
                "csa2014-q39","D","csa2014-q40","C","csa2015-q20","E","csa2015-q26","E",
                "csa2015-q31","E","csa2015-q37","D","csa2015-q38","E");
        assertEquals(10, c.questions().size());
        for (var q : c.questions()) {
            assertEquals(keys.get(q.id()), c.question(q.id()).answer());
            assertEquals(List.of("A","B","C","D","E"), q.options().stream().map(OpeningContent.Option::id).toList());
            assertEquals(q.id().contains("2014") ? 60 : 79, c.source(q.id()).answerKeyPdfPage());
            assertFalse(c.source(q.id()).selectionReason().isBlank());
            assertFalse(c.question(q.id()).explanation().isBlank());
        }
        assertFalse(Arrays.stream(OpeningContent.QuestionView.class.getRecordComponents())
                .anyMatch(f -> f.getName().toLowerCase().contains("answer")));
    }
    @ParameterizedTest @ValueSource(strings={"csa2014-q15","csa2014-q22","csa2014-q40","csa2015-q26",
            "csa2015-q37","csa2015-q20","csa2014-q23","csa2015-q31","csa2014-q39","csa2015-q38"})
    void selectedQuestionReallyHasTheKeyedJavaBehavior(String id) throws Exception {
        var q = content().question(id); String code = q.view().code();
        String option = q.view().options().stream().filter(o -> o.id().equals(q.answer())).findFirst().orElseThrow().text();
        switch (id) {
            case "csa2014-q15" -> {
                String methods = "static boolean one(int[] data){" + segment(code,"I","II") + "}"
                        + "static boolean two(int[] data){" + segment(code,"II","III") + "}"
                        + "static boolean three(int[] data){" + segment(code,"III",null) + "}";
                passed(methods, "if(!one(new int[]{1,1,2}) || one(new int[]{1,2,0}) || !one(new int[]{})) throw new AssertionError();"
                        + "try { two(new int[]{1,2,3}); throw new AssertionError(); } catch(ArrayIndexOutOfBoundsException expected) {}"
                        + "if(!three(new int[]{1,2,0})) throw new AssertionError(); System.out.print(\"I only\");", option);
            }
            case "csa2014-q22" -> {
                String classes = code.substring(0, code.indexOf("// Segment")).replace("public class", "public static class");
                String body = code.substring(code.indexOf("Book[] books"));
                var failed = run(classes, body, "");
                assertNotEquals(0, failed.compilation().exitCode());
                assertTrue(failed.compilation().stderr().contains("pagesPerMinute"), failed.compilation().stderr());
                assertTrue(option.startsWith("Line 4"));
                passed(classes, body.replace("System.out.println(books[0].pagesPerMinute());", ""),
                        "The Jungle 300\n100\nCaptains Courageous 400\n");
            }
            case "csa2014-q40" -> passed(code, "whatsItDo(\"WATCH\");", option + "\n");
            case "csa2015-q26" -> passed(code, "start();", option);
            case "csa2015-q37" -> {
                String counted = "static int count;" + code.replace("int mid = (start + end) / 2;", "count++; int mid = (start + end) / 2;");
                passed(counted, "int[] a=new int[2000];for(int i=0;i<a.length;i++)a[i]=2*i;int max=0;"
                        + "for(int target=-1;target<=4000;target++){count=0;binarySearch(a,target);max=Math.max(max,count);}System.out.print(max);", option);
            }
            case "csa2015-q20" -> passed(code,
                    "for(int n=1;n<=7;n++){int cases=(int)Math.pow(3,n);for(int mask=0;mask<cases;mask++){"
                    + "int[] a=new int[n];int m=mask;int[] freq=new int[3];for(int i=0;i<n;i++){a[i]=m%3;m/=3;freq[a[i]]++;}"
                    + "int index=mystery(a);int max=Arrays.stream(freq).max().orElseThrow();if(freq[a[index]]!=max)throw new AssertionError();}}"
                    + "System.out.print(\"mode index verified\");", "mode index verified");
            case "csa2014-q23" -> passed(code,
                    "var example=new OpeningExample();example.animals=new ArrayList<>(List.of(\"bear\",\"zebra\",\"bass\",\"cat\",\"koala\",\"baboon\"));"
                    + "example.manipulate();System.out.print(example.animals);", option.replace("\"", ""));
            case "csa2015-q31" -> passed("", code + "for(String[] row:board)System.out.println(String.join(\" \",row));", option + "\n");
            case "csa2014-q39" -> passed(code, "System.out.print(new OpeningExample().recur(27));", option);
            case "csa2015-q38" -> {
                String methods = ""; String[] names = {"one","two","three"}; String[] markers = {"I","II","III"};
                for(int i=0;i<3;i++) methods += "static String " + names[i] + "(String[] words,int startIndex){String result=\"\";"
                        + segment(code,markers[i],i==2?null:markers[i+1]) + "return result;}";
                passed(methods,
                        "String[] sample={\"Bear\",\"Apple\",\"Gorilla\",\"House\",\"Car\"};if(one(sample,2).equals(\"CarHouseGorilla\"))throw new AssertionError();"
                        + "for(int n=1;n<=12;n++){String[] a=new String[n];for(int i=0;i<n;i++)a[i]=\"[\"+i+\"]\";"
                        + "for(int start=0;start<=n+1;start++){String expected=\"\";for(int i=n-1;i>=start;i--)expected+=a[i];"
                        + "if(!two(a,start).equals(expected)||!three(a,start).equals(expected))throw new AssertionError();}}System.out.print(\"II and III\");", option);
            }
            default -> fail("Missing executable verification for " + id);
        }
    }
    @ParameterizedTest @ValueSource(strings={"missing","unknown","duplicate","wrong-key","nine-questions","nan","zero-duration","zero-heal","bad-knowledge"})
    void invalidContentFailsRatherThanInventingDefaults(String kind) throws Exception {
        String s = source(); String invalid = switch(kind) {
            case "missing" -> s.replace("story.intro=", "ignored.intro=");
            case "unknown" -> s + "\nhp.max=100\n"; // The superseded cap is not a valid field.
            case "duplicate" -> s.replace("question.ids=", "question.ids=csa2014-q15,");
            case "wrong-key" -> s.replace("question.csa2014-q15.answer=A", "question.csa2014-q15.answer=F");
            case "nine-questions" -> s.replace("csa2014-q15,csa2014-q22", "csa2014-q22");
            case "nan" -> s.replace("initial.san=1", "initial.san=NaN");
            case "zero-duration" -> s.replace("duck.seconds=5", "duck.seconds=0");
            case "zero-heal" -> s.replace("duck.hp=3", "duck.hp=0");
            case "bad-knowledge" -> s.replace("initial.knowledge=0,0,0,0,0,0", "initial.knowledge=0,0");
            default -> throw new AssertionError();
        };
        assertThrows(IOException.class, () -> OpeningContent.read(new StringReader(invalid)));
    }
    @Test void introDoesNotPredetermineDefeatAndBothEndingsExplainSummerSchool() throws Exception {
        var c = content(); assertTrue(c.intro().getFirst().contains("Grade 11"));
        assertTrue(c.intro().stream().noneMatch(s -> s.contains("failed ICS4U")));
        assertTrue(c.defeat().stream().anyMatch(s -> s.contains("67")));
        assertTrue(c.victory().stream().anyMatch(s -> s.contains("89")));
        for (var lines : List.of(c.defeat(), c.victory())) {
            assertTrue(lines.stream().anyMatch(s -> s.contains("summer school")));
            assertTrue(lines.getLast().contains("Day 1"));
        }
    }
}
