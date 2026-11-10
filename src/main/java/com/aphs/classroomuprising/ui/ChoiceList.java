package com.aphs.classroomuprising.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.beans.InvalidationListener;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

/**
 * The game's one menu control: a staggered, slightly crooked list of options with a single
 * selection cursor that travels between them (mouse, or Up/Down / W/S, Enter / Space).
 *
 * <ul>
 *   <li>The cursor is one slanted slab plus a lagging accent shard. It overshoots, pulls back
 *       and settles when it moves, breathes while idle and twitches now and then.</li>
 *   <li>Only the part of the text under the slab turns dark: dark copies of every option sit
 *       above the slab, clipped to the slab's exact shape.</li>
 *   <li>Neighbours step aside to make room for the selected option; unselected options catch a
 *       stepped shimmer.</li>
 *   <li>Click protection: after one activation the list locks, so double clicks and key repeats
 *       cannot run a second action. The screen calls {@link #unlock()} only when the action was
 *       refused and the same options remain valid.</li>
 * </ul>
 */
public class ChoiceList extends VBox {
    public record Choice(String label, String hint, String tone, boolean enabled, Runnable action) {
        public static Choice of(String label, String hint, Runnable action) {
            return new Choice(label, hint, "normal", true, action);
        }
        public Choice tone(String value) { return new Choice(label, hint, value, enabled, action); }
        public Choice enabled(boolean value) { return new Choice(label, hint, tone, value, action); }
    }

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass UNAVAILABLE = PseudoClass.getPseudoClass("unavailable");
    /** Each option sits at its own small angle, so the list reads as hand-set rather than ruled. */
    private static final double[] TILTS = {-1.4, 0.9, -0.6, 1.6, -1.1, 0.5, -1.8, 1.2};

    /** One option: its row, plus the dark copy that shows through the cursor. */
    private static final class Row {
        Choice choice;
        final Slant box;
        final VBox content;
        final Label label;
        final Label ghostLabel;
        final Pane ghost = new Pane();
        final VBox ghostContent;
        final Translate room = new Translate();
        Label hint;
        Label ghostHint;

        Row(Choice choice, Slant box, VBox content, Label label, VBox ghostContent, Label ghostLabel) {
            this.choice = choice;
            this.box = box;
            this.content = content;
            this.label = label;
            this.ghostLabel = ghostLabel;
            this.ghostContent = ghostContent;
            box.getTransforms().add(room);
            Translate ghostRoom = new Translate();
            ghostRoom.yProperty().bind(room.yProperty());
            ghost.getTransforms().add(ghostRoom);
            ghost.translateXProperty().bind(box.translateXProperty());
            ghost.translateYProperty().bind(box.translateYProperty());
            ghost.rotateProperty().bind(box.rotateProperty());
            ghost.opacityProperty().bind(box.opacityProperty());
            ghostLabel.scaleXProperty().bind(label.scaleXProperty());
            ghostLabel.scaleYProperty().bind(label.scaleYProperty());
            ghost.getChildren().add(ghostContent);
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final Pane overlay = new Pane();
    private final Polygon shard = new Polygon();
    private final Polygon slab = new Polygon();
    private final Polygon slabClip = new Polygon();
    private final Group ghosts = new Group();
    private final Pane echoes = new Pane();

    private final DoubleProperty cx = new SimpleDoubleProperty(), cy = new SimpleDoubleProperty(),
            cw = new SimpleDoubleProperty(), ch = new SimpleDoubleProperty(), ca = new SimpleDoubleProperty();
    private final DoubleProperty sx = new SimpleDoubleProperty(), sy = new SimpleDoubleProperty(),
            sw = new SimpleDoubleProperty(), sh = new SimpleDoubleProperty(), sa = new SimpleDoubleProperty();
    private final DoubleProperty breath = new SimpleDoubleProperty(1);
    private final DoubleProperty twitch = new SimpleDoubleProperty(0);

    private final Timeline breathing;
    private final Timeline shimmer;
    private final PauseTransition twitchTimer = new PauseTransition();
    private final Random random = new Random(11);
    private Animation travel;
    private Animation shardTravel;
    private boolean placed;
    private boolean spawnPending;
    private long shimmerTick;

    private int selected = -1;
    private boolean locked;
    private double stagger = 22;
    private IntConsumer onSelect = index -> {};

    public ChoiceList() {
        getStyleClass().add("choice-list");
        setFocusTraversable(true);
        setSpacing(10);
        addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);

        shard.getStyleClass().add("cursor-shard");
        slab.getStyleClass().add("cursor-slab");
        ghosts.setClip(slabClip);
        overlay.setManaged(false);
        overlay.setMouseTransparent(true);
        overlay.setVisible(false);
        overlay.getChildren().addAll(shard, slab, ghosts, echoes);
        getChildren().add(overlay);

        InvalidationListener cursor = o -> drawCursor();
        for (DoubleProperty p : List.of(cx, cy, cw, ch, ca, breath, twitch)) p.addListener(cursor);
        InvalidationListener accent = o -> drawShard();
        for (DoubleProperty p : List.of(sx, sy, sw, sh, sa, breath)) p.addListener(accent);

        breathing = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(breath, 1.0)),
                new KeyFrame(Duration.millis(1300), new KeyValue(breath, 1.03, MotionTokens.IN_OUT)));
        breathing.setAutoReverse(true);
        breathing.setCycleCount(Animation.INDEFINITE);
        shimmer = new Timeline(new KeyFrame(Duration.millis(1000.0 / MotionTokens.STEP_RATE), e -> shimmerStep()));
        shimmer.setCycleCount(Animation.INDEFINITE);
        twitchTimer.setOnFinished(e -> twitchNow());

