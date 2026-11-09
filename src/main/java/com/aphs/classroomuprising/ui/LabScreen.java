package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.JavaAssessmentRunner;
import com.aphs.classroomuprising.StyleRule;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.Consumer;

/**
 * Java Lab: a plain, readable editor with real compilation and test feedback.
 * Decoration stays on the frame; the editor is flat and monospaced.
 *
 * <p>Runs go through {@link LabRunService}: the UI never blocks, RUN is locked while a run is
 * in progress, and results produced from older source are marked stale.</p>
 */
final class LabScreen extends Screen {
    /** What the backend must supply for one Lab (not provided by the Demo profile yet). */
    record LabTask(String tag, String title, String statement, String className, String starterSource,
                   List<JavaAssessmentRunner.TestCase> tests, List<StyleRule> rubric,
                   JavaAssessmentRunner.Limits limits) {
        LabTask {
            tests = List.copyOf(tests);
            rubric = List.copyOf(rubric);
        }
    }

    private static final PseudoClass STALE = PseudoClass.getPseudoClass("stale");

    private final LabTask task;
    private final Consumer<LabRunService.RunResult> onSubmit;
    private final LabRunService runs;
    private LabRunService.RunResult last;

    @FXML private Label labTag;
    @FXML private Label labTitle;
    @FXML private Label statement;
    @FXML private HBox editorBox;
    @FXML private Pane gutterClip;
    @FXML private Label gutter;
    @FXML private TextArea editor;
    @FXML private TextArea console;
    @FXML private Label staleBanner;
    @FXML private VBox testRows;
    @FXML private VBox styleRows;
    @FXML private VBox notice;
    @FXML private ChoiceList actions;

    /**
     * @param onSubmit receives the latest current result when the player submits; turning
     *                 evidence into a mark is a pending rule (T06) and is not done here
     */
    LabScreen(AppContext ctx, LabTask task, Consumer<LabRunService.RunResult> onSubmit) {
        super(ctx);
        this.task = task;
        this.onSubmit = onSubmit;
        this.runs = LabRunService.forLocalJdk(task.limits(), Platform::runLater);
    }

