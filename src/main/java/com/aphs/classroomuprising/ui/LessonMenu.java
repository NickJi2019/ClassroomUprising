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

    /**
     * When you stay quiet, a classmate takes the chance instead and gives the first appropriate
     * response in authored order (every decision has one; {@link LessonDecision} requires it).
     * Shown only: the class is already settled as quiet and the classmate's answer changes
     * nothing for you.
     */
    static LessonDecision.Option classmateAnswer(LessonDecision decision) {
        return decision.responses().stream().filter(o -> o.outcome() == LessonDecision.Outcome.APPROPRIATE)
                .findFirst().orElseThrow();
    }

    /** The name tag of the classmate who answers (no art: the voice comes from the room). */
    static final String CLASSMATE = "Classmate";

    /** Mr. Reward's expression as he puts a question to the class: pointing at you. */
    static final String ASKING = "gotcha";

    /**
     * Mr. Reward's reaction once the class is settled: approval for an appropriate response,
     * mockery for an inappropriate one, a stare for staying quiet. Only the outcome the core
     * settled chooses it; it changes nothing.
     */
    static String reaction(LessonDecision.Outcome outcome) {
        return switch (outcome) {
            case APPROPRIATE -> "thumbs-up";
            case INAPPROPRIATE -> "ridicule";
            case SKIP -> "staring";
        };
    }

    /** The line with this expression, unless the line already names its own. */
    static DialogueScript.Line withExpression(DialogueScript.Line line, String expression) {
        if (line.expression() != null) return line;
        return new DialogueScript.Line(line.speaker(), expression, line.text(), line.characterId(), line.slide());
    }

    /** The side prompt over the responses. */
    static String responsePrompt(ContentCatalog.Lesson lesson) {
        return spoken(lesson.decision()) ? UiText.responsePrompt(lesson.interaction()) : lesson.decision().prompt();
    }
}
