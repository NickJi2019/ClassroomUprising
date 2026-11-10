# Backend request: narrate the Grade 11 review slides

Date: 2026-11-10. From: frontend. To: backend owner of `src/main/resources/demo/demo.properties`.

Backend integration status: applied to the bundled lesson text on 2026-11-10. The two property lines below are preserved verbatim. The old example checks have been updated, and per-day slide-sequence checks cover all 31 spoken lines. The request and frontend observations below are retained as the handoff record.

The class deck *Review Overview* is now in the game: 12 useful pages are bundled as lecture slides (`assets/slides/gr11-review/`). The dialogue box must carry Mr. Reward's spoken explanation of the slide on screen, so the Day 1–2 lecture text has to be rewritten around those slides. The frontend owns the slides, the projector and the cue parsing; the lecture text and its content tests are yours.

## What to change

1. In `src/main/resources/demo/demo.properties`, replace the two lines `lesson.1.text=...` and `lesson.2.text=...` with the two lines below, exactly. Leave every other key (interactions, choices, effects, days) unchanged.
2. In `Grade11ReviewContentTest`, replace `dayOneJavaExamplesProduceTheTaughtValues` and `dayTwoJavaExamplesProduceTheTaughtValues`: they run code examples from the old lecture (`int tickets = 7;`, `int total = 0;`, ...) that the new lecture no longer contains. Suggested checks on code that appears verbatim in the new text: `System.out.println("'sup?");` prints `'sup?`; `for (int i = 0; i < 100; i += 2)` with `y += i` from `y = 0` ends with `i` last 98 and `y` = 2450; `double cSide = pythag(aSide, bSide);` with `aSide = 3.0`, `bSide = 4.0` and the slide's `pythag` (`Math.sqrt(a * a + b * b)`) gives 5.0; `int[] values = {4, 7, 2};` has `values.length` 3.
3. Update the backend docs (Sprint1/R05, implementation status) as you see fit.

```properties
lesson.1.text=Mr. Reward: [slide:review-purpose] Welcome back. For the next while we review Grade 11 Java. Most of you have not written Java since ICS3U, so the first job is to remember how it works.\nMr. Reward: Some of you took ICS3U online, or at a school that taught another language. This review is your chance to learn enough basic Java to keep up in this course.\nMr. Reward: The other option was me inventing a huge, painful project to fill the time. So, you're welcome. Buckle up: this is going to be dense.\nMr. Reward: [slide:file-structure] Start with the file. A Java source file ends in .java with a lowercase j, and its name must match the class inside it exactly, capital letters included.\nMr. Reward: The line public class followed by that name comes after any import statements and before all other code. Every { needs a matching }, and the class's own pair wraps the whole file.\nMr. Reward: [slide:example-java] Here is Example.java. The class is named Example, so the file is Example.java. Inside it is the main method: public static void main(String[] args).\nMr. Reward: main is the entry point: the computer starts at its first line and runs each line after it in order. Here there is one line, System.out.println("'sup?");, which prints 'sup?\nMr. Reward: [slide:data-types] Computers are glorified calculators, so it all comes down to data. Java has eight basic types. boolean holds true or false; char holds one character in single quotes.\nMr. Reward: byte, short, int and long hold whole numbers, from smallest to largest. float and double hold numbers with decimal places. Everything else is built from these eight.\nMr. Reward: [slide:variables] A variable is a label for a piece of data. Declare one as TYPE LABEL, like int count; or double price; The label starts with a letter and follows the style guide.\nMr. Reward: public and private only belong at class level, outside every method. In this course, use private there. Inside a method, neither is allowed.\nMr. Reward: [slide:math-and-symbols] +, -, * and / do arithmetic, and + also joins two Strings. ++, --, += and -= change a variable in place. A single = assigns a value, as in x = 22;\nMr. Reward: Now the rant: = stores a value, but == compares two values and gives true or false, like >, <, >=, <= and !=. For the contents of two String objects, use .equals instead.\nMr. Reward: !, && and || combine boolean expressions. They short-circuit: in a && b, if a is false, b is never even checked, because the answer is already decided.\nMr. Reward: [slide:decisions] Computers only answer yes or no questions, so we decide with if. In if (age >= 16) { ... }, the block runs only when the condition is true.\nMr. Reward: else if asks another question, and else catches everything left. Both are optional, but the order is always if, else if, else. However many branches you write, only one block runs.
lesson.2.text=Mr. Reward: [slide:loops] Yesterday we covered files, data, operators and decisions. Today we finish the Grade 11 review: loops, methods, and the topics you will learn on your own.\nMr. Reward: Java has two kinds of loop. A counted loop is a for loop: for (COUNTER; CONDITION; STEP). The condition is checked before every pass through the body.\nMr. Reward: In for (int i = 0; i < 100; i += 2), i takes 0, 2, 4 and so on up to 98. y += i adds each one to a running total, so y finishes at 2450, and every pass prints both.\nMr. Reward: A conditional loop is a while loop: while (CONDITION). It repeats as long as the condition is true, like asking for a name until the user actually types one.\nMr. Reward: Notice the balanced {} around everything that belongs to each loop. And if nothing inside a while loop can change its condition, the loop never ends.\nMr. Reward: [slide:methods] A method is a named group of code you can run on demand. Its header is (public/private) TYPE NAME(LIST, OF, PARAMETERS).\nMr. Reward: TYPE is what the method returns. void means nothing comes back; any other type, like int or boolean, must be returned. Parameters are TYPE LABEL pairs: the inputs it needs.\nMr. Reward: [slide:method-example] Here is pythag. It takes double a and double b, computes Math.sqrt(a * a + b * b), and the return statement sends the answer back to the caller.\nMr. Reward: The call double cSide = pythag(aSide, bSide); copies 3.0 into a and 4.0 into b, then stores 5.0 in cSide. System.out.println(cSide); displays it.\nMr. Reward: Because values are copied in, changing a or b inside the method does not change aSide or bSide outside it. Remember that when a method seems to ignore you.\nMr. Reward: [slide:the-rest] This is where the self-directed part kicks in: casting, Strings and their methods, the Math and Random classes, scope, switch, constants, arrays and more.\nMr. Reward: Arrays are on that list for a reason. int[] values = {4, 7, 2}; holds three ints at indices 0, 1 and 2. values.length is 3, so values[values.length] is already past the end.\nMr. Reward: Self-directed does not mean alone. Ask questions, but show some effort first. And if you are feeling adventurous, figure out what static really means.\nMr. Reward: [slide:automated-testing] Before this unit ends, something new: automated testing. You will build a generic test harness and copy it into later work to test your code before you submit it.\nMr. Reward: Sometimes I will mark the quality of those tests too. Start with a normal case and a boundary case. That is the review. Now go practise.
```

