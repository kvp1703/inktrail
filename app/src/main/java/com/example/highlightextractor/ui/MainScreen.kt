package com.example.highlightextractor.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.highlightextractor.core.ColorPreset
import com.example.highlightextractor.core.ImageLoader
import com.example.highlightextractor.ui.components.DottedRule
import com.example.highlightextractor.ui.components.HueRangeSlider
import com.example.highlightextractor.ui.components.InkBadge
import com.example.highlightextractor.ui.components.InkBlock
import com.example.highlightextractor.ui.components.InkButton
import com.example.highlightextractor.ui.components.InkCheckRow
import com.example.highlightextractor.ui.components.InkSlider
import com.example.highlightextractor.ui.components.InkToggleRow
import com.example.highlightextractor.ui.components.PaperBackdrop
import com.example.highlightextractor.ui.components.PhotoMiniButton
import com.example.highlightextractor.ui.components.SectionHead
import com.example.highlightextractor.ui.components.Sparkle
import com.example.highlightextractor.ui.components.StickyNote
import com.example.highlightextractor.ui.components.TapeStrip
import com.example.highlightextractor.ui.components.Ticker
import com.example.highlightextractor.ui.theme.LocalInkPalette
import com.example.highlightextractor.ui.theme.MarkerInk
import com.example.highlightextractor.ui.theme.MonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val ink = LocalInkPalette.current

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addPhotos(uris)
    }

    val documentCreator = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) viewModel.saveResultTo(uri)
    }

    var showGuide by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ink.bg),
    ) {
        PaperBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            Header()

            Spacer(Modifier.height(20.dp))
            Ticker(
                text = "HIGHLIGHTS, EXTRACTED  //  NO CLOUD  //  NO LOGIN  //  " +
                    "READS THE MARKER, NOT THE PAGE  //  ON-DEVICE OCR  //  ",
            )
            Spacer(Modifier.height(34.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                PhotosSection(
                    photos = state.photos,
                    onSelect = { photoPicker.launch("image/*") },
                    onRemove = viewModel::removePhoto,
                    onMoveUp = { viewModel.movePhoto(it, -1) },
                    onMoveDown = { viewModel.movePhoto(it, 1) },
                )

                ColorSection(
                    selectedIndex = state.selectedPresetIndex,
                    activePreset = state.activePreset,
                    threshold = state.threshold,
                    onSelectPreset = viewModel::selectPreset,
                    onHueRangeChange = viewModel::setHueRange,
                    onSaturationChange = viewModel::setSaturationMin,
                    onValueChange = viewModel::setValueMin,
                    onThresholdChange = viewModel::setThreshold,
                    onShowGuide = { showGuide = true },
                )

                ExtractSection(
                    photoCount = state.photos.size,
                    isProcessing = state.isProcessing,
                    progressDone = state.progressDone,
                    progressTotal = state.progressTotal,
                    onExtract = viewModel::startExtraction,
                    onCancel = viewModel::cancelExtraction,
                )

                ResultsSection(
                    state = state,
                    onResultChange = viewModel::updateResultText,
                    onToggleSeparators = viewModel::setAddSeparators,
                    onToggleDebug = viewModel::setDebugPreview,
                    onSave = { documentCreator.launch("highlights.txt") },
                    onShare = { shareText(context, state.resultText) },
                )

                Footer()
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (showGuide) {
        ColorGuideDialog(onDismiss = { showGuide = false })
    }
}

// ---------------------------------------------------------------- header

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header() {
    val ink = LocalInkPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "INK",
                style = MaterialTheme.typography.labelLarge,
                color = ink.fg,
            )
            Text(
                text = "TRAIL",
                style = MaterialTheme.typography.labelLarge,
                color = ink.onAccent,
                modifier = Modifier
                    .padding(start = 3.dp)
                    .background(ink.acid)
                    .padding(horizontal = 3.dp),
            )
            Text(
                text = "™",
                style = MaterialTheme.typography.labelSmall,
                color = ink.dim,
                modifier = Modifier.padding(start = 3.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "FIELD UNIT / 001",
                style = MaterialTheme.typography.labelSmall,
                color = ink.dim,
            )
        }

        Spacer(Modifier.height(26.dp))
        Text(
            text = "AN ANALOG PROBLEM,",
            style = MaterialTheme.typography.labelMedium,
            color = ink.dim,
        )
        Text(
            text = "SOLVED DIGITALLY.",
            style = MaterialTheme.typography.labelMedium,
            color = ink.hot,
        )

        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val heroStyle = if (maxWidth < 300.dp) {
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = 39.sp,
                    lineHeight = 36.sp,
                    letterSpacing = (-1.1).sp,
                )
            } else {
                MaterialTheme.typography.displayLarge
            }
            Column {
                Text(
                    text = "HIGHLIGHTS,",
                    style = heroStyle,
                    color = ink.fg,
                )
                Text(
                    text = "EXTRACTED.",
                    style = heroStyle,
                    color = ink.onAccent,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(ink.acid)
                        .padding(horizontal = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Point a camera at a marked-up page. Get back only the words " +
                "under the marker. No cloud, no account, no nonsense.",
            style = MaterialTheme.typography.bodyLarge,
            color = ink.dim,
        )

        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            InkBadge(
                text = "NO CLOUD",
                background = Color.Transparent,
                foreground = ink.fg,
                border = ink.fg,
            )
            InkBadge(
                text = "NO LOGIN",
                background = Color.Transparent,
                foreground = ink.fg,
                border = ink.fg,
            )
            InkBadge(
                text = "100% ON-DEVICE",
                background = ink.acid,
                foreground = ink.onAccent,
                border = ink.fg,
            )
        }
    }
}

