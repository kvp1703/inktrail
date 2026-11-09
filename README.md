# Ink Trail

An offline Android app that takes photos of paper pages, finds the words covered by a
highlighter marker, and extracts them as editable text.

All processing happens on-device: no network, no permissions, no accounts. OCR uses the
bundled ML Kit Latin text-recognition model.

> **Using this file as context for a new AI session:** this README documents the app's
> purpose, architecture, algorithms, current UI behaviour, build setup, and known
> limitations. It is written to be sufficient context on its own.

---

## What the app does

1. The user selects one or more page photos (system photo picker).
2. Photos are ordered top-to-bottom as page order.
3. A highlighter color is chosen (Yellow by default; presets or custom HSV range).
4. OCR runs on each page and every recognized word is tested against a pixel-level
   highlighter mask.
5. Consecutive highlighted words are grouped into lines/segments and shown in an editable
   result box, which can be copied, saved as `.txt`, or shared.

### Example

Input: a page photo with a few yellow-highlighted sentences.  
Output: only the highlighted sentences, one highlighted line per output line.

---

## Tech stack

| Item | Value |
| --- | --- |
| Language | Kotlin 1.9.24 |
| UI | Jetpack Compose + Material 3 (Compose BOM 2024.06.00), custom ink-and-paper design system |
| Architecture | Single `MainActivity` → Compose `MainScreen` + `MainViewModel` (`StateFlow`) |
| OCR | ML Kit `com.google.mlkit:text-recognition` 16.0.0 (bundled Latin model, offline) |
| Image loading | Android `BitmapFactory` + `ExifInterface` |
| Build | Gradle 8.7, Android Gradle Plugin 8.5.0 |
| Android SDK | `compileSdk 34`, `minSdk 26`, `targetSdk 34` |
| Java | 17 (source/target/jvmTarget) |
| Package | `com.example.highlightextractor` |
| App label | `Ink Trail` |
| Root project | `HighlightExtractor` (folder: `inktrail`) |

Dependencies are declared in `gradle/libs.versions.toml`.

---

## Project structure

```
app/src/main/
├── AndroidManifest.xml                 # No permissions; single launcher activity
├── java/com/example/highlightextractor/
│   ├── MainActivity.kt                 # Edge-to-edge host; applies InktrailTheme
│   ├── ui/
│   │   ├── MainScreen.kt               # Header, ticker, four sections, field-notes dialog
│   │   ├── MainViewModel.kt            # State, photo ordering, extraction orchestration
│   │   ├── components/
│   │   │   └── InkKit.kt               # Paper backdrop, blocks, buttons, ticker, sliders
│   │   └── theme/
│   │       └── InkTheme.kt             # Ink palette, typography, Material colour bridge
│   └── core/
│       ├── ColorPreset.kt              # Built-in presets + HSV matching rules
│       ├── ImageLoader.kt              # Decode, EXIF orientation, downscale, thumbnails
│       ├── HighlightMask.kt            # Pixel mask: adaptive chroma + HSV bounds
│       └── HighlightExtractor.kt       # OCR, rotation correction, word grouping, debug render
└── res/
    ├── drawable/ic_launcher_foreground.xml  # Vector mark: ink lines + marker stroke
    ├── mipmap-anydpi-v26/                   # Adaptive launcher icon
    ├── values/{colors,strings,themes}.xml
    └── values-night/themes.xml         # Dark launch background
```

There are **no tests** and no other modules.

---

## UI overview (`MainScreen.kt`)

The interface is styled as a risograph zine rather than a Material card stack: warm paper,
black ink, fluorescent highlighter yellow, hot pink, cobalt, oversized serif headlines,
monospace labels, hard offset shadows, taped photo prints and a dotted paper backdrop.
Everything in the Compose UI is custom; no bitmap or font assets are loaded.

Chrome shared across the screen:
- **Masthead** — `INK TRAIL` wordmark, `AN ANALOG PROBLEM, / SOLVED DIGITALLY.` kicker,
  a two-line `HIGHLIGHTS, EXTRACTED.` headline (the second line is a highlighter block;
  the type scales down below 300 dp), and `NO CLOUD / NO LOGIN / 100% ON-DEVICE` badges.
- **Ticker** — a continuously scrolling black strip with acid-yellow mono text. It is a
  `Canvas` ticker measured once with `TextMeasurer`, so it loops seamlessly.
- **Paper backdrop** — dot grid, a large translucent marker smear, a cobalt ring and
  deterministic print grain. No scrolling parallax; the backdrop is fixed.
