package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.GameState;

import java.util.Map;
import java.util.Optional;

/**
 * In-game tips for the tutorial (the first two days). They explain how to play at the moment a
 * system first matters and are shown as tip cards, never as a character's dialogue: Mr. Reward
 * teaches, the game explains itself.
 *
 * <p>The text only names approved rules and controls. It states no amounts: exact changes come
 * from the backend's previews on each screen. Whether a tip applies is read from the snapshot
 * ({@link GameState#isTutorial()}), not from a day number kept here.</p>
 */
final class TutorialTips {
    private TutorialTips() {}

    record Tip(String id, String title, String body) {}

    /** Where in the day a tip belongs. */
    enum Moment { LESSON_CHOICE, OTHER_CLASSES, HUB_AFTER_SCHOOL, HUB_EVENING, MORNING_BONUS, DAY_END }

    private static final Map<Moment, Tip> TIPS = Map.of(
            Moment.LESSON_CHOICE, new Tip("lesson-choice", "ONE CHOICE PER LESSON",
                    "After the lecture you get exactly one choice, and its effect is applied once. "
                            + "During the tutorial, penalties only appear as warnings."),
            Moment.OTHER_CLASSES, new Tip("other-classes", "OTHER CLASSES",
                    "This slot belongs to your other class. Focusing on it changes nothing; revising ICS raises XP "
                            + "and costs SAN. The line under each option shows the exact change."),
            Moment.HUB_AFTER_SCHOOL, new Tip("hub-after-school", "STUDENT HUB",
                    "Each activity uses one time slot. SLEEP NOW ends today early and gives you a bonus slot "
                            + "tomorrow morning, where activities have half their effect."),
            Moment.HUB_EVENING, new Tip("hub-evening", "SLEEP CLOSES THE DAY",
                    "Sleeping in the evening or late night ends the day. Using the late-night slot lowers SAN and "
                            + "XP gain the next day; during the tutorial that is only a warning."),
            Moment.MORNING_BONUS, new Tip("morning-bonus", "BONUS MORNING SLOT",
                    "You slept right after school, so this morning has a half slot. Activities here have half "
                            + "their usual effect."),
            Moment.DAY_END, new Tip("day-end", "SAVING",
                    "The game saves automatically here, at the end of each day, and nowhere else. "
                            + "CONTINUE on the title screen reopens this moment."));

    /** The tip for this moment, only while the run is in its tutorial. */
    static Optional<Tip> at(Moment moment, GameState view) {
        if (!view.openingCompleted() || !view.isTutorial()) return Optional.empty();
        return Optional.of(TIPS.get(moment));
    }
}
