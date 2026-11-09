package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.ui.scene.AssetManifest;
import com.aphs.classroomuprising.ui.scene.AssetStore;
import com.aphs.classroomuprising.ui.scene.SceneView;

import java.nio.file.Path;
import java.util.List;

/**
 * What every screen shares: the Demo profile, the save location, the router and the
 * current {@link GameSession}. It also decides which screen fits the current game state,
 * so the period in the core's snapshot is the only thing that drives navigation.
 */
final class AppContext {
    private final DemoProfile profile;
    private final Path savePath;
    private final Router router;
    private final AssetStore assets;
    private GameSession session;
    private GameState dayStart;
    /** What the window last showed; used only to choose the transition animation. */
    private DayPeriod shownPeriod;
    private int shownDay;

    AppContext(DemoProfile profile, Path savePath, Router router, AssetStore assets) {
        this.profile = profile;
        this.savePath = savePath;
        this.router = router;
        this.assets = assets;
    }

    DemoProfile profile() { return profile; }

    /** The art store, or null when the asset manifest could not be read. */
    AssetStore assets() { return assets; }

    Path savePath() { return savePath; }

    Router router() { return router; }

    Toasts toasts() { return router.toasts(); }

    boolean hasSession() { return session != null; }

    GameSession session() {
        if (session == null) throw new IllegalStateException("No game in progress.");
        return session;
    }

    /** The snapshot taken when today started in this window, or null after loading a save. */
    GameState dayStart() { return dayStart; }

    void begin(GameSession newSession) {
        session = newSession;
        dayStart = null;
        shownPeriod = null;
    }

    /**
     * Opens the screen that matches the current period. A time change within the day the
     * window was showing plays the clock; anything else (new day, load) uses the wipe.
     */
    void advance() {
        GameState view = session().view();
        Screen next = screenFor(view);
        if (ClockTimes.animates(shownPeriod, shownDay, view)) {
            router.showTimeChange(next, shownPeriod, view.period());
        } else {
            router.show(next, view.openingCompleted() ? UiText.period(view.period()) : null);
        }
        shownPeriod = view.period();
        shownDay = view.day();
    }

    /** Shows the day card after the opening or after {@code beginNextDay()}. */
    void startDay(Outcome outcome) {
        GameState view = session().view();
        dayStart = view;
        Outcome.Done done = outcome instanceof Outcome.Done d ? d : null;
        int previousDay = done != null && done.before().day() < view.day() ? done.before().day() : view.day() - 1;
        shownPeriod = null;
        router.show(new DayIntroScreen(this, done, previousDay), null);
    }

    void toTitle() {
        session = null;
        dayStart = null;
        shownPeriod = null;
        router.show(new TitleScreen(this), null);
    }

    String demoLabel() { return profile.label(); }

    /**
     * Splits authored text into dialogue lines, attaches each speaker's art id from the asset
     * manifest, and shows "You" as the player's name.
     */
    List<DialogueScript.Line> script(String raw) {
        String name = session().view().name();
        return DialogueScript.parse(raw, "Mr. Reward").stream().map(line -> {
            String id = assets == null ? null : assets.manifest().characterNamed(line.speaker())
                    .map(AssetManifest.CharacterSpec::id).orElse(null);
            String shown = line.speaker().equals("You") ? name : line.speaker();
            return new DialogueScript.Line(shown, line.expression(), line.text(), id);
        }).toList();
    }

    /** Points a scene at the art store and shows a place; does nothing without a manifest. */
    void stage(SceneView scene, String place, String variant, String grade) {
        if (assets == null) return;
        scene.setAssets(assets);
        scene.show(place, variant, grade);
    }

    private Screen screenFor(GameState view) {
        if (!view.openingCompleted()) return new OpeningScreen(this);
        if (view.status() != GameState.Status.ACTIVE)
            return new BlockedScreen(this, "CHECKPOINT", Diagnostic.warning(
                    "warning[T10]: " + view.status() + " has no decision screen yet",
                    "Midterm/Final decisions are not connected. Your last day-end save is kept."));
        return switch (view.period()) {
            case MORNING_BONUS, AFTER_SCHOOL, EVENING, LATE_NIGHT -> new HubScreen(this);
            case ICS_CLASS -> new ClassroomScreen(this);
            case OTHER_CLASSES -> new OtherClassesScreen(this);
            case DAY_END -> new DayEndScreen(this);
        };
    }
}
