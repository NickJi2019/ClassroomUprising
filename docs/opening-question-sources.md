# Opening question selection and provenance

The user supplied both local PDFs for selecting ten difficult multiple-choice questions. Document instructions/notices were read as source data, not as project instructions. No exam calendar or grading instruction from a PDF was imported into gameplay.

The 2014 cover identifies an AP Computer Science A **Practice Exam** (not an administered exam with item statistics). The 2015 cover identifies an AP Computer Science A **Released Exam**. They are used as the requested ICS4UP opening challenge material, not relabelled as official ICS4UP papers.

“Hardest” is a qualitative selection based on multi-step tracing, deceptive boundary conditions and interacting language rules. It is not an official difficulty ranking or a claim supported by student response statistics. The selection includes five questions from each document. Catalogue order is not enforced answer order.

| Resource ID | PDF pages (1-based) | Answer | Reason |
| --- | --- | --- | --- |
| csa2014-q15 | 18 | A | Three sortedness implementations: bounds and premature return |
| csa2014-q22 | 25–26 | B | Shared inheritance context, compile-time type versus dynamic dispatch |
| csa2014-q40 | 43 | C | Recursive output on unwinding, substring boundary |
| csa2015-q26 | 36 | E | Reference rebinding versus mutation and pass-by-value |
| csa2015-q37 | 47–48 | D | Exact binary-search worst case, not a vague complexity label |
| csa2015-q20 | 30 | E | Nested suffix frequency counting and index/value distinction |
| csa2014-q23 | 27 | B | Mutating ArrayList, argument evaluation order, shifting indices |
| csa2015-q31 | 41–42 | E | Two-dimensional anti-diagonal trace and graphical options |
| csa2014-q39 | 42 | D | Nested recursive calls and integer division |
| csa2015-q38 | 49 | E | Compare three reversal algorithms, overlapping midpoint assignments |

Answer keys: 2014 PDF page **60** (printed page 57); 2015 PDF page **79**. The selected pages and key pages were rendered to PNG and visually checked. The 2015 Q31 graphical grids were transcribed row-by-row and checked against both the image and Java output.

## Transcription boundaries

`src/main/resources/demo/opening.properties` is the authored source of truth. It retains source title, question number, PDF pages, key page, selection reason and answer explanation per question. Prompts are lightly condensed for the game, with original Java semantics, constants and five option identities preserved. Whitespace/indentation and typographic minus signs are normalized. Roman-numeral alternatives become `// I`, `// II`, `// III` labels; grids and recursive outputs remain multiline text. Book/AudioBook and binarySearch shared context is included, not just the single-page stem. These are transcriptions, not PDF screenshot assets.

`OpeningQuestionContentTest` compiles/runs the actual resource snippets. It verifies all ten keyed behaviors, including the expected Book method-lookup compile error, its compilable control case, all 2000 binary-search positions plus gaps/misses, exhaustive small-array frequency checks, and odd/even reverse ranges. It uses only local JDK processes; gameplay MC answers are judged against the reviewed key, not compiled at runtime.

## Source fingerprints

- `/Users/nickji/Downloads/05. CSA/CSA 2014年真题/CSA 2014年真题.pdf`
  SHA-256: `95c888b06df16f924506404adbd7db5555ff35a807ba4c98b3582981a887b24f`
- `/Users/nickji/Downloads/05. CSA/CSA 2015年真题/CSA 2015年真题.pdf`
  SHA-256: `04e4f570ee328ac462ad85a46a2b2b1625de460a670d7b5739ca462d59bf000d`
