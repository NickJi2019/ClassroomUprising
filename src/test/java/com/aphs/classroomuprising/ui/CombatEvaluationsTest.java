package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CombatSession;
import com.aphs.classroomuprising.CourseType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Evaluation dispatch: one at a time, results tied to their ticket and work, abandoned results dropped. */
class CombatEvaluationsTest {
    private static final CombatSession.AnswerTicket TICKET =
            new CombatSession.AnswerTicket(UUID.randomUUID(), UUID.randomUUID(), "q1");
    private static final CombatEvaluator.Work WORK =
            new CombatEvaluator.Work("lab-1", CourseType.LAB, "q1", "Prompt", "answer text");
    /** Fixture verdict, not a grading rule. */
    private static final CombatEvaluator.Evaluation FIXTURE = new CombatEvaluator.Evaluation(1, "fixture");

    /** Holds background work until the test releases it, to control the order of events. */
    private static final class HeldWorker implements Executor {
        final List<Runnable> tasks = new ArrayList<>();
        @Override public void execute(Runnable task) { tasks.add(task); }
        void release() { new ArrayList<>(tasks).forEach(Runnable::run); tasks.clear(); }
    }

    @Test void theResultCarriesItsTicketAndTheExactWork() {
        var worker = new HeldWorker();
        var service = new CombatEvaluations(work -> FIXTURE, worker, Runnable::run);
        var result = new AtomicReference<CombatEvaluations.Result>();
        assertTrue(service.dispatch(TICKET, WORK, result::set));
        assertTrue(service.isBusy());
        worker.release();
        var evaluated = assertInstanceOf(CombatEvaluations.Result.Evaluated.class, result.get());
        assertEquals(TICKET, evaluated.ticket());
        assertEquals(WORK, evaluated.work());
        assertEquals(FIXTURE, evaluated.evaluation());
        assertFalse(service.isBusy());
    }

    @Test void onlyOneEvaluationRunsAtATime() {
        var worker = new HeldWorker();
        var calls = new AtomicInteger();
        var service = new CombatEvaluations(work -> { calls.incrementAndGet(); return FIXTURE; }, worker, Runnable::run);
        assertTrue(service.dispatch(TICKET, WORK, r -> {}));
        assertFalse(service.dispatch(TICKET, WORK, r -> {}), "a second dispatch is refused while busy");
        worker.release();
        assertEquals(1, calls.get());
    }

    @Test void abandonedResultsNeverReachTheScreen() {
        var worker = new HeldWorker();
        var service = new CombatEvaluations(work -> FIXTURE, worker, Runnable::run);
        var delivered = new AtomicInteger();
        service.dispatch(TICKET, WORK, r -> delivered.incrementAndGet());
        service.abandon();
        assertFalse(service.isBusy());
        worker.release();
        assertEquals(0, delivered.get(), "a late worker result after leaving the run is dropped");

        assertTrue(service.dispatch(TICKET, WORK, r -> delivered.incrementAndGet()));
        worker.release();
        assertEquals(1, delivered.get());
    }

    @Test void anEvaluatorFailureIsReportedNotTurnedIntoAMark() {
        var worker = new HeldWorker();
        var service = new CombatEvaluations(work -> { throw new IllegalStateException("runner offline"); },
                worker, Runnable::run);
        var result = new AtomicReference<CombatEvaluations.Result>();
        service.dispatch(TICKET, WORK, result::set);
        worker.release();
        var failed = assertInstanceOf(CombatEvaluations.Result.Failed.class, result.get());
        assertEquals("runner offline", failed.reason());
        assertEquals(TICKET, failed.ticket());
        assertEquals(WORK, failed.work(), "the captured work is kept for a retry on identical input");
        assertFalse(service.isBusy());
    }

    @Test void resultsAreDeliveredThroughTheUiExecutor() {
        var worker = new HeldWorker();
        var ui = new HeldWorker();
        var service = new CombatEvaluations(work -> FIXTURE, worker, ui);
        var delivered = new AtomicInteger();
        service.dispatch(TICKET, WORK, r -> delivered.incrementAndGet());
        worker.release();
        assertEquals(0, delivered.get());
        assertTrue(service.isBusy(), "still busy until the UI thread takes the result");
        ui.release();
        assertEquals(1, delivered.get());
    }

    @Test void noEvaluatorShipsSoEachKindNamesItsRule() {
        assertEquals("T06", CombatEvaluator.pendingRule(CourseType.LAB));
        assertEquals("T07", CombatEvaluator.pendingRule(CourseType.UNIT_TEST));
        assertEquals("T07", CombatEvaluator.pendingRule(CourseType.FINAL));
    }
}
