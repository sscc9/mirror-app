package com.example.ui.mirror

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RingLightColor(val displayName: String, val hexColor: Long) {
    COOL_WHITE("冷白光", 0xFFF0F6FF),
    SOFT_WARM("柔和光", 0xFFFFF1E6),
    WARM_GOLD("暖金光", 0xFFFFD8A8)
}

data class MirrorUiState(
    val isCameraPermissionGranted: Boolean = false,
    val isControlsVisible: Boolean = false, // Default hidden as requested
    val zoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 8.0f,
    val screenBrightness: Float = 0.8f, // Default to 80% screen brightness
    val isRingLightOn: Boolean = false,
    val ringLightBrightness: Float = 0.6f,
    val ringLightColor: RingLightColor = RingLightColor.SOFT_WARM,
    val isMirrored: Boolean = true, // Default to true for standard mirror view (horizontal-reversed)
    val isGridOn: Boolean = false,
    val isFrozen: Boolean = false,
    val frozenBitmap: Bitmap? = null,
    val isSavingPhoto: Boolean = false,
    val message: String? = null
)

class MirrorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MirrorUiState())
    val uiState = _uiState.asStateFlow()

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isCameraPermissionGranted = granted) }
    }

    fun toggleControlsVisibility() {
        _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(isControlsVisible = visible) }
    }

    fun setZoomRatio(ratio: Float) {
        _uiState.update { it.copy(zoomRatio = ratio.coerceIn(1.0f, it.maxZoomRatio)) }
    }

    fun setMaxZoomRatio(maxRatio: Float) {
        _uiState.update { it.copy(maxZoomRatio = if (maxRatio > 1f) maxRatio else 8.0f) }
    }

    fun setScreenBrightness(brightness: Float) {
        _uiState.update { it.copy(screenBrightness = brightness.coerceIn(0.1f, 1.0f)) }
    }

    fun toggleRingLight() {
        _uiState.update { it.copy(isRingLightOn = !it.isRingLightOn) }
    }

    fun setRingLightBrightness(brightness: Float) {
        _uiState.update { it.copy(ringLightBrightness = brightness.coerceIn(0.1f, 1.0f)) }
    }

    fun setRingLightColor(color: RingLightColor) {
        _uiState.update { it.copy(ringLightColor = color) }
    }

    fun toggleMirrorMode() {
        _uiState.update { it.copy(isMirrored = !it.isMirrored) }
    }

    fun toggleGrid() {
        _uiState.update { it.copy(isGridOn = !it.isGridOn) }
    }

    fun freezeFrame(bitmap: Bitmap?) {
        _uiState.update { 
            it.copy(
                isFrozen = true, 
                frozenBitmap = bitmap,
                isControlsVisible = true
            ) 
        }
    }

    fun unfreezeFrame() {
        _uiState.update { it.copy(isFrozen = false, frozenBitmap = null) }
    }

    fun setSavingPhoto(saving: Boolean) {
        _uiState.update { it.copy(isSavingPhoto = saving) }
    }

    fun showMessage(msg: String?) {
        _uiState.update { it.copy(message = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
