package com.example.highlightextractor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.highlightextractor.ui.theme.LocalInkPalette

/**
 * A hard-edged block with an offset shadow. This is the structural unit of the app:
 * no rounded glass, no soft elevation, just ink pressed onto paper.
 */
@Composable
fun InkBlock(
    modifier: Modifier = Modifier,
    background: Color,
    borderColor: Color,
    shadowColor: Color = borderColor,
    shadowOffset: Dp = 6.dp,
    rotation: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .padding(end = shadowOffset, bottom = shadowOffset),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(shadowOffset, shadowOffset)
                .background(shadowColor),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .border(2.dp, borderColor)
                .padding(contentPadding),
            content = content,
        )
    }
}

/**
 * A label made of ink with a chunky offset shadow that presses down when tapped.
 */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color,
    foreground: Color,
    border: Color,
    shadowColor: Color,
    enabled: Boolean = true,
    rotation: Float = 0f,
    fillWidth: Boolean = false,
    leading: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val push by animateDpAsState(
        targetValue = if (pressed && enabled) 3.dp else 0.dp,
        label = "ink-button-push",
    )
    val ink = LocalInkPalette.current
    val buttonBackground = if (enabled) background else ink.panel
    val buttonForeground = if (enabled) foreground else ink.dim
    val buttonBorder = if (enabled) border else ink.dim
    val buttonShadow = if (enabled) shadowColor else ink.fg.copy(alpha = 0.16f)

    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .padding(end = 5.dp, bottom = 5.dp),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(5.dp, 5.dp)
                .background(buttonShadow),
        )
        Row(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .offset(push, push)
                .background(buttonBackground)
                .border(2.dp, buttonBorder)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                Text(
                    text = leading,
                    style = MaterialTheme.typography.labelLarge,
                    color = buttonForeground,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = buttonForeground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun InkBadge(
    text: String,
    modifier: Modifier = Modifier,
    background: Color,
    foreground: Color,
    border: Color,
) {
    Box(
        modifier = modifier
            .background(background)
            .border(1.5.dp, border)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
        )
    }
}

@Composable
fun SectionHead(
    number: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val ink = LocalInkPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .graphicsLayer { rotationZ = -2f }
                    .background(if (ink.isDark) ink.panel else ink.fg)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    color = ink.acid,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ink.fg,
                )
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.dim,
                )
            }
            if (trailing != null) {
                Text(
                    text = trailing.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.fg,
                    textAlign = TextAlign.End,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(ink.fg),
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .width(28.dp)
                    .height(8.dp)
                    .background(ink.acid)
                    .border(1.dp, ink.fg),
            )
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .background(ink.hot),
            )
        }
    }
}

@Composable
fun DottedRule(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        val dash = 3.dp.toPx()
        val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawRect(color = color, topLeft = Offset(x, 0f), size = Size(dash, size.height))
            x += dash + gap
        }
    }
}

@Composable
fun InkCheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInkPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
            ) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(if (checked) ink.invertBg else Color.Transparent)
                .border(2.dp, ink.fg),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    val check = Path().apply {
                        moveTo(size.width * 0.08f, size.height * 0.52f)
                        lineTo(size.width * 0.4f, size.height * 0.86f)
                        lineTo(size.width * 0.94f, size.height * 0.14f)
                    }
                    drawPath(
                        path = check,
                        color = ink.acid,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = ink.fg,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ink.dim)
        }
    }
}

@Composable
fun InkToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInkPalette.current
    val knobOffset by animateDpAsState(if (checked) 25.dp else 2.dp, label = "toggle-knob")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
            ) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(53.dp)
                .height(28.dp)
                .background(if (checked) ink.acid else Color.Transparent)
                .border(2.dp, ink.fg)
                .padding(2.dp),
        ) {
            Box(
                Modifier
                    .offset(x = knobOffset)
                    .size(20.dp)
                    .background(if (checked) ink.onAccent else ink.dim),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = ink.fg,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = ink.dim)
        }
    }
}

