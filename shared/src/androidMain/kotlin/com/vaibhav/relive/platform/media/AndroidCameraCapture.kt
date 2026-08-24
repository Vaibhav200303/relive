package com.vaibhav.relive.platform.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vaibhav.relive.domain.model.MediaType
import com.vaibhav.relive.ui.icons.CameraIcons
import com.vaibhav.relive.ui.feedback.ReliveHapticCue
import com.vaibhav.relive.ui.feedback.rememberReliveHaptics
import java.io.File
import java.util.concurrent.Executor

/**
 * WhatsApp-style minimal camera. Full-screen preview under a compact black
 * control bar. The shutter is the fixed geometric center of the screen;
 * gallery lives to its left, switch-camera to its right, and zoom
 * presets sit directly above on the same center axis. Photo/Video mode
 * selector sits below and above the system navigation area. Flash is a
 * small secondary control in the upper-left of the preview.
 *
 * See ADR-0018 addendum for the flash/torch policy, camera-switch behavior,
 * zoom policy, and platform-native capture-feedback sources on Android and
 * iOS.
 */
@Composable
actual fun CameraCaptureSurface(
    mediaStore: MediaStore,
    onCaptured: (RawMedia) -> Unit,
    onCancel: () -> Unit,
    onOpenGallery: () -> Unit,
) {
    val store = mediaStore as? AndroidMediaStore
        ?: error("Android CameraCaptureSurface requires AndroidMediaStore")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }

    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission.value = granted; if (!granted) onCancel() }
    LaunchedEffect(Unit) {
        if (!hasPermission.value) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission.value) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            BackHandler(enabled = true, onBack = onCancel)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission needed", color = Color.White)
            }
        }
        return
    }

    var mode by remember { mutableStateOf(CameraMode.Photo) }
    var lens by remember { mutableStateOf(LensFacing.Back) }
    var flash by remember { mutableStateOf(FlashMode.Off) }
    var hasFrontCamera by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    var zoomMin by remember { mutableStateOf(1f) }
    var zoomMax by remember { mutableStateOf(1f) }
    var zoomRatio by remember { mutableStateOf(1f) }
    // Ruler-vs-presets: true while a pinch is in progress or during the
    // brief post-pinch linger. Timing lives here in the composable; the
    // dynamic-label logic itself is deterministic and tested in common.
    var isPinching by remember { mutableStateOf(false) }
    var rulerVisible by remember { mutableStateOf(false) }
    var pinchTick by remember { mutableStateOf(0) }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    val recorder = remember { Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }
    var boundCamera: Camera? by remember { mutableStateOf(null) }
    // Track the in-flight temp file so cancel/back can delete it if no
    // capture ever completed.
    var pendingTempFile: File? by remember { mutableStateOf(null) }
    // Photo review state — a just-captured photo the user has not yet
    // accepted. While non-Live, the shutter row swaps for retake/confirm and
    // the live preview is hidden behind the frozen capture. The captured
    // temp file is owned by [uiState] here — it becomes an attachment only
    // when the user taps ✓ (confirm).
    var uiState: CameraUiState by remember { mutableStateOf<CameraUiState>(CameraUiState.Live) }
    val isReviewing = uiState.isReviewing
    // Video playback UI state for VideoReview. Kept next to uiState so it
    // resets whenever we return to Live.
    var videoPlayback by remember { mutableStateOf(VideoReviewPlayback.Initial) }
    // Editor state (trim + mute + duration + cursor). Reset alongside
    // videoPlayback on Retake/Confirm so a fresh review starts untrimmed and
    // unmuted regardless of the previous session.
    var videoEdit by remember { mutableStateOf(VideoReviewEditState.initial(0L)) }

    // Native camera feedback via MediaActionSound. Respects region-enforced
    // shutter policy (Japan/Korea) and uses the system-authored shutter and
    // video-recording sounds instead of a synthetic tone. Load ahead of first
    // use so playback latency at shutter press is negligible.
    val mediaSound = remember {
        MediaActionSound().apply {
            try {
                load(MediaActionSound.SHUTTER_CLICK)
                load(MediaActionSound.START_VIDEO_RECORDING)
                load(MediaActionSound.STOP_VIDEO_RECORDING)
            } catch (_: Throwable) { /* best-effort preload */ }
        }
    }
    val vibrator = remember { getVibrator(context) }
    val feedbackScope = rememberCoroutineScope()
    // Frozen-preview snapshot captured from the live PreviewView at the
    // instant the shutter is pressed. Used as the immediate visual backing
    // for PhotoReview while the JPEG decodes/normalizes asynchronously.
    var frozenPreview: ImageBitmap? by remember { mutableStateOf(null) }
    // Post-processing (EXIF normalize) job for the current photo review. Null
    // when no processing in flight. Confirm waits on this before handing the
    // RawMedia off; Retake cancels it.
    var photoProcessingJob: Job? by remember { mutableStateOf(null) }
    var photoPendingConfirm by remember { mutableStateOf(false) }
    // Short white flash overlay tied to shutter press. Non-blocking, purely
    // visual capture-feedback that resembles the native shutter flash.
    var captureFlashTick by remember { mutableStateOf(0) }
    var captureFlashAlpha by remember { mutableStateOf(0f) }

    fun discardPendingTempFile() {
        pendingTempFile?.delete()
        pendingTempFile = null
    }

    // Delete the frozen review capture (if any). Called on retake, back, and
    // dispose so repeated retakes never leave orphan files (photo or video)
    // on disk.
    fun discardReviewFile() {
        when (val current = uiState) {
            is CameraUiState.PhotoReview ->
                try { File(current.captured.sourcePath).delete() } catch (_: Throwable) {}
            is CameraUiState.VideoReview ->
                try { File(current.captured.sourcePath).delete() } catch (_: Throwable) {}
            CameraUiState.Live -> Unit
        }
    }

    fun releaseCamera() {
        boundCamera?.cameraControl?.enableTorch(false)
        boundCamera = null
        try { ProcessCameraProvider.getInstance(context).get().unbindAll() } catch (_: Throwable) {}
    }

    val cancel: () -> Unit = {
        activeRecording?.stop()
        activeRecording = null
        isRecording = false
        photoProcessingJob?.cancel()
        photoProcessingJob = null
        photoPendingConfirm = false
        frozenPreview = null
        discardPendingTempFile()
        discardReviewFile()
        uiState = CameraUiState.Live
        releaseCamera()
        onCancel()
    }

    // Retake: discard the captured file and drop back to Live. Preview is
    // still bound behind the review overlay, so removing the overlay
    // restores the live feed without a full CameraX re-bind. Video playback
    // state is also reset; the VideoView is disposed by leaving VideoReview.
    val retake: () -> Unit = {
        photoProcessingJob?.cancel()
        photoProcessingJob = null
        photoPendingConfirm = false
        frozenPreview = null
        discardReviewFile()
        videoPlayback = VideoReviewPlayback.Initial
        videoEdit = VideoReviewEditState.initial(0L)
        uiState = CameraUiState.Live
    }

    // Confirm: hand the captured photo or video to the caller. The file is
    // now the callee's responsibility (composer + media processor), so we
    // clear review state without deleting.
    val confirm: () -> Unit = {
        when (val current = uiState) {
            is CameraUiState.PhotoReview -> {
                val job = photoProcessingJob
                if (job != null && job.isActive) {
                    // Processing still running: mark confirm pending. The
                    // LaunchedEffect below will fire onCaptured once the
                    // normalize job completes so the persisted file is
                    // correctly oriented.
                    photoPendingConfirm = true
                } else {
                    photoProcessingJob = null
                    frozenPreview = null
                    uiState = CameraUiState.Live
                    onCaptured(current.captured)
                }
            }
            is CameraUiState.VideoReview -> {
                val edit = videoEdit
                val out = current.captured.copy(
                    trimStartMs = if (edit.isTrimmed) edit.trimStartMs else null,
                    trimEndMs = if (edit.isTrimmed) edit.trimEndMs else null,
                    muteAudio = edit.isMuted,
                )
                videoPlayback = VideoReviewPlayback.Initial
                videoEdit = VideoReviewEditState.initial(0L)
                uiState = CameraUiState.Live
                onCaptured(out)
            }
            CameraUiState.Live -> Unit
        }
    }

    // Bind camera use cases whenever mode or lens changes. Torch and zoom are
    // reapplied after binding so they reflect the *current* lens's real
    // capabilities and reported ranges.
    LaunchedEffect(mode, lens) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            provider.unbindAll()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val selector = when (lens) {
                LensFacing.Back -> CameraSelector.DEFAULT_BACK_CAMERA
                LensFacing.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
            }
            hasFrontCamera = try { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) } catch (_: Throwable) { false }
            imageCapture.flashMode = flash.toImageCaptureFlash()
            val cam = when (mode) {
                CameraMode.Photo -> provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                CameraMode.Video -> provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
            }
            boundCamera = cam
            hasFlashUnit = cam.cameraInfo.hasFlashUnit()
            val coerced = flash.coercedFor(hasFlashUnit)
            if (coerced != flash) flash = coerced
            imageCapture.flashMode = coerced.toImageCaptureFlash()
            if (mode == CameraMode.Video && hasFlashUnit) {
                cam.cameraControl.enableTorch(coerced == FlashMode.On)
            } else {
                cam.cameraControl.enableTorch(false)
            }
            // Read the newly bound lens's actual zoom range and reconcile any
            // in-flight ratio. When the ratio is still supported it survives
            // Photo↔Video / Back↔Back rebinds; when it isn't (typical when
            // toggling to the fixed-zoom front lens), fall back to the lens
            // default.
            val zs = cam.cameraInfo.zoomState.value
            val minR = zs?.minZoomRatio ?: 1f
            val maxR = zs?.maxZoomRatio ?: 1f
            zoomMin = minR
            zoomMax = maxR
            val nextRatio = if (zoomRatio in minR..maxR) zoomRatio else defaultZoomRatio(minR, maxR)
            zoomRatio = nextRatio
            try { cam.cameraControl.setZoomRatio(nextRatio) } catch (_: Throwable) { /* best-effort */ }
        }, executor)
    }
    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            photoProcessingJob?.cancel()
            discardPendingTempFile()
            discardReviewFile()
            releaseCamera()
            try { mediaSound.release() } catch (_: Throwable) {}
        }
    }

    // Await pending confirm: user tapped ✓ while EXIF normalization was
    // still running. Join the job (or finish immediately if already done),
    // then hand the captured photo off. Retake/cancel cancels the job and
    // clears photoPendingConfirm, so this effect no-ops in those paths.
    LaunchedEffect(photoProcessingJob, photoPendingConfirm) {
        val job = photoProcessingJob
        if (!photoPendingConfirm || job == null) return@LaunchedEffect
        try { job.join() } catch (_: Throwable) {}
        val current = uiState
        if (photoPendingConfirm && current is CameraUiState.PhotoReview) {
            photoProcessingJob = null
            photoPendingConfirm = false
            frozenPreview = null
            uiState = CameraUiState.Live
            onCaptured(current.captured)
        } else {
            photoPendingConfirm = false
        }
    }

    // Short shutter flash overlay. Fades from ~0.55 to 0 over ~180 ms so it
    // reads as a native capture flash rather than a full-screen loading state.
    LaunchedEffect(captureFlashTick) {
        if (captureFlashTick == 0) return@LaunchedEffect
        captureFlashAlpha = 0.55f
        val steps = 6
        repeat(steps) {
            delay(30)
            captureFlashAlpha *= 0.55f
        }
        captureFlashAlpha = 0f
    }

    // Elapsed timer runs only while recording; reset on stop.
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingElapsedMs = 0L
            val start = System.currentTimeMillis()
            while (isRecording) {
                recordingElapsedMs = System.currentTimeMillis() - start
                delay(200)
            }
        } else {
            recordingElapsedMs = 0L
        }
    }

    // System Back during PhotoReview discards the capture and returns to
    // live; otherwise it closes the camera and returns to the composer.
    BackHandler(enabled = true, onBack = { if (isReviewing) retake() else cancel() })

    // rememberUpdatedState so preview gestures always see current lens state.
    val onPreviewDoubleTap by rememberUpdatedState({
        if (!isRecording && hasFrontCamera) lens = lens.toggled()
    })
    val currentZoomForPinch by rememberUpdatedState(zoomRatio)
    val onPreviewPinch by rememberUpdatedState({ scaleFactor: Float ->
        val next = clampZoomRatio(currentZoomForPinch * scaleFactor, zoomMin, zoomMax)
        if (next != currentZoomForPinch) {
            zoomRatio = next
            try { boundCamera?.cameraControl?.setZoomRatio(next) } catch (_: Throwable) {}
        }
    })
    val onPinchStart by rememberUpdatedState({
        isPinching = true
        rulerVisible = true
    })
    val onPinchEnd by rememberUpdatedState({
        isPinching = false
        pinchTick += 1
    })

    // After pinch ends, keep ruler on screen briefly then swap back to
    // presets. Restarted on every new pinch via pinchTick.
    LaunchedEffect(pinchTick) {
        if (pinchTick == 0) return@LaunchedEffect
        delay(RULER_LINGER_MS)
        if (!isPinching) rulerVisible = false
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Top black band. Compact strip above the preview holding secondary
        // controls (flash). Sits above status-bar/cutout via statusBars
        // inset so nothing overlaps the notch, and the visible content row
        // stays around 48dp regardless of inset height.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.statusBars)
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isReviewing) FlashButton(
                flash = flash,
                enabled = hasFlashUnit,
                onClick = {
                    val next = when (mode) {
                        CameraMode.Photo -> flash.nextPhoto()
                        CameraMode.Video -> flash.nextVideo()
                    }
                    flash = next
                    imageCapture.flashMode = next.toImageCaptureFlash()
                    if (mode == CameraMode.Video) {
                        boundCamera?.cameraControl?.enableTorch(next == FlashMode.On)
                    }
                },
            )
        }

        // Preview area — dominant middle band between the two black strips.
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Live CameraX preview stays in the tree even during review so
            // the binding is preserved; retake just drops the overlay.
            // Preview-only gestures (double-tap switch, pinch-zoom) are
            // disabled while reviewing.
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isReviewing) {
                        if (isReviewing) return@pointerInput
                        detectTapGestures(onDoubleTap = { onPreviewDoubleTap() })
                    }
                    .pointerInput(isReviewing) {
                        if (isReviewing) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            var started = false
                            do {
                                val event = awaitPointerEvent()
                                val zoom = event.calculateZoom()
                                if (zoom != 1f) {
                                    if (!started) {
                                        started = true
                                        onPinchStart()
                                    }
                                    onPreviewPinch(zoom)
                                }
                            } while (event.changes.any { it.pressed })
                            if (started) onPinchEnd()
                        }
                    },
            )

            // Photo review overlay: frozen full-size capture inside the same
            // preview band. Black backdrop covers the preview edges so
            // letterboxed portrait/landscape captures don't leak the live
            // feed behind them.
            val reviewState = uiState as? CameraUiState.PhotoReview
            if (reviewState != null) {
                // Decoded/normalized JPEG. Loaded asynchronously so review
                // opens without blocking the main thread on a full-size
                // BitmapFactory.decodeFile. Reloads once processing (EXIF
                // normalize) finishes so orientation is correct.
                var decoded: ImageBitmap? by remember(reviewState.captured.sourcePath) {
                    mutableStateOf(null)
                }
                LaunchedEffect(reviewState.captured.sourcePath, photoProcessingJob) {
                    val path = reviewState.captured.sourcePath
                    // Wait for normalize to bake EXIF orientation into pixels
                    // before decoding. Decoding beforehand via
                    // BitmapFactory.decodeFile ignores EXIF and shows sensor
                    // orientation, causing a visible rotation snap when the
                    // normalized image finally lands. frozenPreview covers
                    // the wait so the review is never empty.
                    val job = photoProcessingJob
                    if (job != null) {
                        try { job.join() } catch (_: Throwable) {}
                    }
                    val full = withContext(Dispatchers.IO) {
                        decodeDownsampled(path, PHOTO_REVIEW_MAX_DIM)
                    }
                    if (full != null) decoded = full.asImageBitmap()
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    val shown: ImageBitmap? = decoded ?: frozenPreview
                    if (shown != null) {
                        Image(
                            bitmap = shown,
                            contentDescription = "Captured photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

            // Shutter flash overlay (non-blocking; sits above the preview and
            // below the review overlay so the flash appears at press then
            // fades under the frozen capture).
            if (captureFlashAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = captureFlashAlpha)),
                )
            }

            // Video review overlay: recorded clip in the same preview band
            // with centered Play/Pause overlay controls. Uses the framework
            // VideoView so we don't pull in ExoPlayer, and so recorded
            // orientation metadata is respected without a hardcoded rotation.
            val videoReviewState = uiState as? CameraUiState.VideoReview
            if (videoReviewState != null) {
                VideoReviewEditor(
                    filePath = videoReviewState.captured.sourcePath,
                    edit = videoEdit,
                    playback = videoPlayback,
                    onEditChange = { videoEdit = it },
                    onSingleTap = { videoPlayback = videoPlayback.singleTap() },
                    onTapPlay = {
                        // Snap the cursor to trimStart when needed *before*
                        // signalling isPlaying, so the editor's playback loop
                        // seeks to the correct spot on the first tick.
                        videoEdit = videoEdit.pressPlay()
                        videoPlayback = videoPlayback.tapPlay()
                    },
                    onTapPause = {
                        videoEdit = videoEdit.pressPause()
                        videoPlayback = videoPlayback.tapPause()
                    },
                    onOverlayTimeout = { videoPlayback = videoPlayback.pauseOverlayTimeout() },
                    onPlaybackReachedTrimEnd = {
                        videoEdit = videoEdit.reachedTrimEnd()
                        videoPlayback = videoPlayback.playbackEnded()
                    },
                )
            }

            // Recording timer floats over the preview, just below the top
            // black band. No status-bar inset needed — the band absorbs it.
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xB3000000))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(formatElapsed(recordingElapsedMs), color = Color.White)
                }
            }
        }

        // Bottom black control bar. Absorbs system navigation via nav-bar
        // inset so gestures don't clip the shutter row.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Zoom control slot — same fixed area holds either the quick
            // preset row (normal) or the live ruler (during pinch). Fixed
            // height keeps the shutter row from shifting when swapping.
            val slots = zoomSlotsFor(zoomMin, zoomMax)
            if (!isReviewing && (slots.size > 1 || zoomMax > zoomMin + 0.01f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ZOOM_SLOT_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    if (rulerVisible) {
                        ZoomRulerRow(
                            ratio = zoomRatio,
                            minRatio = zoomMin,
                            maxRatio = zoomMax,
                        )
                    } else if (slots.size > 1) {
                        ZoomPresetSlotRow(
                            slots = slots,
                            ratio = zoomRatio,
                            onSelect = { spec ->
                                val target = clampZoomRatio(spec.anchorRatio, zoomMin, zoomMax)
                                zoomRatio = target
                                try { boundCamera?.cameraControl?.setZoomRatio(target) } catch (_: Throwable) {}
                            },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Main control row: shutter is the geometric center of the
            // screen — enforced by a full-width Box with side controls
            // pinned to CenterStart / CenterEnd. Using SpaceBetween on a Row
            // would let the shutter drift whenever left/right widths differ.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!isReviewing) {
                    GalleryButton(onClick = onOpenGallery)
                }
                if (isReviewing) {
                    RetakeButton(onClick = retake)
                } else ShutterButton(
                    mode = mode,
                    isRecording = isRecording,
                    onClick = {
                        when (mode) {
                            CameraMode.Photo -> if (!isCapturingPhoto) {
                                isCapturingPhoto = true
                                // Native shutter feedback fires at press, not
                                // after the JPEG lands — matches WhatsApp /
                                // system-camera latency.
                                playShutter(mediaSound)
                                hapticTick(vibrator)
                                captureFlashTick += 1
                                // Snapshot the current preview frame as an
                                // instant visual backing for PhotoReview.
                                // Best-effort — a null snapshot falls back to
                                // the decoded JPEG when it lands.
                                frozenPreview = try {
                                    val bmp = previewView.bitmap
                                    when {
                                        bmp == null -> null
                                        // PreviewView shows the front camera
                                        // mirrored (selfie view); CameraX
                                        // saves the JPEG un-mirrored. Flip
                                        // the frozen snapshot to match the
                                        // decoded file — otherwise the
                                        // decoded image replacing frozen
                                        // reads as a horizontal flip.
                                        lens == LensFacing.Front -> {
                                            val m = Matrix().apply { postScale(-1f, 1f) }
                                            Bitmap.createBitmap(
                                                bmp, 0, 0, bmp.width, bmp.height, m, true,
                                            ).asImageBitmap()
                                        }
                                        else -> bmp.asImageBitmap()
                                    }
                                } catch (_: Throwable) { null }
                                takePhoto(
                                    store = store,
                                    imageCapture = imageCapture,
                                    executor = executor,
                                    scope = feedbackScope,
                                    onPending = { pendingTempFile = it },
                                    onCaptured = { raw, job ->
                                        // Enter PhotoReview immediately with
                                        // the raw file. Normalize runs in
                                        // [job]; Confirm awaits it before
                                        // handing the file to the caller.
                                        pendingTempFile = null
                                        isCapturingPhoto = false
                                        photoProcessingJob = job
                                        photoPendingConfirm = false
                                        uiState = enterPhotoReview(raw)
                                    },
                                    onError = { isCapturingPhoto = false },
                                )
                            }
                            CameraMode.Video -> {
                                if (!isRecording) {
                                    activeRecording = startRecording(
                                        context = context,
                                        store = store,
                                        videoCapture = videoCapture,
                                        executor = executor,
                                        onPending = { pendingTempFile = it },
                                        onStarted = {
                                            // Fire native start feedback only
                                            // when CameraX confirms recording
                                            // actually began.
                                            playStartVideo(mediaSound)
                                            hapticTick(vibrator)
                                        },
                                        onFinal = { raw ->
                                            pendingTempFile = null
                                            activeRecording = null
                                            isRecording = false
                                            boundCamera?.cameraControl?.enableTorch(false)
                                            playStopVideo(mediaSound)
                                            hapticTick(vibrator)
                                            videoPlayback = VideoReviewPlayback.Initial
                                            uiState = enterVideoReview(raw)
                                        },
                                        onError = {
                                            pendingTempFile = null
                                            activeRecording = null
                                            isRecording = false
                                            boundCamera?.cameraControl?.enableTorch(false)
                                        },
                                    )
                                    isRecording = true
                                } else {
                                    activeRecording?.stop()
                                }
                            }
                        }
                    },
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        isReviewing -> ConfirmButton(onClick = confirm)
                        hasFrontCamera -> SwitchButton(
                            enabled = !isRecording,
                            onClick = { lens = lens.toggled() },
                        )
                        else -> Spacer(Modifier.size(48.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Photo / Video selector — hidden during review; the mode is
            // implicitly Photo and the user should only see retake/confirm.
            if (!isReviewing) ModeSelector(
                mode = mode,
                enabled = !isRecording,
                onSelect = { mode = it },
            )
        }
    }
}

// Height of the zoom-control slot (presets or ruler). Sized to comfortably
// fit both variants so swapping does not shift the shutter row underneath.
private val ZOOM_SLOT_HEIGHT: Dp = 48.dp

// Post-pinch linger on the ruler before it hides and presets return.
// Matches the Pixel-camera feel (~700–900 ms).
private const val RULER_LINGER_MS: Long = 850L

// Max dimension for the review-overlay decode. Comfortably above any
// realistic device viewport, keeps the decode cheap versus the full sensor
// resolution (~4000 px on flagship handsets).
private const val PHOTO_REVIEW_MAX_DIM = 2048

private fun playShutter(sound: MediaActionSound) {
    try { sound.play(MediaActionSound.SHUTTER_CLICK) } catch (_: Throwable) {}
}

private fun playStartVideo(sound: MediaActionSound) {
    try { sound.play(MediaActionSound.START_VIDEO_RECORDING) } catch (_: Throwable) {}
}

private fun playStopVideo(sound: MediaActionSound) {
    try { sound.play(MediaActionSound.STOP_VIDEO_RECORDING) } catch (_: Throwable) {}
}

private fun FlashMode.toImageCaptureFlash(): Int = when (this) {
    FlashMode.Off -> ImageCapture.FLASH_MODE_OFF
    FlashMode.On -> ImageCapture.FLASH_MODE_ON
}

@Suppress("DEPRECATION")
private fun getVibrator(context: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

private fun hapticTick(vibrator: Vibrator?) {
    val v = vibrator ?: return
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Throwable) { /* haptic is best-effort */ }
}

// Pill segmented control: two icon halves with a cream circular highlight
// that animates between them. Photo lives on the left, Video on the right.
// Highlight background is animated with animateDpAsState so mode switches
// glide across the pill instead of snapping.
@Composable
private fun ModeSelector(mode: CameraMode, enabled: Boolean, onSelect: (CameraMode) -> Unit) {
    val segmentWidth: Dp = 56.dp
    val height: Dp = 52.dp
    val highlightSize: Dp = 44.dp
    val highlightInset: Dp = (segmentWidth - highlightSize) / 2
    val verticalInset: Dp = (height - highlightSize) / 2
    val cream = Color(0xFFEDE9C8)
    val charcoal = Color(0xFF1F1F1F)
    val targetX = if (mode == CameraMode.Photo) highlightInset else segmentWidth + highlightInset
    val animX by animateDpAsState(
        targetValue = targetX,
        animationSpec = tween(durationMillis = 180),
        label = "modeHighlight",
    )
    Box(
        modifier = Modifier
            .height(height)
            .width(segmentWidth * 2)
            .clip(RoundedCornerShape(percent = 50))
            .background(charcoal),
    ) {
        Box(
            modifier = Modifier
                .offset(x = animX, y = verticalInset)
                .size(highlightSize)
                .clip(CircleShape)
                .background(cream),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            ModeIconSegment(
                width = segmentWidth,
                height = height,
                enabled = enabled,
                selected = mode == CameraMode.Photo,
                contentDesc = "Photo mode",
                onClick = { onSelect(CameraMode.Photo) },
            ) {
                Icon(
                    imageVector = CameraIcons.PhotoCamera,
                    contentDescription = null,
                    tint = if (mode == CameraMode.Photo) charcoal else Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            ModeIconSegment(
                width = segmentWidth,
                height = height,
                enabled = enabled,
                selected = mode == CameraMode.Video,
                contentDesc = "Video mode",
                onClick = { onSelect(CameraMode.Video) },
            ) {
                Icon(
                    imageVector = CameraIcons.Videocam,
                    contentDescription = null,
                    tint = if (mode == CameraMode.Video) charcoal else Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ModeIconSegment(
    width: Dp,
    height: Dp,
    enabled: Boolean,
    selected: Boolean,
    contentDesc: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val haptics = rememberReliveHaptics()
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                onClick = {
                    if (!selected) haptics.perform(ReliveHapticCue.Selection)
                    onClick()
                },
            )
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun ZoomPresetSlotRow(
    slots: List<ZoomSlotSpec>,
    ratio: Float,
    onSelect: (ZoomSlotSpec) -> Unit,
) {
    val activeSlot = activeZoomSlot(ratio, slots)
    val haptics = rememberReliveHaptics()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x66000000))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEach { spec ->
            val active = spec.slot == activeSlot
            val label = zoomSlotLabel(spec.slot, ratio, slots)
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable {
                        if (!active) haptics.perform(ReliveHapticCue.Selection)
                        onSelect(spec)
                    }
                    .padding(horizontal = if (active) 10.dp else 8.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Zoom ${spec.anchorLabel}" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (active) Color.Black else Color.White,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/**
 * Live zoom ruler shown during pinch. Log-scaled tick strip with the current
 * value in a small pill above. Occupies the same slot as [ZoomPresetSlotRow]
 * so swapping does not shift the shutter row.
 */
@Composable
private fun ZoomRulerRow(ratio: Float, minRatio: Float, maxRatio: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xB3000000))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(formatZoomLabel(ratio), color = Color.White, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xB3000000))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            RulerCanvas(ratio = ratio, minRatio = minRatio, maxRatio = maxRatio)
        }
    }
}

@Composable
private fun RulerCanvas(ratio: Float, minRatio: Float, maxRatio: Float) {
    val safeMin = minRatio.coerceAtLeast(0.05f)
    val safeMax = maxRatio.coerceAtLeast(safeMin + 0.01f)
    val logMin = kotlin.math.log10(safeMin.toDouble()).toFloat()
    val logMax = kotlin.math.log10(safeMax.toDouble()).toFloat()
    fun posFrac(r: Float): Float {
        val lr = kotlin.math.log10(r.coerceIn(safeMin, safeMax).toDouble()).toFloat()
        return ((lr - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .height(28.dp),
    ) {
        val w = size.width
        val h = size.height
        val baseline = h * 0.55f
        // Minor ticks: 20 evenly spaced in log space.
        val minorColor = Color.White.copy(alpha = 0.35f)
        val majorColor = Color.White
        val minorCount = 20
        for (i in 0..minorCount) {
            val t = i.toFloat() / minorCount
            val x = t * w
            drawLine(
                color = minorColor,
                start = Offset(x, baseline - 4.dp.toPx()),
                end = Offset(x, baseline + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
        // Major ticks at integer / half stops within the visible range.
        val majors = listOf(0.5f, 1f, 2f, 4f, 8f).filter { it in safeMin..safeMax }
        for (mv in majors) {
            val x = posFrac(mv) * w
            drawLine(
                color = majorColor,
                start = Offset(x, baseline - 7.dp.toPx()),
                end = Offset(x, baseline + 7.dp.toPx()),
                strokeWidth = 1.8.dp.toPx(),
            )
        }
        // Current-ratio indicator: taller amber tick.
        val cx = posFrac(ratio) * w
        drawLine(
            color = Color(0xFFFFD54F),
            start = Offset(cx, baseline - 12.dp.toPx()),
            end = Offset(cx, baseline + 12.dp.toPx()),
            strokeWidth = 2.4.dp.toPx(),
        )
    }
}

@Composable
private fun GalleryButton(onClick: () -> Unit) {
    val haptics = rememberReliveHaptics()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x80000000))
            .clickable {
                haptics.perform(ReliveHapticCue.Action)
                onClick()
            }
            .semantics { contentDescription = "Open gallery" },
        contentAlignment = Alignment.Center,
    ) {
        GalleryGlyph(size = 22.dp, color = Color.White, strokeWidth = 1.8.dp)
    }
}

@Composable
private fun ShutterButton(
    mode: CameraMode,
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val innerFill = when {
        mode == CameraMode.Video && isRecording -> Color(0xFFFF3B30)
        mode == CameraMode.Video -> Color(0xFFFF3B30)
        else -> Color.White
    }
    val contentDesc = when {
        mode == CameraMode.Video && isRecording -> "Stop recording"
        mode == CameraMode.Video -> "Start recording"
        else -> "Take photo"
    }
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center,
    ) {
        // Outer white ring.
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        // Small black gap between ring and inner disc.
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.Black),
        )
        // Inner disc — square when recording (stop affordance), else large.
        if (mode == CameraMode.Video && isRecording) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(innerFill),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(innerFill),
            )
        }
    }
}

/**
 * Retake control shown during PhotoReview. Sits in the exact centered slot
 * the shutter normally occupies (same 84dp footprint), so the center of the
 * bottom row never shifts when swapping between Live and PhotoReview.
 */
@Composable
private fun RetakeButton(onClick: () -> Unit) {
    val haptics = rememberReliveHaptics()
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .clickable {
                haptics.perform(ReliveHapticCue.Action)
                onClick()
            }
            .semantics { contentDescription = "Retake" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A)),
        )
        Icon(
            imageVector = CameraIcons.Refresh,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}

/** Confirmation ✓ shown during PhotoReview on the right side of the row. */
@Composable
private fun ConfirmButton(onClick: () -> Unit) {
    val haptics = rememberReliveHaptics()
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFFEDE9C8))
            .clickable {
                haptics.perform(ReliveHapticCue.Confirm)
                onClick()
            }
            .semantics { contentDescription = "Confirm" },
        contentAlignment = Alignment.Center,
    ) {
        CheckGlyph(size = 28.dp, color = Color(0xFF1F1F1F), strokeWidth = 3.dp)
    }
}

