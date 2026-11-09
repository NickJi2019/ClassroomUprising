package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.AssessmentRecord;
import com.aphs.classroomuprising.CourseType;
import com.aphs.classroomuprising.DayPlan;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.SchoolCalendar;
import com.aphs.classroomuprising.TimedEffect;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Read-only pages of the ESC menu, built from one snapshot. */
final class PausePanels {
    private PausePanels() {}

    static Node status(GameState s) {
        VBox page = page("STATUS", s.name());
        GridPane grid = new GridPane();
        grid.setHgap(28);
        grid.setVgap(10);
        int row = 0;
        row = stat(grid, row, "HP", StatDelta.number(s.student().hp()), "stat-hp");
        double effects = s.effectiveSan() - s.student().san();
        row = stat(grid, row, "SAN", StatDelta.number(s.effectiveSan())
                + (Math.abs(effects) > 1e-9 ? "   (base " + StatDelta.number(s.student().san()) + ", effects " + StatDelta.signed(effects) + ")" : ""), "stat-san");
        row = stat(grid, row, "CODING VIBE", StatDelta.number(s.student().codingVibe()), "stat-vibe");
        row = stat(grid, row, "XP GAIN TODAY", "×" + StatDelta.number(s.xpMultiplier()), "stat-vibe");
        stat(grid, row, "ABSENCES", s.grades().unexcusedAbsences() + "  (first two are free)", "stat-plain");
        page.getChildren().add(grid);

        page.getChildren().add(section("KNOWLEDGE BY UNIT"));
        HBox units = new HBox(10);
        for (int i = 0; i < 6; i++) {
            Label name = label("U" + (i + 1), "display", "unit-name");
            Label value = label(StatDelta.number(s.student().unitKnowledge().get(i)), "display", "unit-value");
            VBox cell = new VBox(2, name, value);
            cell.getStyleClass().add("unit-cell");
            cell.setAlignment(Pos.CENTER);
            units.getChildren().add(cell);
        }
        page.getChildren().add(units);

        page.getChildren().add(section("EFFECTS"));
        boolean any = false;
        for (TimedEffect effect : s.effects()) {
            any = true;
            String when = effect.activeOn(s.day()) ? "today"
                    : effect.lastDay() == Integer.MAX_VALUE ? "until results return" : "Day " + effect.firstDay();
            page.getChildren().add(label(UiText.effect(effect) + " — " + when + ":  SAN " + StatDelta.signed(effect.sanDelta())
                    + ",  XP gain ×" + StatDelta.number(effect.xpMultiplier()), "mono", "panel-line"));
        }
        if (s.hasMorningBonus()) {
            any = true;
            page.getChildren().add(label("Slept early — a ½ bonus slot tomorrow morning", "mono", "panel-line"));
        }
        if (!any) page.getChildren().add(label("No active effects.", "mono", "panel-muted"));
        return page;
    }

    static Node calendar(GameState s) {
        VBox page = page("CALENDAR", "40 days · Midterm Day 20 · Final Day 40");
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        for (DayPlan plan : SchoolCalendar.days()) {
            int index = plan.day() - 1;
            boolean makeup = s.makeups().containsKey(plan.day());
            Label number = label("%02d".formatted(plan.day()), "display", "cal-day");
            Label type = label(makeup ? "MAKEUP" : abbreviation(plan.course()), "mono", "cal-type");
            VBox cell = new VBox(0, number, type);
            cell.getStyleClass().addAll("cal-cell", "cal-" + plan.course().name().toLowerCase(java.util.Locale.ROOT));
            if (plan.day() < s.day()) cell.getStyleClass().add("cal-past");
            if (plan.day() == s.day()) cell.getStyleClass().add("cal-today");
            grid.add(cell, index % 8, index / 8);
        }
        page.getChildren().add(grid);
        page.getChildren().add(label("L lesson  ·  LAB  ·  TEST unit test  ·  REV review  ·  MID / FIN exams",
                "mono", "panel-muted"));
        return page;
    }

    static Node grades(GameState s) {
        VBox page = page("GRADES", "Ordinary results return 3 days after submission");
        var records = s.grades().records();
        if (records.isEmpty()) page.getChildren().add(label("No submitted assessments yet.", "mono", "panel-muted"));
        for (AssessmentRecord record : records) {
            String result = record.officialScore().isPresent()
                    ? StatDelta.number(record.officialScore().getAsDouble()) + "%"
                    : "returns Day " + record.returnDay();
            String estimate = s.estimates().containsKey(record.id())
                    ? "   estimate " + StatDelta.number(s.estimates().get(record.id())) + "%" : "";
            page.getChildren().add(label(UiText.course(record.type()) + "  Day " + record.submittedDay()
                    + (record.makeup() ? " (makeup)" : "") + "   " + result + estimate, "mono", "panel-line"));
        }
        int deduction = s.grades().absenceDeductionPoints();
        page.getChildren().add(section("ATTENDANCE"));
        page.getChildren().add(label(s.grades().unexcusedAbsences() + " unexcused absences"
                + (deduction > 0 ? "  ·  −" + deduction + " overall points" : "  ·  no deduction"), "mono", "panel-line"));
        page.getChildren().add(label("Final percentage is not shown: the grading weights are still pending (T02).",
                "mono", "panel-muted"));
        return page;
    }

    private static String abbreviation(CourseType type) {
        return switch (type) {
            case LESSON -> "L";
            case LAB -> "LAB";
            case UNIT_TEST -> "TEST";
            case REVIEW -> "REV";
            case MIDTERM -> "MID";
            case FINAL -> "FIN";
        };
    }

    private static VBox page(String title, String subtitle) {
        VBox page = new VBox(12, label(title, "display", "panel-title"), label(subtitle, "mono", "panel-muted"));
        page.getStyleClass().add("pause-page");
        return page;
    }

    private static Label section(String text) { return label(text, "display", "section-label"); }

    private static int stat(GridPane grid, int row, String name, String value, String tone) {
        grid.add(label(name, "display", "panel-stat-name", tone), 0, row);
        grid.add(label(value, "display", "panel-stat-value"), 1, row);
        return row + 1;
    }

    private static Label label(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        label.setWrapText(true);
        return label;
    }
}
