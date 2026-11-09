package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DemoProfile;
import com.aphs.classroomuprising.GameState;

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
    private GameSession session;
    private GameState dayStart;

    AppContext(DemoProfile profile, Path savePath, Router router) {
        this.profile = profile;
        this.savePath = savePath;
        this.router = router;
    }

    DemoProfile profile() { return profile; }

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
    }

    /** Opens the screen that matches the current period, with the period name on the wipe. */
    void advance() {
        GameState view = session().view();
        router.show(screenFor(view), view.openingCompleted() ? UiText.period(view.period()) : null);
    }

    /** Shows the day card after the opening or after {@code beginNextDay()}. */
    void startDay(Outcome outcome) {
        dayStart = session().view();
        router.show(new DayIntroScreen(this, outcome instanceof Outcome.Done done ? done : null), null);
    }

    void toTitle() {
        session = null;
        dayStart = null;
        router.show(new TitleScreen(this), null);
    }

    String demoLabel() { return profile.label(); }

    /** Splits authored text into dialogue lines; "You" is shown as the player's name. */
    List<DialogueScript.Line> script(String raw) {
        String name = session().view().name();
        return DialogueScript.parse(raw, "Mr. Reward").stream()
                .map(line -> line.speaker().equals("You") ? new DialogueScript.Line(name, line.text()) : line)
                .toList();
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
