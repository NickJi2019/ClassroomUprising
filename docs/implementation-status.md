# Foundation implementation status

Date: 2026-11-09. Requested action: “实现”, following the approved IPO/class/sequence update.

This record separates implemented behavior from the full design target. No production balancing configuration or missing game rule is approved by this implementation.

| Area | Current implementation | Boundary |
| --- | --- | --- |
| Calendar | Exact 40-day schedule; six units; Midterm day 20 and Final day 40 | Authored content is external |
| Daily flow | Opening acknowledgement, one Lesson choice, other classes, Hub slots, half-slot and closed-day transition | No GUI or actual opening battle |
| Effects | Explicit numeric deltas; tutorial negative effects waived; dated next-day effects | Amounts and composition are not production policy defaults |
| Attendance | Two free absences, then percentage-point deductions; leave exempt; no major-exam skip | Tutorial accounting must be supplied; skipped-class free activities not implemented |
| Questions | Fixed ordinary banks; sampled major banks with supplied size/RNG | No actual course bank or timer/answer policy |
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

No fixed damage, ending, SAN, XP, grade or detection constants were filled in. Fixture numbers appear only in explicitly labelled demos/tests. PendingRuleException identifies the missing T-number.

## Checks

Run the Gradle test and jar tasks. The JUnit suite includes calendar and rule tests, invalid-input/non-mutation checks, save round trips/corruption/failure checks, and real trusted-Java process tests.

The **--demo** command performs a real fixture save/load and Day 1–4 flow. **--lab-demo** executes two actual input/output cases and a line-length rubric example. Neither is a complete game playthrough.

Command output, hashes and rollback evidence accompany this change as verification artifacts, not fabricated gameplay results.

## Remaining design work

The T01–T14 register in Sprint1 remains authoritative: Final combat, automatic grading/feedback, Retake reset/ending policy, production activity values, actual lesson/question/rubric content, normal sleep choices and social/reward mechanics remain open.

The class diagram shows implemented classes. It no longer presents the planned GameUI/StoryController/AssessmentController objects as working code. The sequence diagram traces callable prototype operations and labels separate planned boundaries. The IPO remains the target, not a claim that every row has been delivered.