@Composable
private fun CheckGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        drawLine(
            color,
            Offset(px * 0.20f, px * 0.55f),
            Offset(px * 0.44f, px * 0.78f),
            strokeWidth = sw,
        )
        drawLine(
            color,
            Offset(px * 0.44f, px * 0.78f),
            Offset(px * 0.82f, px * 0.28f),
            strokeWidth = sw,
        )
    }
}

@Composable
private fun FlashButton(
    flash: FlashMode,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.4f
    val contentDesc = if (flash == FlashMode.On) "Turn flash off" else "Turn flash on"
    val haptics = rememberReliveHaptics()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x80000000))
            .clickable(
                enabled = enabled,
                onClick = {
                    haptics.perform(
                        if (flash == FlashMode.On) ReliveHapticCue.ToggleOff else ReliveHapticCue.ToggleOn,
                    )
                    onClick()
                },
            )
            .semantics { contentDescription = contentDesc },
        contentAlignment = Alignment.Center,
    ) {
        FlashGlyph(
            size = 22.dp,
            color = Color.White.copy(alpha = alpha),
            strokeWidth = 1.8.dp,
            on = flash == FlashMode.On,
        )
    }
}

@Composable
private fun SwitchButton(enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.4f
    val haptics = rememberReliveHaptics()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x80000000))
            .clickable(
                enabled = enabled,
                onClick = {
                    haptics.perform(ReliveHapticCue.Selection)
                    onClick()
                },
            )
            .semantics { contentDescription = "Switch camera" },
        contentAlignment = Alignment.Center,
    ) {
        SwitchGlyph(size = 24.dp, color = Color.White.copy(alpha = alpha), strokeWidth = 2.0.dp)
    }
}

