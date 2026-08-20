package com.vaibhav.relive.platform.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
actual fun MicPermissionAdapter(
    pending: Boolean,
    onResult: (MicPermissionResult) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    // Whether we have already launched at least one request in this composition
    // — combined with a callback shouldShow=false this is the reliable signal
    // for "Don't ask again".
    val launchedBefore = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onResult(MicPermissionResult.Granted)
            return@rememberLauncherForActivityResult
        }
        val canAskAgain = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
        } ?: false
        // shouldShow=false after a launched request means either a fresh
        // pre-decision state OR a permanent block. Once we've launched at
        // least once, a subsequent shouldShow=false is a permanent block.
        val permanent = !canAskAgain && launchedBefore.value
        onResult(if (permanent) MicPermissionResult.PermanentlyDenied else MicPermissionResult.Denied)
    }

    LaunchedEffect(pending) {
        if (!pending) return@LaunchedEffect
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            onResult(MicPermissionResult.Granted)
        } else {
            launchedBefore.value = true
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

private fun android.content.Context.findActivity(): Activity? {
    var ctx: android.content.Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