@Composable
fun StickyNote(
    text: String,
    background: Color,
    rotation: Float = 1.4f,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF151410),
    kicker: String? = null,
) {
    val ink = LocalInkPalette.current
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation }
            .padding(end = 3.dp, bottom = 3.dp),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(3.dp, 3.dp)
                .background(ink.fg),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(background)
                .border(1.5.dp, ink.fg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (kicker != null) {
                Text(
                    text = kicker,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                )
                Spacer(Modifier.width(9.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}

/** Small square control used on photo cards. */
@Composable
fun PhotoMiniButton(
    symbol: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInkPalette.current
    Box(
        modifier = modifier
            .size(30.dp)
            .semantics { this.contentDescription = contentDescription }
            .border(1.5.dp, ink.fg.copy(alpha = if (enabled) 1f else 0.25f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelLarge,
            color = ink.fg,
        )
    }
}

@Composable
fun TapeStrip(
    color: Color,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    width: Dp = 52.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(17.dp)
            .graphicsLayer {
                rotationZ = rotation
                alpha = 0.78f
            }
            .background(color)
            .border(1.dp, color.copy(alpha = 0.35f)),
    )
}

/** Four-point sparkle, used as a printed accent. */
@Composable
fun Sparkle(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            quadraticBezierTo(size.width * 0.56f, size.height * 0.44f, size.width, size.height / 2f)
            quadraticBezierTo(size.width * 0.56f, size.height * 0.56f, size.width / 2f, size.height)
            quadraticBezierTo(size.width * 0.44f, size.height * 0.56f, 0f, size.height / 2f)
            quadraticBezierTo(size.width * 0.44f, size.height * 0.44f, size.width / 2f, 0f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

/** Fixed paper backdrop: dot grid, marker smears and a little print grain. */
@Composable
fun PaperBackdrop(modifier: Modifier = Modifier) {
    val ink = LocalInkPalette.current
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = ink.bg)

        val step = 24.dp.toPx()
        val dot = 0.9.dp.toPx()
        var y = step
        while (y < size.height) {
            var x = step
            while (x < size.width) {
                drawCircle(
                    color = ink.fg.copy(alpha = 0.09f),
                    radius = dot,
                    center = Offset(x, y),
                )
                x += step
            }
            y += step
        }

        // A fat highlighter stroke dragged across the top right corner.
        rotate(degrees = -13f, pivot = Offset(size.width, 0f)) {
            drawLine(
                color = ink.acid.copy(alpha = 0.42f),
                start = Offset(size.width * 0.54f, 44.dp.toPx()),
                end = Offset(size.width * 1.14f, 44.dp.toPx()),
                strokeWidth = 70.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // A cobalt ring resting against the lower-left edge.
        drawCircle(
            color = ink.cobalt.copy(alpha = 0.12f),
            radius = size.width * 0.42f,
            center = Offset(-size.width * 0.16f, size.height * 0.82f),
            style = Stroke(width = 16.dp.toPx()),
        )

        // Print grain. Deterministic so it does not shimmer.
        for (i in 0 until 420) {
            val gx = ((i * 97.31f) % size.width)
            val gy = ((i * 197.73f + (i % 11) * 43f) % size.height)
            drawCircle(
                color = ink.fg.copy(alpha = 0.055f),
                radius = (0.55f + (i % 3) * 0.3f) * density,
                center = Offset(gx, gy),
            )
        }
    }
}

/** Infinite ticker: text is measured once and drawn repeatedly on a canvas. */
@Composable
fun Ticker(
    text: String,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInkPalette.current
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium.copy(color = ink.acid)
    val layout = remember(text, style, measurer) {
        measurer.measure(
            text = AnnotatedString("   $text   ✳   "),
            style = style,
            softWrap = false,
            maxLines = 1,
        )
    }
    val transition = rememberInfiniteTransition(label = "ticker")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ticker-progress",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(ink.invertBg)
            .clipToBounds(),
    ) {
        val segment = layout.size.width.toFloat()
        if (segment <= 0f) return@Canvas

        val top = ((size.height - layout.size.height) / 2f).coerceAtLeast(0f)
        var x = (-progress * segment) % segment - segment
        while (x < size.width) {
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(x, top),
            )
            x += segment
        }
    }
}

/** Chunky slider with a hatched marker fill and a tilted ink thumb. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    fillColor: Color,
    railColor: Color,
    borderColor: Color,
    thumbColor: Color,
    thumbAccent: Color,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(40.dp),
        valueRange = valueRange,
        thumb = {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(28.dp)
                    .graphicsLayer { rotationZ = -8f }
                    .background(thumbColor)
                    .border(2.dp, borderColor),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(thumbAccent),
                )
            }
        },
        track = { state ->
            val fraction = fractionOf(state.value, state.valueRange)
            Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                val railHeight = 14.dp.toPx()
                val top = (size.height - railHeight) / 2f
                drawRect(
                    color = railColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, railHeight),
                )
                drawRect(
                    color = fillColor,
                    topLeft = Offset(0f, top),
                    size = Size(size.width * fraction, railHeight),
                )

                val hatchStep = 11.dp.toPx()
                var hatchX = 0f
                while (hatchX < size.width * fraction) {
                    drawLine(
                        color = borderColor.copy(alpha = 0.2f),
                        start = Offset(hatchX, top + railHeight),
                        end = Offset(hatchX + railHeight, top),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                    hatchX += hatchStep
                }

                for (i in 0..4) {
                    val tickX = (size.width * i / 4f).coerceIn(1f, size.width - 1f)
                    drawLine(
                        color = borderColor.copy(alpha = if (i == 0 || i == 4) 0.85f else 0.3f),
                        start = Offset(tickX, top - 4.dp.toPx()),
                        end = Offset(tickX, top),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }

                drawRect(
                    color = borderColor,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        },
    )
}

/** Range slider whose track is the actual hue wheel, dimmed outside the picked range. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HueRangeSlider(
    start: Float,
    end: Float,
    onValueChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInkPalette.current
    val rainbow = remember {
        Brush.horizontalGradient(
            0f to Color(0xFFFF3B30),
            0.17f to Color(0xFFFFD60A),
            0.34f to Color(0xFF34C759),
            0.5f to Color(0xFF32D7D7),
            0.67f to Color(0xFF3B6BFF),
            0.84f to Color(0xFFB14CFF),
            1f to Color(0xFFFF3B30),
        )
    }

    RangeSlider(
        value = start..end,
        onValueChange = { onValueChange(it.start, it.endInclusive) },
        modifier = modifier.fillMaxWidth().height(40.dp),
        valueRange = 0f..360f,
        startThumb = { RangeThumb(ink.fg, ink.bg, ink.acid) },
        endThumb = { RangeThumb(ink.fg, ink.bg, ink.acid) },
        track = { state ->
            val startFraction = fractionOf(state.activeRangeStart, state.valueRange)
            val endFraction = fractionOf(state.activeRangeEnd, state.valueRange)
            Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                val railHeight = 14.dp.toPx()
                val top = (size.height - railHeight) / 2f

                drawRect(
                    brush = rainbow,
                    topLeft = Offset(0f, top),
                    size = Size(size.width, railHeight),
                )
                drawRect(
                    color = ink.bg.copy(alpha = 0.78f),
                    topLeft = Offset(0f, top),
                    size = Size(size.width * startFraction, railHeight),
                )
                drawRect(
                    color = ink.bg.copy(alpha = 0.78f),
                    topLeft = Offset(size.width * endFraction, top),
                    size = Size(size.width * (1f - endFraction), railHeight),
                )

                drawRect(
                    color = ink.fg,
                    topLeft = Offset(size.width * startFraction, top),
                    size = Size(size.width * (endFraction - startFraction), railHeight),
                    style = Stroke(width = 3.dp.toPx()),
                )
                drawRect(
                    color = ink.fg,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        },
    )
}

@Composable
private fun RangeThumb(
    background: Color,
    border: Color,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .size(width = 20.dp, height = 30.dp)
            .background(background)
            .border(2.dp, border),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(12.dp)
                .background(accent),
        )
    }
}

private fun fractionOf(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val span = range.endInclusive - range.start
    if (span == 0f) return 0f
    return ((value - range.start) / span).coerceIn(0f, 1f)
}
