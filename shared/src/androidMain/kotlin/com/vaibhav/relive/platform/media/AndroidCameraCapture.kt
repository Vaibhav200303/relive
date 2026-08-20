package com.vaibhav.relive.platform.media

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vaibhav.relive.domain.model.MediaType
import java.io.File
import java.util.concurrent.Executor

/**
 * Single CameraX experience where the user picks Photo or Video before
 * capturing. Cancel/Back both return without a capture and discard any
 * in-flight temporary file. Controls respect safe-drawing insets so they
 * remain fully tappable across cutouts and gesture-nav bars.
 */
@Composable
actual fun CameraCaptureSurface(
    mediaStore: MediaStore,
    onCaptured: (RawMedia) -> Unit,
    onCancel: () -> Unit,
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
    var isRecording by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val recorder = remember { Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }
    // Track the in-flight temp file so cancel/back can delete it if no
    // capture ever completed.
    var pendingTempFile: File? by remember { mutableStateOf(null) }

    fun discardPendingTempFile() {
        pendingTempFile?.delete()
        pendingTempFile = null
    }

    val cancel: () -> Unit = {
        activeRecording?.stop()
        activeRecording = null
        isRecording = false
        discardPendingTempFile()
        onCancel()
    }

    // Bind camera use cases whenever mode changes.
    LaunchedEffect(mode) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            provider.unbindAll()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            when (mode) {
                CameraMode.Photo -> provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                CameraMode.Video -> provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
            }
        }, executor)
    }
    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            discardPendingTempFile()
            ProcessCameraProvider.getInstance(context).get().unbindAll()
        }
    }

    // System Back closes camera and returns to composer without exiting Relive.
    BackHandler(enabled = true, onBack = cancel)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mode toggle.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x66000000))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ModeChip("Photo", selected = mode == CameraMode.Photo, enabled = !isRecording) { mode = CameraMode.Photo }
                ModeChip("Video", selected = mode == CameraMode.Video, enabled = !isRecording) { mode = CameraMode.Video }
            }
            Spacer(Modifier.height(24.dp))
            // Cancel | Shutter | (spacer) — Cancel sits in the safe area next
            // to the shutter so it is always reachable.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CancelPill(onClick = cancel)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            when (mode) {
                                CameraMode.Photo -> takePhoto(
                                    store = store,
                                    imageCapture = imageCapture,
                                    executor = executor,
                                    onPending = { pendingTempFile = it },
                                    onCaptured = { raw ->
                                        pendingTempFile = null
                                        onCaptured(raw)
                                    },
                                )
                                CameraMode.Video -> {
                                    if (!isRecording) {
                                        activeRecording = startRecording(
                                            context = context,
                                            store = store,
                                            videoCapture = videoCapture,
                                            executor = executor,
                                            onPending = { pendingTempFile = it },
                                            onFinal = { raw ->
                                                pendingTempFile = null
                                                activeRecording = null
                                                isRecording = false
                                                onCaptured(raw)
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
                // Symmetry spacer so the shutter stays centered.
                Spacer(Modifier.size(72.dp))
            }
        }
    }
}

private enum class CameraMode { Photo, Video }

@Composable
private fun ModeChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, color = if (selected) Color.Black else Color.White)
    }
}

@Composable
private fun CancelPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x80000000))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text("Cancel", color = Color.White)
    }
}

private fun takePhoto(
    store: AndroidMediaStore,
    imageCapture: ImageCapture,
    executor: Executor,
    onPending: (File) -> Unit,
    onCaptured: (RawMedia) -> Unit,
) {
    val tmp = store.newTempFile("relive-cam-", ".jpg")
    onPending(tmp)
    val out = ImageCapture.OutputFileOptions.Builder(tmp).build()
    imageCapture.takePicture(out, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(result: ImageCapture.OutputFileResults) {
            onCaptured(RawMedia(MediaType.Image, tmp.absolutePath, ownedByRelive = true))
        }
        override fun onError(exc: ImageCaptureException) { tmp.delete() }
    })
}

private fun startRecording(
    context: android.content.Context,
    store: AndroidMediaStore,
    videoCapture: VideoCapture<Recorder>,
    executor: Executor,
    onPending: (File) -> Unit,
    onFinal: (RawMedia) -> Unit,
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
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    tmp.delete()
                } else {
                    onFinal(RawMedia(MediaType.Video, tmp.absolutePath, ownedByRelive = true))
                }
            }
        }
    }
}
