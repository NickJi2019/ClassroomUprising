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
| Facing (optional) | `character.<id>.faces = right` (or `left`): the way the art looks. On the other side of the screen the figure is mirrored about its anchor, so characters face the middle. Leave it out for art that faces the viewer |
| Photos | Cut-out photos work. HDR photos (iPhone: 16-bit PNG with a BT.2100 PQ profile) must be converted to SDR sRGB first; read as ordinary sRGB they look grey and flat. Stripping the profile does not convert them. A drawing photographed in HDR can still come out grey after the system conversion when its paper is dimmer than HDR reference white: raise the exposure (in linear light) until the paper reads white |

In dialogue text, a line can ask for an expression: `Mr. Reward[smile]: Good question.` The tag is not shown to the player. *This text format needs agreement with the content authors before they rely on it.*

The game animates the art itself (gentle breathing and sway, stepping back and dimming when another character speaks), so poses should be relaxed and centred on the anchor.

### Mr. Reward (delivered)

Seven cut-out photos of the teacher, delivered by the team on 2026-11-10, placed on one shared 1217×837 canvas (1.25× the 669 px display height). Each was scaled so the head is 128 px wide on screen and the face sits at the same point: face boxes from macOS Vision, head width measured across the eyes (steadier than the face box for the turned and tilted heads). The canvas runs from just above the highest head to the slot line, so the body always reaches the bottom of the screen; the anchor (0.3202, 1.0) is the face's x. Six photos were HDR and were tone-mapped to SDR with the system decoder first; one was already SDR. The photos face right; on the right of the screen (the opening) he is mirrored.

| Expression | File | Original photo | Used for |
| --- | --- | --- | --- |
| `neutral`, `speechless` | `neutral.png` | speechless | Lectures and anything without a tag |
| `smile` | `smile.png` | smiling, eyes closed | Available to dialogue tags |
| `stern`, `tough` | `stern.png` | finger to lips, squinting | Struck by the player's right answer in the opening battle; available to dialogue tags |
| `gotcha` | `gotcha.png` | pointing, full figure | Asking the class a question; the opening's first line |
| `ridicule` | `ridicule.png` | laughing and pointing | Reply to an inappropriate response; the opening's "see you this summer" |
| `staring` | `staring.png` | wide-eyed stare | Astonished when the player wins the opening exam (no longer used after a quiet class) |
| `thumbs-up` | `thumbs-up.png` | thumbs up | Reply to an appropriate response |

`ui/reward-logo.png` is the team's hand-drawn "Mr. Reward" title, shown on a red band as he steps into the opening. Its source is an HDR (BT.2100 PQ) photo whose paper sits at about half of reference white, so the system's SDR conversion left the paper grey (sRGB 188). After that conversion its exposure was raised ×1.6 in linear light so the paper is sRGB 232 (near white) and the ink stays black; trimmed to the drawing, 900 px wide, no metadata.

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

While Mr. Reward lectures, slides appear on a projector panel beside him, and the dialogue box shows what he says about each one.

- The lecture text decides when a slide appears: a line that starts with `[slide:<name>]` turns to that slide, and it stays up until another cue. The cue itself is not shown. Example: `Mr. Reward: [slide:file-structure] Start with the file...`
- The manifest only says which image a name is: `slide.<name> = slides/gr11-review/07-file-structure.png`.
- Show only slides worth talking about. Title pages, jokes, section dividers and pages nobody explains are better left out than flashed past.
- **16:9**, delivered at **1600×900** or larger (the panel shows 608×342; the enlarged view about 1064×598, so small code must stay readable there). PNG for text and code.
- A slide that is cued but not delivered shows a light **"MISSING SLIDE"** plate with its name and file; a cue with no manifest line shows **"NOT LISTED IN THE MANIFEST"**. A test fails in both cases for the bundled lectures.

The Grade 11 review (Days 1–2) uses 12 of the 18 pages of the class deck *Review Overview*, rendered from its PDF at 1600×900. Left out: 1 (title), 3 (quote about self-direction), 4–6 (jokes and the "Basic Java" divider) and 17 (asking-for-help policy, covered in one spoken line).

| Name | Deck page | Lecture |
| --- | --- | --- |
| `review-purpose` | 2 | Day 1: why we review |
| `file-structure` | 7 | Day 1 |
| `example-java` | 8 | Day 1 |
| `data-types` | 9 | Day 1 |
| `variables` | 10 | Day 1 |
| `math-and-symbols` | 11 | Day 1 |
| `decisions` | 12 | Day 1 |
| `loops` | 13 | Day 2 |
| `methods` | 14 | Day 2 |
| `method-example` | 15 | Day 2 |
| `the-rest` | 16 | Day 2: self-directed topics |
| `automated-testing` | 18 | Day 2: what comes next |

Page 9 carries a small photo of a TV character (a pun on "Data"); replace or remove it if the game is ever shared outside the class.

## Music