        sceneProperty().addListener((obs, old, now) -> {
            if (now == null) {
                breathing.stop();
                shimmer.stop();
                twitchTimer.stop();
            } else {
                breathing.play();
                shimmer.play();
                scheduleTwitch();
            }
        });
    }

    public double getStagger() { return stagger; }

    /** Horizontal step between rows, giving the diagonal list shape. */
    public void setStagger(double stagger) { this.stagger = stagger; }

    public void setOnSelect(IntConsumer listener) { onSelect = listener; }

    public void setChoices(List<Choice> list) {
        if (travel != null) travel.stop();
        if (shardTravel != null) shardTravel.stop();
        rows.clear();
        ghosts.getChildren().clear();
        getChildren().removeIf(node -> node != overlay);
        selected = -1;
        locked = false;
        placed = false;
        spawnPending = false;
        overlay.setVisible(false);

        boolean rightAligned = getAlignment().getHpos() == HPos.RIGHT;
        boolean large = getStyleClass().contains("large");
        for (int i = 0; i < list.size(); i++) {
            Choice choice = list.get(i);
            Label label = label(choice.label(), "choice-label");
            VBox content = new VBox(label);
            content.getStyleClass().add("choice-text");
            Label ghostLabel = label(choice.label(), "choice-label");
            VBox ghostContent = new VBox(ghostLabel);
            ghostContent.getStyleClass().addAll("choice-text", "choice-ghost");
            Label hint = null, ghostHint = null;
            if (hasHint(choice)) {
                hint = label(choice.hint(), "choice-hint");
                ghostHint = label(choice.hint(), "choice-hint");
                content.getChildren().add(hint);
                ghostContent.getChildren().add(ghostHint);
            }
            Slant box = new Slant(content);
            box.getStyleClass().addAll("choice", "tone-" + choice.tone());
            box.pseudoClassStateChanged(UNAVAILABLE, !choice.enabled());
            box.setMaxWidth(USE_PREF_SIZE);
            box.setRotate(TILTS[i % TILTS.length] * (large ? 1.3 : 0.8));
            double step = i * stagger;
            VBox.setMargin(box, rightAligned ? new Insets(0, step, 0, 0) : new Insets(0, 0, 0, step));

            Row row = new Row(choice, box, content, label, ghostContent, ghostLabel);
            row.hint = hint;
            row.ghostHint = ghostHint;
            int index = i;
            box.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> pick(index));
            box.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                select(index);
                activate();
            });
            rows.add(row);
            getChildren().add(box);
            ghosts.getChildren().add(row.ghost);
            Motion.slideIn(box, 40 + i * MotionTokens.STAGGER.toMillis());
        }
        getChildren().remove(overlay);
        getChildren().add(overlay);
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).choice.enabled()) { select(i); break; }
        }
    }

    /**
     * Changes the options' text, tone and availability in place, so the cursor stays where it
     * is (for lists whose entries change state, such as combat targets). Falls back to
     * {@link #setChoices} when the number of options or the presence of hints differs.
     * The list is unlocked afterwards. If the selected option became unavailable the cursor
     * moves to the first available one, or hides when none is left.
     */
    public void update(List<Choice> list) {
        boolean sameShape = list.size() == rows.size();
        for (int i = 0; sameShape && i < list.size(); i++) sameShape = hasHint(list.get(i)) == (rows.get(i).hint != null);
        if (!sameShape || rows.isEmpty()) {
            setChoices(list);
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Row row = rows.get(i);
            Choice choice = list.get(i);
            row.choice = choice;
            row.label.setText(choice.label());
            row.ghostLabel.setText(choice.label());
            if (row.hint != null) {
                row.hint.setText(choice.hint());
                row.ghostHint.setText(choice.hint());
            }
            row.box.getStyleClass().removeIf(c -> c.startsWith("tone-"));
            row.box.getStyleClass().add("tone-" + choice.tone());
            row.box.pseudoClassStateChanged(UNAVAILABLE, !choice.enabled());
        }
        locked = false;
        if (selected >= 0 && rows.get(selected).choice.enabled()) {
            String tone = rows.get(selected).choice.tone();
            slab.getStyleClass().setAll("cursor-slab", "tone-" + tone);
            shard.getStyleClass().setAll("cursor-shard", "tone-" + tone);
        } else {
            int next = -1;
            for (int i = 0; i < rows.size() && next < 0; i++) if (rows.get(i).choice.enabled()) next = i;
            if (next >= 0) select(next);
            else clearSelection();
        }
        requestLayout();
    }

    /** No option selected: the cursor hides and the options settle back into line. */
    private void clearSelection() {
        if (selected >= 0) rows.get(selected).box.pseudoClassStateChanged(SELECTED, false);
        selected = -1;
        placed = false;
        overlay.setVisible(false);
        Timeline settle = new Timeline();
        for (Row row : rows) {
            settle.getKeyFrames().add(new KeyFrame(MotionTokens.QUICK,
                    new KeyValue(row.room.yProperty(), 0, MotionTokens.OUT),
                    new KeyValue(row.label.scaleXProperty(), 1, MotionTokens.OUT),
                    new KeyValue(row.label.scaleYProperty(), 1, MotionTokens.OUT)));
        }
        settle.play();
    }

    private static boolean hasHint(Choice choice) { return choice.hint() != null && !choice.hint().isBlank(); }

    public void unlock() { locked = false; }

    /** Whether any options are showing (the list always holds its cursor layer, so do not count children). */
    public boolean hasChoices() { return !rows.isEmpty(); }

    public boolean isLocked() { return locked; }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        overlay.resizeRelocate(0, 0, getWidth(), getHeight());
        for (Row row : rows) {
            row.ghost.resizeRelocate(row.box.getLayoutX(), row.box.getLayoutY(), row.box.getWidth(), row.box.getHeight());
            row.ghostContent.resizeRelocate(row.content.getLayoutX(), row.content.getLayoutY(),
                    row.content.getWidth(), row.content.getHeight());
        }
        if (selected < 0 || rows.get(selected).box.getWidth() <= 0) return;
        if (spawnPending) {
            spawnPending = false;
            spawn(target(selected));
        } else if (placed && (travel == null || travel.getStatus() != Animation.Status.RUNNING)) {
            CursorGeometry.Pose want = target(selected);
            if (Math.abs(want.cx() - cx.get()) > 0.5 || Math.abs(want.cy() - cy.get()) > 0.5
                    || Math.abs(want.w() - cw.get()) > 0.5) jump(want);
        }
    }

    // ---- selection and cursor motion ------------------------------------------------

    /** The player moved the cursor: select and give the cursor sound if it actually moved. */
    private void pick(int index) {
        int before = selected;
        select(index);
        if (selected != before) Sfx.CURSOR.play(this);
    }

    private void select(int index) {
        if (index < 0 || index >= rows.size() || index == selected || !rows.get(index).choice.enabled()) return;
        if (selected >= 0) rows.get(selected).box.pseudoClassStateChanged(SELECTED, false);
        selected = index;
        Row row = rows.get(index);
        row.box.pseudoClassStateChanged(SELECTED, true);
        slab.getStyleClass().setAll("cursor-slab", "tone-" + row.choice.tone());
        shard.getStyleClass().setAll("cursor-shard", "tone-" + row.choice.tone());
        makeRoom();
        onSelect.accept(index);
        if (!placed) {
            spawnPending = true;
            requestLayout();
        } else if (row.box.getWidth() > 0) {
            move(target(index));
        }
    }

    /** Where the cursor should sit for an option, in this list's coordinates. */
    private CursorGeometry.Pose target(int index) {
        Slant box = rows.get(index).box;
        boolean large = getStyleClass().contains("large");
        return new CursorGeometry.Pose(
                box.getLayoutX() + box.getWidth() / 2 + (large ? 6 : 3),
                box.getLayoutY() + box.getHeight() / 2,
                box.getWidth() + (large ? 30 : 18),
                box.getHeight() + (large ? 0 : 4),
                box.getRotate() - 1.2);
    }

    private CursorGeometry.Pose current() {
        return new CursorGeometry.Pose(cx.get(), cy.get(), cw.get(), ch.get(), ca.get());
    }

    private void move(CursorGeometry.Pose to) {
        if (travel != null) travel.stop();
        travel = path(current(), CursorGeometry.travel(current(), to), cx, cy, cw, ch, ca);
        travel.play();
        followWithShard(to, Duration.millis(60));
        twitchNow();
    }

    private void spawn(CursorGeometry.Pose at) {
        List<CursorGeometry.Step> steps = CursorGeometry.spawn(at);
        CursorGeometry.Pose start = steps.get(0).pose();
        set(start, cx, cy, cw, ch, ca);
        set(shardPose(start), sx, sy, sw, sh, sa);
        placed = true;
        overlay.setVisible(true);
        overlay.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(120), overlay);
        fade.setToValue(1);
        if (travel != null) travel.stop();
        travel = new ParallelTransition(fade, path(start, steps.subList(1, steps.size()), cx, cy, cw, ch, ca));
        travel.play();
        followWithShard(at, Duration.millis(80));
    }

    private void jump(CursorGeometry.Pose to) {
        set(to, cx, cy, cw, ch, ca);
        set(shardPose(to), sx, sy, sw, sh, sa);
    }

    /** The accent shard trails the cursor: offset, a little smaller, tilted further, arriving later. */
    private static CursorGeometry.Pose shardPose(CursorGeometry.Pose p) { return p.plus(11, 7, 0.96, 4); }

    private void followWithShard(CursorGeometry.Pose to, Duration delay) {
        if (shardTravel != null) shardTravel.stop();
        CursorGeometry.Pose s = shardPose(to);
        Timeline t = new Timeline(new KeyFrame(Duration.millis(360),
                new KeyValue(sx, s.cx(), MotionTokens.OUT), new KeyValue(sy, s.cy(), MotionTokens.OUT),
                new KeyValue(sw, s.w(), MotionTokens.OUT), new KeyValue(sh, s.h(), MotionTokens.OUT),
                new KeyValue(sa, s.angle(), MotionTokens.OUT)));
        t.setDelay(delay);
        shardTravel = t;
        t.play();
    }

    /** Builds a timeline through the stepped poses: the first leg is fast-out, the rest ease both ways. */
    private static Timeline path(CursorGeometry.Pose from, List<CursorGeometry.Step> steps, DoubleProperty x, DoubleProperty y,
                                 DoubleProperty w, DoubleProperty h, DoubleProperty a) {
        Timeline t = new Timeline(new KeyFrame(Duration.ZERO,
                new KeyValue(x, from.cx()), new KeyValue(y, from.cy()), new KeyValue(w, from.w()),
                new KeyValue(h, from.h()), new KeyValue(a, from.angle())));
        for (int i = 0; i < steps.size(); i++) {
            CursorGeometry.Step step = steps.get(i);
            var curve = i == 0 ? MotionTokens.OUT : MotionTokens.IN_OUT;
            t.getKeyFrames().add(new KeyFrame(Duration.millis(step.millis()),
                    new KeyValue(x, step.pose().cx(), curve), new KeyValue(y, step.pose().cy(), curve),
                    new KeyValue(w, step.pose().w(), curve), new KeyValue(h, step.pose().h(), curve),
                    new KeyValue(a, step.pose().angle(), curve)));
        }
        return t;
    }

    private static void set(CursorGeometry.Pose p, DoubleProperty x, DoubleProperty y, DoubleProperty w,
                            DoubleProperty h, DoubleProperty a) {
        x.set(p.cx()); y.set(p.cy()); w.set(p.w()); h.set(p.h()); a.set(p.angle());
    }

    private void drawCursor() {
        var pose = new CursorGeometry.Pose(cx.get() + twitch.get() * 0.8, cy.get(),
                cw.get() * breath.get(), ch.get() * (1 + (breath.get() - 1) * 0.5), ca.get() + twitch.get());
        double[] c = CursorGeometry.corners(pose, Slant.SLOPE);
        Double[] points = {c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7]};
        slab.getPoints().setAll(points);
        slabClip.getPoints().setAll(points);
    }

    private void drawShard() {
        var pose = new CursorGeometry.Pose(sx.get(), sy.get(), sw.get() * breath.get(), sh.get(), sa.get());
        double[] c = CursorGeometry.corners(pose, Slant.SLOPE);
        shard.getPoints().setAll(c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7]);
    }

    /** A quick jolt of the cursor angle, then back; repeats at irregular intervals while idle. */
    private void twitchNow() {
        new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(twitch, 0)),
                new KeyFrame(Duration.millis(50), new KeyValue(twitch, 3.5, MotionTokens.OUT)),
                new KeyFrame(Duration.millis(140), new KeyValue(twitch, -1.0, MotionTokens.IN_OUT)),
                new KeyFrame(Duration.millis(230), new KeyValue(twitch, 0, MotionTokens.IN_OUT))).play();
        scheduleTwitch();
    }

    private void scheduleTwitch() {
        twitchTimer.stop();
        twitchTimer.setDuration(Duration.millis(2600 + random.nextInt(1600)));
        if (getScene() != null) twitchTimer.playFromStart();
    }

    /** Options above the selection lift a little and those below drop, giving it room. */
    private void makeRoom() {
        boolean large = getStyleClass().contains("large");
        double room = large ? 8 : 5;
        Timeline shift = new Timeline();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            double y = i < selected ? -room : i > selected ? room : 0;
            double scale = i == selected ? (large ? 1.06 : 1.04) : 1.0;
            shift.getKeyFrames().add(new KeyFrame(MotionTokens.QUICK,
                    new KeyValue(row.room.yProperty(), y, MotionTokens.OUT),
                    new KeyValue(row.label.scaleXProperty(), scale, MotionTokens.OUT),
                    new KeyValue(row.label.scaleYProperty(), scale, MotionTokens.OUT)));
        }
        shift.play();
    }

    /** A bright band crosses each unselected option now and then, in stepped jumps. */
    private void shimmerStep() {
        shimmerTick++;
        double now = shimmerTick / (double) MotionTokens.STEP_RATE;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            // Every 3.4 s a band takes 0.9 s to cross the option; rows are offset by 0.6 s.
            double phase = (now + i * 0.6) % 3.4;
            if (i == selected || !row.choice.enabled() || phase >= 0.9) {
                if (!row.label.getStyle().isEmpty()) row.label.setStyle("");
                continue;
            }
            double p = -0.15 + phase / 0.9 * 1.3;
            String base = switch (row.choice.tone()) {
                case "cyan" -> "#5CE1E6";
                case "danger" -> "#FF4D6D";
                case "caret" -> "#FFD23F";
                default -> "#AEB6E0";
            };
            row.label.setStyle(String.format(java.util.Locale.ROOT,
                    "-fx-text-fill: linear-gradient(from 0%% 0%% to 100%% 0%%, %s 0%%, %s %.1f%%, #FFFFFF %.1f%%, %s %.1f%%, %s 100%%);",
                    base, base, pct(p - 0.12), pct(p), base, pct(p + 0.12), base));
        }
    }

    private static double pct(double v) { return Math.max(0, Math.min(1, v)) * 100; }

    private void activate() {
        if (locked || selected < 0 || !rows.get(selected).choice.enabled()) return;
        locked = true;
        Row row = rows.get(selected);
        Sfx.CONFIRM.play(this);
        flash();
        echo();
        ScaleTransition punch = new ScaleTransition(MotionTokens.SNAP, row.label);
        punch.setToX(row.label.getScaleX() * 1.08);
        punch.setToY(row.label.getScaleY() * 1.08);
        punch.setAutoReverse(true);
        punch.setCycleCount(2);
        punch.play();
        javafx.geometry.Point2D at = localToScene(cx.get(), cy.get());
        if (at != null) fireEvent(new RevealOriginEvent(at.getX(), at.getY()));
        row.choice.action().run();
    }

    private void flash() {
        slab.getStyleClass().add("flash");
        PauseTransition back = new PauseTransition(MotionTokens.SNAP);
        back.setOnFinished(e -> slab.getStyleClass().remove("flash"));
        back.play();
    }

    /** An outline of the cursor expands and fades from the point of confirmation. */
    private void echo() {
        Polygon ring = new Polygon();
        ring.getPoints().setAll(slab.getPoints());
        ring.getStyleClass().addAll("cursor-echo", slab.getStyleClass().size() > 1 ? slab.getStyleClass().get(1) : "tone-normal");
        echoes.getChildren().add(ring);
        ScaleTransition grow = new ScaleTransition(Duration.millis(300), ring);
        grow.setToX(1.22);
        grow.setToY(1.5);
        grow.setInterpolator(MotionTokens.OUT);
        FadeTransition fade = new FadeTransition(Duration.millis(300), ring);
        fade.setFromValue(0.9);
        fade.setToValue(0);
        ParallelTransition both = new ParallelTransition(grow, fade);
        both.setOnFinished(e -> echoes.getChildren().remove(ring));
        both.play();
    }

    private void moveSelection(int direction) {
        if (rows.isEmpty()) return;
        int index = selected;
        for (int step = 0; step < rows.size(); step++) {
            index = Math.floorMod(index + direction, rows.size());
            if (rows.get(index).choice.enabled()) { pick(index); return; }
        }
    }

    private void onKey(KeyEvent event) {
        if (rows.isEmpty()) return; // let the screen handle keys (e.g. advancing dialogue)
        switch (event.getCode()) {
            case UP, W -> moveSelection(-1);
            case DOWN, S -> moveSelection(1);
            case ENTER, SPACE -> activate();
            default -> { return; }
        }
        event.consume();
    }

    private static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClass.equals("choice-label") ? "display" : "body", styleClass);
        return label;
    }
}
