package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.CombatSession;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Sends submitted combat answers to the {@link CombatEvaluator} away from the UI thread.
 *
 * <ul>
 *   <li>One evaluation at a time: {@link #dispatch} returns false while one is running.</li>
 *   <li>Each result carries the ticket and the exact work it was produced from, so the screen
 *       records it with the same ticket, and a failed evaluation can be retried on identical work.</li>
 *   <li>{@link #abandon()} (leaving the screen, quitting the run) starts a new generation; results
 *       of older generations are dropped and never reach the screen. The core's ticket check is a
 *       second guard, not a replacement for this one.</li>
 * </ul>
 * All methods except the evaluation itself run on the UI thread.
 */
final class CombatEvaluations implements AutoCloseable {
    sealed interface Result {
        CombatSession.AnswerTicket ticket();
        CombatEvaluator.Work work();
        record Evaluated(CombatSession.AnswerTicket ticket, CombatEvaluator.Work work,
                         CombatEvaluator.Evaluation evaluation) implements Result {}
        record Failed(CombatSession.AnswerTicket ticket, CombatEvaluator.Work work, String reason) implements Result {}
    }

    private final CombatEvaluator evaluator;
    private final Executor worker;
    private final Executor uiThread;
    private long generation;
    private boolean busy;

    /**
     * @param worker   runs the blocking evaluation
     * @param uiThread delivers results back on the UI thread (Platform::runLater in the game)
     */
    CombatEvaluations(CombatEvaluator evaluator, Executor worker, Executor uiThread) {
        this.evaluator = Objects.requireNonNull(evaluator);
        this.worker = worker;
        this.uiThread = uiThread;
    }

    /** One daemon worker thread. */
    static CombatEvaluations background(CombatEvaluator evaluator, Executor uiThread) {
        ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "combat-evaluator");
            thread.setDaemon(true);
            return thread;
        });
        return new CombatEvaluations(evaluator, worker, uiThread);
    }

    boolean isBusy() { return busy; }

    /**
     * Evaluates {@code work} for {@code ticket}. The result is delivered once on the UI thread,
     * unless {@link #abandon()} was called first.
     * @return false (and does nothing) while another evaluation is running
     */
    boolean dispatch(CombatSession.AnswerTicket ticket, CombatEvaluator.Work work, Consumer<Result> onResult) {
        Objects.requireNonNull(ticket);
        Objects.requireNonNull(work);
        if (busy) return false;
        busy = true;
        long mine = generation;
        worker.execute(() -> {
            Result result;
            try {
                result = new Result.Evaluated(ticket, work, evaluator.evaluate(work));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = new Result.Failed(ticket, work, "The evaluation was interrupted.");
            } catch (Exception e) {
                result = new Result.Failed(ticket, work, e.getMessage() == null ? e.toString() : e.getMessage());
            }
            Result delivered = result;
            uiThread.execute(() -> {
                if (mine != generation) return; // abandoned: never touches the current run
                busy = false;
                onResult.accept(delivered);
            });
        });
        return true;
    }

    /** Forgets any evaluation in flight; its result will be dropped. */
    void abandon() {
        generation++;
        busy = false;
    }

    @Override
    public void close() {
        abandon();
        if (worker instanceof ExecutorService service) service.shutdownNow();
    }
}
