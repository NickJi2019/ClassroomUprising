package com.aphs.classroomuprising;

/** A rubric item supplied by the teacher/team, independent of test correctness and mark weights. */
public interface StyleRule {
    record Finding(String id, boolean passed, String explanation) {}
    String id();
    Finding inspect(String source);
    /** A mechanical example rule, only active when explicitly included in a rubric. */
    record LineLength(String id, int maximum) implements StyleRule {
        public LineLength {
            if(id==null || id.isBlank() || maximum<1) throw new IllegalArgumentException("Invalid style rule.");
        }
        public Finding inspect(String source) {
            boolean pass=source.lines().allMatch(line->line.length()<=maximum);
            return new Finding(id,pass,"Maximum line length: "+maximum);
        }
    }
}