@Composable
private fun FlashGlyph(size: Dp, color: Color, strokeWidth: Dp, on: Boolean) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val path = Path().apply {
            moveTo(px * 0.55f, px * 0.05f)
            lineTo(px * 0.20f, px * 0.55f)
            lineTo(px * 0.45f, px * 0.55f)
            lineTo(px * 0.35f, px * 0.95f)
            lineTo(px * 0.80f, px * 0.40f)
            lineTo(px * 0.55f, px * 0.40f)
            close()
        }
        if (on) drawPath(path, color = color) else drawPath(path, color = color, style = Stroke(width = sw))
    }
}

/**
 * Minimal loop-arrow rotate icon: two curved arrows chasing each other. No
 * camera body — surrounding UI already establishes context.
 */
@Composable
private fun SwitchGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val radius = px * 0.34f
        val center = Offset(px * 0.5f, px * 0.5f)
        val topArc = Path().apply {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(center = center, radius = radius),
                startAngleDegrees = 200f,
                sweepAngleDegrees = 140f,
                forceMoveTo = true,
            )
        }
        drawPath(topArc, color = color, style = Stroke(width = sw))
        val bottomArc = Path().apply {
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(center = center, radius = radius),
                startAngleDegrees = 20f,
                sweepAngleDegrees = 140f,
                forceMoveTo = true,
            )
        }
        drawPath(bottomArc, color = color, style = Stroke(width = sw))
        val topTip = Offset(
            x = center.x + radius * kotlin.math.cos(Math.toRadians(340.0)).toFloat(),
            y = center.y + radius * kotlin.math.sin(Math.toRadians(340.0)).toFloat(),
        )
        drawLine(color, topTip, Offset(topTip.x - sw * 2.2f, topTip.y - sw * 1.4f), strokeWidth = sw)
        drawLine(color, topTip, Offset(topTip.x + sw * 0.4f, topTip.y - sw * 2.6f), strokeWidth = sw)
        val botTip = Offset(
            x = center.x + radius * kotlin.math.cos(Math.toRadians(160.0)).toFloat(),
            y = center.y + radius * kotlin.math.sin(Math.toRadians(160.0)).toFloat(),
        )
        drawLine(color, botTip, Offset(botTip.x + sw * 2.2f, botTip.y + sw * 1.4f), strokeWidth = sw)
        drawLine(color, botTip, Offset(botTip.x - sw * 0.4f, botTip.y + sw * 2.6f), strokeWidth = sw)
    }
}

