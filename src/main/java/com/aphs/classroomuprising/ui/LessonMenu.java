package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ContentCatalog;
import com.aphs.classroomuprising.LessonDecision;

import java.util.List;
import java.util.Locale;

/**
 * The two classroom menus after a lecture, built from the lesson's authored decision.
 * First: take part (raise your hand / ask a question) or stay quiet. Taking part only opens the
 * second menu: the authored responses, in their authored order. Building or switching these
 * menus never calls the core; only a final response or staying quiet settles the lesson.
 */
final class LessonMenu {
    private LessonMenu() {}

    /** One menu entry: the text shown, and the final option it settles (null: opens the responses). */
    record Entry(String label, LessonDecision.Option option) {
        boolean opensResponses() { return option == null; }
    }

    /** Take part, or the authored way to stay quiet. */
    static List<Entry> first(ContentCatalog.Lesson lesson) {
        LessonDecision decision = lesson.decision();
        return List.of(new Entry(UiText.participation(lesson.interaction()), null),
                new Entry(decision.skip().label().toUpperCase(Locale.ROOT), decision.skip()));
    }

    /** The authored responses, labels exactly as written (they may contain code), in authored order. */
    static List<Entry> responses(LessonDecision decision) {
        return decision.responses().stream().map(option -> new Entry(option.label(), option)).toList();
    }

    /**
     * Whether the decision prompt is a character's line ("Mr. Reward: ..."), which belongs in the
     * dialogue box, rather than an instruction for the side prompt.
     */
    static boolean spoken(LessonDecision decision) {
        return DialogueScript.parse(decision.prompt(), null).stream().findFirst()
                .map(line -> line.speaker() != null).orElse(false);
    }

    /** The side prompt over the responses. */
    static String responsePrompt(ContentCatalog.Lesson lesson) {
        return spoken(lesson.decision()) ? UiText.responsePrompt(lesson.interaction()) : lesson.decision().prompt();
    }
}