    @Override
    protected Parent build() {
        StackPane root = Views.load("lab.fxml", this);
        labTag.setText(task.tag());
        labTitle.setText("// " + task.title());
        statement.setText(task.statement());
        HBox.setHgrow(editor, Priority.ALWAYS);

        editor.setText(task.starterSource());
        editor.textProperty().addListener((obs, old, now) -> {
            runs.sourceEdited();
            updateGutter();
            markStale();
        });
        editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB && !e.isShiftDown() && !e.isControlDown()) {
                editor.replaceSelection("    ");
                e.consume();
            }
        });
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(gutterClip.widthProperty());
        clip.heightProperty().bind(gutterClip.heightProperty());
        gutterClip.setClip(clip);
        gutter.translateYProperty().bind(editor.scrollTopProperty().negate());
        updateGutter();

        for (JavaAssessmentRunner.TestCase test : task.tests()) testRows.getChildren().add(row(test.id(), "not run", "result-idle"));
        if (task.rubric().isEmpty()) styleRows.getChildren().add(row("no style rules", "", "result-idle"));
        for (StyleRule rule : task.rubric()) styleRows.getChildren().add(row(rule.id(), "not run", "result-idle"));
        console.setText("$ ready. Press RUN TESTS to compile " + task.className() + ".java");
        showActions(false);
        return root;
    }

    @Override
    protected boolean allowsPause() { return true; }

    @Override
    protected void focus() { editor.requestFocus(); }

    @Override
    protected void onHidden() { runs.close(); }

    private void showActions(boolean running) {
        actions.setChoices(List.of(
                ChoiceList.Choice.of(running ? "RUNNING…" : "RUN TESTS", running ? "Compiling and testing" : "Compile and run every test", this::run)
                        .enabled(!running),
                ChoiceList.Choice.of("SUBMIT", "Uses the latest run of this exact source", this::submit).tone("cyan")
                        .enabled(!running)));
    }

    private void run() {
        String source = editor.getText();
        boolean started = runs.start(task.className(), source, task.tests(), task.rubric(), this::show);
        if (!started) {
            actions.unlock();
            return;
        }
        notice(notice, null);
        console.setText("$ javac " + task.className() + ".java …");
        showActions(true);
    }

    private void show(LabRunService.RunResult result) {
        last = result;
        showActions(false);
        switch (result) {
            case LabRunService.RunResult.Failed failed -> {
                console.setText("$ could not run: " + failed.reason());
                notice(notice, Diagnostic.of(new Outcome.Rejected(failed.reason())));
            }
            case LabRunService.RunResult.Finished finished -> render(finished.report());
        }
        markStale();
    }

    private void render(JavaAssessmentRunner.Report report) {
        testRows.getChildren().clear();
        for (JavaAssessmentRunner.TestResult test : report.tests()) {
            boolean pass = test.status() == JavaAssessmentRunner.Status.PASSED;
            testRows.getChildren().add(row(test.id(), test.status().name().replace('_', ' '), pass ? "result-pass" : "result-fail"));
        }
        styleRows.getChildren().clear();
        for (StyleRule.Finding finding : report.style())
            styleRows.getChildren().add(row(finding.explanation(), finding.passed() ? "OK" : "FIX", finding.passed() ? "result-pass" : "result-fail"));
        if (report.style().isEmpty()) styleRows.getChildren().add(row("no style rules", "", "result-idle"));

        var compile = report.compilation();
        StringBuilder text = new StringBuilder("$ javac " + task.className() + ".java\n");
        if (compile.exitCode() != 0 || compile.timedOut() || compile.outputLimit()) {
            text.append(compile.timedOut() ? "compiler timed out\n" : compile.outputLimit() ? "compiler output limit\n" : "");
            text.append(compile.stderr().isBlank() ? compile.stdout() : compile.stderr());
        } else {
            long passed = report.tests().stream().filter(t -> t.status() == JavaAssessmentRunner.Status.PASSED).count();
            text.append("compiled\n").append(passed).append(" / ").append(report.tests().size()).append(" tests passed\n");
            report.tests().stream().filter(t -> t.status() != JavaAssessmentRunner.Status.PASSED).findFirst()
                    .ifPresent(t -> text.append(firstFailure(t)));
        }
        console.setText(text.toString());
    }

    private String firstFailure(JavaAssessmentRunner.TestResult result) {
        String expected = task.tests().stream().filter(t -> t.id().equals(result.id()))
                .map(JavaAssessmentRunner.TestCase::expectedOutput).findFirst().orElse("");
        var process = result.process();
        return switch (result.status()) {
            case WRONG_OUTPUT -> "\n" + result.id() + ": expected\n" + expected + "\nbut printed\n" + process.stdout();
            case RUNTIME_ERROR -> "\n" + result.id() + ": exception\n" + process.stderr();
            case TIMEOUT -> "\n" + result.id() + ": took too long";
            case OUTPUT_LIMIT -> "\n" + result.id() + ": printed too much";
            default -> "";
        };
    }

    /** Dims results that no longer describe the source in the editor. */
    private void markStale() {
        boolean stale = last != null && !runs.isCurrent(last);
        staleBanner.setVisible(stale);
        staleBanner.setManaged(stale);
        testRows.pseudoClassStateChanged(STALE, stale);
        styleRows.pseudoClassStateChanged(STALE, stale);
    }

    private void submit() {
        if (last == null || !runs.isCurrent(last) || !(last instanceof LabRunService.RunResult.Finished)) {
            notice(notice, Diagnostic.warning("warning: run the tests on this exact source first",
                    "A result from older source is not evidence for the current code."));
            actions.unlock();
            return;
        }
        onSubmit.accept(last);
        actions.unlock();
    }

    private void updateGutter() {
        int lines = (int) editor.getText().chars().filter(c -> c == '\n').count() + 1;
        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lines; i++) numbers.append(i).append('\n');
        gutter.setText(numbers.toString());
    }

    private static HBox row(String name, String status, String tone) {
        Label left = new Label(name);
        left.getStyleClass().addAll("mono", "result-name");
        left.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(left, Priority.ALWAYS);
        Label right = new Label(status);
        right.getStyleClass().addAll("mono", "result-status", tone);
        HBox row = new HBox(10, left, right);
        row.getStyleClass().add("result-row");
        return row;
    }
}
