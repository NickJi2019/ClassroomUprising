# Art asset specification

How to deliver backgrounds, character art, textures, lecture slides, music and sound effects so they appear in the game without code changes.

## How it works

- All art lives in `src/main/resources/com/aphs/classroomuprising/assets/`.
- [`manifest.properties`](../src/main/resources/com/aphs/classroomuprising/assets/manifest.properties) in that folder maps a **key** (what the code asks for, e.g. `background.classroom`) to a **file**. Code never names files.
- To add art: save the file at the path the manifest lists, then rebuild (`./gradlew installDist`). To add a new variant or expression, add one line to the manifest.
- Anything listed but not delivered is shown in the game as a grey **"MISSING ART"** plate that names the key and the expected file. The game also logs each missing file once. Plates are deliberately plain: they show where art goes, not what it should look like.
- Use only original art or art the team has the rights to. Do not use screenshots, renders, fonts or sounds taken from other games.

## Canvas

The game is laid out on a **1280×720** canvas (16:9) that scales to the window. Deliver art at **2×** so it stays sharp on high-density screens.

## Backgrounds

| Rule | Value |
| --- | --- |
| Size | 2560×1440 (16:9) |
| Format | PNG or JPG |
| Safe area | Keep important content inside the central ~2400×1300: the art is scaled up slightly and drifts slowly, so about 80 px may be cropped at each edge |
| Key | `background.<place>` |
| Optional time-of-day art | `background.<place>.<time>` where `<time>` is `morning_bonus`, `ics_class`, `other_classes`, `after_school`, `evening`, `late_night` or `day_end`. When it is missing, the base art is used and tinted by the colour grade |
| Optional foreground | `foreground.<place>`: transparent PNG at the same size, drawn **in front of** characters (desks, a railing). It drifts more than the background, which gives depth |