// ---------------------------------------------------------------- 01 photos

@Composable
private fun PhotosSection(
    photos: List<Uri>,
    onSelect: () -> Unit,
    onRemove: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHead(
            number = "01",
            title = "THE PAGES",
            subtitle = "Photos in. Order matters.",
            trailing = if (photos.isEmpty()) {
                "0 SHEETS"
            } else {
                "${photos.size} SHEET${if (photos.size == 1) "" else "S"}"
            },
        )
        Spacer(Modifier.height(16.dp))

        if (photos.isEmpty()) {
            EmptyPagesTile(onSelect = onSelect)
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                itemsIndexed(photos) { index, uri ->
                    PhotoCard(
                        index = index,
                        uri = uri,
                        canMoveUp = index > 0,
                        canMoveDown = index < photos.lastIndex,
                        onRemove = { onRemove(index) },
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                    )
                }
                item {
                    AddPageTile(onSelect = onSelect)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "TOP TO BOTTOM = PAGE ORDER. USE ARROWS TO RE-SHUFFLE.",
                style = MaterialTheme.typography.labelSmall,
                color = LocalInkPalette.current.dim,
            )
        }
    }
}

@Composable
private fun EmptyPagesTile(onSelect: () -> Unit) {
    val ink = LocalInkPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .drawBehind {
                drawRect(color = ink.panel)
                drawRect(
                    color = ink.fg,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(11.dp.toPx(), 7.dp.toPx()),
                            phase = 0f,
                        ),
                    ),
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect,
            ),
    ) {
        TapeStrip(
            color = ink.acid,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp),
            rotation = 2.5f,
            width = 64.dp,
        )
        Text(
            text = "DROP ZONE",
            style = MaterialTheme.typography.labelSmall,
            color = ink.dim,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        )
        Text(
            text = "JPG / PNG / HEIC",
            style = MaterialTheme.typography.labelSmall,
            color = ink.dim,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(ink.acid)
                    .border(2.dp, ink.fg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ink.onAccent,
                )
            }
            Spacer(Modifier.height(13.dp))
            Text(
                text = "ADD PAGE PHOTOS",
                style = MaterialTheme.typography.labelLarge,
                color = ink.fg,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap to open the photo picker.",
                style = MaterialTheme.typography.bodySmall,
                color = ink.dim,
            )
        }
    }
}