- One line per cue: `bgm.<cue> = audio/bgm/school-day.mp3`. Each screen asks for a cue; the code never names files.
- Cues in use: `title` (title screen), `opening` (opening scene), `school` (day card, classroom, other classes, bonus morning), `after-school` (Hub after school), `night` (Hub in the evening and late night, day end), `combat` (assessment combat).
- Formats JavaFX can play: **MP3**, **AAC in .m4a**, **WAV**, **AIFF** (not Ogg, not FLAC). MP3 at 160–192 kbps or AAC at 128–192 kbps, 44.1 or 48 kHz, stereo.
- Tracks loop from end to start, so make the end lead back into the beginning without a gap or click; keep about 1–3 minutes per loop. Normalise tracks to a similar loudness (about −16 LUFS) so cues do not jump in volume.
- The same cue keeps playing across screens; a new cue fades the old track out (0.5 s) and the new one in (0.9 s), so tracks need no fade-in or fade-out of their own.
- Missing music is silent. A file that cannot be played (wrong format, damaged) is also silent and is logged; the game carries on.

## Interface sounds

- One line per sound: `sfx.<name> = audio/sfx/confirm.wav`. Use short **WAV** files (16-bit, 44.1 kHz, mono is fine), under about half a second, starting immediately with no silence at the front.
- Sounds in use: `cursor` (cursor moves), `confirm` (option chosen), `menu-open`, `menu-close` (Esc menu), `dialogue` (next dialogue line), `slide` (lecture slide changes), `tip` (tip card appears), `transition` (screen change), `clock` (time-of-day clock starts), `cutin` (combat call-out, item banner), `hit` (HP lost), `shatter` (the screen breaks like glass when the player is defeated in the opening exam: a sharp crack, a ringing break and falling shards; synthesized here in the spirit of the glass-break sound the team asked for, not copied from any game; replace it with a licensed recording by saving `shatter.wav`).
- The bundled files are simple synthesized placeholders made for this project; replace them by saving files with the same names. Keep `cursor` and `dialogue` very quiet and short: they play often.

## Naming

Lower-case letters, digits, `-` and `_` only; images `.png`, `.jpg` or `.jpeg`, music and sounds `.mp3`, `.m4a`, `.wav` or `.aiff`; paths relative to the assets folder (e.g. `backgrounds/classroom.png`). The game refuses to start with a malformed manifest and says which line is wrong.

## Files currently requested

| File | Status |
| --- | --- |
| `backgrounds/empty-classroom.jpg` (title) | delivered: the team's photo of the empty computer lab, 4:3 cut to 16:9, 1920×1080, no metadata |
| `backgrounds/exam-seat.jpg` (opening) | delivered: the team's photo of an exam seat (desk, monitor, test paper, pencil), 4:3 cut to 16:9, 1920×1080, no metadata |
| `backgrounds/computer-lab.jpg` (listed for both `classroom` and `computer-lab`) | delivered: the team's photo of the school computer lab, 1920×1080 |
| `backgrounds/classroom-desks.png` (foreground) | missing |
| `backgrounds/hallway.jpg` | delivered: the team's photo of a school hallway, 1920×1080 |
| `backgrounds/student-hub.jpg` | delivered: the team's photo of the Student Hub, 1920×1080, used for every Hub period (the evening grade darkens it) |
| `backgrounds/student-hub-evening.jpg` | optional, not listed: a dedicated evening photo can be added as `background.hub.evening` |
| `backgrounds/dorm.jpg` (dorm, every period) | delivered: a dorm room at dusk with the lights on, cut to 16:9 keeping the wall lettering, 1920×1080, no metadata; the never-delivered late-night variant was taken out of the manifest, so late night shows this photo under the late-night grade |
| `characters/reward/*.png` (7 expressions, see Mr. Reward) | delivered: the team's photos of the teacher |
| `ui/reward-logo.png` | delivered: the team's hand-drawn title |
| `characters/student/neutral.png`, `worried.png` | missing |
| `textures/grain.png`, `textures/motes.png` | generated placeholders |
| `ui/clock-face.png`, `ui/clock-hour-hand.png`, `ui/clock-minute-hand.png` | optional, missing (drawn by the game) |
| `slides/gr11-review/*.png` (12 slides, see Lecture slides) | delivered (from the class deck) |
| `audio/bgm/title.mp3`, `opening.mp3`, `school-day.mp3`, `after-school.mp3`, `night.mp3`, `combat.mp3` | missing (no music plays) |
| `audio/sfx/*.wav` (12 sounds, see Interface sounds) | synthesized placeholders |

The photos were taken at school by the team, cropped to 16:9 and saved without their camera metadata (the originals carry GPS positions). The Student Hub photo is an iPhone HEIC whose pixels are stored sideways with no rotation flag; it was turned upright, converted from Display P3 to sRGB, and a 16:9 band was cut from the portrait frame (seasonal decorations, fireplace screen, shelves, sofa and games table). They are 1920×1080, below the 2560×1440 asked for above, because the originals are 2000 pixels wide; they look slightly soft on high-density screens. Students appear in the lab photo; the team confirmed on 2026-11-10 that they agreed to be shown in the game.

The manifest is the authoritative list; this table is a snapshot from 2026-11-10.
