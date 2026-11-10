package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ui.scene.AssetStore;
import javafx.animation.FadeTransition;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;

/**
 * The log: every line this scene's dialogue box has shown, newest at the bottom, to read again
 * at any time (also from the choice menu after a lecture). Selecting a line replays it with the
 * lecture slide that was up when it was said; Z or Enter shows that slide large. Up/Down or the
 * mouse wheel move through the lines; Esc (or H, Backspace) goes back: the large slide, then the log. While it is open the
 * scene waits. It only reads what was shown; it changes nothing in the game.
 */
final class DialogueLog extends StackPane {
    /** Lines visible at once; the list scrolls to keep the selected one in view. */
    static final int ROWS = 6;
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final double PREVIEW_W = 464, PREVIEW_H = 261;

    private final AssetStore assets;
    private final VBox rows = new VBox(10);
    private final Label above = new Label();
    private final Label below = new Label();
    private final Label count = new Label();
    private final Label slideName = new Label();
    private final KeyHints keys = new KeyHints();
    private VBox side;
    private boolean slides;
    private final ImageView preview = new ImageView();
    private final Label noSlide = new Label("NO SLIDE ON THIS LINE");
    private final StackPane large = new StackPane();
    private final ImageView largeView = new ImageView();
    private List<DialogueHistory.Entry> entries = List.of();
    private int selected;
    private Runnable onClose = () -> {};
    private boolean closing;

    DialogueLog(AssetStore assets) {
        this.assets = assets;
        getStyleClass().add("dialogue-log");
        setVisible(false);
        setFocusTraversable(true);

        Label title = new Label("LOG");
        title.getStyleClass().addAll("display", "log-title");
        count.getStyleClass().addAll("mono", "log-count");
        VBox head = new VBox(0, title, count);

        above.getStyleClass().addAll("mono", "log-more");
        below.getStyleClass().addAll("mono", "log-more");
        VBox list = new VBox(10, above, rows, below);
        list.setPrefWidth(660);

        Label slideKind = new Label("SLIDE");
        slideKind.getStyleClass().addAll("display", "slide-kind");
        slideName.getStyleClass().addAll("display", "slide-number");
        HBox tagText = new HBox(10, slideKind, slideName);
        tagText.setAlignment(Pos.CENTER_LEFT);
        Slant tag = new Slant(tagText);
        tag.getStyleClass().add("slide-tag");
        tag.setMaxWidth(USE_PREF_SIZE);
        preview.setFitWidth(PREVIEW_W);
        preview.setFitHeight(PREVIEW_H);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        noSlide.getStyleClass().addAll("mono", "log-no-slide");
        StackPane screen = new StackPane(preview, noSlide);
        screen.getStyleClass().add("log-slide");
        screen.setMinSize(PREVIEW_W, PREVIEW_H);
        screen.setMaxSize(PREVIEW_W, PREVIEW_H);
        Label zoomHint = new Label("Z / ENTER  VIEW LARGE");
        zoomHint.getStyleClass().addAll("mono", "log-hint");
        side = new VBox(10, tag, screen, zoomHint);

        AnchorPane layout = new AnchorPane(head, list, side, keys);
        layout.setPickOnBounds(false);
        AnchorPane.setLeftAnchor(head, 60.0);
        AnchorPane.setTopAnchor(head, 34.0);
        AnchorPane.setLeftAnchor(list, 60.0);
        AnchorPane.setTopAnchor(list, 150.0);
        AnchorPane.setLeftAnchor(side, 760.0);
        AnchorPane.setTopAnchor(side, 156.0);
        AnchorPane.setRightAnchor(keys, 18.0);
        AnchorPane.setBottomAnchor(keys, 4.0);

        largeView.setFitWidth(1120);
        largeView.setFitHeight(630);
        largeView.setPreserveRatio(true);
        largeView.setSmooth(true);
        large.getChildren().add(largeView);
        large.getStyleClass().add("log-large");
        large.setVisible(false);
        large.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> showLarge(false));

        Region scrim = new Region();
        scrim.getStyleClass().add("log-scrim");
        getChildren().addAll(scrim, layout, large);

        addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
        addEventHandler(ScrollEvent.SCROLL, e -> {
            if (large.isVisible()) return;
            select(selected + (e.getDeltaY() > 0 ? -1 : 1));
            e.consume();
        });
        screen.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> toggleLarge());
    }

    /** Whether the log has the keys (it is shown and not on its way out). */
    boolean isOpen() { return isVisible() && !closing; }

    /** Opens on the newest line. {@code whenClosed} runs once the log is closed (e.g. to give focus back). */
    void open(List<DialogueHistory.Entry> lines, Runnable whenClosed) {
        if (lines.isEmpty() || isVisible()) return;
        entries = List.copyOf(lines);
        onClose = whenClosed;
        selected = entries.size() - 1;
        // Scenes without lecture slides (the opening) get no slide panel at all.
        slides = entries.stream().anyMatch(entry -> entry.slide() != null);
        side.setVisible(slides);
        showLarge(false);
        count.setText(entries.size() + (entries.size() == 1 ? " LINE" : " LINES") + "  ·  THIS SCENE");
        render();
        setOpacity(0);
        setVisible(true);
        FadeTransition in = new FadeTransition(MotionTokens.QUICK, this);
        in.setToValue(1);
        in.play();
        Motion.slideIn(rows, 0);
        Sfx.MENU_OPEN.play(this);
        requestFocus();
    }

    /** Closes the log; the scene gets its keys back at once while the log fades out. */
    void close() {
        if (!isOpen()) return;
        closing = true;
        Sfx.MENU_CLOSE.play(this);
        setMouseTransparent(true);
        FadeTransition out = new FadeTransition(Duration.millis(140), this);
        out.setToValue(0);
        out.setOnFinished(e -> {
            setVisible(false);
            setMouseTransparent(false);
            closing = false;
        });
        out.play();
        Runnable then = onClose;
        onClose = () -> {};
        then.run();
    }

    /** Esc: closes the large slide first, then the log. False when the log is not open. */
    boolean back() {
        if (!isOpen()) return false;
        if (large.isVisible()) showLarge(false);
        else close();
        return true;
    }

    private void onKey(KeyEvent e) {
        switch (e.getCode()) {
            case UP, W -> select(selected - 1);
            case DOWN, S -> select(selected + 1);
            case PAGE_UP -> select(selected - ROWS);
            case PAGE_DOWN -> select(selected + ROWS);
            case HOME -> select(0);
            case END -> select(entries.size() - 1);
            case Z, ENTER, SPACE -> toggleLarge();
            case ESCAPE, H, BACK_SPACE -> back();
            default -> { }
        }
        e.consume();
    }

    private void select(int index) {
        int next = Math.max(0, Math.min(entries.size() - 1, index));
        if (next == selected) return;
        selected = next;
        showLarge(false);
        Sfx.CURSOR.play(this);
        render();
    }

    private void toggleLarge() {
        String slide = entries.isEmpty() ? null : entries.get(selected).slide();
        if (large.isVisible() || slide == null || assets == null) {
            showLarge(false);
            return;
        }
        largeView.setImage(assets.slide(slide));
        showLarge(true);
    }

    private void showLarge(boolean on) {
        large.setVisible(on);
        if (on) keys.set(KeyHints.Hint.of("Z / ESC", "CLOSE SLIDE"));
        else if (slides) keys.set(KeyHints.Hint.of("↑↓", "MOVE"), KeyHints.Hint.of("Z", "SLIDE"), KeyHints.Hint.of("ESC", "CLOSE LOG"));
        else keys.set(KeyHints.Hint.of("↑↓", "MOVE"), KeyHints.Hint.of("ESC", "CLOSE LOG"));
    }

    /** The key hints shown now, for checks. */
    List<String> keyHints() { return keys.texts(); }

    private void render() {
        int first = DialogueHistory.firstVisible(entries.size(), selected, ROWS);
        int last = Math.min(entries.size(), first + ROWS);
        rows.getChildren().clear();
        for (int i = first; i < last; i++) rows.getChildren().add(row(i));
        above.setText(first > 0 ? "▲  " + first + " EARLIER" : " ");
        below.setText(last < entries.size() ? "▼  " + (entries.size() - last) + " LATER" : " ");

        DialogueHistory.Entry entry = entries.get(selected);
        boolean hasSlide = entry.slide() != null && assets != null;
        slideName.setText(entry.slide() == null ? "—" : entry.slide().toUpperCase(Locale.ROOT));
        preview.setImage(hasSlide ? assets.slide(entry.slide()) : null);
        noSlide.setVisible(!hasSlide);
    }

    private HBox row(int index) {
        DialogueScript.Line line = entries.get(index).line();
        Label who = new Label(line.speaker().toUpperCase(Locale.ROOT));
        who.getStyleClass().addAll("display", "log-speaker");
        who.setMinWidth(USE_PREF_SIZE);
        Label said = new Label(line.text());
        said.getStyleClass().add("log-text");
        said.setWrapText(true);
        said.setMaxWidth(500);
        HBox row = new HBox(16, who, said);
        row.getStyleClass().add("log-row");
        row.setPadding(new Insets(6, 12, 6, 12));
        row.pseudoClassStateChanged(SELECTED, index == selected);
        row.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> select(index));
        return row;
    }
}