@Composable
private fun PhotoCard(
    index: Int,
    uri: Uri,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val ink = LocalInkPalette.current
    val tilt = remember(index) {
        listOf(-2.4f, 1.9f, -1.4f, 2.7f, -2f)[index % 5]
    }

    InkBlock(
        modifier = Modifier.width(158.dp),
        background = ink.sheet,
        borderColor = ink.fg,
        shadowColor = ink.fg,
        shadowOffset = 4.dp,
        rotation = tilt,
        contentPadding = PaddingValues(7.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(146.dp)
                    .background(ink.panel),
            ) {
                PhotoThumbnail(uri = uri, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(ink.invertBg)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium,
                        color = ink.acid,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                PhotoMiniButton(
                    symbol = "↑",
                    contentDescription = "Move page ${index + 1} up",
                    enabled = canMoveUp,
                    onClick = onMoveUp,
                )
                PhotoMiniButton(
                    symbol = "↓",
                    contentDescription = "Move page ${index + 1} down",
                    enabled = canMoveDown,
                    onClick = onMoveDown,
                )
                Spacer(Modifier.weight(1f))
                PhotoMiniButton(
                    symbol = "×",
                    contentDescription = "Remove page ${index + 1}",
                    enabled = true,
                    onClick = onRemove,
                )
            }
        }
        TapeStrip(
            color = ink.acid,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp),
            rotation = -3.5f,
            width = 44.dp,
        )
    }
}

@Composable
private fun AddPageTile(onSelect: () -> Unit) {
    val ink = LocalInkPalette.current
    Box(
        modifier = Modifier
            .width(112.dp)
            .height(198.dp)
            .graphicsLayer { rotationZ = 1.6f }
            .padding(end = 4.dp, bottom = 4.dp),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(ink.fg),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ink.panel)
                .border(2.dp, ink.fg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onSelect,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.displayMedium,
                    color = ink.fg,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ADD MORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.fg,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "PAGES",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.dim,
                )
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ink = LocalInkPalette.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching { ImageLoader.loadThumbnail(context, uri, 480) }.getOrNull()
        }
        bitmap = loaded
        failed = loaded == null
    }

    Box(modifier = modifier) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (failed) "NO" else "···",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ink.dim,
                )
                Text(
                    text = if (failed) "PREVIEW" else "LOADING",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.dim,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 02 color

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSection(
    selectedIndex: Int,
    activePreset: ColorPreset,
    threshold: Float,
    onSelectPreset: (Int) -> Unit,
    onHueRangeChange: (Float, Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onShowGuide: () -> Unit,
) {
    val ink = LocalInkPalette.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHead(
            number = "02",
            title = "THE MARKER",
            subtitle = "Point the scanner at one colour.",
            trailing = activePreset.name.uppercase(),
        )
        Spacer(Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            ColorPreset.builtIn.forEachIndexed { index, preset ->
                PresetChip(
                    name = preset.name,
                    color = MarkerInk.forPreset(preset.name),
                    selected = selectedIndex == index,
                    onClick = { onSelectPreset(index) },
                )
            }
            PresetChip(
                name = ColorPreset.CUSTOM_NAME,
                color = MarkerInk.yellow,
                selected = selectedIndex == ColorPreset.builtIn.size,
                onClick = { onSelectPreset(ColorPreset.builtIn.size) },
            )
        }

        Spacer(Modifier.height(16.dp))
        InkBlock(
            modifier = Modifier.fillMaxWidth(),
            background = ink.panel,
            borderColor = ink.fg,
            shadowColor = ink.fg,
            shadowOffset = 5.dp,
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MarkerSwatch(
                    name = activePreset.name,
                    color = MarkerInk.forPreset(activePreset.name),
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "USING ${activePreset.name.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ink.fg,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (expanded) {
                            "Tweak the gates below."
                        } else {
                            "The default wins most of the time."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ink.dim,
                    )
                }
                Spacer(Modifier.width(8.dp))
                InkButton(
                    text = if (expanded) "HIDE" else "TUNE",
                    onClick = { expanded = !expanded },
                    background = ink.acid,
                    foreground = ink.onAccent,
                    border = ink.fg,
                    shadowColor = ink.fg,
                    rotation = -1f,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "FIELD NOTES: HOW COLOUR MATCHING WORKS →",
            style = MaterialTheme.typography.labelSmall.copy(
                textDecoration = TextDecoration.Underline,
            ),
            color = ink.fg,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onShowGuide,
                )
                .padding(vertical = 5.dp),
        )

        if (expanded) {
            Spacer(Modifier.height(16.dp))
            TuningPanel(
                activePreset = activePreset,
                threshold = threshold,
                onHueRangeChange = onHueRangeChange,
                onSaturationChange = onSaturationChange,
                onValueChange = onValueChange,
                onThresholdChange = onThresholdChange,
            )
        }
    }
}

