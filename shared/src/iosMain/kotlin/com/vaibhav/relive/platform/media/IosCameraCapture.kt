package com.vaibhav.relive.platform.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vaibhav.relive.domain.model.MediaType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerMediaType
import platform.UIKit.UIImagePickerControllerMediaURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraCaptureSurface(
    mediaStore: MediaStore,
    onCaptured: (RawMedia) -> Unit,
    onCancel: () -> Unit,
) {
    val store = mediaStore as? IosMediaStore
        ?: error("iOS CameraCaptureSurface requires IosMediaStore")
    Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    LaunchedEffect(Unit) { present(store, onCaptured, onCancel) }
}

@OptIn(ExperimentalForeignApi::class)
private fun present(
    store: IosMediaStore,
    onCaptured: (RawMedia) -> Unit,
    onCancel: () -> Unit,
) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return onCancel()
    val picker = UIImagePickerController()
    picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
    picker.mediaTypes = listOf("public.image", "public.movie")
    val delegate = object : NSObject(),
        UIImagePickerControllerDelegateProtocol,
        UINavigationControllerDelegateProtocol {
        override fun imagePickerController(
            picker: UIImagePickerController,
            didFinishPickingMediaWithInfo: Map<Any?, *>,
        ) {
            val info = didFinishPickingMediaWithInfo
            val type = info[UIImagePickerControllerMediaType] as? String
            picker.dismissViewControllerAnimated(true, completion = null)
            when (type) {
                "public.image" -> {
                    val image = info[UIImagePickerControllerOriginalImage] as? UIImage
                    if (image != null) {
                        val jpeg = UIImageJPEGRepresentation(image, 0.85)
                        if (jpeg != null) {
                            val path = store.newTempPath("jpg")
                            NSFileManager.defaultManager.createFileAtPath(path, contents = jpeg, attributes = null)
                            onCaptured(RawMedia(MediaType.Image, path, ownedByRelive = true))
                            return
                        }
                    }
                    onCancel()
                }
                "public.movie" -> {
                    val url = info[UIImagePickerControllerMediaURL] as? NSURL
                    if (url != null) {
                        val path = store.newTempPath("mov")
                        NSFileManager.defaultManager.copyItemAtURL(url, NSURL.fileURLWithPath(path), error = null)
                        onCaptured(RawMedia(MediaType.Video, path, ownedByRelive = true))
                        return
                    }
                    onCancel()
                }
                else -> onCancel()
            }
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true, completion = null)
            onCancel()
        }
    }
    picker.delegate = delegate
    root.presentViewController(picker, animated = true, completion = null)
}
