package com.aphs.classroomuprising;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Versioned UTF-8 day-end snapshots. No object deserialization and no mutation during load. */
public final class SaveManager {
    public void saveAtDayEnd(GameState state, Path destination) throws IOException {
        GameState s = state.copy();
        if (!s.canSave()) throw new IllegalStateException("Only an active run at DAY_END can be saved (T12).");
        validate(s);
        Properties p = encode(s);
        Path target = destination.toAbsolutePath();
        Path parent = target.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".classroom-save-", ".tmp");
        try {
            try (Writer out = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                p.store(out, "ClassroomUprising day-end snapshot version 1");
            }
            // Unsupported atomic replacement is reported rather than silently risking the old save.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    public GameState load(Path path) throws IOException {
        if (Files.size(path) > 2_000_000) throw new IOException("Snapshot exceeds the format size limit.");
        Properties p = new Properties();
        try (Reader in = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(in); }
        try {
            var r = new Fields(p);
            if (r.integer("version") != 1) throw new IllegalArgumentException("Unsupported save version.");
            var knowledge = new ArrayList<Double>();
            for (int i = 0; i < 6; i++) knowledge.add(r.number("student.unit." + i));
            var student = new StudentState(r.number("student.hp"), r.number("student.san"), r.number("student.vibe"), knowledge);
            var s = new GameState(r.text("name"), student);
            s.day = r.integer("day"); s.period = DayPeriod.valueOf(r.text("period"));
            s.status = GameState.Status.valueOf(r.text("status"));
            s.openingCompleted = r.bool("opening"); s.lessonShown = r.bool("lessonShown");
            s.nextMorningBonus = r.bool("morningBonus"); s.nextMorningSanBonus = r.number("morningSan");
            s.run = new RunProgress(r.integer("retakes"));
            s.grades.unexcusedAbsences = r.integer("absences");
            for (int i = 0, n = r.count("grades.count"); i < n; i++) {
                String k = "grade." + i + ".";
                var qs = new ArrayList<String>();
                for (int j = 0, m = r.count(k + "questions"); j < m; j++) qs.add(r.text(k + "q." + j));
                var a = new AssessmentRecord(r.text(k+"id"), CourseType.valueOf(r.text(k+"type")),
                        r.integer(k+"submitted"), r.integer(k+"return"), qs, r.bool(k+"makeup"),
                        r.number(k+"score"), r.bool(k+"released"), r.text(k+"evidence"));
                s.grades.submit(a);
            }
            for (int i = 0, n = r.count("effects.count"); i < n; i++) {
                String k = "effect." + i + ".";
                s.effects.add(new TimedEffect(r.text(k+"id"), r.integer(k+"first"), r.integer(k+"last"),
                        r.number(k+"san"), r.number(k+"xp")));
            }
            for (int i = 0, n = r.count("makeups.count"); i < n; i++) {
                int day = r.integer("makeup."+i+".day"), original = r.integer("makeup."+i+".original");
                if (s.makeups.put(day, original) != null) throw new IllegalArgumentException("Duplicate makeup slot.");
            }
            for (String item : r.strings("missed")) {
                if (!s.missedAssessments.add(Integer.parseInt(item))) throw new IllegalArgumentException("Duplicate absence.");
            }
            s.seriousCheating.addAll(r.strings("zeros")); s.feedbackApplied.addAll(r.strings("feedback"));
            s.rewards.addAll(r.strings("rewards"));
            readNumbers(r, "estimate", s.estimates); readNumbers(r, "relationship", s.relationships);
            r.finish(); validate(s); return s;
        } catch (RuntimeException e) { throw new IOException("Invalid snapshot: " + e.getMessage(), e); }
    }
    private static void readNumbers(Fields r, String prefix, Map<String, Double> into) {
        for (int i = 0, n = r.count(prefix+".count"); i < n; i++)
            if (into.put(r.text(prefix+"."+i+".id"), r.number(prefix+"."+i+".value")) != null)
                throw new IllegalArgumentException("Duplicate " + prefix);
    }
    private static Properties encode(GameState s) {
        var p = new Properties();
        put(p,"version",1); put(p,"name",s.name); put(p,"day",s.day); put(p,"period",s.period);
        put(p,"status",s.status); put(p,"opening",s.openingCompleted); put(p,"lessonShown",s.lessonShown);
        put(p,"morningBonus",s.nextMorningBonus); put(p,"morningSan",s.nextMorningSanBonus);
        put(p,"retakes",s.run.retakesUsed()); put(p,"absences",s.grades.unexcusedAbsences);
        put(p,"student.hp",s.student.hp()); put(p,"student.san",s.student.san()); put(p,"student.vibe",s.student.codingVibe());
        for (int i = 0; i < 6; i++) put(p,"student.unit."+i,s.student.unitKnowledge().get(i));
        var records = s.grades.records(); put(p,"grades.count",records.size());
        for (int i = 0; i < records.size(); i++) {
            var a = records.get(i); String k = "grade."+i+".";
            put(p,k+"id",a.id); put(p,k+"type",a.type); put(p,k+"submitted",a.submittedDay);
            put(p,k+"return",a.returnDay); put(p,k+"makeup",a.makeup); put(p,k+"score",a.score);
            put(p,k+"released",a.released); put(p,k+"evidence",a.evidence); put(p,k+"questions",a.questionIds.size());
            for (int j = 0; j < a.questionIds.size(); j++) put(p,k+"q."+j,a.questionIds.get(j));
        }
        put(p,"effects.count",s.effects.size());
        for (int i = 0; i < s.effects.size(); i++) {
            var e = s.effects.get(i); String k = "effect."+i+".";
            put(p,k+"id",e.id()); put(p,k+"first",e.firstDay()); put(p,k+"last",e.lastDay());
            put(p,k+"san",e.sanDelta()); put(p,k+"xp",e.xpMultiplier());
        }
        put(p,"makeups.count",s.makeups.size()); int i = 0;
        for (var e : s.makeups.entrySet()) { put(p,"makeup."+i+".day",e.getKey()); put(p,"makeup."+i+".original",e.getValue()); i++; }
        strings(p,"missed",s.missedAssessments.stream().map(Object::toString).toList());
        strings(p,"zeros",s.seriousCheating); strings(p,"feedback",s.feedbackApplied); strings(p,"rewards",s.rewards);
        numbers(p,"estimate",s.estimates); numbers(p,"relationship",s.relationships);
        return p;
    }
    private static void put(Properties p, String key, Object value) { p.setProperty(key, value.toString()); }
    private static void strings(Properties p, String key, Collection<String> values) {
        put(p,key+".count",values.size()); int i=0; for (String v:values) put(p,key+"."+i++,v);
    }
    private static void numbers(Properties p, String key, Map<String,Double> values) {
        put(p,key+".count",values.size()); int i=0;
        for (var e:values.entrySet()) { put(p,key+"."+i+".id",e.getKey());put(p,key+"."+i+".value",e.getValue());i++; }
    }
    private static void validate(GameState s) {
        SchoolCalendar.day(s.day);
        if (!s.canSave() || s.grades.unexcusedAbsences < 0 || s.nextMorningSanBonus < 0
                || (!s.nextMorningBonus && s.nextMorningSanBonus != 0))
            throw new IllegalArgumentException("Invalid day-end state.");
        s.effectiveSan(); s.xpMultiplier();
        Set<String> effects = new HashSet<>();
        for (var e:s.effects) if (!effects.add(e.id())) throw new IllegalArgumentException("Duplicate effect id.");
        Set<Integer> originals = new HashSet<>();
        for (var e:s.makeups.entrySet()) {
            DayPlan original = SchoolCalendar.day(e.getValue());
            if (!original.course().isAssessment() || original.course().isMajor()
                    || !SchoolCalendar.makeupCandidates(original.day()).contains(e.getKey())
                    || !originals.add(original.day()))
                throw new IllegalArgumentException("Invalid makeup mapping.");
        }
        for (int missed:s.missedAssessments) {
            var d=SchoolCalendar.day(missed);
            if (missed>s.day || !d.course().isAssessment() || d.course().isMajor() || s.grades.records.containsKey(d.assessmentId()))
                throw new IllegalArgumentException("Invalid unresolved missed assessment.");
        }
        for (var a:s.grades.records()) {
            var original=SchoolCalendar.days().stream().filter(d->d.course().isAssessment() && d.assessmentId().equals(a.id)).findFirst()
                    .orElseThrow(()->new IllegalArgumentException("Unknown assessment id."));
            if (original.course()!=a.type || a.submittedDay>s.day || a.submittedDay<original.day()
                    || a.makeup!=(a.submittedDay!=original.day()) || a.released!=(a.returnDay<=s.day))
                throw new IllegalArgumentException("Inconsistent mark dates/type.");
        }
        for (var e:s.estimates.entrySet()) {
            if (!Double.isFinite(e.getValue()) || e.getValue()<0 || e.getValue()>100
                    || s.grades.record(e.getKey()).released || !effects.contains("estimate:"+e.getKey()))
                throw new IllegalArgumentException("Invalid provisional estimate.");
        }
        for (String id:s.feedbackApplied) if (!s.grades.record(id).released) throw new IllegalArgumentException("Feedback before official result.");
        for (String id:s.seriousCheating) if (s.grades.record(id).score!=0) throw new IllegalArgumentException("Serious cheating mark is not zero.");
        s.relationships.forEach((id,value)-> { if(id.isBlank()) throw new IllegalArgumentException("Empty peer id."); StatChange.requireFinite(value); });
    }
    private static final class Fields {
        private final Properties values; private final Set<String> read = new HashSet<>();
        Fields(Properties values) { this.values=values; }
        String text(String key) {
            String value=values.getProperty(key);
            if(value==null) throw new IllegalArgumentException("Missing field "+key);
            read.add(key);return value;
        }
        int integer(String key) { return Integer.parseInt(text(key)); }
        int count(String key) { int n=integer(key); if(n<0 || n>10_000) throw new IllegalArgumentException("Invalid count.");return n; }
        double number(String key) { double v=Double.parseDouble(text(key));StatChange.requireFinite(v);return v; }
        boolean bool(String key) {
            String v=text(key); if(!v.equals("true")&&!v.equals("false")) throw new IllegalArgumentException("Invalid boolean.");return Boolean.parseBoolean(v);
        }
        List<String> strings(String prefix) {
            var result=new ArrayList<String>();int count=count(prefix+".count");
            for(int i=0;i<count;i++) result.add(text(prefix+"."+i));
            if(new HashSet<>(result).size()!=result.size()) throw new IllegalArgumentException("Duplicate set entry.");
            return result;
        }
        void finish() { if(read.size()!=values.size()) throw new IllegalArgumentException("Unknown snapshot fields."); }
    }
}
