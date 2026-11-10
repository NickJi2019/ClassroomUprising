package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DemoProfile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The log's record of shown lines: each line keeps the slide that was up when it was said. */
class DialogueHistoryTest {
    @Test void eachLineKeepsTheSlideThatWasUp() {
        DialogueHistory history = new DialogueHistory();
        history.beginScript();
        DialogueScript.parse("""
                Mr. Reward: Before any slide.
                Mr. Reward: [slide:loops] One.
                Mr. Reward: Two.
                Mr. Reward: [slide:methods] Three.
                Mr. Reward: Four.
                """, "Mr. Reward").forEach(history::shown);
        assertEquals(java.util.Arrays.asList(null, "loops", "loops", "methods", "methods"),
                history.entries().stream().map(DialogueHistory.Entry::slide).toList());
        assertEquals("Two.", history.entries().get(2).line().text());
    }

    @Test void aNewScriptStartsWithNoSlide() {
        DialogueHistory history = new DialogueHistory();
        history.beginScript();
        history.shown(DialogueScript.parse("Mr. Reward: [slide:loops] Lecture.", "Mr. Reward").get(0));
        history.beginScript();   // e.g. the teacher's question after the lecture, when the projector has gone
        history.shown(new DialogueScript.Line("Mr. Reward", "Which operator compares two values?"));
        assertEquals("loops", history.entries().get(0).slide());
        assertNull(history.entries().get(1).slide());
        assertEquals(2, history.entries().size(), "earlier lines stay in the log");
    }

    @Test void theWholeReviewLectureReplaysWithItsSlides() throws IOException {
        var lecture = DialogueScript.parse(DemoProfile.load().content().lesson(1).text(), "Mr. Reward");
        DialogueHistory history = new DialogueHistory();
        history.beginScript();
        lecture.forEach(history::shown);
        List<DialogueHistory.Entry> log = history.entries();
        assertEquals(lecture.size(), log.size());
        String current = null;
        for (int i = 0; i < lecture.size(); i++) {
            if (lecture.get(i).slide() != null) current = lecture.get(i).slide();
            assertEquals(current, log.get(i).slide(), "line " + (i + 1));
        }
        assertNotNull(log.get(0).slide(), "the review lecture cues a slide on its first line");
    }

    @Test void theLogScrollsOnlyAsFarAsNeeded() {
        assertEquals(0, DialogueHistory.firstVisible(3, 2, 6), "short logs never scroll");
        assertEquals(10, DialogueHistory.firstVisible(16, 15, 6), "opens on the newest line at the bottom");
        assertEquals(0, DialogueHistory.firstVisible(16, 0, 6));
        assertEquals(0, DialogueHistory.firstVisible(16, 5, 6), "row 6 is still in the first page");
        assertEquals(1, DialogueHistory.firstVisible(16, 6, 6));
        assertEquals(10, DialogueHistory.firstVisible(16, 20, 6), "never past the end");
    }
}