/** Simple gallery / stacked-photos glyph. */
@Composable
private fun GalleryGlyph(size: Dp, color: Color, strokeWidth: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val px = size.toPx()
        val sw = strokeWidth.toPx()
        val back = androidx.compose.ui.geometry.Rect(px * 0.12f, px * 0.22f, px * 0.78f, px * 0.78f)
        val front = androidx.compose.ui.geometry.Rect(px * 0.24f, px * 0.34f, px * 0.90f, px * 0.90f)
        drawRoundRect(
            color = color,
            topLeft = Offset(back.left, back.top),
            size = androidx.compose.ui.geometry.Size(back.width, back.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(px * 0.06f, px * 0.06f),
            style = Stroke(width = sw),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(front.left, front.top),
            size = androidx.compose.ui.geometry.Size(front.width, front.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(px * 0.06f, px * 0.06f),
            style = Stroke(width = sw),
        )
        // Small "sun" inside the front frame to read as an image.
        drawCircle(
            color = color,
            radius = px * 0.06f,
            center = Offset(front.left + front.width * 0.30f, front.top + front.height * 0.30f),
        )
    }
}

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000
    val mm = s / 60
    val ss = s % 60
    return "%02d:%02d".format(mm, ss)
}

private fun normalizeExifOrientation(file: File) {
    val path = file.absolutePath
    val exif = try { ExifInterface(path) } catch (_: Throwable) { return }
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL,
    )
    if (orientation == ExifInterface.ORIENTATION_NORMAL ||
        orientation == ExifInterface.ORIENTATION_UNDEFINED
    ) return
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f); matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f); matrix.postScale(-1f, 1f)
        }
        else -> return
    }
    val src = try { BitmapFactory.decodeFile(path) } catch (_: Throwable) { null } ?: return
    val rotated = try {
        Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    } catch (_: Throwable) {
        src.recycle(); return
    }
    try {
        java.io.FileOutputStream(file).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        try {
            val fresh = ExifInterface(path)
            fresh.setAttribute(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString(),
            )
            fresh.saveAttributes()
        } catch (_: Throwable) { /* orientation baked in pixels; tag reset best-effort */ }
    } catch (_: Throwable) {
        // If write fails the original file remains; consumers still get the
        // (rotated-by-EXIF) file — worst case Photo Review shows sensor-
        // orientation, but we do not corrupt the capture.
    } finally {
        if (rotated !== src) rotated.recycle()
        src.recycle()
    }
}

