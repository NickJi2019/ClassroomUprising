package com.aphs.classroomuprising;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Swing demo entry point plus the existing command-line core checks. */
public final class Main {
    private Main() {}
    public static void main(String[] args) throws Exception {
        String command=args.length==0?"--gui":args[0];
        switch(command) {
            case "--gui" -> javax.swing.SwingUtilities.invokeLater(() ->
                    GameWindow.open(args.length > 1 ? Path.of(args[1]) : Path.of("demo-save.properties")));

            case "--calendar" -> {
                for(var d:SchoolCalendar.days())
                    System.out.printf(Locale.ROOT,"Day %02d | %-9s | unit %d%s%n",d.day(),d.course(),d.unit(),d.tutorial()?" | tutorial":"");
            }
            case "--demo" -> demo(args.length>1?Path.of(args[1]):Files.createTempDirectory("classroom-demo-").resolve("day-end.properties"));
            case "--lab-demo" -> labDemo();
            case "--rules" -> System.out.println("Production values/content remain TBD: T01-T14 in docs/Sprint1.md. No balancing defaults are installed.");
            case "--help" -> System.out.println("""
                    Classroom Uprising - pure Java Swing demo (JDK 21+)
                    --gui [save-file]    Open the playable Days 1-8 fixture (default when no arguments).
                    --calendar           Show the approved forty-day schedule.
                    --rules              Show the unresolved-policy boundary.
                    --demo [save-file]   Run a labelled fixture scenario and a real day-end save/load.
                    --lab-demo           Compile and run a built-in trusted Java fixture with tests and a style rule.
                    GUI lesson/rubric values are demo fixtures; production grading and Final combat remain TBD.
                    """);
            default -> throw new IllegalArgumentException("Unknown command. Use --help.");
        }
    }
    private static void demo(Path save) throws Exception {
        System.out.println("DEMO ONLY: numeric values and lesson text below are test fixtures, NOT approved game rules.");
        var study=StatChange.learning(1,4,2,-2);
        var lessons=new LinkedHashMap<Integer,ContentCatalog.Lesson>();
        for(int day=1;day<=3;day++)
            lessons.put(day,new ContentCatalog.Lesson("Fixture lesson "+day,
                    day==2?ContentCatalog.Interaction.RAISE_HAND:ContentCatalog.Interaction.ASK_TEACHER,Map.of("learn",study)));
        var content=new ContentCatalog(lessons,Map.of("study",study),Map.of(),Map.of(),Map.of(),study);
        var rules=new RuleValues(4.0,0.5,3.0,7.0,12.0,RuleValues.TutorialAbsences.IGNORE);
        var game=new GameController(new GameState("Demo student",new StudentState(10,20,0,Collections.nCopies(6,0.0))),rules,content,new Random(7));
        game.completeOpening();game.showLesson();
        System.out.println(game.lessonChoice("learn").warnings());
        game.otherClasses(false);game.sleepImmediatelyAfterSchool();
        var saves=new SaveManager();saves.saveAtDayEnd(game.snapshot(),save);
        System.out.println("Saved Day 1 at DAY_END.");
        game=new GameController(saves.load(save),rules,content,new Random(7));game.beginNextDay();
        game.hubActivity("study");
        System.out.printf(Locale.ROOT,"Day 2 after half-slot: phase=%s, XP=%.1f, SAN=%.1f%n",game.snapshot().period(),game.snapshot().student().codingVibe(),game.snapshot().effectiveSan());
        game.showLesson();game.lessonChoice("learn");game.otherClasses(false);
        game.hubActivity("study");game.hubActivity("study");
        System.out.println(game.hubActivity("study").warnings());
        game.beginNextDay();
        System.out.printf(Locale.ROOT,"Day 3 after tutorial late night: XP multiplier=%.1f%n",game.snapshot().xpMultiplier());
        game.showLesson();game.lessonChoice("learn");game.otherClasses(false);
        game.hubActivity("study");game.hubActivity("study");game.hubActivity("study");
        saves.saveAtDayEnd(game.snapshot(),save);
        game=new GameController(saves.load(save),rules,content,new Random(7));game.beginNextDay();
        System.out.printf(Locale.ROOT,"Day 4 after ordinary late night: XP multiplier=%.1f, base SAN=%.1f, effective SAN=%.1f%n",
                game.snapshot().xpMultiplier(),game.snapshot().student().san(),game.snapshot().effectiveSan());
        System.out.println("Fixture save: "+save.toAbsolutePath());
        System.out.println("DEMO PASS: tutorial, half-slot, next-day effects and day-end round trip.");
    }
    private static void labDemo() throws Exception {
        System.out.println("LAB DEMO: trusted built-in source; test/rubric values are fixtures, not course grading.");
        var runner=new JavaAssessmentRunner(Path.of(System.getProperty("java.home")),
                new JavaAssessmentRunner.Limits(Duration.ofSeconds(15),Duration.ofSeconds(3),16384));
        String source="""
                public class Sum {
                    public static void main(String[] args) {
                        var scanner = new java.util.Scanner(System.in);
                        System.out.println(scanner.nextInt() + scanner.nextInt());
                    }
                }
                """;
        var report=runner.evaluate("Sum",source,List.of(
                new JavaAssessmentRunner.TestCase("positive","2 3\n","5\n"),
                new JavaAssessmentRunner.TestCase("negative","-4 1\n","-3\n")),
                List.of(new StyleRule.LineLength("line-length",120)));
        for(var test:report.tests()) System.out.println(test.id()+": "+test.status());
        for(var rule:report.style()) System.out.println(rule.id()+": "+(rule.passed()?"PASSED":"FAILED"));
        if(report.tests().stream().anyMatch(t->t.status()!=JavaAssessmentRunner.Status.PASSED))
            throw new IllegalStateException("Lab demo did not pass.");
    }
}
