package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPlan;
import com.aphs.classroomuprising.SchoolCalendar;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A window onto the 40-day calendar, centred on one day. {@link #advance} scrolls it to the
 * next day and moves the highlight, for the day-change card.
 */
public class CalendarStrip extends Pane {
    private static final double CELL = 70;
    private static final double GAP = 6;
    private static final double STEP = CELL + GAP;
    private static final int VISIBLE = 9;
    private static final double WIDTH = STEP * VISIBLE - GAP;
    private static final double HEIGHT = 58;

    /** Empty cells before Day 1 and after Day 40, so the strip never shows a blank half. */
    private static final int PADDING = VISIBLE / 2;

    private final HBox row = new HBox(GAP);
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
        getChildren().add(row);
    }

    private static VBox ghost() {
        Label number = new Label("--");
        number.getStyleClass().addAll("display", "cal-day");
        VBox cell = new VBox(number);
        cell.getStyleClass().addAll("cal-cell", "cal-ghost");
        return cell;
    }

    /** Centres the strip on {@code day} without animation. */
    public void focus(int day) {
        row.setTranslateX(offset(day));
        mark(day);
    }

    /** Scrolls from one day to another and moves the highlight when it arrives. */
    public Animation advance(int fromDay, int toDay) {
        focus(fromDay);
        Timeline scroll = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(row.translateXProperty(), offset(fromDay))),
                new KeyFrame(Duration.millis(520), new KeyValue(row.translateXProperty(), offset(toDay),
                        Interpolator.SPLINE(0.5, 0, 0.2, 1))));
        scroll.setOnFinished(e -> mark(toDay));
        return scroll;
    }

    private static double offset(int day) {
        return WIDTH / 2 - ((day - 1 + PADDING) * STEP + CELL / 2);
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
