# Foundation implementation status

Date: 2026-11-09. Current action: remove the previous UI and all Main command-line routes; prepare FXGL and libGDX dependencies only.

This record separates implemented behavior from the full design target. No production balancing configuration or missing game rule is approved by this implementation.

| Area | Current implementation | Boundary |
| --- | --- | --- |
| Calendar | Exact 40-day schedule; six units; Midterm day 20 and Final day 40 | Authored content is external |
| Daily flow | Opening acknowledgement, one Lesson choice, other classes, Hub slots, half-slot and closed-day transition | Core API only; no frontend or actual opening battle |
| Effects | Explicit numeric deltas; tutorial negative effects waived; dated next-day effects | Amounts and composition are not production policy defaults |
| Attendance | Two free absences, then percentage-point deductions; leave exempt; no major-exam skip | Tutorial accounting must be supplied; skipped-class free activities not implemented |
| Questions | Fixed ordinary banks; sampled major banks with supplied size/RNG | Test fixtures only; no production bank or timer/answer policy |
| Assessments | Submitted versus official marks; ordinary/makeup return after three days; majors immediate | Marks come from an evaluator, not an invented answer/HP formula |
| Estimates | Separate value and source-labelled provisional effect; removal on official return | Accuracy and repeat policy are not invented |
| Grade feedback | Supplied effect applied exactly once after official return | Automatic average/95 calculation remains T02/T03 |
| Makeups | Caller selects first/second later Lesson; collisions/no-Lesson rejected; same questions, no Lesson reward, configured SAN cost | Detection is a condition flag, not an invented probability |
| Cheating | Configured SAN loss; serious zero survives submission; tutorial exemption | Detection/reward content not implemented |
| Grade arithmetic | 65/10/25 with supplied Midterm relative weight and Participation percent, only after all marks become official | No missing-score zero, floor/cap or HP mapping |
| Run transitions | Two-opportunity counter and selected-owned-reward carryover helper | No complete reset/new-run launch; Midterm Continue/Retake reports T10 |
| Saving | Atomic versioned closed-day snapshot and explicit next-day operation; validation | Drop/Retake/Final policy and migrations remain T12 |
| Java Lab | Actual JDK compilation, child-JVM cases, bounded output/time, independent style findings | Trusted local runner, not a security sandbox or automatic course grader |
| Relationships/rewards | State representation and persistence | No authored interaction/item gameplay |

No fixed damage, ending, SAN, XP, grade or detection constants were filled in. Fixture numbers appear only in tests. PendingRuleException identifies the missing T-number.

## Checks

Run the Gradle test and jar tasks. The JUnit suite includes calendar and rule tests, invalid-input/non-mutation checks, save round trips/corruption/failure checks, and real trusted-Java process tests.

The previous GUI, its demo content/rubric, GUI-specific tests and launcher have been removed. Main is an empty entry point without argument parsing, output, debug hooks or engine startup. Framework dependency tests cover FXGL/JavaFX API availability, a single JavaFX native platform variant, libGDX native pixel operations and a GLFW native query without opening a window. Entry-point tests check that old argument strings have no behavior.

Command output, hashes and rollback evidence accompany this change as verification artifacts, not fabricated gameplay results.

## Remaining design work

The T01–T14 register in Sprint1 remains authoritative: Final combat, automatic grading/feedback, Retake reset/ending policy, production activity values, actual lesson/question/rubric content, normal sleep choices and social/reward mechanics remain open.

The class diagram shows the remaining core classes and the empty Main entry point. It does not present the planned StoryController/AssessmentController objects as working code. The sequence diagram traces callable prototype operations and labels separate planned boundaries. The IPO remains the target, not a claim that every row has been delivered.

## Framework infrastructure

Gradle supplies FXGL 21.1, JavaFX 21.0.11, libGDX 1.14.2, the LWJGL3 desktop backend and native libraries. The Java toolchain and compilation target are 21. OpenJFX's Gradle plugin selects host-specific JavaFX binaries, and FXGL's transitive OpenJFX entries are excluded to prevent mixed architecture jars.

Both framework APIs are available to future frontend code. Neither is started by Main, and no integration between their event loops is implemented. The existing rule, persistence and Java-assessment source files are unchanged. InstallDist packages runtime dependencies; a normal application JAR alone is not a self-contained framework distribution.
