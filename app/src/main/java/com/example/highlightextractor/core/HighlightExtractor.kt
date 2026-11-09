package com.example.highlightextractor.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** A recognized word together with the result of the highlight test (for the debug preview). */
data class DebugWord(
    val text: String,
    val box: Rect,
    val ratio: Float,
    val isHighlighted: Boolean,
)

data class ExtractionResult(
    val segments: List<String>,
    val words: List<DebugWord>,
    val debugBitmap: Bitmap? = null,
    /** 0 when the photo was already upright, otherwise 90, 180 or 270. */
    val rotationDegrees: Int = 0,
)

private data class WordInfo(
    val text: String,
    val box: Rect,
    val lineIndex: Int,
    val ratio: Float,
    val isHighlighted: Boolean,
)

object HighlightExtractor {

    private val whitespace = Regex("\\s+")

    /** Below this many letters the first OCR pass is treated as unreliable and rotations are tried. */
    private const val MIN_TEXT_CHARS = 60

    /** An upright page may be photographed at a slight tilt; larger deviations mean rotation. */
    private const val MAX_UPRIGHT_TILT = 35f

    private val RETRY_ROTATIONS = intArrayOf(90, 180, 270)

    /**
     * Runs ML Kit OCR on the whole page, tests every word against the highlight mask and
     * returns one string per highlight segment. Call from a background dispatcher.
     *
     * Photos are re-oriented before extraction using ML Kit's line angles, so sideways or
     * upside-down shots still produce text in normal reading order. This also repairs images
     * whose EXIF orientation is missing or wrong (some gallery providers re-encode a photo
     * after applying its EXIF rotation, which would otherwise rotate it twice).
     */
    suspend fun extract(
        bitmap: Bitmap,
        preset: ColorPreset,
        threshold: Float,
        makeDebug: Boolean = false,
    ): ExtractionResult {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var retryBitmap: Bitmap? = null

        try {
            var working = bitmap
            var text = recognize(recognizer, working)
            var rotation = 0

            val firstChars = nonWhitespaceCount(text.text)
            val firstAngle = medianLineAngle(text)
            val angleCorrection = if (firstAngle != null) correctiveRotation(firstAngle) else null
            var orientationResolved = angleCorrection == 0

            // 1) ML Kit reports the geometric angle of every line. Rotate exactly as needed.
            //    This also repairs photos whose EXIF orientation is missing or simply wrong.
            if (firstChars >= MIN_TEXT_CHARS && angleCorrection != null && angleCorrection != 0) {
                val candidate = rotate(bitmap, angleCorrection)
                val candidateText = try {
                    recognize(recognizer, candidate)
                } catch (t: Throwable) {
                    candidate.recycle()
                    throw t
                }

                val candidateAngle = medianLineAngle(candidateText)
                val candidateIsLevel = candidateAngle == null ||
                    abs(normalizeAngle(candidateAngle)) <= MAX_UPRIGHT_TILT
                val candidateChars = nonWhitespaceCount(candidateText.text)

                if (candidateIsLevel && candidateChars >= MIN_TEXT_CHARS / 2) {
                    retryBitmap = candidate
                    working = candidate
                    text = candidateText
                    rotation = angleCorrection
                    orientationResolved = true
                } else {
                    candidate.recycle()
                }
            }

            // 2) Fallback for results without usable angles: score right-angle rotations by
            //    line geometry and whether the reading order runs backwards.
            if (!orientationResolved && !looksUpright(text)) {
                var bestScore = orientationScore(text)

                for (angle in RETRY_ROTATIONS) {
                    val candidate = rotate(bitmap, angle)
                    val candidateText = try {
                        recognize(recognizer, candidate)
                    } catch (t: Throwable) {
                        candidate.recycle()
                        throw t
                    }

                    val score = orientationScore(candidateText)
                    if (score > bestScore) {
                        retryBitmap?.recycle()
                        retryBitmap = candidate
                        working = candidate
                        text = candidateText
                        rotation = angle
                        bestScore = score
                    } else {
                        candidate.recycle()
                    }

                    if (rotation != 0 && looksUpright(text)) break
                }
            }

            // The mask is built from the same bitmap the OCR boxes refer to.
            val mask = HighlightMask.build(working, preset)

            // If the photo still is not upright (no angle data and the rotation guesses
            // failed), ML Kit's own reading order is safer than sorting by image position.
            val geometricSort = isUprightResult(text)

            val words = ArrayList<WordInfo>()
            var lineIndex = 0

            // ML Kit's block order already handles side-by-side columns reasonably, so blocks
            // are kept as-is; lines are sorted top-to-bottom and words left-to-right.
            for (block in text.textBlocks) {
                val lines = if (geometricSort) {
                    block.lines.sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }
                } else {
                    block.lines
                }
                for (line in lines) {
                    val elements = if (geometricSort) {
                        line.elements.sortedBy { it.boundingBox?.left ?: Int.MAX_VALUE }
                    } else {
                        line.elements
                    }
                    for (element in elements) {
                        val box = element.boundingBox ?: continue
                        val word = element.text.trim()
                        if (word.isEmpty() || box.width() <= 0 || box.height() <= 0) continue
                        val ratio = mask.ratioFor(box)
                        words += WordInfo(
                            text = word,
                            box = box,
                            lineIndex = lineIndex,
                            ratio = ratio,
                            isHighlighted = ratio >= threshold,
                        )
                    }
                    lineIndex++
                }
            }

            val debugWords = words.map { DebugWord(it.text, it.box, it.ratio, it.isHighlighted) }
            val debugBitmap = if (makeDebug) renderDebugPreview(working, debugWords) else null

            return ExtractionResult(
                segments = buildSegments(words),
                words = debugWords,
                debugBitmap = debugBitmap,
                rotationDegrees = rotation,
            )
        } finally {
            retryBitmap?.recycle()
            recognizer.close()
        }
    }

    private suspend fun recognize(recognizer: TextRecognizer, bitmap: Bitmap): Text =
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()

    private fun nonWhitespaceCount(value: String): Int = value.count { !it.isWhitespace() }

    /** Median baseline angle of all recognized lines; null when there is too little geometry. */
    private fun medianLineAngle(text: Text): Float? {
        val angles = text.textBlocks
            .flatMap { it.lines }
            .filter { it.boundingBox != null }
            .map { it.angle }
        if (angles.size < 3) return null
        return angles.sorted()[angles.size / 2]
    }

    /** Whether image-space top/left sorting matches reading order for this result. */
    private fun isUprightResult(text: Text): Boolean {
        val angle = medianLineAngle(text)
        if (angle != null && abs(normalizeAngle(angle)) > MAX_UPRIGHT_TILT) return false
        return looksUpright(text)
    }

    /** Maps the line's clockwise angle to the clockwise bitmap rotation that levels it. */
    private fun correctiveRotation(angle: Float): Int? {
        val normalized = ((angle % 360f) + 360f) % 360f
        val nearest = (normalized / 90f).roundToInt() * 90
        if (abs(normalized - nearest) > MAX_UPRIGHT_TILT) return null
        return (360 - (nearest % 360)) % 360
    }

    private fun normalizeAngle(angle: Float): Float {
        var value = angle % 360f
        if (value > 180f) value -= 360f
        if (value < -180f) value += 360f
        return value
    }

    /**
     * ML Kit returns lines and words in reading order. In a correctly oriented photo those
     * orders also run top-to-bottom and left-to-right in image coordinates. If they run
     * backwards, the bitmap is upside down; if the line boxes are tall, it is sideways.
     */
    private fun looksUpright(text: Text): Boolean {
        if (nonWhitespaceCount(text.text) < MIN_TEXT_CHARS) return false

        val boxes = text.textBlocks.flatMap { it.lines }.mapNotNull { it.boundingBox }
        if (boxes.size < 3) return true

        val horizontal = boxes.count { it.width() >= it.height() }
        if (horizontal * 2 < boxes.size) return false

        return !readingOrderIsDescending(text)
    }

    /**
     * True for an upside-down page. Line order and word order are checked separately:
     * depending on the ML Kit version one of them may already be normalized, and combining
     * their counts could cancel out.
     */
    private fun readingOrderIsDescending(text: Text): Boolean {
        var lineAscending = 0
        var lineDescending = 0
        for (block in text.textBlocks) {
            val tops = block.lines.mapNotNull { it.boundingBox?.top }
            for (i in 0 until tops.size - 1) {
                if (tops[i + 1] >= tops[i]) lineAscending++ else lineDescending++
            }
        }

        var wordAscending = 0
        var wordDescending = 0
        for (line in text.textBlocks.flatMap { it.lines }) {
            val lefts = line.elements.mapNotNull { it.boundingBox?.left }
            for (i in 0 until lefts.size - 1) {
                if (lefts[i + 1] >= lefts[i]) wordAscending++ else wordDescending++
            }
        }

        return lineDescending > lineAscending || wordDescending > wordAscending
    }

    /** Bigger is better: prefer readable text with horizontal lines in normal reading order. */
    private fun orientationScore(text: Text): Int {
        val chars = nonWhitespaceCount(text.text)
        if (chars == 0) return 0

        val boxes = text.textBlocks.flatMap { it.lines }.mapNotNull { it.boundingBox }
        if (boxes.size < 3) return chars

        val horizontal = boxes.count { it.width() >= it.height() }
        if (horizontal * 2 < boxes.size) return chars / 8

        return if (readingOrderIsDescending(text)) chars / 4 else chars
    }

    private fun rotate(source: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Consecutive highlighted words on the same line form one segment. A segment is broken
     * by a non-highlighted word or by the highlight continuing onto a new line, so each
     * highlighted line starts on its own line in the results. If the highlight continues on
     * the next line and the previous word ends with '-', the hyphen is dropped and the parts
     * are glued instead of starting a new line.
     */
    private fun buildSegments(words: List<WordInfo>): List<String> {
        val segments = ArrayList<String>()
        val current = StringBuilder()
        var currentLine = -1

        fun flush() {
            if (current.isNotEmpty()) {
                val text = normalize(current.toString())
                if (text.length >= 3) segments += text
                current.setLength(0)
            }
        }

        for (word in words) {
            if (!word.isHighlighted) {
                flush()
                currentLine = -1
                continue
            }

            if (current.isEmpty()) {
                current.append(word.text)
            } else if (word.lineIndex == currentLine) {
                current.append(' ').append(word.text)
            } else if (current.endsWith("-")) {
                // Hyphenated word split across two lines: join without space.
                current.setLength(current.length - 1)
                current.append(word.text)
            } else {
                // The highlight continues on a new line: start a new result line.
                flush()
                current.append(word.text)
            }
            currentLine = word.lineIndex
        }
        flush()
        return segments
    }

    private fun normalize(value: String): String =
        value.replace(whitespace, " ").trim()
}

/**
 * Draws every word box on a scaled copy of [bitmap]: green = counted as highlighted,
 * red = not highlighted. Used by the debug preview.
 */
private fun renderDebugPreview(
    bitmap: Bitmap,
    words: List<DebugWord>,
    maxSide: Int = 1400,
): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    val scale = if (longest > maxSide) maxSide.toFloat() / longest else 1f
    val outWidth = max(1, (bitmap.width * scale).roundToInt())
    val outHeight = max(1, (bitmap.height * scale).roundToInt())

    val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawBitmap(
        bitmap,
        null,
        Rect(0, 0, outWidth, outHeight),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )

    val scaleX = outWidth.toFloat() / bitmap.width
    val scaleY = outHeight.toFloat() / bitmap.height
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = max(2f, outWidth / 400f)
        isAntiAlias = true
    }

    for (word in words) {
        paint.color = if (word.isHighlighted) Color.GREEN else Color.RED
        canvas.drawRect(
            word.box.left * scaleX,
            word.box.top * scaleY,
            word.box.right * scaleX,
            word.box.bottom * scaleY,
            paint,
        )
    }
    return output
}
