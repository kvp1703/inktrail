package com.example.highlightextractor.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.highlightextractor.core.ColorPreset
import com.example.highlightextractor.core.HighlightExtractor
import com.example.highlightextractor.core.ImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val photos: List<Uri> = emptyList(),
    /** Index into [ColorPreset.builtIn]; the value [ColorPreset.builtIn.size] means Custom. */
    val selectedPresetIndex: Int = 0,
    val customPreset: ColorPreset = ColorPreset.customDefault,
    val threshold: Float = 0.5f,
    val addSeparators: Boolean = false,
    val debugPreview: Boolean = false,
    val isProcessing: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
    val resultText: String = "",
    val debugBitmap: Bitmap? = null,
    val errorMessages: List<String> = emptyList(),
    val noHighlights: Boolean = false,
    val hasResult: Boolean = false,
    val statusMessage: String? = null,
) {
    val activePreset: ColorPreset
        get() = if (selectedPresetIndex in ColorPreset.builtIn.indices) {
            ColorPreset.builtIn[selectedPresetIndex]
        } else {
            customPreset
        }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var extractionJob: Job? = null
    private var runCounter = 0
    private var currentRunId = 0

    // ---------- Photo selection / ordering ----------

    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _uiState.update { it.copy(photos = it.photos + uris, statusMessage = null) }
    }

    fun removePhoto(index: Int) {
        _uiState.update { state ->
            if (index !in state.photos.indices) {
                state
            } else {
                state.copy(photos = state.photos.filterIndexed { i, _ -> i != index })
            }
        }
    }

    /** Moves the photo at [index] by [offset] (-1 = up, +1 = down). */
    fun movePhoto(index: Int, offset: Int) {
        _uiState.update { state ->
            val target = index + offset
            if (index !in state.photos.indices || target !in state.photos.indices) {
                state
            } else {
                val reordered = state.photos.toMutableList()
                val item = reordered.removeAt(index)
                reordered.add(target, item)
                state.copy(photos = reordered)
            }
        }
    }

    // ---------- Color preset / sliders ----------

    fun selectPreset(index: Int) {
        val maxIndex = ColorPreset.builtIn.size
        _uiState.update { it.copy(selectedPresetIndex = index.coerceIn(0, maxIndex)) }
    }

    fun setHueRange(minHue: Float, maxHue: Float) {
        updateCustom { preset ->
            preset.copy(
                hMin = minHue.coerceIn(0f, 360f),
                hMax = maxHue.coerceIn(0f, 360f),
            )
        }
    }

    fun setSaturationMin(value: Float) {
        updateCustom { it.copy(sMin = value.coerceIn(0f, 1f)) }
    }

    fun setValueMin(value: Float) {
        updateCustom { it.copy(vMin = value.coerceIn(0f, 1f)) }
    }

    /**
     * Editing any slider clones the currently shown values into the Custom preset and selects
     * it, so tweaking a built-in preset never mutates the built-in itself.
     */
    private fun updateCustom(transform: (ColorPreset) -> ColorPreset) {
        _uiState.update { state ->
            val base = state.activePreset
            val updated = transform(base.copy(name = ColorPreset.CUSTOM_NAME))
            state.copy(
                customPreset = updated,
                selectedPresetIndex = ColorPreset.builtIn.size,
            )
        }
    }

    fun setThreshold(value: Float) {
        _uiState.update { it.copy(threshold = value.coerceIn(0.2f, 0.9f)) }
    }

    fun setAddSeparators(value: Boolean) {
        _uiState.update { it.copy(addSeparators = value) }
    }

    fun setDebugPreview(value: Boolean) {
        _uiState.update { it.copy(debugPreview = value) }
    }

    fun updateResultText(value: String) {
        _uiState.update { it.copy(resultText = value, noHighlights = false) }
    }

    // ---------- Extraction ----------

    fun startExtraction() {
        if (extractionJob?.isActive == true) return
        val snapshot = _uiState.value
        if (snapshot.photos.isEmpty()) return

        val appContext = getApplication<Application>()
        val runId = ++runCounter
        currentRunId = runId

        extractionJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    progressDone = 0,
                    progressTotal = snapshot.photos.size,
                    resultText = "",
                    debugBitmap = null,
                    errorMessages = emptyList(),
                    noHighlights = false,
                    hasResult = false,
                    statusMessage = null,
                )
            }

            val preset = snapshot.activePreset
            val threshold = snapshot.threshold
            val separators = snapshot.addSeparators
            val wantDebug = snapshot.debugPreview

            val sections = mutableListOf<String>()
            val errors = mutableListOf<String>()
            var totalSegments = 0
            var debugPreviewBitmap: Bitmap? = null
            var finished = false

            try {
                for ((index, uri) in snapshot.photos.withIndex()) {
                    ensureActive()
                    _uiState.update { it.copy(progressDone = index + 1) }

                    var bitmap: Bitmap? = null
                    try {
                        val loaded = withContext(Dispatchers.Default) {
                            ImageLoader.loadUprightBitmap(appContext, uri)
                        }
                        bitmap = loaded

                        val result = withContext(Dispatchers.Default) {
                            HighlightExtractor.extract(
                                bitmap = loaded,
                                preset = preset,
                                threshold = threshold,
                                makeDebug = wantDebug && index == 0,
                            )
                        }
                        totalSegments += result.segments.size

                        if (wantDebug && index == 0) {
                            debugPreviewBitmap = result.debugBitmap
                        }

                        val section = buildSection(index, result.segments, separators)
                        if (section.isNotEmpty()) sections += section
                    } catch (e: CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        val reason = t.message ?: t.javaClass.simpleName
                        errors += "Photo ${index + 1} failed: $reason"
                    } finally {
                        bitmap?.recycle()
                    }
                }
                finished = true
            } finally {
                // A newer run may have started while this one was unwinding after a cancel.
                if (currentRunId == runId) {
                    val text = sections.joinToString(if (separators) "\n\n" else "\n")
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            resultText = text,
                            debugBitmap = debugPreviewBitmap,
                            errorMessages = errors,
                            noHighlights = totalSegments == 0 && errors.isEmpty(),
                            hasResult = true,
                            statusMessage = if (finished) it.statusMessage else "Extraction cancelled.",
                        )
                    }
                }
            }
        }
    }

    fun cancelExtraction() {
        extractionJob?.cancel()
    }

    // ---------- Saving ----------

    fun saveResultTo(uri: Uri) {
        val text = _uiState.value.resultText
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stream = getApplication<Application>().contentResolver
                        .openOutputStream(uri, "wt")
                        ?: throw IllegalStateException("Could not open the selected file")
                    stream.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                        it.flush()
                    }
                }
                _uiState.update { it.copy(statusMessage = "Saved as UTF-8 text.") }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                val reason = t.message ?: t.javaClass.simpleName
                _uiState.update { it.copy(statusMessage = "Could not save: $reason") }
            }
        }
    }

    override fun onCleared() {
        extractionJob?.cancel()
        super.onCleared()
    }

    private fun buildSection(index: Int, segments: List<String>, separators: Boolean): String {
        if (!separators) return segments.joinToString("\n")
        val header = "--- Photo ${index + 1} ---"
        return if (segments.isEmpty()) header else header + "\n" + segments.joinToString("\n")
    }
}