@Composable
private fun PresetChip(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink = LocalInkPalette.current
    Box(
        modifier = Modifier
            .graphicsLayer { rotationZ = if (selected) -2f else 0f }
            .padding(end = 4.dp, bottom = 4.dp),
    ) {
        if (selected) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(3.dp, 3.dp)
                    .background(ink.fg),
            )
        }
        Row(
            modifier = Modifier
                .background(if (selected) ink.invertBg else ink.sheet)
                .border(2.dp, ink.fg)
                .selectable(
                    selected = selected,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarkerSwatch(
                name = name,
                color = color,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) ink.invertFg else ink.fg,
            )
            if (selected) {
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.acid,
                )
            }
        }
    }
}

@Composable
private fun MarkerSwatch(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val ink = LocalInkPalette.current
    Canvas(modifier = modifier.border(1.5.dp, ink.fg)) {
        if (name == ColorPreset.CUSTOM_NAME) {
            val halfW = size.width / 2f
            val halfH = size.height / 2f
            drawRect(color = MarkerInk.yellow, size = Size(halfW, halfH))
            drawRect(
                color = MarkerInk.pink,
                topLeft = Offset(halfW, 0f),
                size = Size(halfW, halfH),
            )
            drawRect(
                color = MarkerInk.blue,
                topLeft = Offset(0f, halfH),
                size = Size(halfW, halfH),
            )
            drawRect(
                color = MarkerInk.green,
                topLeft = Offset(halfW, halfH),
                size = Size(halfW, halfH),
            )
        } else {
            drawRect(color = color)
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

@Composable
private fun TuningPanel(
    activePreset: ColorPreset,
    threshold: Float,
    onHueRangeChange: (Float, Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    onThresholdChange: (Float) -> Unit,
) {
    val ink = LocalInkPalette.current
    val markerColor = MarkerInk.forPreset(activePreset.name)

    InkBlock(
        modifier = Modifier.fillMaxWidth(),
        background = ink.sheet,
        borderColor = ink.fg,
        shadowColor = ink.acid,
        shadowOffset = 6.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FINE TUNE / EXPERIMENTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.dim,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "AUTO → CUSTOM",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.hot,
                )
            }

            Spacer(Modifier.height(16.dp))
            ParamHeader(
                label = "HUE GATE",
                value = "${activePreset.hMin.roundToInt()}° → ${activePreset.hMax.roundToInt()}°",
            )
            HueRangeSlider(
                start = activePreset.hMin,
                end = activePreset.hMax,
                onValueChange = onHueRangeChange,
            )

            Spacer(Modifier.height(12.dp))
            ParamHeader(
                label = "SAT FLOOR",
                value = "${(activePreset.sMin * 100).roundToInt()}%",
            )
            InkSlider(
                value = activePreset.sMin,
                onValueChange = onSaturationChange,
                fillColor = markerColor,
                railColor = ink.panel,
                borderColor = ink.fg,
                thumbColor = ink.fg,
                thumbAccent = ink.acid,
            )

            Spacer(Modifier.height(12.dp))
            ParamHeader(
                label = "BRIGHT FLOOR",
                value = "${(activePreset.vMin * 100).roundToInt()}%",
            )
            InkSlider(
                value = activePreset.vMin,
                onValueChange = onValueChange,
                fillColor = markerColor.copy(alpha = 0.65f),
                railColor = ink.panel,
                borderColor = ink.fg,
                thumbColor = ink.fg,
                thumbAccent = ink.acid,
            )

            Spacer(Modifier.height(16.dp))
            DottedRule(color = ink.fg.copy(alpha = 0.3f))
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "SENSITIVITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = ink.fg,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "How much of a word must be marked to keep it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ink.dim,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .background(ink.acid)
                        .border(1.5.dp, ink.fg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "${(threshold * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = ink.onAccent,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            InkSlider(
                value = threshold,
                onValueChange = onThresholdChange,
                valueRange = 0.2f..0.9f,
                fillColor = ink.acid,
                railColor = ink.panel,
                borderColor = ink.fg,
                thumbColor = ink.fg,
                thumbAccent = ink.hot,
            )

            Spacer(Modifier.height(6.dp))
            Text(
                text = "EDITING SPAWNS A CUSTOM PRESET. THE BUILT-INS STAY PURE.",
                style = MaterialTheme.typography.labelSmall,
                color = ink.dim,
            )
        }
    }
}

@Composable
private fun ParamHeader(label: String, value: String) {
    val ink = LocalInkPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ink.fg,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = ink.dim,
        )
    }
}

