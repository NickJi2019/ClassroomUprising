package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ui.scene.AssetStore;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.util.List;
import java.util.Locale;

/**
 * The lecture slides: a projector panel beside the teacher that follows the lecture. A line of
 * the lecture that starts with a cue ({@code [slide:name]}, see {@link DialogueScript}) turns to
 * that slide; lines without a cue keep the slide that is up. The images come from the asset
 * manifest ({@code slide.name}). Z or a click opens the slide large over the whole
 * screen to read it; while it is open the lecture waits, and Z, Enter, Space, Backspace, Esc or a
 * click closes it. It only presents the lesson the core returned; it changes nothing.
 */
final class SlideProjector extends Pane {
    /** The slide area on the 1280x720 canvas (16:9). */
    static final double X = 600, Y = 118, SW = 608, SH = 342;
    private static final double LEAN = 22, PAD = 14;
    private static final double ZOOM = 1.75, ZOOM_CX = 640, ZOOM_CY = 346;

    private final AssetStore assets;
    /** The lesson's slide names, in the order the lecture first cues them. */
    private final List<String> slides;
    private final Pane rig = new Pane();
    private final StackPane screen = new StackPane();
    private final Polygon frame = parallelogram(-PAD, -PAD, SW + PAD, SH + PAD);
    private final Polygon shard = parallelogram(-PAD, -PAD, SW + PAD, SH + PAD);
    private final Label number = new Label();
    private final Slant tag;
    private final Region scrim = new Region();
    private final Label zoomHint = new Label();
    private final Scale zoom = new Scale(1, 1, SW / 2, SH / 2);
    private int shown = -1;
    private boolean zoomed;
    private Animation moving;

    SlideProjector(AssetStore assets, List<String> slides) {
        this.assets = assets;
        this.slides = List.copyOf(slides);
        setPickOnBounds(false);
        setPrefSize(ClassroomApp.WIDTH, ClassroomApp.HEIGHT);

        scrim.getStyleClass().add("slide-scrim");
        scrim.setPrefSize(ClassroomApp.WIDTH, ClassroomApp.HEIGHT);
        scrim.setOpacity(0);
        scrim.setVisible(false);
        scrim.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> setZoomed(false));
        zoomHint.getStyleClass().addAll("display", "slide-zoom-hint");
        zoomHint.setLayoutX(0);
        zoomHint.setLayoutY(ClassroomApp.HEIGHT - 26);
        zoomHint.setPrefWidth(ClassroomApp.WIDTH - 28);
        zoomHint.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        zoomHint.setMouseTransparent(true);
        zoomHint.setOpacity(0);

        frame.getStyleClass().add("slide-frame");
        shard.getStyleClass().add("slide-shard");
        shard.setTranslateX(12);
        shard.setTranslateY(12);
        screen.getStyleClass().add("slide-screen");
        screen.setPrefSize(SW, SH);
        screen.setMinSize(SW, SH);
        screen.setMaxSize(SW, SH);
        screen.resize(SW, SH);
        screen.setClip(new Rectangle(SW, SH));

        Label kind = new Label("SLIDE");
        kind.getStyleClass().addAll("display", "slide-kind");
        number.getStyleClass().addAll("display", "slide-number");
        HBox tagText = new HBox(10, kind, number);
        tagText.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        tag = new Slant(tagText);
        tag.getStyleClass().add("slide-tag");
        tag.setLayoutX(-PAD - 6);
        tag.setLayoutY(-PAD - 40);

