package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.ContentCatalog;
import com.aphs.classroomuprising.CourseType;
import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.DayPlan;
import com.aphs.classroomuprising.TimedEffect;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Player-facing names for core identifiers. Wording only; no rules live here. */
final class UiText {
    private UiText() {}

    static String course(CourseType type) {
        return switch (type) {
            case LESSON -> "LESSON";
            case LAB -> "LAB";
            case UNIT_TEST -> "UNIT TEST";
            case REVIEW -> "REVIEW";
            case MIDTERM -> "MIDTERM";
            case FINAL -> "FINAL";
        };
    }

    /** Short label for calendar cells. */
    static String courseShort(CourseType type) {
        return switch (type) {
            case LESSON -> "L";
            case LAB -> "LAB";
            case UNIT_TEST -> "TEST";
            case REVIEW -> "REV";
            case MIDTERM -> "MID";
            case FINAL -> "FIN";
        };
    }

    /** The first assessment strictly after {@code day} on the fixed calendar, if any. */
    static java.util.Optional<DayPlan> nextAssessment(int day) {
        return com.aphs.classroomuprising.SchoolCalendar.days().stream()
                .filter(p -> p.day() > day && p.course().isAssessment())
                .findFirst();
    }

    /** "NEXT  LAB · DAY 05 · IN 2 DAYS", or empty after the last assessment. */
    static String nextAssessmentLine(int day) {
        return nextAssessment(day).map(p -> {
            int wait = p.day() - day;
            return "NEXT  " + course(p.course()) + " · DAY %02d · ".formatted(p.day())
                    + (wait == 1 ? "TOMORROW" : "IN " + wait + " DAYS");
        }).orElse("");
    }

    /** "LAB · UNIT 1"; the two tutorial days are the Grade 11 review lessons. */
    static String courseLine(DayPlan plan) {
        String suffix = plan.tutorial() ? " · GR11 REVIEW" : plan.unit() > 0 ? " · UNIT " + plan.unit() : "";
        return course(plan.course()) + suffix;
    }

    static String period(DayPeriod period) {
        return switch (period) {
            case MORNING_BONUS -> "MORNING BONUS";
            case ICS_CLASS -> "ICS4U";
            case OTHER_CLASSES -> "OTHER CLASSES";
            case AFTER_SCHOOL -> "AFTER SCHOOL";
            case EVENING -> "EVENING";
            case LATE_NIGHT -> "LATE NIGHT";
            case DAY_END -> "DAY END";
        };
    }

    static String interactionPrompt(ContentCatalog.Interaction interaction) {
        return switch (interaction) {
            case ASK_TEACHER -> "You have a question about this lesson.";
            case RAISE_HAND -> "Mr. Reward asks the class. Raise your hand?";
        };
    }

    /** The first-level button that opens a lesson's response menu. */
    static String participation(ContentCatalog.Interaction interaction) {
        return switch (interaction) {
            case ASK_TEACHER -> "ASK A QUESTION";
            case RAISE_HAND -> "RAISE YOUR HAND";
        };
    }

    /** The side prompt over the responses when the decision prompt itself is spoken in the dialogue box. */
    static String responsePrompt(ContentCatalog.Interaction interaction) {
        return switch (interaction) {
            case ASK_TEACHER -> "Choose your question.";
            case RAISE_HAND -> "Choose your answer.";
        };
    }

    /**
     * The HUD course line with the lesson's authored title. Only lessons have titles, so the title
     * replaces the "LESSON" word (keeping the line clear of the tip card); the unit stays.
     */
    static String courseLine(DayPlan plan, String title) {
        if (title == null || title.isBlank()) return courseLine(plan);
        String unit = !plan.tutorial() && plan.unit() > 0 ? "UNIT " + plan.unit() + " · " : "";
        return unit + title.strip().toUpperCase(java.util.Locale.ROOT);
    }

    static String lessonChoice(String id) {
        return switch (id) {
            case "ask" -> "ASK A QUESTION";
            case "quiet" -> "STAY QUIET";
            default -> readable(id);
        };
    }

    static String hubActivity(String id) { return readable(id); }

    /** Stable order for authored choices: alphabetical, with the silent option last. */
    static List<String> choiceOrder(Collection<String> ids) {
        return ids.stream()
                .sorted(Comparator.comparing((String id) -> id.equals("quiet")).thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    static String effect(TimedEffect effect) {
        if (effect.id().startsWith("late-night:")) return "Stayed up late";
        if (effect.id().startsWith("estimate:")) return "Self-estimate (provisional)";
        return effect.id();
    }

    private static String readable(String id) {
        return id.replace('_', ' ').replace('-', ' ').toUpperCase(Locale.ROOT);
    }
}