Places used now: `title`, `opening` (last semester's exam hall), `classroom`, `hallway`, `hub` (Student Hub), `dorm`, `computer-lab`. Assessment combats reuse them with the `combat` grade: a Lab in `computer-lab`, a Unit Test in `classroom`, the Midterm and Final in `opening` (the exam hall).

Avoid strong detail behind the left and right edges where menus sit; the game adds dark gradients there for readability.

## Characters

| Rule | Value |
| --- | --- |
| Format | Transparent PNG, no background, no baked floor shadow |
| Size | Same canvas size for every expression of one character, figure in exactly the same place, so expressions can crossfade. Deliver about 2× the display height (e.g. ~1300 px tall for a 660 px display height) |
| Anchor | The point that stands on the character's slot, as fractions of the image: `character.<id>.anchor = 0.5, 1.0` means bottom centre (the feet). Keep it the same for all expressions |
| Display height | `character.<id>.height` in canvas pixels (720 = full screen height) |
| Expressions | `character.<id>.expression.<name>`. `neutral` is required; others use short lower-case names (`smile`, `stern`, `worried`, …). A missing expression falls back to `neutral` |
| Dialogue name | `character.<id>.name` must match the speaker name used in dialogue text (e.g. `Mr. Reward`); `You` is the player |

In dialogue text, a line can ask for an expression: `Mr. Reward[smile]: Good question.` The tag is not shown to the player. *This text format needs agreement with the content authors before they rely on it.*

The game animates the art itself (gentle breathing and sway, stepping back and dimming when another character speaks), so poses should be relaxed and centred on the anchor.

## Textures

Tileable (seamless) PNGs with transparency, used by the ambient layer:

| Key | Size | Use |
| --- | --- | --- |
| `texture.grain` | 256×256 | Film grain over the whole scene, re-positioned 12 times a second |
| `texture.motes` | 512×512 | Soft light specks that rise slowly in steps (8 per second) |

The current two files are generated placeholders and can be replaced.

## Colour grades

Each time of day, plus `combat` for assessment combats, has a grade line in the manifest: `grade.<name> = #tint, BLEND_MODE, tintOpacity, saturation, brightness`. The tint is a full-screen colour blended over the background (`MULTIPLY` darkens, `SCREEN` lightens, `OVERLAY`/`SOFT_LIGHT` shift contrast and hue); saturation and brightness run from -1 to 1. Artists can tune these without code changes. Characters are not graded, so they stay readable in any light.

## Interface artwork (optional)

Some interface parts can use artwork; until it is delivered the game draws them itself.

| Key | Rule |
| --- | --- |
| `ui.clock-face` | Square transparent PNG (e.g. 640×640). Shown 320×320 on the canvas. The 24-hour schedule ring is drawn around it by the game |
| `ui.clock-hour-hand`, `ui.clock-minute-hand` | Same square size as the face, transparent, the hand drawn pointing to **12 o'clock**; the game rotates each image about its centre |

## Transition frame sequences (optional)

Hand-drawn frames can replace the built-in transitions. Each sequence is a numbered run of **1280×720 transparent PNGs** (deliver at 2560×1440 for sharpness) played at a fixed frame time with no in-betweening:

```
sequence.<name>.frames  = transitions/<name>/frame-%02d.png   (numbered from 00)
sequence.<name>.count   = 10
sequence.<name>.frame-ms = 50     (optional, default 50 = 20 frames per second)
sequence.<name>.swap-at = 5       (optional, default count/2: the frame on which the screen behind changes)
```

| Name | Used for |
| --- | --- |
| `screen` | Every screen change except the clock (time of day) and the day card |
| `menu-open` | Opening the Esc menu; the menu appears on the swap frame |

Frames should cover the whole canvas around the swap frame, so the change underneath is hidden. A sequence is used only when every frame is delivered; otherwise the built-in transition plays. None are listed in the manifest yet.

## Lecture slides

While Mr. Reward lectures, the lesson's slides appear on a projector panel beside him and change as the lecture goes on.

- One line per slide: `slide.<day>.<line> = slides/day-01/02-variables-and-types.png`. The slide appears when the lecture reaches line `<line>` of that day's lesson and stays until the next listed slide. Lines count from 1, one per dialogue page, in the order of the lesson text in `demo/demo.properties`. The first slide should start at line 1.
- **16:9**, delivered at **1600×900** or larger (the panel shows 608×342; the enlarged view shows about 1064×598, so small code must stay readable at that size). PNG for text and code, JPEG only for photos.
- Keep text large: at most about 6 lines of code or 4 bullet points per slide. Use the game's colours if you like, but a plain light background reads best on the dark interface.
- A missing slide shows a light **"MISSING SLIDE"** plate with the day, line and file.
- If the lecture text is reordered or lines are split or merged, update the line numbers here too; a test fails if a listed line does not exist.

Days 1–2 (the Grade 11 review) currently request six slides each:

| Day | Line | File | Topic |
| --- | --- | --- | --- |
| 1 | 1 | `slides/day-01/01-review-plan.png` | Plan: values, expressions, decisions |
| 1 | 2 | `slides/day-01/02-variables-and-types.png` | `int`, `double`, `String`, `boolean`; assignment |
| 1 | 5 | `slides/day-01/03-integer-division.png` | `7 / 2` versus `7 / 2.0` |
| 1 | 8 | `slides/day-01/04-operator-order.png` | `2 + 3 * 4` versus `(2 + 3) * 4` |
| 1 | 9 | `slides/day-01/05-conditions-and-if.png` | `&&`, if/else, `=` versus `==`, `.equals`, println |
| 1 | 13 | `slides/day-01/06-trace-it.png` | Trace values, evaluate, follow the branch |
| 2 | 1 | `slides/day-02/01-review-plan.png` | Plan: repetition, arrays, methods |
| 2 | 2 | `slides/day-02/02-for-loops.png` | for loop parts; printing 012; accumulator |
| 2 | 5 | `slides/day-02/03-while-loops.png` | while loop; loops that never stop |
| 2 | 7 | `slides/day-02/04-arrays.png` | `int[] values = {4, 7, 2}`; indices; length; sum |
| 2 | 10 | `slides/day-02/05-methods.png` | `doubleValue`; parameters, return, call |
| 2 | 13 | `slides/day-02/06-test-and-explain.png` | Normal and boundary cases; summary |

## Music

- One line per cue: `bgm.<cue> = audio/bgm/school-day.mp3`. Each screen asks for a cue; the code never names files.
- Cues in use: `title` (title screen), `opening` (opening scene), `school` (day card, classroom, other classes, bonus morning), `after-school` (Hub after school), `night` (Hub in the evening and late night, day end), `combat` (assessment combat).
- Formats JavaFX can play: **MP3**, **AAC in .m4a**, **WAV**, **AIFF** (not Ogg, not FLAC). MP3 at 160–192 kbps or AAC at 128–192 kbps, 44.1 or 48 kHz, stereo.
- Tracks loop from end to start, so make the end lead back into the beginning without a gap or click; keep about 1–3 minutes per loop. Normalise tracks to a similar loudness (about −16 LUFS) so cues do not jump in volume.
- The same cue keeps playing across screens; a new cue fades the old track out (0.5 s) and the new one in (0.9 s), so tracks need no fade-in or fade-out of their own.
- Missing music is silent. A file that cannot be played (wrong format, damaged) is also silent and is logged; the game carries on.

## Interface sounds

- One line per sound: `sfx.<name> = audio/sfx/confirm.wav`. Use short **WAV** files (16-bit, 44.1 kHz, mono is fine), under about half a second, starting immediately with no silence at the front.
- Sounds in use: `cursor` (cursor moves), `confirm` (option chosen), `menu-open`, `menu-close` (Esc menu), `dialogue` (next dialogue line), `slide` (lecture slide changes), `tip` (tip card appears), `transition` (screen change), `clock` (time-of-day clock starts), `cutin` (combat call-out), `hit` (an answer cost HP).
- The bundled files are simple synthesized placeholders made for this project; replace them by saving files with the same names. Keep `cursor` and `dialogue` very quiet and short: they play often.

## Naming

Lower-case letters, digits, `-` and `_` only; images `.png`, `.jpg` or `.jpeg`, music and sounds `.mp3`, `.m4a`, `.wav` or `.aiff`; paths relative to the assets folder (e.g. `backgrounds/classroom.png`). The game refuses to start with a malformed manifest and says which line is wrong.

## Files currently requested

| File | Status |
| --- | --- |
| `backgrounds/title.png` | missing |
| `backgrounds/opening-exam-hall.png` | missing |
| `backgrounds/classroom.png` | missing |
| `backgrounds/classroom-desks.png` (foreground) | missing |
| `backgrounds/hallway.png` | missing |
| `backgrounds/student-hub.png` | missing |
| `backgrounds/student-hub-evening.png` | missing |
| `backgrounds/dorm-room.png` | missing |
| `backgrounds/dorm-room-night.png` | missing |
| `backgrounds/computer-lab.png` | missing |
| `characters/reward/neutral.png`, `smile.png`, `stern.png` | missing |
| `characters/student/neutral.png`, `worried.png` | missing |
| `textures/grain.png`, `textures/motes.png` | generated placeholders |
| `ui/clock-face.png`, `ui/clock-hour-hand.png`, `ui/clock-minute-hand.png` | optional, missing (drawn by the game) |
| `slides/day-01/*.png`, `slides/day-02/*.png` (12 slides, see Lecture slides) | missing |
| `audio/bgm/title.mp3`, `opening.mp3`, `school-day.mp3`, `after-school.mp3`, `night.mp3`, `combat.mp3` | missing (no music plays) |
| `audio/sfx/*.wav` (11 sounds, see Interface sounds) | synthesized placeholders |

The manifest is the authoritative list; this table is a snapshot from 2026-11-10.