## Slide cue format (agreed contract)

- A line may start with a cue right after the speaker: `Mr. Reward: [slide:file-structure] Start with the file...`. The projector turns to that slide when the line begins and keeps it until the next cue. The cue is removed before the line is shown and does not count toward the 200-character limit.
- A cue counts only at the very start of the text; `[...]` later in a line (for example `String[] args`) is ordinary text.
- Valid names are the `slide.<name>` keys in `assets/manifest.properties`: `review-purpose`, `file-structure`, `example-java`, `data-types`, `variables`, `math-and-symbols`, `decisions`, `loops`, `methods`, `method-example`, `the-rest`, `automated-testing`. The frontend test `SlidesAndSoundTest.everySlideTheLecturesCueIsListedAndDelivered` fails if a lecture cues a name that is not listed or not delivered.
- You may reword lines, split them or reorder them freely; keep each cue on the first line that talks about that slide.

## Checks already run by the frontend (on a copy, not on the bundled file)

- With these two lines swapped in: `reviewLecturesParseAsShortMrRewardLinesWithoutGameplayHints` (both days), `theTwoDaysCoverDifferentReviewTopics`, `theArrayBoundaryExampleReallyIsOutOfBounds`, `GameSessionTest` and all frontend slide tests pass. Only the two example-running tests named above fail, as expected.
- Each line is at most 180 characters after the cue, contains no gameplay hint words, and fits the dialogue box.
- In the real classroom screen (scratch harness), all 31 lines showed the expected slide, and the projector left when each lecture ended.
- The code claims were run on a JDK: `'sup?`, 98 and 2450, 5.0, `values[values.length]` throws `ArrayIndexOutOfBoundsException`, and `a && b` with `a` false never evaluates `b`.

## The new lectures at a glance

**Day 1**

