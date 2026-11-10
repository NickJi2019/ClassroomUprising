package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.StudentState;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * What the XP total is made of: Coding Vibe, then each unit's knowledge, from one snapshot.
 * Shown when the player opens the XP total (HUD or Esc menu); it only reads.
 */
final class XpBreakdown extends VBox {
    private final Label vibe = new Label();
    private final List<Label> units = new ArrayList<>();

    XpBreakdown() {
        super(8);
        getStyleClass().add("xp-breakdown");
        Label title = new Label("XP  =  CODING VIBE  +  KNOWLEDGE");
        title.getStyleClass().addAll("display", "xp-breakdown-title");

        Label vibeName = new Label("CODING VIBE");
        vibeName.getStyleClass().addAll("display", "xp-part-name");
        vibe.getStyleClass().addAll("display", "xp-part-value");
        HBox vibeRow = new HBox(12, vibeName, vibe);
        vibeRow.setAlignment(Pos.BASELINE_LEFT);

        Label knowledge = new Label("KNOWLEDGE BY UNIT");
        knowledge.getStyleClass().addAll("display", "xp-part-name");
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        for (int i = 0; i < 6; i++) {
            Label name = new Label("U" + (i + 1));
            name.getStyleClass().addAll("display", "unit-name");
            Label value = new Label();
            value.getStyleClass().addAll("display", "unit-value");
            units.add(value);
            VBox cell = new VBox(0, name, value);
            cell.getStyleClass().add("unit-cell");
            cell.setAlignment(Pos.CENTER);
            grid.add(cell, i % 3, i / 3);
        }
        getChildren().addAll(title, vibeRow, knowledge, grid);
    }

    void show(StudentState student) {
        List<Stats.Part> parts = Stats.xpParts(student);
        vibe.setText(StatDelta.number(parts.get(0).value()));
        for (int i = 0; i < units.size(); i++) units.get(i).setText(StatDelta.number(parts.get(i + 1).value()));
    }

    /** The parts as shown, for checks: "CODING VIBE 4", "U1 2", ... */
    List<String> texts() {
        var out = new ArrayList<String>();
        out.add("CODING VIBE " + vibe.getText());
        for (int i = 0; i < units.size(); i++) out.add("U" + (i + 1) + " " + units.get(i).getText());
        return out;
    }
}
