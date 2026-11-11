package com.aphs.classroomuprising.ui.scene;

import javafx.animation.Animation;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The art behind a screen, in layers from back to front:
 * background (graded, drifting) → characters → foreground plane (stronger parallax) →
 * ambient motes, vignette and grain → an optional dim for screens that need calm.
 * Screens put their UI above this node. It never takes input.
 *
 * <p>Everything visual here comes from image files named in the asset manifest;
 * missing files show as labelled placeholder plates.</p>
 */
public class SceneView extends StackPane {
    private static final double W = BackgroundLayer.W, H = BackgroundLayer.H;

    private AssetStore assets;
    private final BackgroundLayer background = new BackgroundLayer(40, 1.0);
    private final BackgroundLayer foreground = new BackgroundLayer(70, 1.8);
    private final Pane characters = new Pane();
    private final Rectangle dim = new Rectangle(W, H);
    private final Map<String, CharacterView> cast = new LinkedHashMap<>();
    private final List<Animation> running = new ArrayList<>();
    private AmbientLayer ambient;

    public SceneView() {
        getStyleClass().add("scene-view");
        setMinSize(W, H);
        setPrefSize(W, H);
        setMaxSize(W, H);
        setClip(new Rectangle(W, H));
        setMouseTransparent(true);
        characters.setPrefSize(W, H);
        characters.setMinSize(W, H);
        characters.setMaxSize(W, H);
        dim.getStyleClass().add("scene-dim");
        dim.setOpacity(0);
        foreground.setVisible(false);
        getChildren().addAll(background, characters, foreground, dim);
        // Stop every animation when the screen is replaced, so nothing runs off-screen.
        sceneProperty().addListener((obs, old, now) -> {
            if (now == null) running.forEach(Animation::stop);
            else running.forEach(Animation::play);
        });
    }

    /** Must be called before {@link #show}; repeated calls with the same store do nothing. */
    public void setAssets(AssetStore store) {
        if (assets == store) return;
        if (assets != null) throw new IllegalStateException("A scene keeps one asset store.");
        assets = store;
        ambient = new AmbientLayer(store.texture("motes"), store.texture("grain"));
        getChildren().add(getChildren().indexOf(dim), ambient);
        track(ambient.animations());
        track(List.of(background.drift(), foreground.drift()));
    }

    /**
     * Shows a place's art.
     * @param variant   optional art variant such as a time of day ("evening"); may be null
     * @param gradeName colour grade from the manifest (e.g. "after_school")
     */
    public void show(String place, String variant, String gradeName) {
        Grade grade = assets.grade(gradeName);
        background.show(assets.background(place, variant), grade, true);
        Optional<Image> front = assets.foreground(place);
        foreground.setVisible(front.isPresent());
        front.ifPresent(image -> foreground.show(image, grade, true));
    }

    /** Re-grades the art in place, e.g. as time passes. */
    public Animation regrade(String gradeName, Duration duration) {
        return background.regrade(assets.grade(gradeName), duration);
    }

    /** Darkens the art (0 = none, 1 = black) so dense UI stays readable. */
    public void setDim(double amount) { dim.setOpacity(amount); }

    /**
     * Places a character with its anchor at a horizontal position (0 = left edge, 1 = right edge)
     * on the bottom of the canvas, and plays its entrance.
     * @return false when the manifest has no such character
     */
    public boolean enter(String characterId, double slotX, String expression, Duration delay) {
        Optional<AssetManifest.CharacterSpec> spec = assets.manifest().character(characterId);
        if (spec.isEmpty()) return false;
        CharacterView view = new CharacterView(assets, spec.get(), expression);
        // The view's origin is the character's anchor, so layout puts the anchor on the slot.
        view.setLayoutX(W * slotX);
        view.setLayoutY(H + 6);
        view.face(slotX < 0.5);   // characters look towards the middle of the screen
        characters.getChildren().add(view);
        cast.put(characterId, view);
        track(view.animations());
        Animation entrance = view.enter(slotX < 0.5, delay);
        entrance.play();
        return true;
    }

    /**
     * Gives the floor to one character (null = narration: nobody is in focus).
     * Unknown ids are ignored; an expression, if given, is applied to the speaker.
     */
    public void speak(String characterId, String expression) {
        for (CharacterView view : cast.values()) {
            boolean speaking = view.id().equals(characterId);
            view.setSpeaking(speaking || characterId == null && cast.size() == 1);
            if (speaking && expression != null) view.setExpression(expression, true);
        }
    }

    /** A character on screen is struck: it jolts and flashes red. Unknown ids are ignored. */
    public void hit(String characterId) {
        CharacterView view = cast.get(characterId);
        if (view != null) view.hit();
    }

    /**
     * Roughly the middle of a character's body (chest height), in this view's coordinates; empty
     * when that character is not on screen. Used to aim effects at a character.
     */
    public Optional<javafx.geometry.Point2D> centreOf(String characterId) {
        CharacterView view = cast.get(characterId);
        if (view == null) return Optional.empty();
        return Optional.of(characters.localToParent(view.getLayoutX(), view.getLayoutY() - view.height() * 0.6));
    }

    /** The manifest id for a dialogue speaker name, if that speaker has art. */
    public Optional<String> characterFor(String speakerName) {
        return assets.manifest().characterNamed(speakerName).map(AssetManifest.CharacterSpec::id);
    }

    private void track(List<Animation> animations) {
        running.addAll(animations);
        if (getScene() != null) animations.forEach(Animation::play);
    }
}