| # | Slide | What Mr. Reward says |
| --- | --- | --- |
| 1 | `review-purpose` | Welcome back. For the next while we review Grade 11 Java. Most of you have not written Java since ICS3U, so the first job is to remember how it works. |
| 2 |  | Some of you took ICS3U online, or at a school that taught another language. This review is your chance to learn enough basic Java to keep up in this course. |
| 3 |  | The other option was me inventing a huge, painful project to fill the time. So, you're welcome. Buckle up: this is going to be dense. |
| 4 | `file-structure` | Start with the file. A Java source file ends in .java with a lowercase j, and its name must match the class inside it exactly, capital letters included. |
| 5 |  | The line public class followed by that name comes after any import statements and before all other code. Every { needs a matching }, and the class's own pair wraps the whole file. |
| 6 | `example-java` | Here is Example.java. The class is named Example, so the file is Example.java. Inside it is the main method: public static void main(String[] args). |
| 7 |  | main is the entry point: the computer starts at its first line and runs each line after it in order. Here there is one line, System.out.println("'sup?");, which prints 'sup? |
| 8 | `data-types` | Computers are glorified calculators, so it all comes down to data. Java has eight basic types. boolean holds true or false; char holds one character in single quotes. |
| 9 |  | byte, short, int and long hold whole numbers, from smallest to largest. float and double hold numbers with decimal places. Everything else is built from these eight. |
| 10 | `variables` | A variable is a label for a piece of data. Declare one as TYPE LABEL, like int count; or double price; The label starts with a letter and follows the style guide. |
| 11 |  | public and private only belong at class level, outside every method. In this course, use private there. Inside a method, neither is allowed. |
| 12 | `math-and-symbols` | +, -, * and / do arithmetic, and + also joins two Strings. ++, --, += and -= change a variable in place. A single = assigns a value, as in x = 22; |
| 13 |  | Now the rant: = stores a value, but == compares two values and gives true or false, like >, <, >=, <= and !=. For the contents of two String objects, use .equals instead. |
| 14 |  | !, && and \|\| combine boolean expressions. They short-circuit: in a && b, if a is false, b is never even checked, because the answer is already decided. |
| 15 | `decisions` | Computers only answer yes or no questions, so we decide with if. In if (age >= 16) { ... }, the block runs only when the condition is true. |
| 16 |  | else if asks another question, and else catches everything left. Both are optional, but the order is always if, else if, else. However many branches you write, only one block runs. |

**Day 2**

| # | Slide | What Mr. Reward says |
| --- | --- | --- |
| 1 | `loops` | Yesterday we covered files, data, operators and decisions. Today we finish the Grade 11 review: loops, methods, and the topics you will learn on your own. |
| 2 |  | Java has two kinds of loop. A counted loop is a for loop: for (COUNTER; CONDITION; STEP). The condition is checked before every pass through the body. |
| 3 |  | In for (int i = 0; i < 100; i += 2), i takes 0, 2, 4 and so on up to 98. y += i adds each one to a running total, so y finishes at 2450, and every pass prints both. |
| 4 |  | A conditional loop is a while loop: while (CONDITION). It repeats as long as the condition is true, like asking for a name until the user actually types one. |
| 5 |  | Notice the balanced {} around everything that belongs to each loop. And if nothing inside a while loop can change its condition, the loop never ends. |
| 6 | `methods` | A method is a named group of code you can run on demand. Its header is (public/private) TYPE NAME(LIST, OF, PARAMETERS). |
| 7 |  | TYPE is what the method returns. void means nothing comes back; any other type, like int or boolean, must be returned. Parameters are TYPE LABEL pairs: the inputs it needs. |
| 8 | `method-example` | Here is pythag. It takes double a and double b, computes Math.sqrt(a * a + b * b), and the return statement sends the answer back to the caller. |
| 9 |  | The call double cSide = pythag(aSide, bSide); copies 3.0 into a and 4.0 into b, then stores 5.0 in cSide. System.out.println(cSide); displays it. |
| 10 |  | Because values are copied in, changing a or b inside the method does not change aSide or bSide outside it. Remember that when a method seems to ignore you. |
| 11 | `the-rest` | This is where the self-directed part kicks in: casting, Strings and their methods, the Math and Random classes, scope, switch, constants, arrays and more. |
| 12 |  | Arrays are on that list for a reason. int[] values = {4, 7, 2}; holds three ints at indices 0, 1 and 2. values.length is 3, so values[values.length] is already past the end. |
| 13 |  | Self-directed does not mean alone. Ask questions, but show some effort first. And if you are feeling adventurous, figure out what static really means. |
| 14 | `automated-testing` | Before this unit ends, something new: automated testing. You will build a generic test harness and copy it into later work to test your code before you submit it. |
| 15 |  | Sometimes I will mark the quality of those tests too. Start with a normal case and a boundary case. That is the review. Now go practise. |