- **Footer** — `STAY INKED.`, a drawn barcode, privacy line and copyright.

### 01 / The pages
- **Add page photos** is a 190 dp dashed drop zone with a taped corner; the whole tile is
  clickable and launches `ActivityResultContracts.GetMultipleContents` (`image/*`).
- Selected photos appear as slightly rotated polaroid prints with tape, a hard black
  shadow, a numbered black tab and `↑ ↓ ×` square controls. List order = page order.
- A dashed/rotated **Add more pages** tile ends the strip.
- Thumbnails load asynchronously (`ImageLoader.loadThumbnail`, 480 px) with a `LOADING`
  / `NO PREVIEW` fallback.

### 02 / The marker
- **Collapsed by default.** Preset chips (Yellow, Green, Pink, Orange, Blue, Custom)
  show a real marker swatch; the selected chip turns into a black sticker with a hard
  shadow and an acid check. Custom shows a four-colour riso swatch.
- A summary block shows the active marker and a **Tune / Hide** button.
- **Field notes** opens a full custom `Dialog` (not an AlertDialog): a paper sheet with
  a taped corner, huge serif title and A/B/C/D guide entries explaining hue, saturation,
  brightness and sensitivity, plus a quick-rescue note.
- Expanded tuning uses custom-drawn sliders:
  - Hue: a `RangeSlider` whose track is the actual hue spectrum, dimmed outside the
    selected range, with 20×30 dp ink-tab thumbs.
  - Saturation, brightness and sensitivity: hatched marker-fill tracks with tilted
    black thumbs. Sensitivity range is 0.2–0.9.
  - Editing any slider clones the active preset into **Custom**; built-ins stay pure.

### 03 / The engine
- A loud inverted panel (`INK IN. / TEXT OUT.`, in cream and acid) with a hot-pink hard
  shadow. `MODE / EXTRACT` and a live `STANDBY / CRUNCHING` indicator sit at the top.
- **Extract highlights** is a full-width acid button with a cobalt shadow. It is
  disabled until at least one photo exists, and disabled buttons become neutral ghost
  stickers.
- While running, a highlighter progress bar, `PAGE N OF M`, a percentage and **Cancel
  run** replace the button. Processing still happens on `Dispatchers.Default`; the UI
  only formats the state.

### 04 / The extract
- The result is a notebook sheet: dark file header (`HL_EXTRACT.TXT`, UTF-8, live char
  count), dotted paper, red margin rule and a taped corner. The text is editable via a
  monospace `BasicTextField` with a hot-pink caret; placeholder copy appears when empty.
- Action stickers: **Copy text** (shows `COPIED ✓` for 1.6 s), **Save .txt** and
  **Share**.
- A bordered options block holds custom controls:
  - **Photo separators** — a square ink checkbox with an acid check.
  - **Debug vision** — a lever-style acid/ink toggle.
- Feedback is printed as rotated sticky notes: hot-pink `MISS` for no highlights,
  acid `NOTE` for status/cancel/save, peach `ERR` for per-photo failures.
- The debug image sits in a taped frame labelled `DEBUG VISION / FIRST PAGE` with a
  `GREEN = KEPT / RED = DROPPED` legend.

The entire screen supports light **paper** and dark **blackout** themes: the palette is
swapped through `LocalInkPalette`, while acid surfaces keep black text (via `onAccent`)
and loud panels stay dark (via `invertBg` / `invertFg`) in both themes.

---

## Data / state model

`MainUiState` (in `MainViewModel.kt`) is the single UI state:

| Field | Meaning |
| --- | --- |
| `photos: List<Uri>` | Selected photos in page order |
| `selectedPresetIndex: Int` | Index into `ColorPreset.builtIn`; `builtIn.size` = Custom |
| `customPreset: ColorPreset` | Custom values, cloned from the active preset on first edit |
| `threshold: Float` | Sensitivity, 0.2–0.9 (default 0.5) |
| `addSeparators: Boolean` | `--- Photo N ---` headers |
| `debugPreview: Boolean` | Draw word boxes on first photo next run |
| `isProcessing`, `progressDone`, `progressTotal` | Extraction progress |
| `resultText: String` | Editable extracted text |
| `debugBitmap: Bitmap?` | Last debug annotation image |
| `errorMessages`, `noHighlights`, `hasResult`, `statusMessage` | Feedback |

`activePreset` is a computed property: the selected built-in, or `customPreset`.