// ---------------------------------------------------------------- 03 extract

@Composable
private fun ExtractSection(
    photoCount: Int,
    isProcessing: Boolean,
    progressDone: Int,
    progressTotal: Int,
    onExtract: () -> Unit,
    onCancel: () -> Unit,
) {
    val ink = LocalInkPalette.current
    val targetFraction = if (progressTotal > 0) {
        progressDone.toFloat() / progressTotal
    } else {
        0f
    }
    val fraction by animateFloatAsState(
        targetValue = if (isProcessing) targetFraction else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "extraction-progress",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHead(
            number = "03",
            title = "THE ENGINE",
            subtitle = "Local OCR. Nothing leaves the phone.",
            trailing = if (isProcessing) "RUNNING" else "READY",
        )
        Spacer(Modifier.height(16.dp))

        InkBlock(
            modifier = Modifier.fillMaxWidth(),
            background = ink.invertBg,
            borderColor = if (ink.isDark) ink.dim.copy(alpha = 0.55f) else ink.fg,
            shadowColor = ink.hot,
            shadowOffset = 8.dp,
            rotation = -0.5f,
            contentPadding = PaddingValues(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MODE / EXTRACT",
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.acid,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(
                                if (isProcessing) ink.acid else ink.invertFg.copy(alpha = 0.55f),
                            ),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isProcessing) "CRUNCHING" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.invertFg.copy(alpha = 0.65f),
                    )
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "INK IN.",
                    style = MaterialTheme.typography.displayMedium,
                    color = ink.invertFg,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "TEXT ",
                        style = MaterialTheme.typography.displayMedium,
                        color = ink.invertFg,
                    )
                    Text(
                        text = "OUT.",
                        style = MaterialTheme.typography.displayMedium,
                        color = ink.acid,
                    )
                }

                Spacer(Modifier.height(18.dp))
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .border(2.dp, ink.invertFg)
                            .padding(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(ink.acid),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PAGE ${progressDone.coerceAtLeast(1)} OF $progressTotal",
                            style = MaterialTheme.typography.labelMedium,
                            color = ink.acid,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "${(fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = ink.invertFg,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    InkButton(
                        text = "CANCEL RUN",
                        onClick = onCancel,
                        background = ink.invertBg,
                        foreground = ink.invertFg,
                        border = ink.invertFg,
                        shadowColor = ink.hot,
                        rotation = 0.8f,
                    )
                } else {
                    InkButton(
                        text = "EXTRACT HIGHLIGHTS →",
                        onClick = onExtract,
                        enabled = photoCount > 0,
                        background = ink.acid,
                        foreground = ink.onAccent,
                        border = ink.onAccent,
                        shadowColor = ink.cobalt,
                        fillWidth = true,
                    )
                    if (photoCount == 0) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "NO PAGES IN THE QUEUE — ADD SOME ABOVE ↑",
                            style = MaterialTheme.typography.labelSmall,
                            color = ink.hot,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "ON-DEVICE OCR / LATIN SCRIPT / NO UPLOAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.invertFg.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 04 results

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultsSection(
    state: MainUiState,
    onResultChange: (String) -> Unit,
    onToggleSeparators: (Boolean) -> Unit,
    onToggleDebug: (Boolean) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val ink = LocalInkPalette.current
    val clipboard = LocalClipboardManager.current
    val hasText = state.resultText.isNotBlank()
    var copied by remember { mutableStateOf(false) }
    val debugBitmap = state.debugBitmap

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    val trailing = when {
        state.isProcessing -> "RUNNING"
        state.hasResult && hasText -> "${state.resultText.length} CHARS"
        state.hasResult -> "EMPTY"
        else -> "WAITING"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHead(
            number = "04",
            title = "THE EXTRACT",
            subtitle = "Editable output. Keep it or fling it.",
            trailing = trailing,
        )
        Spacer(Modifier.height(16.dp))

        OutputSheet(
            state = state,
            onResultChange = onResultChange,
        )

        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            InkButton(
                text = if (copied) "COPIED ✓" else "COPY TEXT",
                onClick = {
                    clipboard.setText(AnnotatedString(state.resultText))
                    copied = true
                },
                enabled = hasText,
                background = ink.acid,
                foreground = ink.onAccent,
                border = ink.onAccent,
                shadowColor = ink.hot,
            )
            InkButton(
                text = "SAVE .TXT",
                onClick = onSave,
                enabled = hasText,
                background = ink.sheet,
                foreground = ink.fg,
                border = ink.fg,
                shadowColor = ink.fg,
            )
            InkButton(
                text = "SHARE",
                onClick = onShare,
                enabled = hasText,
                background = ink.sheet,
                foreground = ink.fg,
                border = ink.fg,
                shadowColor = ink.fg,
                rotation = 0.8f,
            )
        }

        Spacer(Modifier.height(18.dp))
        InkBlock(
            modifier = Modifier.fillMaxWidth(),
            background = ink.sheet,
            borderColor = ink.fg,
            shadowColor = ink.fg,
            shadowOffset = 4.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 3.dp),
        ) {
            Column {
                InkCheckRow(
                    checked = state.addSeparators,
                    onCheckedChange = onToggleSeparators,
                    title = "Photo separators",
                    subtitle = "Stamp a --- Photo N --- header before each page's text.",
                )
                DottedRule(color = ink.fg.copy(alpha = 0.25f))
                InkToggleRow(
                    checked = state.debugPreview,
                    onCheckedChange = onToggleDebug,
                    title = "Debug vision",
                    subtitle = "Draw green/red word boxes on the next first-page run.",
                )
            }
        }

        if (state.noHighlights) {
            Spacer(Modifier.height(14.dp))
            StickyNote(
                text = "No highlights found. Try another marker colour, lower the " +
                    "sensitivity, or open Field Notes before you rage-quit.",
                background = ink.hot,
                kicker = "MISS",
            )
        }

        state.statusMessage?.let { message ->
            Spacer(Modifier.height(14.dp))
            StickyNote(
                text = message,
                background = ink.acid,
                kicker = "NOTE",
            )
        }

        if (state.errorMessages.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.errorMessages.forEach { message ->
                    StickyNote(
                        text = message,
                        background = Color(0xFFFFC6B4),
                        kicker = "ERR",
                        rotation = -1.2f,
                    )
                }
            }
        }

        if (state.debugPreview && debugBitmap != null) {
            Spacer(Modifier.height(18.dp))
            DebugPreview(bitmap = debugBitmap)
        }
    }
}

