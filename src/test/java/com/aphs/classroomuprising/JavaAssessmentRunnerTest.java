package com.aphs.classroomuprising;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JavaAssessmentRunnerTest {
    private JavaAssessmentRunner runner(long millis,int bytes) {
        return new JavaAssessmentRunner(Path.of(System.getProperty("java.home")),
                new JavaAssessmentRunner.Limits(Duration.ofSeconds(15),Duration.ofMillis(millis),bytes));
    }
    private List<JavaAssessmentRunner.TestCase> tests(String expected) {
        return List.of(new JavaAssessmentRunner.TestCase("fixture","",expected));
    }
    @Test void compilesCompleteJavaAndExecutesBuiltinCases() throws Exception {
        String source="""
                package fixtures;
                public class Sum {
                    public static void main(String[] args) {
                        var s = new java.util.Scanner(System.in);
                        System.out.println(s.nextInt() + s.nextInt());
                    }
                }
                """;
        var report=runner(3000,8192).evaluate("fixtures.Sum",source,List.of(
                new JavaAssessmentRunner.TestCase("positive","2 3\n","5\n"),
                new JavaAssessmentRunner.TestCase("negative","-7 2\n","-5\n")),
                List.of(new StyleRule.LineLength("line",120)));
        assertEquals(0,report.compilation().exitCode());
        assertTrue(report.tests().stream().allMatch(t->t.status()==JavaAssessmentRunner.Status.PASSED));
        assertTrue(report.style().getFirst().passed());
    }
    @Test void wrongAnswerIsNotCompileFailure() throws Exception {
        var r=runner(3000,8192).evaluate("Answer","public class Answer { public static void main(String[] a) { System.out.print(2); } }",tests("3"),List.of());
        assertEquals(JavaAssessmentRunner.Status.WRONG_OUTPUT,r.tests().getFirst().status());
    }
    @Test void compileErrorHasDiagnosticsAndNoFakeSuccess() throws Exception {
        var r=runner(3000,8192).evaluate("Broken","public class Broken { syntax error }",tests(""),List.of());
        assertEquals(JavaAssessmentRunner.Status.COMPILE_ERROR,r.tests().getFirst().status());
        assertFalse(r.compilation().stderr().isBlank());
    }
    @Test void runtimeErrorSeparateFromWrongAnswer() throws Exception {
        var r=runner(3000,8192).evaluate("Fail","public class Fail { public static void main(String[] a) { throw new RuntimeException(\"fixture\"); } }",tests(""),List.of());
        assertEquals(JavaAssessmentRunner.Status.RUNTIME_ERROR,r.tests().getFirst().status());
        assertTrue(r.tests().getFirst().process().stderr().contains("fixture"));
    }
    @Test void endlessProgramTimesOut() throws Exception {
        var r=runner(250,8192).evaluate("Loop","public class Loop { public static void main(String[] a) { while(true) {} } }",tests(""),List.of());
        assertEquals(JavaAssessmentRunner.Status.TIMEOUT,r.tests().getFirst().status());
    }
    @Test void outputIsBounded() throws Exception {
        var r=runner(3000,2048).evaluate("Loud","public class Loud { public static void main(String[] a) { while(true) System.out.print(\"0123456789\"); } }",tests(""),List.of());
        var t=r.tests().getFirst();assertEquals(JavaAssessmentRunner.Status.OUTPUT_LIMIT,t.status());
        assertTrue(t.process().stdout().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=2048);
    }
    @Test void styleEvidenceIndependentFromCorrectness() throws Exception {
        var r=runner(3000,8192).evaluate("Hello","public class Hello { public static void main(String[] a) { System.out.print(\"ok\"); } }",
                tests("ok"),List.of(new StyleRule.LineLength("short-lines",10)));
        assertEquals(JavaAssessmentRunner.Status.PASSED,r.tests().getFirst().status());
        assertFalse(r.style().getFirst().passed());
    }
    @Test void pathTraversalClassNameRejectedBeforeExecution() {
        assertThrows(IllegalArgumentException.class,()->runner(3000,8192).evaluate("../Outside","",tests(""),List.of()));
    }
    @Test void emptyTestsDoNotProduceAVacuousPass() {
        assertThrows(IllegalArgumentException.class,()->runner(3000,8192).evaluate("A","",List.of(),List.of()));
    }
}
