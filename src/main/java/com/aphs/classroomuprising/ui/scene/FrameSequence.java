package com.aphs.classroomuprising.ui.scene;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.List;

/**
 * A run of hand-drawn frames played at a fixed frame time (no in-betweening), e.g. a screen
 * transition drawn by an artist. One callback fires on the swap frame, so the screen behind
 * can change while the frames cover it.
 */
public final class FrameSequence {
    private final List<Image> frames;
    private final double frameMillis;
    private final int swapAt;

    FrameSequence(List<Image> frames, double frameMillis, int swapAt) {
        if (frames.isEmpty() || swapAt < 0 || swapAt >= frames.size()) throw new IllegalArgumentException("Bad sequence.");
        this.frames = List.copyOf(frames);
        this.frameMillis = frameMillis;
        this.swapAt = swapAt;
    }

    public int frameCount() { return frames.size(); }

    /**
     * An image view sized to the canvas and the animation that plays the frames on it.
     * The view is cleared after the last frame.
     */
    public Animation play(ImageView view, double width, double height, Runnable onSwap) {
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(false);
        view.setImage(frames.get(0));
        Timeline timeline = new Timeline();
        for (int i = 0; i < frames.size(); i++) {
            Image frame = frames.get(i);
            boolean swap = i == swapAt;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * frameMillis), e -> {
                view.setImage(frame);
                if (swap) onSwap.run();
            }));
        }
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(frames.size() * frameMillis), e -> view.setImage(null)));
        return timeline;
    }
}