@Composable
private fun OutputSheet(
    state: MainUiState,
    onResultChange: (String) -> Unit,
) {
    val ink = LocalInkPalette.current
    var focused by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
        InkBlock(
            modifier = Modifier.fillMaxWidth(),
            background = ink.sheet,
            borderColor = ink.fg,
            shadowColor = ink.cobalt,
            shadowOffset = 7.dp,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ink.invertBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "HL_EXTRACT.TXT",
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.acid,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "UTF-8 / ${state.resultText.length} CH",
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.invertFg.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(if (focused) ink.acid else ink.hot),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 250.dp)
                        .drawBehind {
                            val step = 20.dp.toPx()
                            val dot = 0.8.dp.toPx()
                            var y = step
                            while (y < size.height) {
                                var x = step
                                while (x < size.width) {
                                    drawCircle(
                                        color = ink.fg.copy(alpha = 0.1f),
                                        radius = dot,
                                        center = Offset(x, y),
                                    )
                                    x += step
                                }
                                y += step
                            }
                            drawLine(
                                color = ink.red.copy(alpha = 0.55f),
                                start = Offset(34.dp.toPx(), 0f),
                                end = Offset(34.dp.toPx(), size.height),
                                strokeWidth = 1.5.dp.toPx(),
                            )
                        }
                        .padding(start = 46.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                ) {
                    BasicTextField(
                        value = state.resultText,
                        onValueChange = onResultChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 222.dp)
                            .onFocusChanged { focused = it.isFocused },
                        textStyle = TextStyle(
                            fontFamily = MonoFamily,
                            fontSize = 14.sp,
                            lineHeight = 23.sp,
                            color = ink.fg,
                        ),
                        cursorBrush = SolidColor(ink.hot),
                        decorationBox = { innerTextField ->
                            Box {
                                if (state.resultText.isEmpty()) {
                                    Text(
                                        text = "The highlighted words will land here.\n\n" +
                                            "Add page photos, pick a marker colour, then hit extract.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ink.dim,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.resultText.isEmpty()) {
                            "WAITING FOR INK"
                        } else {
                            "EDITABLE / TAP TO WRITE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.dim,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "LOCAL ONLY",
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.dim,
                    )
                }
            }
        }

        TapeStrip(
            color = ink.acid,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-22).dp, y = (-13).dp),
            rotation = 7f,
            width = 62.dp,
        )
    }
}

