package com.aphs.classroomuprising.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;

/**
 * Placeholder character silhouettes until real art exists. Each figure has one accent shape
 * so characters stay recognisable: Mr. Reward's glasses bar, the student's headphones.
 */
final class Portraits {
    private Portraits() {}

    static Node reward(double height) {
        Circle head = new Circle(100, 82, 46);
        head.getStyleClass().add("portrait-shape");
        SVGPath body = new SVGPath();
        body.setContent("M18 300 Q28 152 100 140 Q172 152 182 300Z");
        body.getStyleClass().add("portrait-shape");
        Polygon glasses = new Polygon(68, 70, 150, 70, 146, 86, 64, 86);
        glasses.getStyleClass().add("portrait-accent");
        return scaled(new Group(body, head, glasses), height);
    }

    static Node student(double height) {
        Circle head = new Circle(100, 86, 44);
        head.getStyleClass().add("portrait-shape");
        SVGPath body = new SVGPath();
        body.setContent("M24 300 Q34 160 100 146 Q166 160 176 300Z");
        body.getStyleClass().add("portrait-shape");
        SVGPath headphones = new SVGPath();
        headphones.setContent("M52 92 Q52 34 100 34 Q148 34 148 92 L140 92 Q140 44 100 44 Q60 44 60 92Z");
        headphones.getStyleClass().add("portrait-accent-cyan");
        return scaled(new Group(body, head, headphones), height);
    }

    private static Node scaled(Group figure, double height) {
        double factor = height / 300.0;
        figure.getTransforms().add(new Scale(factor, factor, 0, 0));
        return new Group(figure);
    }
}