State is exposed as `StateFlow<MainUiState>` and collected with
`collectAsStateWithLifecycle`. The ViewModel survives configuration changes but not
process death.

---

## Extraction pipeline

Entry point: `MainViewModel.startExtraction()`.

1. **Guard & snapshot** — ignore if a job is already active or there are no photos.
   Capture a snapshot of all settings so mid-run UI edits cannot change the run.
2. **Per photo** (sequential, on `Dispatchers.Default` for CPU/OCR work):
   - `ImageLoader.loadUprightBitmap(context, uri)`.
   - `HighlightExtractor.extract(bitmap, preset, threshold, makeDebug = debug && firstPhoto)`.
   - `buildSection(...)` and accumulate output.
   - `finally { bitmap.recycle() }`.
3. **Cancellation safety** — a monotonically increasing run id (`currentRunId`) ensures a
   cancelled older run cannot overwrite the results of a newer run.
4. **Final result**:
   - Per photo, segments are joined with `\n`.
   - With separators: `--- Photo N ---\n` + segments.
   - Photos are joined with `\n\n` when separators are on, otherwise `\n`.
   - Sets `noHighlights` when no segments were produced and there were no errors.

Save uses the Storage Access Framework (`CreateDocument("text/plain")`) and writes UTF-8
with mode `"wt"`. Share uses `ACTION_SEND` with `text/plain`.

### Image loading (`ImageLoader.kt`)

- Decodes bounds first to pick `inSampleSize`.
- Longest side capped at **2000 px** (`MAX_DIMENSION`) for speed and memory.
- Applies all EXIF orientations (rotations, flips, transpose/transverse).
- Retries decoding with a larger sample size on `OutOfMemoryError` (up to 64×).
- Recycles replaced bitmaps.
- `loadThumbnail` is the same idea with a target pixel size (240 px in the UI).
- Must be called from a background thread.

### OCR & orientation (`HighlightExtractor.kt`)

Uses `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)` and is closed in
`finally`.

Orientation handling is deliberately robust because phone photos may be sideways,
upside down, or have wrong/missing EXIF orientation:

1. Run OCR once.
2. Compute the **median line angle** from ML Kit. If there is enough text
   (`MIN_TEXT_CHARS = 60`) and the angle maps to a right-angle correction
   (`correctiveRotation`), rotate the bitmap and re-run OCR. Accept the rotation when the
   new result is level (within `MAX_UPRIGHT_TILT = 35°`) and still has enough text.
3. Fallback: if the result still does not look upright, try 90°, 180°, 270° and score each
   result by readable character count, horizontal line boxes, and whether reading order
   runs backwards (`orientationScore`). Pick the best.
4. The mask is built from the **same bitmap** the accepted OCR boxes refer to.

Word collection:
- Blocks keep ML Kit order (handles columns reasonably).
- Lines are sorted top-to-bottom and elements left-to-right **only** when the result is
  upright (`isUprightResult`). Otherwise ML Kit's reading order is trusted.
- Each word gets a box, a highlight ratio, and `isHighlighted = ratio >= threshold`.

### Highlight mask (`HighlightMask.kt`)

A pixel is highlighted only when **two independent tests agree**:

1. **Adaptive paper-chroma test** — the page's own background chroma is measured, so cream
   paper or warm indoor light does not get mistaken for yellow marker.
2. **Absolute HSV bounds** — the pixel must fall inside the preset's hue range and be at
   least `sMin` saturated and `vMin` bright.

Implementation:
- Pass 1: histogram of `normalizedChroma` for non-dark pixels (`V >= 0.35`).
- Paper = 40th percentile, peak = 99.9th percentile.
- If `peak - paper < 24`, there is no real colored mark → no highlights.
- Cutoff = `paper + margin`, where
  `margin = (separation * 0.28).coerceIn(18, 45)`.
- Pass 2: classify pixels. `V < 0.35` → dark (ignored). Otherwise highlighted when
  `normalizedChroma >= cutoff` **and** `preset.matches(h, s, v)`.
- 3×3 majority filter (`>= 5` neighbours) removes specks and fills pinholes.
- `ratioFor(box)` = fraction of non-dark pixels inside the word box that are highlighted.

`normalizedChroma` is measured along the selected hue family and divided by the brightest
channel, making it mostly independent of exposure/shadows:
- hue center < 75° (yellow/orange): `min(r, g) - b`
- < 165° (green): `g - (r + b)/2`
- < 255° (blue/cyan): `b - r`
- otherwise (pink/magenta): `min(r, b) - g`