@Composable
private fun DebugPreview(bitmap: Bitmap) {
    val ink = LocalInkPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "DEBUG VISION / FIRST PAGE",
                style = MaterialTheme.typography.labelSmall,
                color = ink.fg,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "GREEN = KEPT / RED = DROPPED",
                style = MaterialTheme.typography.labelSmall,
                color = ink.dim,
            )
        }
        Spacer(Modifier.height(8.dp))
        InkBlock(
            modifier = Modifier.fillMaxWidth(),
            background = ink.panel,
            borderColor = ink.fg,
            shadowColor = ink.hot,
            shadowOffset = 6.dp,
            rotation = -0.6f,
            contentPadding = PaddingValues(6.dp),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Debug preview with word boxes",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

// ---------------------------------------------------------------- guide

@Composable
private fun ColorGuideDialog(onDismiss: () -> Unit) {
    val ink = LocalInkPalette.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            InkBlock(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                background = ink.sheet,
                borderColor = ink.fg,
                shadowColor = ink.acid,
                shadowOffset = 9.dp,
                rotation = -0.6f,
                contentPadding = PaddingValues(20.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FIELD NOTES",
                            style = MaterialTheme.typography.labelLarge,
                            color = ink.fg,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "COLOR / 02",
                            style = MaterialTheme.typography.labelSmall,
                            color = ink.dim,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "HOW THE\nMARKER MATH WORKS.",
                        style = MaterialTheme.typography.displayMedium,
                        color = ink.fg,
                    )
                    Spacer(Modifier.height(12.dp))
                    DottedRule(color = ink.fg.copy(alpha = 0.3f))
                    Spacer(Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GuideNote(
                            index = "A",
                            title = "Hue gate",
                            body = "The colour family on a 0°–360° circle. Widen it a little; " +
                                "camera white balance always lies.",
                        )
                        GuideNote(
                            index = "B",
                            title = "Sat floor",
                            body = "How much colour a pixel needs. Lower it for a faded marker, " +
                                "raise it to ignore creamy paper.",
                        )
                        GuideNote(
                            index = "C",
                            title = "Bright floor",
                            body = "How light a pixel needs to be. Raise it to reject shadows " +
                                "and black ink.",
                        )
                        GuideNote(
                            index = "D",
                            title = "Sensitivity",
                            body = "How much of a word must be marked before it is kept. Lower " +
                                "to catch ghosts, raise to reject strays.",
                        )
                        GuideNote(
                            index = "!",
                            title = "Quick rescue",
                            body = "Missing words: lower a floor or the sensitivity. Extra words: " +
                                "raise sensitivity or narrow the hue gate. Whole line missing: " +
                                "check the hue gate first.",
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    InkButton(
                        text = "GOT IT",
                        onClick = onDismiss,
                        background = ink.acid,
                        foreground = ink.onAccent,
                        border = ink.onAccent,
                        shadowColor = ink.fg,
                        fillWidth = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideNote(index: String, title: String, body: String) {
    val ink = LocalInkPalette.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(ink.acid)
                .border(1.5.dp, ink.fg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index,
                style = MaterialTheme.typography.labelSmall,
                color = ink.onAccent,
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = ink.fg,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = ink.dim,
            )
        }
    }
}

// ---------------------------------------------------------------- footer

@Composable
private fun Footer() {
    val ink = LocalInkPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        DottedRule(color = ink.fg.copy(alpha = 0.35f))
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "STAY INKED.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = ink.fg,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "MADE FOR PAPER PEOPLE.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.dim,
                )
            }
            Sparkle(
                color = ink.acid,
                modifier = Modifier.size(46.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
            val pattern = intArrayOf(
                2, 1, 3, 1, 1, 4, 1, 2, 3, 1, 2, 2, 1, 1, 3, 1, 4, 1, 2, 3,
                1, 1, 2, 4, 1, 3, 2, 1, 1, 3, 2, 1, 4, 1, 2, 2, 3, 1, 1, 2,
            )
            var x = 0f
            var i = 0
            while (x < size.width * 0.62f) {
                val barWidth = pattern[i % pattern.size].dp.toPx()
                if (i % 2 == 0) {
                    drawRect(
                        color = ink.fg,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                    )
                }
                x += barWidth
                i++
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NO CLOUD / NO ACCOUNT / NO NONSENSE",
                style = MaterialTheme.typography.labelSmall,
                color = ink.dim,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "© 2026",
                style = MaterialTheme.typography.labelSmall,
                color = ink.dim,
            )
        }
    }
}

private fun shareText(context: Context, text: String) {
    if (text.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Extracted highlights")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share highlights"))
}
