package com.aphs.classroomuprising;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

/** Executable integration check of real buttons and the background Java runner; no JUnit runtime needed. */
public final class GuiSmoke {
    private static GameWindow ui;
    private static JFrame frame;
    private static Path output;
    private GuiSmoke() {}
    public static void main(String[] args) throws Exception {
        output = Path.of(args[0]); Files.createDirectories(output);
        boolean nativeFrame = args.length > 1 && args[1].equals("native");
        try {
            edt(() -> {
                if (nativeFrame) { frame = GameWindow.open(output.resolve("smoke-save.properties")); ui = (GameWindow) frame.getContentPane(); check(frame.isShowing(), "native frame showing"); }
                else ui = new GameWindow(output.resolve("smoke-save.properties"));
                GameWindow.snapshot(ui, output.resolve("01-opening.png"));
                click("opening"); GameWindow.snapshot(ui, output.resolve("02-classroom.png"));
                click("participate"); click("other-focus"); GameWindow.snapshot(ui, output.resolve("03-hub.png"));
                click("sleep"); check(button("save").isEnabled(), "day-end save enabled"); click("save");
                check(Files.isRegularFile(output.resolve("smoke-save.properties")), "real save file");
                click("next"); check(ui.session().state().period() == DayPeriod.MORNING_BONUS, "half-slot");
                click("load"); check(ui.session().state().day() == 1 && ui.session().state().period() == DayPeriod.DAY_END, "load same closed day");
                if (frame != null) frame.dispose();
                ui = new GameWindow(output.resolve("lab-save.properties"));
                if (nativeFrame) { frame = new JFrame("Classroom Uprising · automated Lab check"); frame.setContentPane(ui); frame.pack(); frame.setVisible(true); }
                click("lab-shortcut"); check(!button("submit-lab").isEnabled(), "no untested submission"); click("run-lab");
                check(ui.busy() && !button("load").isEnabled(), "navigation locked during worker");
            });
            waitForLab();
            edt(() -> {
                check(text("lab-output").contains("WRONG_OUTPUT"), "starter exposes bug");
                ((JTextArea)find(ui, "source-editor")).setText("public class Sum { syntax error }");
                check(!button("submit-lab").isEnabled(), "editing invalidates evidence"); click("run-lab");
            });
            waitForLab();
            edt(() -> {
                check(text("lab-output").contains("COMPILE_ERROR"), "compile diagnostics shown");
                ((JTextArea)find(ui, "source-editor")).setText(DemoScenario.LAB_STARTER.replace("a - b", "a + b"));
                click("run-lab");
            });
            waitForLab();
            edt(() -> {
                check(text("lab-output").contains("positive: PASSED") && text("lab-output").contains("negative: PASSED")
                        && text("lab-output").contains("zero: PASSED"), "fixed source passes all cases");
                check(text("lab-output").contains("line-length <= 100: PASSED"), "style finding shown");
                GameWindow.snapshot(ui, output.resolve("04-lab.png")); click("submit-lab");
                check(ui.session().state().grades().record("day-5-lab").officialScore().isEmpty(), "ordinary result withheld");
                click("other-focus"); click("sleep"); click("next"); click("relax"); // D6
                click("participate"); click("other-focus"); click("sleep"); click("next"); click("relax"); // D7
                click("answer-3"); click("other-focus"); click("sleep"); click("next"); click("relax"); // D8
                check(ui.session().state().day() == 8, "Day 8 reached");
                check(text("gradebook").contains("100.0% · official"), "official Lab mark visible after three days");
                GameWindow.snapshot(ui, output.resolve("05-result.png"));
                click("participate"); click("other-focus"); click("sleep");
                check(find(ui, "next") == null, "no fake Day 9 content"); click("save");
            });
            System.out.println("GUI_SMOKE PASS: opening, classroom, Hub, half-slot, day-end save/load, async wrong-output/compile-error/pass, stale-result invalidation, submission, Day-8 official mark, demo boundary.");
            System.out.println("FRAME_MODE: " + (nativeFrame ? "native JFrame shown; real Swing button actions invoked on EDT" : "headless component rendering; real Swing button actions invoked on EDT"));
            System.out.println("SCREENSHOTS: 01-opening.png, 02-classroom.png, 03-hub.png, 04-lab.png, 05-result.png");
        } finally { edt(() -> { if (frame != null) frame.dispose(); }); }
    }
    private static void waitForLab() throws Exception {
        long deadline = System.nanoTime() + 45_000_000_000L;
        while (System.nanoTime() < deadline) {
            var active = new AtomicReference<Boolean>(); edt(() -> active.set(ui.busy()));
            if (!active.get()) return;
            Thread.sleep(40);
        }
        throw new AssertionError("Lab worker exceeded 45 seconds");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void click(String name) { var b = button(name); check(b.isEnabled(), "enabled: " + name); b.doClick(0); }
    private static JButton button(String name) { return (JButton) java.util.Objects.requireNonNull(find(ui, name), name); }
    private static String text(String name) { return ((JTextArea)java.util.Objects.requireNonNull(find(ui, name), name)).getText(); }
    private static Component find(Container root, String name) {
        for (var c : root.getComponents()) {
            if (name.equals(c.getName())) return c;
            if (c instanceof Container nested) { var found = find(nested, name); if (found != null) return found; }
        }
        return null;
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void edt(Action action) throws Exception {
        var error = new AtomicReference<Throwable>();
        SwingUtilities.invokeAndWait(() -> { try { action.run(); } catch (Throwable e) { error.set(e); } });
        if (error.get() != null) throw new AssertionError(error.get());
    }
}
