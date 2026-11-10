package com.aphs.classroomuprising.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Compiler-style notice cards. Pending rules and refusals are shown openly with their rule ID
 * instead of being hidden or replaced with an invented success.
 */
final class Diagnostic {
    private Diagnostic() {}

    static Node of(Outcome outcome) {
        return switch (outcome) {
            case Outcome.Pending pending -> card("warning[" + pending.ruleId() + "]: " + pending.detail(),
                    "Not decided by the team yet. Nothing was changed.", "diag-warning");
            case Outcome.Rejected rejected -> card("error: " + rejected.reason(), "Nothing was changed.", "diag-error");
            case Outcome.Done done -> card("ok: " + done.message(), null, "diag-info");
        };
    }

    static Node info(String head, String sub) { return card(head, sub, "diag-info"); }

    static Node warning(String head, String sub) { return card(head, sub, "diag-warning"); }

    static Node error(String head, String sub) { return card(head, sub, "diag-error"); }

    private static Node card(String head, String sub, String tone) {
        Label headLabel = new Label(head);
        headLabel.getStyleClass().addAll("mono", "diag-head");
        headLabel.setWrapText(true);
        VBox card = new VBox(headLabel);
        card.getStyleClass().addAll("diag", tone);
        if (sub != null) {
            Label subLabel = new Label(sub);
            subLabel.getStyleClass().addAll("mono", "diag-sub");
            subLabel.setWrapText(true);
            card.getChildren().add(subLabel);
        }
        Motion.slideIn(card, 0);
        return card;
    }
}