private fun takePhoto(
    store: AndroidMediaStore,
    imageCapture: ImageCapture,
    executor: Executor,
    scope: CoroutineScope,
    onPending: (File) -> Unit,
    onCaptured: (RawMedia, Job) -> Unit,
    onError: () -> Unit,
) {
    val tmp = store.newTempFile("relive-cam-", ".jpg")
    onPending(tmp)
    val out = ImageCapture.OutputFileOptions.Builder(tmp).build()
    imageCapture.takePicture(out, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
            // Enter review immediately with the raw file, then normalize EXIF
            // orientation asynchronously off the main thread. CameraX writes
            // the sensor orientation into the JPEG's EXIF tag rather than
            // rotating pixels; consumers that decode via BitmapFactory
            // (which ignores EXIF) would otherwise see a rotated image.
            // Confirm awaits this job so the persisted file is always
            // correctly oriented; Retake cancels it.
            val raw = RawMedia(MediaType.Image, tmp.absolutePath, ownedByRelive = true)
            val job = scope.launch(Dispatchers.IO) {
                normalizeExifOrientation(tmp)
            }
            onCaptured(raw, job)
        }
        override fun onError(exc: ImageCaptureException) {
            tmp.delete()
            onError()
        }
    })
}

/**
 * Decode [path] into a [Bitmap] no larger than [maxDim] px on the longer
 * axis. Uses BitmapFactory's inSampleSize bounds pass to avoid ever
 * allocating the full-resolution bitmap for a review overlay that will only
 * be composited into the preview band. Returns null on any decode failure.
 */
private fun decodeDownsampled(path: String, maxDim: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val longer = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longer / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeFile(path, opts)
    } catch (_: Throwable) {
        null
    }
}

private fun startRecording(
    context: Context,
    store: AndroidMediaStore,
    videoCapture: VideoCapture<Recorder>,
    executor: Executor,
    onPending: (File) -> Unit,
    onStarted: () -> Unit,
    onFinal: (RawMedia) -> Unit,
    onError: () -> Unit,
): Recording {
    val tmp = store.newTempFile("relive-cam-", ".mp4")
    onPending(tmp)
    val outOptions = FileOutputOptions.Builder(tmp).build()
    val pending = videoCapture.output.prepareRecording(context, outOptions)
    val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (hasAudio) {
        try { pending.withAudioEnabled() } catch (_: Throwable) { /* fall through, video-only */ }
    }
    return pending.start(executor) { event ->
        when (event) {
            is VideoRecordEvent.Start -> onStarted()
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    tmp.delete()
                    onError()
                } else {
                    onFinal(RawMedia(MediaType.Video, tmp.absolutePath, ownedByRelive = true))
                }
            }
        }
    }
}
