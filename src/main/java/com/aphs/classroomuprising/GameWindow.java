package com.aphs.classroomuprising;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Pure-JDK Swing presentation. All UI/state access is on the EDT; compilation runs in SwingWorker. */
public final class GameWindow extends JPanel {
    private static final Color INK = new Color(23, 37, 46), MUTED = new Color(91, 111, 119),
            PAPER = new Color(242, 244, 238), WHITE = new Color(255, 255, 251),
            MINT = new Color(165, 229, 196), GOLD = new Color(244, 194, 107);
    private DemoSession session = new DemoSession(false);
    private final Path saveFile;
    private final JTextArea journal = textArea("Welcome back. Start the semester, or use the labelled Lab shortcut.", 12, false);
    private final JPanel body = new JPanel(new BorderLayout(18, 0));
    private final JLabel dayTitle = label("", 18, INK);
    private JButton saveButton, loadButton, newButton, labButton, runButton, submitButton;
    private JTextArea editor, output;
    private JavaAssessmentRunner.Report lastReport;
    private String evaluatedSource;
    private boolean busy;

    public GameWindow(Path saveFile) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Create Swing UI on the EDT.");
        this.saveFile = saveFile.toAbsolutePath();
        setLayout(new BorderLayout(0, 16)); setBackground(PAPER); setBorder(new EmptyBorder(20, 24, 18, 24));
        setPreferredSize(new Dimension(1280, 860));
        add(header(), BorderLayout.NORTH); body.setOpaque(false); add(body, BorderLayout.CENTER);
        var bottom = new JPanel(new BorderLayout(12, 0)); bottom.setOpaque(false);
        var log = new JScrollPane(journal); log.setBorder(BorderFactory.createLineBorder(new Color(215, 224, 216)));
        log.setPreferredSize(new Dimension(650, 64)); bottom.add(log, BorderLayout.CENTER);
        var note = textArea("DEMO BUILD / DAYS 1-8\nValues, lessons & rubric are fixtures.\nHP is not your grade.", 11, false);
        note.setPreferredSize(new Dimension(245, 60)); bottom.add(note, BorderLayout.EAST); add(bottom, BorderLayout.SOUTH);
        render();
    }
    private JPanel header() {
        var panel = new JPanel(new BorderLayout(15, 0)); panel.setOpaque(false);
        var title = new JPanel(new GridLayout(2, 1)); title.setOpaque(false);
        title.add(label("CLASSROOM / UPRISING", 23, INK)); title.add(label("A second chance, one choice at a time.", 12, MUTED));
        panel.add(title, BorderLayout.WEST);
        var actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); actions.setOpaque(false);
        newButton = button("Restart", "restart", () -> reset(false));
        labButton = button("Lab shortcut · D5", "lab-shortcut", () -> reset(true));
        loadButton = button("Load", "load", () -> perform(() -> { session.load(saveFile); return "Loaded closed day. Choose Next day to continue.\n" + saveFile; }));
        saveButton = button("Save day", "save", () -> perform(() -> { session.save(saveFile); return "Day-end snapshot saved.\n" + saveFile; }));
        actions.add(newButton); actions.add(labButton); actions.add(loadButton); actions.add(saveButton);
        panel.add(actions, BorderLayout.EAST); return panel;
    }
    private void reset(boolean lab) {
        if (busy) return;
        if (session.state().openingCompleted() && JOptionPane.showConfirmDialog(this,
                "Replace the current demo run? Only the last saved day can be loaded.", "Restart demo",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        session = new DemoSession(lab); lastReport = null; evaluatedSource = null;
        log(lab ? "LAB SHORTCUT: a fresh Day 5 fixture, not a completed Days 1-4 playthrough." : "New tutorial run."); render();
    }
    private void render() {
        body.removeAll(); var s = session.state();
        dayTitle.setText(String.format(Locale.ROOT, "DAY %02d / 40", s.day()));
        body.add(sidebar(), BorderLayout.WEST); body.add(stats(), BorderLayout.EAST);
        var center = new JPanel(new BorderLayout(0, 14)); center.setOpaque(false);
        var heading = new JPanel(new BorderLayout()); heading.setOpaque(false);
        heading.add(dayTitle, BorderLayout.WEST);
        heading.add(label(s.isTutorial() ? "TUTORIAL · ALL PENALTIES WAIVED" : "CHAPTER " + s.currentCourse().unit() + " · DEMO", 11, MUTED), BorderLayout.EAST);
        center.add(heading, BorderLayout.NORTH);
        if (session.assessment() != null && session.assessment().type() == CourseType.LAB) center.add(lab(), BorderLayout.CENTER);
        else center.add(scene(), BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);
        saveButton.setEnabled(!busy && s.canSave()); loadButton.setEnabled(!busy);
        newButton.setEnabled(!busy); labButton.setEnabled(!busy);
        revalidate(); repaint();
    }
    private JPanel sidebar() {
        var panel = card(); panel.setPreferredSize(new Dimension(184, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(label("THE SEMESTER", 12, MUTED)); panel.add(Box.createVerticalStrut(16));
        var calendar = new JPanel(new GridLayout(8, 5, 5, 7)); calendar.setOpaque(false);
        var s = session.state();
        for (var day : SchoolCalendar.days()) {
            var l = label(Integer.toString(day.day()), 12, day.day() <= 8 ? INK : MUTED);
            l.setHorizontalAlignment(SwingConstants.CENTER); l.setOpaque(true);
            l.setBackground(day.day() == s.day() ? MINT : day.course().isMajor() ? GOLD : PAPER);
            l.setToolTipText("Day " + day.day() + ": " + day.course() + (day.day() > 8 ? " (outside this demo)" : "")); calendar.add(l);
        }
        calendar.setAlignmentX(Component.LEFT_ALIGNMENT); calendar.setPreferredSize(new Dimension(150, 230)); calendar.setMaximumSize(new Dimension(200, 230));
        panel.add(calendar); panel.add(Box.createVerticalStrut(12));
        panel.add(textArea("Mint = today\nGold = Midterm / Final\nPlayable slice: Days 1-8", 11, false));
        panel.add(Box.createVerticalStrut(24)); panel.add(label("TODAY'S ROUTE", 12, MUTED)); panel.add(Box.createVerticalStrut(12));
        for (var period : DayPeriod.values()) {
            if (period == DayPeriod.MORNING_BONUS && s.period() != period) continue;
            var row = label((s.period() == period ? "›  " : "   ") + periodName(period), 13, s.period() == period ? INK : MUTED);
            row.setBorder(new EmptyBorder(7, 0, 7, 0)); panel.add(row);
        }
        panel.add(Box.createVerticalGlue()); return panel;
    }
    private JPanel stats() {
        var panel = card(); panel.setPreferredSize(new Dimension(213, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); var s = session.state();
        panel.add(label("STUDENT RECORD", 12, MUTED)); panel.add(Box.createVerticalStrut(16));
        panel.add(label("You, again.", 22, INK)); panel.add(Box.createVerticalStrut(16));
        panel.add(label("HP       " + number(s.student().hp()), 19, INK));
        panel.add(label("SAN     " + number(s.effectiveSan()), 19, INK));
        panel.add(label("VIBE    " + number(s.student().codingVibe()), 19, INK));
        panel.add(Box.createVerticalStrut(10));
        panel.add(label("Today's XP rate  ×" + number(s.xpMultiplier()), 12, MUTED));
        panel.add(Box.createVerticalStrut(20)); panel.add(label("UNIT KNOWLEDGE", 11, MUTED));
        for (int i = 0; i < 6; i++) panel.add(label("Unit " + (i + 1) + "    " + number(s.student().unitKnowledge().get(i)), 13, INK));
        panel.add(Box.createVerticalStrut(20)); panel.add(label("GRADEBOOK", 11, MUTED)); panel.add(Box.createVerticalStrut(8));
        StringBuilder marks = new StringBuilder();
        for (var record : s.grades().records()) marks.append("D").append(record.submittedDay()).append(" ").append(record.type()).append("\n")
                .append(record.officialScore().isPresent() ? number(record.officialScore().getAsDouble()) + "% · official" : "Pending · returns D" + record.returnDay()).append("\n\n");
        if (marks.isEmpty()) marks.append("No submitted work yet.\nOrdinary marks return\nthree days later.");
        var gradeText = textArea(marks.toString(), 12, false); gradeText.setName("gradebook"); panel.add(gradeText);
        panel.add(Box.createVerticalStrut(10)); panel.add(textArea("Overall grade: not calculated\nHP / grade mapping is TBD.", 11, false));
        panel.add(Box.createVerticalGlue()); return panel;
    }
    private JPanel scene() {
        var panel = new JPanel(new BorderLayout(0, 16)); panel.setOpaque(false); var s = session.state();
        var art = new CampusScene(s.period(), !s.openingCompleted());
        art.setPreferredSize(new Dimension(600, 220)); panel.add(art, BorderLayout.NORTH);
        var story = card(); story.setLayout(new BorderLayout(0, 14));
        String title, text;
        var choices = new JPanel(new GridLayout(0, 1, 0, 8)); choices.setOpaque(false);
        if (!s.openingCompleted()) {
            title = "Last semester didn't go to plan.";
            text = "FLASHBACK / Mr. Reward's Final\n\nThe last question. The last turn. You lost.\nThis semester, you are taking ICS4U again.\n\nStart with a class, then decide how to spend your afternoon. The first two days teach the loop without penalties.\n\nThis demo uses a text opening; the Final battle is still a separate design task.";
            addAction(choices, "Start the semester", "opening");
        } else if (session.assessment() != null) {
            title = "Unit 1 / knowledge check"; text = session.assessment().questions().getFirst().prompt()
                    + "\n\nDEMO: one question, 100 or 0. The official result is delayed three days. This is not the final course rubric.";
            for (String answer : new String[]{"3", "3.5", "4"}) choices.add(button(answer, "answer-" + answer, () -> perform(() -> session.answerQuiz(answer))));
        } else switch (s.period()) {
            case ICS_CLASS -> {
                title = "ICS4U / " + (s.currentCourse().unit() == 0 ? "Orientation" : "Lesson " + s.day()); text = session.lesson().text();
                String interaction = session.lesson().interaction() == ContentCatalog.Interaction.ASK_TEACHER ? "Ask a question" : "Raise your hand";
                addAction(choices, interaction + "   ·   +4 Vibe, +2 Knowledge, -2 SAN*", "participate");
                addAction(choices, "Listen quietly   ·   +2 Vibe, +1 Knowledge", "listen");
            }
            case OTHER_CLASSES -> {
                title = "Other classes / keep your balance";
                text = "The next teacher starts the lesson. You can focus on this class or quietly review ICS.\n\nOther courses do not have separate grade or knowledge stats in this game.\n* Tutorial negatives are waived; today's XP multiplier still applies.";
                addAction(choices, "Focus on this class   ·   no stat change", "other-focus");
                addAction(choices, "Review ICS   ·   +2 Vibe, +1 Unit 1, -2 SAN*", "other-revise");
            }
            case DAY_END -> {
                title = s.day() == DemoScenario.LAST_DAY ? "Demo complete / a little more prepared" : "Day complete / close the notebook";
                text = "This is the save boundary. Use Save day to keep this snapshot. Loading returns to this closed day, not the following morning.\n\n"
                        + (s.hasMorningBonus() ? "Early sleep earned tomorrow's half-slot and a +3 SAN demo bonus.\n\n" : "")
                        + (s.day() == DemoScenario.LAST_DAY ? "You have reached the end of the playable slice. Day 5 Lab marks are now visible. Days 9-40 remain on the design calendar, not in this GUI demo."
                        : "Next day advances the calendar, applies dated effects and releases due marks.");
                if (s.day() < DemoScenario.LAST_DAY) addAction(choices, "Next day →", "next");
            }
            default -> {
                title = "Student Hub / " + periodName(s.period());
                text = s.period() == DayPeriod.MORNING_BONUS
                        ? "You woke up early. You have HALF a slot: all numerical activity effects are halved."
                        : "One choice uses this time slot. Study a little more, or take a break.";
                text += "\n\nDemo activities: study gives +4 Vibe, +2 Unit 1 Knowledge and -2 SAN; relax gives +4 SAN. Positive XP uses today's multiplier.";
                if (s.period() == DayPeriod.LATE_NIGHT) text += "\n\nStaying up applies -4 SAN and ×0.5 XP tomorrow only. During Days 1-2 this is warning-only.";
                if (s.period() == DayPeriod.AFTER_SCHOOL) text += "\n\nSleep now to end today, then gain +3 SAN and a half-slot tomorrow. Other sleep timings are still TBD.";
                addAction(choices, "Study Java   ·   " + (s.period() == DayPeriod.MORNING_BONUS ? "half slot" : "1 slot"), "study");
                addAction(choices, "Take a break   ·   " + (s.period() == DayPeriod.MORNING_BONUS ? "half slot" : "1 slot"), "relax");
                if (s.period() == DayPeriod.AFTER_SCHOOL) addAction(choices, "Sleep immediately   ·   tomorrow: half-slot + SAN", "sleep");
            }
        }
        story.add(label(title, 20, INK), BorderLayout.NORTH);
        var description = textArea(text, 14, false); description.setName("story");
        var scroll = new JScrollPane(description); scroll.setBorder(null); scroll.setOpaque(false); scroll.getViewport().setOpaque(false);
        story.add(scroll, BorderLayout.CENTER); story.add(choices, BorderLayout.SOUTH); panel.add(story, BorderLayout.CENTER); return panel;
    }
    private JPanel lab() {
        var panel = card(); panel.setLayout(new BorderLayout(0, 12));
        var top = new JPanel(new BorderLayout(0, 7)); top.setOpaque(false);
        top.add(label("Lab 01 / make the tests green", 22, INK), BorderLayout.NORTH);
        top.add(textArea("Write a complete public class Sum. Read two integers; print their sum + newline.\nDemo rubric: 80% test cases + 20% line length (≤100). Compile failure = 0.\nLocal JDK execution: use your own/trusted code; this is not an OS sandbox.", 12, false), BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);
        editor = textArea(DemoScenario.LAB_STARTER, 14, true); editor.setName("source-editor");
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14)); editor.setTabSize(4);
        editor.setLineWrap(false); editor.setOpaque(true); editor.setBackground(INK); editor.setForeground(PAPER); editor.setCaretColor(MINT);
        editor.setBorder(new EmptyBorder(14, 14, 14, 14));
        output = textArea("Run tests to see compiler output, case results and the style check.\nThe starter program has a deliberate bug: try it, then fix the operator.", 12, false);
        output.setName("lab-output"); output.setOpaque(true); output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); output.setBackground(PAPER);
        var split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(editor), new JScrollPane(output));
        split.setResizeWeight(.68); split.setDividerLocation(270); split.setBorder(null); panel.add(split, BorderLayout.CENTER);
        lastReport = null; evaluatedSource = null;
        editor.getDocument().addDocumentListener(new DocumentListener() {
            private void invalidateResult() { lastReport = null; evaluatedSource = null; if (submitButton != null) submitButton.setEnabled(false); }
            public void insertUpdate(DocumentEvent e) { invalidateResult(); }
            public void removeUpdate(DocumentEvent e) { invalidateResult(); }
            public void changedUpdate(DocumentEvent e) { invalidateResult(); }
        });
        var actions = new JPanel(new GridLayout(1, 2, 10, 0)); actions.setOpaque(false);
        runButton = button("Run tests", "run-lab", this::runLab);
        submitButton = button("Submit evaluated code", "submit-lab", () -> {
            if (lastReport == null || !editor.getText().equals(evaluatedSource)) { log("Run tests again after editing."); return; }
            perform(() -> session.submitLab(lastReport));
        });
        submitButton.setEnabled(false); actions.add(runButton); actions.add(submitButton); panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }
    private void runLab() {
        if (busy) return;
        String source = editor.getText(); busy = true; lastReport = null;
        editor.setEditable(false); runButton.setEnabled(false); submitButton.setEnabled(false);
        newButton.setEnabled(false); labButton.setEnabled(false); loadButton.setEnabled(false); saveButton.setEnabled(false);
        output.setText("Compiling and running three cases…\nYou can still scroll and resize this window.");
        new SwingWorker<JavaAssessmentRunner.Report, Void>() {
            protected JavaAssessmentRunner.Report doInBackground() throws Exception { return DemoScenario.evaluateLab(source); }
            protected void done() {
                try {
                    lastReport = get(); evaluatedSource = source;
                    var text = new StringBuilder("javac exit: " + lastReport.compilation().exitCode() + "\n");
                    if (!lastReport.compilation().stderr().isBlank()) text.append(lastReport.compilation().stderr()).append('\n');
                    for (var test : lastReport.tests()) {
                        text.append(test.id()).append(": ").append(test.status()).append('\n');
                        if (test.status() == JavaAssessmentRunner.Status.WRONG_OUTPUT)
                            text.append("  stdout: ").append(test.process().stdout().replace("\n", "\\n")).append('\n');
                        if (!test.process().stderr().isBlank() && test.status() != JavaAssessmentRunner.Status.COMPILE_ERROR)
                            text.append(test.process().stderr()).append('\n');
                    }
                    for (var rule : lastReport.style()) text.append(rule.id()).append(": ").append(rule.passed() ? "PASSED" : "FAILED").append('\n');
                    text.append("\nTechnical checks only. Official mark is returned on Day 8 after submission.");
                    output.setText(text.toString()); output.setCaretPosition(0); submitButton.setEnabled(true);
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); output.setText("Lab evaluation interrupted. Run again."); }
                catch (ExecutionException ex) { output.setText("Lab execution failed: " + ex.getCause().getMessage() + "\nCheck that a full JDK 21+ is installed. Your work has not been submitted."); }
                finally { busy = false; editor.setEditable(true); runButton.setEnabled(true); newButton.setEnabled(true); labButton.setEnabled(true); loadButton.setEnabled(true); }
            }
        }.execute();
    }
    @FunctionalInterface private interface Operation { String run() throws Exception; }
    private void perform(Operation operation) {
        if (busy) return;
        try { log(operation.run()); render(); }
        catch (Exception e) { log("Action not applied: " + e.getMessage()); }
    }
    private void addAction(JPanel choices, String text, String action) { choices.add(button(text, action, () -> perform(() -> session.act(action)))); }
    private void log(String text) { journal.setText(text); journal.setCaretPosition(0); }
    private static JPanel card() {
        var p = new JPanel(); p.setBackground(WHITE); p.setBorder(new EmptyBorder(18, 18, 18, 18)); return p;
    }
    private static JLabel label(String text, int size, Color color) {
        var l = new JLabel(text); l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size)); l.setForeground(color); l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }
    private static JTextArea textArea(String text, int size, boolean editable) {
        var a = new JTextArea(text); a.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, size)); a.setForeground(INK);
        a.setAlignmentX(Component.LEFT_ALIGNMENT); a.setEditable(editable); a.setLineWrap(true); a.setWrapStyleWord(true); a.setOpaque(false); return a;
    }
    private static JButton button(String text, String name, Runnable action) {
        var b = new JButton(text); b.setName(name); b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        b.setForeground(INK); b.setBackground(MINT); b.setOpaque(true); b.setBorder(new EmptyBorder(12, 12, 12, 12));
        b.setFocusPainted(true); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run()); return b;
    }
    private static String number(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private static String periodName(DayPeriod period) {
        return switch (period) {
            case MORNING_BONUS -> "Morning · half slot"; case ICS_CLASS -> "ICS4U"; case OTHER_CLASSES -> "Other classes";
            case AFTER_SCHOOL -> "After school"; case EVENING -> "Evening"; case LATE_NIGHT -> "Late night"; case DAY_END -> "Day end";
        };
    }
    // Package-visible hooks exercise the real components in headless tests, without adding game cheats.
    DemoSession session() { return session; }
    boolean busy() { return busy; }
    public static JFrame open(Path save) {
        var frame = new JFrame("Classroom Uprising · Java Demo"); var ui = new GameWindow(save);
        frame.setContentPane(ui); frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (ui.busy) { ui.log("The Lab is still running. Close after the bounded run completes."); return; }
                if (!ui.session.state().openingCompleted() || JOptionPane.showConfirmDialog(frame,
                        "Close the demo? Only a manually saved day-end snapshot is kept.", "Close demo", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
                    frame.dispose();
            }
        });
        frame.setMinimumSize(new Dimension(1100, 760)); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true); return frame;
    }
    /** Offscreen renderer used for reproducible visual QA; not a claim of desktop interaction. */
    static void snapshot(JComponent component, Path path) throws Exception {
        component.setSize(component.getPreferredSize()); layoutTree(component);
        var image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics(); component.printAll(g); g.dispose();
        Files.createDirectories(path.toAbsolutePath().getParent()); ImageIO.write(image, "png", path.toFile());
    }
    private static void layoutTree(Container c) { c.doLayout(); for (var child : c.getComponents()) if (child instanceof Container nested) layoutTree(nested); }

    /** A small Java2D set: no downloaded images, game engine or asset pipeline. */
    private static final class CampusScene extends JPanel {
        private final DayPeriod period; private final boolean opening;
        CampusScene(DayPeriod period, boolean opening) { this.period = period; this.opening = opening; }
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics); var g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = getWidth() / 680.0; g.scale(scale, getHeight() / 220.0);
            boolean hub = period.isHubActivityPeriod(); boolean night = period == DayPeriod.EVENING || period == DayPeriod.LATE_NIGHT;
            g.setColor(night ? new Color(38, 57, 74) : new Color(213, 226, 216)); g.fillRect(0, 0, 680, 220);
            g.setColor(night ? new Color(80, 108, 137) : new Color(241, 226, 177)); g.fillRect(28, 24, 144, 124);
            g.setColor(night ? PAPER : new Color(255, 249, 222)); g.fillOval(104, 40, 36, 36);
            g.setColor(new Color(150, 173, 169)); g.fillRect(28, 116, 144, 32);
            g.setColor(WHITE); g.fillRect(96, 24, 5, 124); g.fillRect(28, 84, 144, 5);
            g.setColor(new Color(124, 145, 131)); g.fillRect(212, 24, 420, 135);
            g.setColor(new Color(28, 66, 60)); g.fillRect(220, 32, 404, 119);
            g.setColor(MINT); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11)); g.drawString(hub ? "STUDENT HUB / YOUR TIME" : "ICS4U / ROOM 204", 242, 57);
            g.setColor(WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            g.drawString(opening ? "Try again. Learn differently." : hub ? "A little balance goes a long way." : "Small choices. Real progress.", 242, 95);
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); g.drawString(hub ? "study();   rest();   repeat();" : "int nextChance = thisSemester + 1;", 242, 126);
            g.setColor(new Color(185, 168, 139)); g.fillRect(0, 173, 680, 47);
            for (int i = 0; i < 4; i++) {
                int x = 36 + i * 166;
                g.setColor(INK); g.fillRoundRect(x + 18, 179, 8, 41, 3, 3); g.fillRoundRect(x + 107, 179, 8, 41, 3, 3);
                g.setColor(new Color(224, 195, 143)); g.fillRoundRect(x, 169, 134, 15, 5, 5);
                g.setColor(WHITE); g.fillRoundRect(x + 38, 162, 43, 8, 2, 2);
            }
            g.dispose();
        }
    }
}