        rig.getChildren().addAll(shard, frame, screen, tag);
        rig.setLayoutX(X);
        rig.setLayoutY(Y);
        rig.getTransforms().add(zoom);
        rig.setCursor(javafx.scene.Cursor.HAND);
        rig.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> toggleZoom());
        rig.setVisible(false);
        getChildren().addAll(scrim, rig, zoomHint);
        // Decode every slide now, so turning a page never waits on a file.
        this.slides.forEach(assets::slide);
    }

    /** Whether this lesson has any slides. */
    boolean hasSlides() { return !slides.isEmpty(); }

    /** Whether a slide is on screen. */
    boolean showing() { return rig.isVisible() && shown >= 0; }

    /** The slides a lecture cues, each once, in the order it first turns to them. */
    static List<String> cues(List<DialogueScript.Line> script) {
        return script.stream().map(DialogueScript.Line::slide).filter(java.util.Objects::nonNull).distinct().toList();
    }

    /** Follows the lecture to a cued slide: brings the panel in, or turns the page. */
    void show(String name) {
        int next = slides.indexOf(name);
        if (next < 0 || next == shown) return;
        boolean first = shown < 0;
        shown = next;
        String label = String.format(Locale.ROOT, "%02d / %02d", next + 1, slides.size());
        Image image = assets.slide(name);
        if (first) enter(image, label);
        else turn(image, label);
    }

    /** The lecture is over: the panel is thrown off to the right. */
    void close() {
        if (!rig.isVisible()) return;
        setZoomed(false);
        stopMoving();
        Timeline out = new Timeline(new KeyFrame(Duration.millis(220),
                new KeyValue(rig.translateXProperty(), 420, MotionTokens.IN),
                new KeyValue(rig.opacityProperty(), 0, MotionTokens.IN)));
        out.setOnFinished(e -> {
            rig.setVisible(false);
            shown = -1;
        });
        moving = out;
        out.play();
    }

    void toggleZoom() {
        if (showing()) setZoomed(!zoomed);
    }

    boolean zoomed() { return zoomed; }

    /** Closes the large view, if it is open. */
    void unzoom() { setZoomed(false); }

    private void setZoomed(boolean on) {
        if (on == zoomed) return;
        zoomed = on;
        zoomHint.setText("SLIDE " + number.getText() + "     Z / ENTER  CLOSE");
        double scale = on ? ZOOM : 1;
        double dx = on ? ZOOM_CX - (X + SW / 2) : 0;
        double dy = on ? ZOOM_CY - (Y + SH / 2) : 0;
        scrim.setVisible(true);
        Timeline change = new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(zoom.xProperty(), scale, MotionTokens.OUT),
                new KeyValue(zoom.yProperty(), scale, MotionTokens.OUT),
                new KeyValue(rig.translateXProperty(), dx, MotionTokens.OUT),
                new KeyValue(rig.translateYProperty(), dy, MotionTokens.OUT),
                new KeyValue(scrim.opacityProperty(), on ? 1 : 0, MotionTokens.OUT),
                new KeyValue(zoomHint.opacityProperty(), on ? 1 : 0, MotionTokens.OUT),
                // The page tag would be pushed off the top edge; the hint carries the number instead.
                new KeyValue(tag.opacityProperty(), on ? 0 : 1, MotionTokens.OUT)));
        change.setOnFinished(e -> scrim.setVisible(zoomed));
        change.play();
    }

    // ---- motion -------------------------------------------------------------------------

    /** First slide: the frame opens left to right in drawn steps, the tag drops, the slide wipes in. */
    private void enter(Image image, String label) {
        stopMoving();
        number.setText(label);
        rig.setVisible(true);
        rig.setOpacity(1);
        rig.setTranslateX(zoomed ? rig.getTranslateX() : 0);
        screen.getChildren().clear();
        Scale frameOpen = new Scale(0, 1, -PAD, 0);
        Scale shardOpen = new Scale(0, 1, -PAD, 0);
        frame.getTransforms().setAll(frameOpen);
        shard.getTransforms().setAll(shardOpen);
        tag.setOpacity(0);
        tag.setTranslateY(-14);
        Timeline open = new Timeline(
                new KeyFrame(Duration.millis(150), new KeyValue(frameOpen.xProperty(), 1, MotionTokens.steps(5))),
                new KeyFrame(Duration.millis(60), new KeyValue(shardOpen.xProperty(), 0)),
                new KeyFrame(Duration.millis(240), new KeyValue(shardOpen.xProperty(), 1, MotionTokens.OUT)),
                new KeyFrame(Duration.millis(120), new KeyValue(tag.opacityProperty(), 0),
                        new KeyValue(tag.translateYProperty(), -14)),
                new KeyFrame(Duration.millis(260), new KeyValue(tag.opacityProperty(), 1),
                        new KeyValue(tag.translateYProperty(), 0, MotionTokens.OUT)));
        Animation wipe = wipeIn(image, Duration.millis(140));
        ParallelTransition all = new ParallelTransition(open, wipe);
        moving = all;
        all.play();
    }

    /** Next slide: the new page wipes over the old in hard steps behind a bright edge; the old one slides back and dims. */
    private void turn(Image image, String label) {
        stopMoving();
        Sfx.SLIDE.play(this);
        number.setText(label);
        number.setScaleX(1.25);
        number.setScaleY(1.25);
        Timeline punch = new Timeline(new KeyFrame(Duration.millis(200),
                new KeyValue(number.scaleXProperty(), 1, MotionTokens.OUT),
                new KeyValue(number.scaleYProperty(), 1, MotionTokens.OUT)));
        ParallelTransition all = new ParallelTransition(punch, wipeIn(image, Duration.ZERO));
        moving = all;
        all.play();
    }

    private Animation wipeIn(Image image, Duration delay) {
        var old = List.copyOf(screen.getChildren());
        ImageView view = new ImageView(image);
        view.setFitWidth(SW);
        view.setFitHeight(SH);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        Rectangle reveal = new Rectangle(0, SH);
        view.setClip(reveal);
        Rectangle edge = new Rectangle(14, SH);
        edge.getStyleClass().add("slide-edge");
        edge.setManaged(false);
        edge.setTranslateX(-14);
        edge.translateXProperty().bind(reveal.widthProperty().subtract(10));
        screen.getChildren().addAll(view, edge);

        ColorAdjust dim = new ColorAdjust();
        Timeline wipe = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(reveal.widthProperty(), 0)),
                new KeyFrame(Duration.millis(210), new KeyValue(reveal.widthProperty(), SW + 14, MotionTokens.steps(6))));
        for (var node : old) {
            node.setEffect(dim);
            wipe.getKeyFrames().add(new KeyFrame(Duration.millis(210),
                    new KeyValue(node.translateXProperty(), -36, MotionTokens.OUT),
                    new KeyValue(dim.brightnessProperty(), -0.6)));
        }
        wipe.setDelay(delay);
        wipe.setOnFinished(e -> {
            screen.getChildren().removeAll(old);
            screen.getChildren().remove(edge);
            view.setClip(null);
        });
        return wipe;
    }

    /** Ends any running motion at once, in its final state, so a fast reader never sees two half-turned pages. */
    private void stopMoving() {
        if (moving != null) {
            Animation running = moving;
            moving = null;
            if (running.getStatus() == Animation.Status.RUNNING) {
                running.stop();
                finish(running);
            }
        }
        frame.getTransforms().clear();
        shard.getTransforms().clear();
        tag.setOpacity(1);
        tag.setTranslateY(0);
        number.setScaleX(1);
        number.setScaleY(1);
    }

    /** Runs the clean-up an animation would have run on finishing (stop() skips it). */
    private static void finish(Animation animation) {
        if (animation instanceof ParallelTransition group) group.getChildren().forEach(SlideProjector::finish);
        if (animation.getOnFinished() != null) animation.getOnFinished().handle(null);
    }

    private static Polygon parallelogram(double x0, double y0, double x1, double y1) {
        return new Polygon(x0 + LEAN, y0, x1 + LEAN, y0, x1, y1, x0, y1);
    }
}
