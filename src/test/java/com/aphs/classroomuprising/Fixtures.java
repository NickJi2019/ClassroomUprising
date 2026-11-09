package com.aphs.classroomuprising;

import java.util.*;

/** Deliberately synthetic values; these are not production game rules or authored course content. */
final class Fixtures {
    static RuleValues rules() { return new RuleValues(4.0,.5,3.0,7.0,12.0,RuleValues.TutorialAbsences.IGNORE); }
    static StudentState student() { return new StudentState(50,60,0,Collections.nCopies(6,0.0)); }
    static ContentCatalog catalog() {
        var lessons=new LinkedHashMap<Integer,ContentCatalog.Lesson>();
        var reviews=new LinkedHashMap<Integer,StatChange>();
        var banks=new LinkedHashMap<String,List<ContentCatalog.Question>>();
        for(var d:SchoolCalendar.days()) {
            if(d.course()==CourseType.LESSON) lessons.put(d.day(),new ContentCatalog.Lesson("Fixture "+d.day(),
                    d.day()%2==0?ContentCatalog.Interaction.RAISE_HAND:ContentCatalog.Interaction.ASK_TEACHER,
                    Map.of("learn",StatChange.learning(Math.max(1,d.unit()),8,4,-2),"quiet",StatChange.none())));
            if(d.course()==CourseType.REVIEW) reviews.put(d.day(),StatChange.none());
            if(d.course().isAssessment()) banks.put(d.assessmentId(),List.of(
                    new ContentCatalog.Question(d.assessmentId()+"-a","Fixture A"),
                    new ContentCatalog.Question(d.assessmentId()+"-b","Fixture B"),
                    new ContentCatalog.Question(d.assessmentId()+"-c","Fixture C")));
        }
        return new ContentCatalog(lessons,Map.of("study",StatChange.learning(1,8,4,-2),
                "rest",new StatChange(0,5,0,Collections.nCopies(6,0.0))),reviews,banks,
                Map.of(CourseType.MIDTERM,2,CourseType.FINAL,2),StatChange.learning(1,8,4,-2));
    }
    static GameState state(int day,DayPeriod period) {
        var s=new GameState("Fixture student",student());s.day=day;s.period=period;s.openingCompleted=true;return s;
    }
    static GameController game(int day,DayPeriod period) {
        return new GameController(state(day,period),rules(),catalog(),new Random(42));
    }
    static GameController game(GameState state) { return new GameController(state,rules(),catalog(),new Random(42)); }
    static String signature(GameState s) {
        return s.name()+"|"+s.day()+"|"+s.period()+"|"+s.student()+"|"+s.status()+"|"+s.effects()+"|"+s.estimates()
                +"|"+s.grades().unexcusedAbsences()+"|"+s.makeups()+"|"+s.missedAssessments()+"|"+s.nextMorningBonus
                +"|"+s.nextMorningSanBonus+"|"+s.lessonShown+"|"+s.active+"|"+s.grades().records().stream().map(r->r.id+":"+r.score+":"+r.released).toList();
    }
}
