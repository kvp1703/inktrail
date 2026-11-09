package com.example.highlightextractor.core

/**
 * Hue range plus minimum saturation / value that identifies one highlighter color.
 * Hue is 0..360, saturation and value are 0..1 (the same units Color.colorToHSV returns).
 */
data class ColorPreset(
    val name: String,
    val hMin: Float,
    val hMax: Float,
    val sMin: Float,
    val vMin: Float,
) {
    /** True when [hue], [saturation], [value] fall inside this preset. */
    fun matches(hue: Float, saturation: Float, value: Float): Boolean {
        if (saturation < sMin || value < vMin) return false
        // hMin > hMax would mean a range that wraps through red (e.g. 350..10).
        return if (hMin <= hMax) hue >= hMin && hue <= hMax else hue >= hMin || hue <= hMax
    }

    companion object {
        const val CUSTOM_NAME = "Custom"

        // Wide hue, low saturation/value floors: real photos wash the marker out and shift
        // its hue. The adaptive paper-chroma test in HighlightMask rejects the page itself.
        val yellow = ColorPreset("Yellow", 38f, 82f, 0.12f, 0.35f)
        val green = ColorPreset("Green", 75f, 160f, 0.20f, 0.45f)
        val pink = ColorPreset("Pink", 290f, 350f, 0.20f, 0.50f)
        val orange = ColorPreset("Orange", 20f, 45f, 0.30f, 0.50f)
        val blue = ColorPreset("Blue", 170f, 210f, 0.20f, 0.45f)

        /** Presets shown as chips, in display order. */
        val builtIn: List<ColorPreset> = listOf(yellow, green, pink, orange, blue)

        /** Starting values for the Custom sliders (same forgiving starting point as Yellow). */
        val customDefault = ColorPreset(CUSTOM_NAME, 38f, 82f, 0.12f, 0.35f)
    }
}
