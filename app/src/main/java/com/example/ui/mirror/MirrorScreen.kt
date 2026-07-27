package com.example.ui.mirror

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.WindowManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LiquidGlassBox(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Box(
            modifier = modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(cornerRadius) },
                    effects = {
                        vibrancy()
                        blur(6.dp.toPx())
                        lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true, chromaticAberration = true)
                    },
                    highlight = { Highlight.Default },
                    onDrawSurface = {
                        drawRect(Color.Black.copy(alpha = 0.05f))
                    }
                )
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier
                .shadow(16.dp, RoundedCornerShape(cornerRadius))
                .background(Color(0xD9121214), RoundedCornerShape(cornerRadius))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(cornerRadius))
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MirrorScreen(
    viewModel: MirrorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (cameraPermissionState.status.isGranted) {
                MirrorContent(viewModel = viewModel, uiState = uiState)
            } else {
                PermissionRequestScreen(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }

            uiState.message?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp, start = 24.dp, end = 24.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .background(Color(0xE61C1C1E), RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .testTag("app_snackbar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "确定",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearMessage() }
                        )
                    }
                }

                LaunchedEffect(msg) {
                    delay(3000)
                    viewModel.clearMessage()
                }
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("permission_screen"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Face,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.permission_required_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.permission_required_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
                .testTag("grant_permission_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.VpnKey,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.grant_permission),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MirrorContent(
    viewModel: MirrorViewModel,
    uiState: MirrorUiState
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // 1. CRITICAL TECHNICAL POINT: CameraX PreviewView COMPATIBLE Mode
    // Uses TextureView so frames enter the standard composition pipeline for LiquidGlass AGSL sampling
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    // Screen Brightness controller
    LaunchedEffect(uiState.screenBrightness) {
        val activity = context as? Activity
        if (activity != null) {
            val lp = activity.window.attributes
            lp.screenBrightness = uiState.screenBrightness.coerceIn(0.1f, 1.0f)
            activity.window.attributes = lp
        }
    }

    // Apply Zoom to hardware camera controller
    LaunchedEffect(uiState.zoomRatio, camera) {
        camera?.cameraControl?.setZoomRatio(uiState.zoomRatio)
    }

    // Initialize CameraX
    LaunchedEffect(Unit) {
        val cameraProviderProvider = ProcessCameraProvider.getInstance(context)
        val cameraProvider = withContext(Dispatchers.IO) {
            cameraProviderProvider.get()
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            camera?.cameraInfo?.zoomState?.value?.maxZoomRatio?.let { maxZoom ->
                viewModel.setMaxZoomRatio(maxZoom.coerceAtMost(5.0f))
            }
        } catch (exc: Exception) {
            viewModel.showMessage("相机初始化失败: ${exc.localizedMessage}")
        }
    }

    // Gesture and Panel States
    var lastPanelInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val backdrop = rememberLayerBackdrop()

    // 改动1: 左侧 1/4 亮度滑动 HUD State
    var isBrightnessSliding by remember { mutableStateOf(false) }
    var lastBrightnessSlideTime by remember { mutableLongStateOf(0L) }
    var showBrightnessHUD by remember { mutableStateOf(false) }

    // 改动2: 右侧 1/4 缩放滑动 HUD State
    var isZoomSliding by remember { mutableStateOf(false) }
    var lastZoomSlideTime by remember { mutableLongStateOf(0L) }
    var showZoomHUD by remember { mutableStateOf(false) }

    var activeDragRegion by remember { mutableStateOf<String?>(null) } // "left" or "right"

    // 改动3: 5秒无操作自动隐藏底部功能面板
    LaunchedEffect(uiState.isControlsVisible, lastPanelInteractionTime) {
        if (uiState.isControlsVisible) {
            delay(5000)
            viewModel.setControlsVisible(false)
        }
    }

    // 改动1: 800ms 自动淡出左侧亮度 HUD
    LaunchedEffect(isBrightnessSliding, lastBrightnessSlideTime) {
        if (isBrightnessSliding) {
            showBrightnessHUD = true
        } else if (showBrightnessHUD) {
            delay(800)
            showBrightnessHUD = false
        }
    }

    // 改动2: 800ms 自动淡出右侧缩放 HUD
    LaunchedEffect(isZoomSliding, lastZoomSlideTime) {
        if (isZoomSliding) {
            showZoomHUD = true
        } else if (showZoomHUD) {
            delay(800)
            showZoomHUD = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Single tap on central 2/4 area toggles bottom panel
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val w = size.width.toFloat()
                        if (offset.x >= w * 0.25f && offset.x <= w * 0.75f) {
                            viewModel.toggleControlsVisibility()
                            lastPanelInteractionTime = System.currentTimeMillis()
                        }
                    }
                )
            }
            // Vertical Drag Gesture Handling (Left 1/4: Brightness, Right 1/4: Zoom)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val edgePadding = 24.dp.toPx()
                        if (offset.x in edgePadding..(w * 0.25f)) {
                            activeDragRegion = "left"
                            isBrightnessSliding = true
                            lastBrightnessSlideTime = System.currentTimeMillis()
                        } else if (offset.x in (w * 0.75f)..(w - edgePadding)) {
                            activeDragRegion = "right"
                            isZoomSliding = true
                            lastZoomSlideTime = System.currentTimeMillis()
                        } else {
                            activeDragRegion = null
                        }
                    },
                    onDragEnd = {
                        if (activeDragRegion == "left") {
                            isBrightnessSliding = false
                            lastBrightnessSlideTime = System.currentTimeMillis()
                        } else if (activeDragRegion == "right") {
                            isZoomSliding = false
                            lastZoomSlideTime = System.currentTimeMillis()
                        }
                        activeDragRegion = null
                    },
                    onDragCancel = {
                        if (activeDragRegion == "left") {
                            isBrightnessSliding = false
                            lastBrightnessSlideTime = System.currentTimeMillis()
                        } else if (activeDragRegion == "right") {
                            isZoomSliding = false
                            lastZoomSlideTime = System.currentTimeMillis()
                        }
                        activeDragRegion = null
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (activeDragRegion == "left") {
                            isBrightnessSliding = true
                            lastBrightnessSlideTime = System.currentTimeMillis()
                            viewModel.adjustScreenBrightness(-dragAmount * 0.0035f)
                        } else if (activeDragRegion == "right") {
                            isZoomSliding = true
                            lastZoomSlideTime = System.currentTimeMillis()
                            viewModel.adjustZoom(-dragAmount * 0.005f)
                        }
                    }
                )
            }
            .testTag("preview_container")
    ) {
        // 1. Camera Mirror Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .graphicsLayer {
                    scaleX = if (uiState.isMirrored) 1f else -1f
                }
                .testTag("camera_preview")
        )

        // 2. Grid Overlay
        if (uiState.isGridOn) {
            CameraGridOverlay()
        }

        // 3. Frozen Frame Still Overlay
        AnimatedVisibility(
            visible = uiState.isFrozen && uiState.frozenBitmap != null,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            uiState.frozenBitmap?.let { bitmap ->
                Box(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Frozen Look",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 28.dp)
                            .shadow(4.dp, RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "画面已定格",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 改动1 HUD: 左侧 1/4 滑动屏幕亮度 HUD (中央偏左)
        AnimatedVisibility(
            visible = showBrightnessHUD,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        ) {
            VerticalSliderHUD(
                backdrop = backdrop,
                icon = Icons.Rounded.WbSunny,
                progress = uiState.screenBrightness,
                text = "${(uiState.screenBrightness * 100).toInt()}%"
            )
        }

        // 改动2 HUD: 右侧 1/4 滑动缩放倍率 HUD (中央偏右)
        AnimatedVisibility(
            visible = showZoomHUD,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        ) {
            VerticalSliderHUD(
                backdrop = backdrop,
                icon = Icons.Rounded.ZoomIn,
                progress = (uiState.zoomRatio - 1.0f) / (uiState.maxZoomRatio - 1.0f),
                text = String.format(java.util.Locale.US, "%.1fx", uiState.zoomRatio)
            )
        }

        // Top Guide Tip
        var showInstructions by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(3500)
            showInstructions = false
        }
        AnimatedVisibility(
            visible = showInstructions && !uiState.isControlsVisible && !uiState.isFrozen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
        ) {
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "点击中央显示面板 | 左侧滑动亮度 / 右侧滑动变焦",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 改动3: 底部 Liquid Glass 界面面板 (默认隐藏，点击中央滑入，5秒自动淡出隐藏)
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PRIMARY LIQUID GLASS BOTTOM PANEL
                LiquidGlassBox(
                    backdrop = backdrop,
                    cornerRadius = 32.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("controls_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 网格开关
                        MirrorIconButton(
                            icon = Icons.Rounded.Grid3x3,
                            contentDescription = stringResource(R.string.control_grid),
                            isSelected = uiState.isGridOn,
                            onClick = {
                                viewModel.toggleGrid()
                                lastPanelInteractionTime = System.currentTimeMillis()
                            },
                            label = "网格"
                        )

                        // 3. 中央快门 / 定格按钮
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(6.dp, CircleShape)
                                    .background(
                                        color = if (uiState.isFrozen) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        lastPanelInteractionTime = System.currentTimeMillis()
                                        if (uiState.isFrozen) {
                                            viewModel.unfreezeFrame()
                                        } else {
                                            val currentMirrorMode = uiState.isMirrored
                                            imageCapture.takePicture(
                                                mainExecutor,
                                                object : ImageCapture.OnImageCapturedCallback() {
                                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                                        try {
                                                            val rotation = imageProxy.imageInfo.rotationDegrees
                                                            val sourceBitmap = imageProxy.toBitmap()
                                                            val matrix = android.graphics.Matrix()
                                                            if (rotation != 0) {
                                                                matrix.postRotate(rotation.toFloat())
                                                            }
                                                            if (currentMirrorMode) {
                                                                matrix.postScale(-1f, 1f)
                                                            }
                                                            
                                                            val transformedBitmap = Bitmap.createBitmap(
                                                                sourceBitmap,
                                                                0, 0,
                                                                sourceBitmap.width,
                                                                sourceBitmap.height,
                                                                matrix,
                                                                true
                                                            )
                                                            viewModel.freezeFrame(transformedBitmap)
                                                        } catch (e: Exception) {
                                                            viewModel.showMessage("获取图像失败: ${e.localizedMessage}")
                                                        } finally {
                                                            imageProxy.close()
                                                        }
                                                    }

                                                    override fun onError(exception: ImageCaptureException) {
                                                        viewModel.showMessage("定格失败: ${exception.localizedMessage}")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    .testTag("shutter_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFrozen) Icons.Rounded.PlayArrow else Icons.Rounded.Camera,
                                    contentDescription = if (uiState.isFrozen) "恢复" else "定格",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.isFrozen) "恢复" else "定格",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // 4. 镜像 / 真实
                        MirrorIconButton(
                            icon = Icons.Rounded.Compare,
                            contentDescription = stringResource(R.string.control_flip),
                            isSelected = !uiState.isMirrored,
                            onClick = {
                                viewModel.toggleMirrorMode()
                                lastPanelInteractionTime = System.currentTimeMillis()
                            },
                            label = if (uiState.isMirrored) "镜像" else "真实"
                        )

                        // 5. 保存
                        MirrorIconButton(
                            icon = Icons.Rounded.SaveAlt,
                            contentDescription = "保存照片",
                            isSelected = uiState.isSavingPhoto,
                            enabled = uiState.isFrozen && uiState.frozenBitmap != null,
                            onClick = {
                                lastPanelInteractionTime = System.currentTimeMillis()
                                uiState.frozenBitmap?.let { bitmap ->
                                    viewModel.setSavingPhoto(true)
                                    viewModel.showMessage("正在保存照片...")
                                    coroutineScope.launch {
                                        saveBitmapToGallery(context, bitmap) { _, msg ->
                                            viewModel.setSavingPhoto(false)
                                            viewModel.showMessage(msg)
                                        }
                                    }
                                }
                            },
                            label = "保存"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified Vertical Slider HUD for Brightness and Zoom
 */
@Composable
fun VerticalSliderHUD(
    backdrop: Backdrop,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    progress: Float,
    text: String
) {
    LiquidGlassBox(
        backdrop = backdrop,
        cornerRadius = 20.dp,
        modifier = Modifier
            .width(52.dp)
            .height(170.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            
            // Vertical progress bar capsule
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .weight(1f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(progress.coerceIn(0f, 1f))
                        .align(Alignment.BottomCenter)
                        .background(Color.White, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun MirrorIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .graphicsLayer { alpha = if (enabled) 1.0f else 0.35f }
            .testTag("action_${label.lowercase()}")
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                    shape = CircleShape
                )
                .border(
                    width = if (isSelected) 1.dp else 0.dp,
                    color = if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun CameraGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val h1 = size.height / 3f
        val h2 = size.height * 2f / 3f
        val w1 = size.width / 3f
        val w2 = size.width * 2f / 3f

        val lineColor = Color.White.copy(alpha = 0.20f)
        val strokePx = 1.dp.toPx()

        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(0f, h1),
            end = androidx.compose.ui.geometry.Offset(size.width, h1),
            strokeWidth = strokePx
        )
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(0f, h2),
            end = androidx.compose.ui.geometry.Offset(size.width, h2),
            strokeWidth = strokePx
        )

        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(w1, 0f),
            end = androidx.compose.ui.geometry.Offset(w1, size.height),
            strokeWidth = strokePx
        )
        drawLine(
            color = lineColor,
            start = androidx.compose.ui.geometry.Offset(w2, 0f),
            end = androidx.compose.ui.geometry.Offset(w2, size.height),
            strokeWidth = strokePx
        )
    }
}

fun saveBitmapToGallery(
    context: Context,
    bitmap: Bitmap,
    onResult: (Boolean, String) -> Unit
) {
    try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Mirror_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Mirror")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri == null) {
            onResult(false, "无法向媒体库插入新记录")
            return
        }

        resolver.openOutputStream(imageUri).use { outStream ->
            if (outStream == null) {
                onResult(false, "无法写入画卷流")
                return
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }

        onResult(true, "照片已成功保存至手机相册!")
    } catch (e: Exception) {
        onResult(false, "保存失败: ${e.localizedMessage}")
    }
}
