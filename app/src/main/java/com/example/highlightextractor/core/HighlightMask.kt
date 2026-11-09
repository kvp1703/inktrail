package com.example.highlightextractor.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.max

/**
 * Pixel-level highlight mask for one page.
 *
 * A pixel is highlighted when two independent tests agree:
 *  1. its illumination-normalized chroma for the preset's hue family is clearly above the
 *     paper background's own chroma (threshold derived from the page itself), and
 *  2. it passes the preset's absolute hue / saturation / value bounds.
 *
 * The adaptive test is what keeps yellow highlighter detectable on cream paper and under
 * warm indoor light: the paper's yellow-ish cast is measured and subtracted, instead of
 * relying on a fixed saturation cutoff alone.
 */
class HighlightMask private constructor(
    private val width: Int,
    private val height: Int,
    private val highlighted: BooleanArray,
    private val dark: BooleanArray,
) {

    /**
     * Fraction of non-dark pixels inside [box] that are highlighted.
     * Returns 0 when the box is empty, out of bounds or contains only dark pixels.
     */
    fun ratioFor(box: Rect): Float {
        val left = box.left.coerceIn(0, width)
        val right = box.right.coerceIn(0, width)
        val top = box.top.coerceIn(0, height)
        val bottom = box.bottom.coerceIn(0, height)
        if (right <= left || bottom <= top) return 0f

        var usable = 0
        var hits = 0
        for (y in top until bottom) {
            val rowOffset = y * width
            for (x in left until right) {
                val index = rowOffset + x
                if (dark[index]) continue
                usable++
                if (highlighted[index]) hits++
            }
        }
        return if (usable == 0) 0f else hits.toFloat() / usable
    }

    companion object {
        private const val DARK_VALUE = 0.35f
        private const val PAPER_PERCENTILE = 0.40f
        private const val PEAK_PERCENTILE = 0.999f
        private const val MIN_SEPARATION = 24f
        private const val MIN_CHROMA_MARGIN = 18f
        private const val MAX_CHROMA_MARGIN = 45f
        private const val PEAK_CHROMA_FRACTION = 0.28f

        fun build(bitmap: Bitmap, preset: ColorPreset): HighlightMask {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) {
                return HighlightMask(0, 0, BooleanArray(0), BooleanArray(0))
            }

            val hueCenter = hueCenterOf(preset)
            val row = IntArray(width)

            // ----- Pass 1: learn the page's background chroma from its own pixels. -----
            val histogram = IntArray(256)
            var counted = 0
            for (y in 0 until height) {
                bitmap.getPixels(row, 0, width, 0, y, width, 1)
                for (x in 0 until width) {
                    val pixel = row[x]
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    if (max(r, max(g, b)) / 255f < DARK_VALUE) continue
                    val bin = normalizedChroma(r, g, b, hueCenter).toInt().coerceIn(0, 255)
                    histogram[bin]++
                    counted++
                }
            }

            if (counted == 0) {
                return HighlightMask(width, height, BooleanArray(width * height), BooleanArray(width * height))
            }

            val paper = percentile(histogram, counted, PAPER_PERCENTILE).toFloat()
            val peak = percentile(histogram, counted, PEAK_PERCENTILE).toFloat()
            val separation = peak - paper

            // A page without any colored mark has a narrow chroma distribution. Do not
            // invent highlights out of paper tint / JPEG noise in that case.
            if (separation < MIN_SEPARATION) {
                return HighlightMask(
                    width = width,
                    height = height,
                    highlighted = BooleanArray(width * height),
                    dark = BooleanArray(width * height),
                )
            }

            // Sit comfortably above the paper's own chroma, but never let a strong yellow
            // object elsewhere in the photo push the cut so high that the marker is lost.
            val margin = (separation * PEAK_CHROMA_FRACTION)
                .coerceIn(MIN_CHROMA_MARGIN, MAX_CHROMA_MARGIN)
            val chromaThreshold = paper + margin

            // ----- Pass 2: classify every pixel. -----
            val highlighted = BooleanArray(width * height)
            val dark = BooleanArray(width * height)
            val hsv = FloatArray(3)

            for (y in 0 until height) {
                bitmap.getPixels(row, 0, width, 0, y, width, 1)
                val rowOffset = y * width
                for (x in 0 until width) {
                    val index = rowOffset + x
                    val pixel = row[x]
                    Color.colorToHSV(pixel, hsv)
                    if (hsv[2] < DARK_VALUE) {
                        dark[index] = true
                        continue
                    }
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    if (normalizedChroma(r, g, b, hueCenter) >= chromaThreshold &&
                        preset.matches(hsv[0], hsv[1], hsv[2])
                    ) {
                        highlighted[index] = true
                    }
                }
            }

            return HighlightMask(
                width = width,
                height = height,
                highlighted = clean(highlighted, width, height),
                dark = dark,
            )
        }

        /**
         * Chroma measured along the direction of the selected hue family, normalized by
         * brightness. Normalizing by the brightest channel makes the value independent of
         * shadows and exposure, so one threshold works across the whole page.
         */
        private fun normalizedChroma(r: Int, g: Int, b: Int, hueCenter: Float): Float {
            val brightest = max(r, max(g, b))
            if (brightest == 0) return 0f
            val raw = when {
                // Yellow / orange: the blue channel is absorbed most strongly.
                hueCenter < 75f -> (minOf(r, g) - b).toFloat()
                // Green: green above the average of red and blue.
                hueCenter < 165f -> g - (r + b) / 2f
                // Blue / cyan: blue above red.
                hueCenter < 255f -> (b - r).toFloat()
                // Pink / magenta: red and blue above green.
                else -> (minOf(r, b) - g).toFloat()
            }
            return (raw * 255f / brightest).coerceIn(0f, 255f)
        }

        private fun hueCenterOf(preset: ColorPreset): Float {
            val span = if (preset.hMin <= preset.hMax) {
                preset.hMax - preset.hMin
            } else {
                360f - preset.hMin + preset.hMax
            }
            return (preset.hMin + span / 2f) % 360f
        }

        private fun percentile(histogram: IntArray, total: Int, fraction: Float): Int {
            val target = (total * fraction).toInt().coerceAtLeast(1)
            var running = 0
            for (bin in histogram.indices) {
                running += histogram[bin]
                if (running >= target) return bin
            }
            return histogram.lastIndex
        }

        /** 3x3 majority filter: removes isolated specks and fills tiny holes. */
        private fun clean(source: BooleanArray, width: Int, height: Int): BooleanArray {
            val output = BooleanArray(source.size)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var neighbours = 0
                    for (dy in -1..1) {
                        val yy = y + dy
                        if (yy !in 0 until height) continue
                        for (dx in -1..1) {
                            val xx = x + dx
                            if (xx !in 0 until width) continue
                            if (source[yy * width + xx]) neighbours++
                        }
                    }
                    output[y * width + x] = neighbours >= 5
                }
            }
            return output
        }
    }
}
