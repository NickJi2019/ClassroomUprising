package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPlan;
import com.aphs.classroomuprising.SchoolCalendar;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A window onto the 40-day calendar, centred on one day, with a caret marking today.
 * Finished days carry a "//" strike. {@link #advance} strikes out the day that just ended,
 * scrolls one day on (overshooting a little before it settles) and pops today's cell.
 */
public class CalendarStrip extends Pane {
    private static final double CELL = 70;
    private static final double GAP = 6;
    private static final double STEP = CELL + GAP;
    private static final int VISIBLE = 9;
    private static final double WIDTH = STEP * VISIBLE - GAP;
    private static final double TOP = 12;
    private static final double CELL_HEIGHT = 58;
    private static final double HEIGHT = TOP + CELL_HEIGHT;
    /** Empty cells before Day 1 and after Day 40, so the strip never shows a blank half. */
    private static final int PADDING = VISIBLE / 2;

    private final HBox row = new HBox(GAP);
    private final Group marks = new Group();
    private final List<VBox> cells = new ArrayList<>();

    public CalendarStrip() {
        getStyleClass().add("calendar-strip");
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setClip(new Rectangle(WIDTH, HEIGHT));
        for (int i = 0; i < PADDING; i++) row.getChildren().add(ghost());
        for (DayPlan plan : SchoolCalendar.days()) {
            Label number = new Label("%02d".formatted(plan.day()));
            number.getStyleClass().addAll("display", "cal-day");
            Label type = new Label(UiText.courseShort(plan.course()));
            type.getStyleClass().addAll("mono", "cal-type");
            VBox cell = new VBox(number, type);
            cell.getStyleClass().addAll("cal-cell", "cal-" + plan.course().name().toLowerCase(Locale.ROOT));
            cells.add(cell);
            row.getChildren().add(cell);
        }
        for (int i = 0; i < PADDING; i++) row.getChildren().add(ghost());
        row.setLayoutY(TOP);
        marks.setLayoutY(TOP);
        marks.translateXProperty().bind(row.translateXProperty());
        Polygon today = new Polygon(WIDTH / 2 - 8, 0, WIDTH / 2 + 8, 0, WIDTH / 2, 9);
        today.getStyleClass().add("cal-today-marker");
        getChildren().addAll(row, marks, today);
    }

    private static VBox ghost() {
        Label number = new Label("--");
        number.getStyleClass().addAll("display", "cal-day");
        VBox cell = new VBox(number);
        cell.getStyleClass().addAll("cal-cell", "cal-ghost");
        return cell;
    }

    /** Centres the strip on {@code day} without animation; earlier days are struck out. */
    public void focus(int day) {
        row.setTranslateX(offset(day));
        mark(day);
        marks.getChildren().clear();
        for (int d = 1; d < day; d++) marks.getChildren().addAll(strike(d));
    }

    /** Strikes out the day that ended, scrolls to the new one, and pops it when it arrives. */
    public Animation advance(int fromDay, int toDay) {
        focus(fromDay);
        ParallelTransition all = new ParallelTransition();
        if (fromDay >= 1 && toDay > fromDay) {
            List<Line> slashes = strike(fromDay);
            for (int i = 0; i < slashes.size(); i++) {
                Line slash = slashes.get(i);
                double endX = slash.getEndX(), endY = slash.getEndY();
                slash.setEndX(slash.getStartX());
                slash.setEndY(slash.getStartY());
                marks.getChildren().add(slash);
                Timeline draw = new Timeline(new KeyFrame(Duration.millis(140),
                        new KeyValue(slash.endXProperty(), endX, MotionTokens.OUT),
                        new KeyValue(slash.endYProperty(), endY, MotionTokens.OUT)));
                draw.setDelay(Duration.millis(60 + i * 70));
                all.getChildren().add(draw);
            }
        }
        double target = offset(toDay);
        Timeline scroll = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(row.translateXProperty(), offset(fromDay))),
                new KeyFrame(Duration.millis(200), new KeyValue(row.translateXProperty(), offset(fromDay))),
                new KeyFrame(Duration.millis(520), new KeyValue(row.translateXProperty(), target - 10, MotionTokens.OUT)),
                new KeyFrame(Duration.millis(680), new KeyValue(row.translateXProperty(), target, Interpolator.EASE_BOTH)));
        scroll.setOnFinished(e -> {
            mark(toDay);
            if (toDay >= 1 && toDay <= cells.size()) {
                ScaleTransition pop = new ScaleTransition(Duration.millis(110), cells.get(toDay - 1));
                pop.setToX(1.14);
                pop.setToY(1.14);
                pop.setAutoReverse(true);
                pop.setCycleCount(2);
                pop.play();
            }
        });
        all.getChildren().add(scroll);
        return all;
    }

    private static double offset(int day) {
        return WIDTH / 2 - ((day - 1 + PADDING) * STEP + CELL / 2);
    }

    /** A "//" across a day's cell, in the row's coordinates. */
    private static List<Line> strike(int day) {
        double left = (day - 1 + PADDING) * STEP;
        var slashes = new ArrayList<Line>();
        for (int i = 0; i < 2; i++) {
            double x = left + 24 + i * 16;
            Line slash = new Line(x, CELL_HEIGHT - 10, x + 14, 10);
            slash.getStyleClass().add("cal-strike");
            slashes.add(slash);
        }
        return slashes;
    }

    private void mark(int day) {
        for (int i = 0; i < cells.size(); i++) {
            var classes = cells.get(i).getStyleClass();
            classes.removeAll("cal-today", "cal-past");
            int cellDay = i + 1;
            if (cellDay == day) classes.add("cal-today");
            else if (cellDay < day) classes.add("cal-past");
        }
    }
}
