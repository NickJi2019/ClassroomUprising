package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.ui.scene.AssetManifest;
import com.aphs.classroomuprising.ui.scene.AssetStore;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The game window. Everything is laid out on a fixed 1280x720 canvas that is scaled
 * (letterboxed) to the window, so the composition looks the same at any window size.
 * It reads no application parameters.
 */
public final class ClassroomApp extends Application {
    static final double WIDTH = 1280;
    static final double HEIGHT = 720;

    @Override
    public void start(Stage stage) {
        StackPane canvas = new StackPane();
        canvas.getStyleClass().add("canvas");
        canvas.setMinSize(WIDTH, HEIGHT);
        canvas.setPrefSize(WIDTH, HEIGHT);
        canvas.setMaxSize(WIDTH, HEIGHT);
        canvas.setClip(new Rectangle(WIDTH, HEIGHT));

        StackPane window = new StackPane(canvas);
        window.getStyleClass().add("window");
        DoubleBinding scale = Bindings.createDoubleBinding(
                () -> Math.min(window.getWidth() / WIDTH, window.getHeight() / HEIGHT),
                window.widthProperty(), window.heightProperty());
        canvas.scaleXProperty().bind(scale);
        canvas.scaleYProperty().bind(scale);

        Scene scene = new Scene(window, WIDTH, HEIGHT, Color.web("#05071A"));
        Theme.apply(scene);
        Router router = new Router(canvas, scene);

        DemoProfile profile = null;
        String loadError = null;
        try {
            profile = DemoProfile.load();
        } catch (IOException e) {
            loadError = e.getMessage();
        }
        AssetStore assets = null;
        try {
            assets = AssetStore.bundled(AssetManifest.bundled());
        } catch (IOException e) {
            loadError = (loadError == null ? "" : loadError + "; ") + e.getMessage();
        }
        AppContext context = new AppContext(profile, GameSession.defaultSavePath(), router, assets);
        router.bind(context);

        stage.setTitle("Classroom Uprising");
        stage.setScene(scene);
        stage.setMinWidth(640);
        stage.setMinHeight(360);
        stage.show();

        if (profile != null && assets != null) {
            router.show(new TitleScreen(context), null);
        } else {
            router.show(new BlockedScreen(context, "CANNOT START", Diagnostic.of(
                    new Outcome.Rejected("Game data could not be loaded: " + loadError))), null);
        }
    }
}
