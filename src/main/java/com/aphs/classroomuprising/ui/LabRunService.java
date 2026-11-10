package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.JavaAssessmentRunner;
import com.aphs.classroomuprising.StyleRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Runs Java Lab evaluations away from the UI thread.
 *
 * <ul>
 *   <li>Only one run at a time: {@link #start} returns false while a run is in progress.</li>
 *   <li>Every edit bumps the source revision. A result carries the revision it was run on,
 *       so the UI can tell when the editor no longer matches the tested source.</li>
 *   <li>Results are delivered through the UI executor (Platform::runLater in the game).</li>
 * </ul>
 * All public methods must be called from the UI thread.
 */
final class LabRunService implements AutoCloseable {
    /** The blocking evaluation; production uses {@link JavaAssessmentRunner#evaluate}. */
    @FunctionalInterface
    interface Evaluator {
        JavaAssessmentRunner.Report evaluate(String className, String source,
                List<JavaAssessmentRunner.TestCase> tests, List<StyleRule> rubric) throws Exception;
    }

    sealed interface RunResult {
        long revision();
        record Finished(long revision, JavaAssessmentRunner.Report report) implements RunResult {}
        record Failed(long revision, String reason) implements RunResult {}
    }

    private final Evaluator evaluator;
    private final Executor worker;
    private final Executor uiThread;
    private long revision;
    private boolean running;

    /**
     * @param worker   runs the blocking evaluation (one background thread in the game)
     * @param uiThread delivers results back on the UI thread
     */
    LabRunService(Evaluator evaluator, Executor worker, Executor uiThread) {
        this.evaluator = evaluator;
        this.worker = worker;
        this.uiThread = uiThread;
    }

    /** A service using this JVM's JDK and one daemon worker thread. */
    static LabRunService forLocalJdk(JavaAssessmentRunner.Limits limits, Executor uiThread) {
        Path jdk = Path.of(System.getProperty("java.home"));
        Evaluator evaluator = hasCompiler(jdk)
                ? new JavaAssessmentRunner(jdk, limits)::evaluate
                : (className, source, tests, rubric) -> {
                    throw new IllegalStateException("Java Lab needs a full JDK; javac was not found in " + jdk);
                };
        ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "java-lab-runner");
            thread.setDaemon(true);
            return thread;
        });
        return new LabRunService(evaluator, worker, uiThread);
    }

    static boolean hasCompiler(Path jdk) {
        return Files.isRegularFile(jdk.resolve("bin/javac")) || Files.isRegularFile(jdk.resolve("bin/javac.exe"));
    }

    long revision() { return revision; }

    boolean isRunning() { return running; }

    /** Call on every editor change. */
    void sourceEdited() { revision++; }

    /** True when the result was produced from the source currently in the editor. */
    boolean isCurrent(RunResult result) { return result.revision() == revision; }

    /**
     * Starts one evaluation of {@code source} as it is now.
     * @return false (and does nothing) when a run is already in progress
     */
    boolean start(String className, String source, List<JavaAssessmentRunner.TestCase> tests,
                  List<StyleRule> rubric, Consumer<RunResult> onResult) {
        if (running) return false;
        running = true;
        long runRevision = revision;
        List<JavaAssessmentRunner.TestCase> testCopy = List.copyOf(tests);
        List<StyleRule> rubricCopy = List.copyOf(rubric);
        worker.execute(() -> {
            RunResult result;
            try {
                result = new RunResult.Finished(runRevision, evaluator.evaluate(className, source, testCopy, rubricCopy));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = new RunResult.Failed(runRevision, "The run was interrupted.");
            } catch (Exception e) {
                result = new RunResult.Failed(runRevision, e.getMessage() == null ? e.toString() : e.getMessage());
            }
            RunResult delivered = result;
            uiThread.execute(() -> {
                running = false;
                onResult.accept(delivered);
            });
        });
        return true;
    }

    @Override
    public void close() {
        if (worker instanceof ExecutorService service) service.shutdownNow();
    }
}
