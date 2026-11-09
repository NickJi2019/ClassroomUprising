package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.JavaAssessmentRunner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Lab run control: one run at a time, stale detection after edits, errors reported. */
class LabRunServiceTest {
    private static final List<JavaAssessmentRunner.TestCase> TESTS =
            List.of(new JavaAssessmentRunner.TestCase("echo", "hi", "hi"));
    private static final JavaAssessmentRunner.Report EMPTY_REPORT = new JavaAssessmentRunner.Report(
            new JavaAssessmentRunner.ProcessEvidence(0, false, false, "", ""), List.of(), List.of());

    /** Holds background work until the test releases it, to control the order of events. */
    private static final class HeldWorker implements Executor {
        final List<Runnable> tasks = new ArrayList<>();
        @Override public void execute(Runnable task) { tasks.add(task); }
        void release() { new ArrayList<>(tasks).forEach(Runnable::run); tasks.clear(); }
    }

    @Test void resultOfUnchangedSourceIsCurrent() {
        var worker = new HeldWorker();
        var service = new LabRunService((c, s, t, r) -> EMPTY_REPORT, worker, Runnable::run);
        var result = new AtomicReference<LabRunService.RunResult>();
        assertTrue(service.start("Main", "class Main {}", TESTS, List.of(), result::set));
        worker.release();
        assertTrue(service.isCurrent(result.get()));
        assertFalse(service.isRunning());
    }

    @Test void editingDuringARunMakesThatResultStale() {
        var worker = new HeldWorker();
        var service = new LabRunService((c, s, t, r) -> EMPTY_REPORT, worker, Runnable::run);
        var result = new AtomicReference<LabRunService.RunResult>();
        service.start("Main", "class Main {}", TESTS, List.of(), result::set);
        service.sourceEdited();
        worker.release();
        assertNotNull(result.get());
        assertFalse(service.isCurrent(result.get()), "a result for older source must not describe the editor");
    }

    @Test void editingAfterARunMakesTheShownResultStale() {
        var worker = new HeldWorker();
        var service = new LabRunService((c, s, t, r) -> EMPTY_REPORT, worker, Runnable::run);
        var result = new AtomicReference<LabRunService.RunResult>();
        service.start("Main", "class Main {}", TESTS, List.of(), result::set);
        worker.release();
        service.sourceEdited();
        assertFalse(service.isCurrent(result.get()));
    }

    @Test void aSecondRunIsRefusedWhileOneIsInProgress() {
        var worker = new HeldWorker();
        var calls = new AtomicInteger();
        var service = new LabRunService((c, s, t, r) -> { calls.incrementAndGet(); return EMPTY_REPORT; }, worker, Runnable::run);
        assertTrue(service.start("Main", "a", TESTS, List.of(), r -> {}));
        assertFalse(service.start("Main", "a", TESTS, List.of(), r -> {}));
        worker.release();
        assertEquals(1, calls.get());
        assertTrue(service.start("Main", "a", TESTS, List.of(), r -> {}), "a new run is allowed after delivery");
    }

    @Test void evaluatorFailureIsDeliveredAndReleasesTheLock() {
        var worker = new HeldWorker();
        var service = new LabRunService((c, s, t, r) -> { throw new IOException("disk full"); }, worker, Runnable::run);
        var result = new AtomicReference<LabRunService.RunResult>();
        service.start("Main", "a", TESTS, List.of(), result::set);
        worker.release();
        var failed = assertInstanceOf(LabRunService.RunResult.Failed.class, result.get());
        assertEquals("disk full", failed.reason());
        assertFalse(service.isRunning());
    }

    @Test void realJdkCompilesAndRunsOnTheBackgroundThread() throws Exception {
        assertTrue(LabRunService.hasCompiler(Path.of(System.getProperty("java.home"))), "tests need a full JDK");
        var limits = new JavaAssessmentRunner.Limits(Duration.ofSeconds(30), Duration.ofSeconds(10), 64_000);
        var done = new CountDownLatch(1);
        var thread = new AtomicReference<String>();
        var result = new AtomicReference<LabRunService.RunResult>();
        String source = "public class Echo { public static void main(String[] a) { "
                + "System.out.print(new java.util.Scanner(System.in).next()); } }";
        try (var service = LabRunService.forLocalJdk(limits, task -> {
            thread.set(Thread.currentThread().getName());
            task.run();
            done.countDown();
        })) {
            assertTrue(service.start("Echo", source, TESTS, List.of(), result::set));
            assertTrue(done.await(60, TimeUnit.SECONDS));
        }
        assertEquals("java-lab-runner", thread.get());
        var finished = assertInstanceOf(LabRunService.RunResult.Finished.class, result.get());
        assertEquals(JavaAssessmentRunner.Status.PASSED, finished.report().tests().get(0).status());
    }

    @Test void realJdkReportsCompileErrors() throws Exception {
        var limits = new JavaAssessmentRunner.Limits(Duration.ofSeconds(30), Duration.ofSeconds(10), 64_000);
        var done = new CountDownLatch(1);
        var result = new AtomicReference<LabRunService.RunResult>();
        try (var service = LabRunService.forLocalJdk(limits, task -> { task.run(); done.countDown(); })) {
            service.start("Broken", "public class Broken { int x = ; }", TESTS, List.of(), result::set);
            assertTrue(done.await(60, TimeUnit.SECONDS));
        }
        var finished = assertInstanceOf(LabRunService.RunResult.Finished.class, result.get());
        assertEquals(JavaAssessmentRunner.Status.COMPILE_ERROR, finished.report().tests().get(0).status());
        assertFalse(finished.report().compilation().stderr().isBlank());
    }
}