### Color presets (`ColorPreset.kt`)

Hue `0..360`, saturation/value `0..1` (same units as `Color.colorToHSV`).

| Preset | Hue range | Min sat | Min value |
| --- | --- | --- | --- |
| Yellow | 38–82 | 0.12 | 0.35 |
| Green | 75–160 | 0.20 | 0.45 |
| Pink | 290–350 | 0.20 | 0.50 |
| Orange | 20–45 | 0.30 | 0.50 |
| Blue | 170–210 | 0.20 | 0.45 |
| Custom | starts as Yellow values | | |

`matches` supports wrap-around ranges (`hMin > hMax`, e.g. 350→10).

### Segmenting (`HighlightExtractor.buildSegments`)

- Consecutive highlighted words **on the same line** form one segment.
- A segment breaks on a non-highlighted word.
- A segment now also breaks when the highlight continues **onto a new line**, so each
  highlighted line appears on its own line in the result box.
- Exception: if the previous word ends with `-`, the hyphen is dropped and the parts are
  joined (split-word repair).
- Segments shorter than 3 characters after whitespace normalization are dropped.

### Debug preview

`renderDebugPreview` draws every word box on a scaled copy (max side 1400 px):
green = highlighted, red = not highlighted. Only generated for the **first** photo, and
only when the debug switch was on when extraction started.

---

## Design decisions & gotchas

- **No permissions** — photos come from the system picker; OCR is fully offline.
- **Default is good enough** — the tuning controls are collapsed by default because Yellow
  works for most pages; advanced controls are optional.
- **Built-in presets are immutable** — any slider edit clones into Custom.
- **Settings apply to the next run** — results are not recomputed when a slider or the
  separators checkbox changes.
- **Debug preview is first-photo only** and next-run only.
- **OCR is Latin-only** (bundled model).
- **2000 px cap** is a speed/memory trade-off; very small text may need a higher cap.
- **No persistence** — photos/results are lost on process death; ViewModel only survives
  configuration changes.
- **UI strings are hardcoded** in `MainScreen.kt`; only `app_name` is in `strings.xml`.
- **Theme** — `InktrailTheme` swaps a custom `InkPalette` between warm paper and blackout,
  and the activity is edge-to-edge with transparent system bars (`windowBackground` is
  paper/dark paper in the XML themes). Acid surfaces always use black `onAccent` text;
  the loud panels keep a dark `invertBg` in both themes.
- **No runtime visual asset dependencies** — the type is generic Serif / Monospace / Sans;
  the grain, barcode, tape, sparkles and hatched sliders are drawn with Compose `Canvas`.
  The adaptive launcher icon is a small vector drawable.
- **Motion** — the ticker is the only continuous animation; the rest is state-driven
  (button press offsets, progress fill, toggle knob). No animation framework dependency.

---

## Build & run

### Requirements
- Android SDK with API 34
- JDK 17–21 (Gradle 8.7 does **not** support JDK 25)
- Gradle 8.7, or Android Studio (recommended)

### Important: incomplete Gradle wrapper
`gradle/wrapper/gradle-wrapper.properties` exists, but **`gradlew`, `gradlew.bat`, and
`gradle/wrapper/gradle-wrapper.jar` are missing** from the repo. Use Android Studio, or
invoke a local Gradle 8.7 installation.

### Windows local build (this machine)
```powershell
$env:JAVA_HOME = "C:\Users\kshitij\.jdks\jbr-21.0.11"
& "C:\Users\kshitij\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\bin\gradle.bat" :app:assembleDebug --console=plain
```

Quick compile check:
```powershell
& "...\gradle.bat" :app:compileDebugKotlin --console=plain
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

`local.properties` points at the local SDK and should not be committed.

### Recommended next step for a new session
If continuing development, consider adding the missing Gradle wrapper files
(`gradle wrapper --gradle-version 8.7`) so builds are reproducible without a local Gradle.

---

## Ideas / possible next steps

- Add unit tests for `buildSegments`, `ColorPreset.matches`, and `HighlightMask` math.
- Move UI strings into `strings.xml` for localization.
- Show the guide inline as expandable help instead of a dialog.
- Add an "auto / any highlighter color" mode.
- Persist photos/results across process death (e.g. `SavedStateHandle` or saved file).
- Re-run formatting when the separators checkbox changes.
- Support multi-photo debug preview or a per-photo preview picker.
- Add Compose screenshot/golden tests for the editorial layout.
- Add a reduced-motion setting that pauses the ticker.
