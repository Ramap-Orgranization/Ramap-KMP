package com.peto.ramap.platform.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationSelectionOrdered
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberImagesPicker(
    maxSelectionCount: Int,
    onImagesPicked: (List<PickedImage>) -> Unit,
    onRejected: () -> Unit,
): () -> Unit {
    val controller = LocalUIViewController.current
    val picked by rememberUpdatedState(onImagesPicked)
    val rejected by rememberUpdatedState(onRejected)
    val delegate = remember { ImagesPickerDelegate({ picked(it) }, { rejected() }) }
    return remember(controller, delegate, maxSelectionCount) {
        {
            if (maxSelectionCount > 0 && !delegate.isPicking) {
                val configuration = PHPickerConfiguration()
                configuration.selectionLimit = maxSelectionCount.coerceAtMost(3).toLong()
                configuration.selection = PHPickerConfigurationSelectionOrdered
                configuration.filter = PHPickerFilter.imagesFilter
                val picker = PHPickerViewController(configuration = configuration)
                delegate.isPicking = true
                delegate.picker = picker
                picker.delegate = delegate
                controller.presentViewController(picker, true, null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
class ImagesPickerDelegate(
    private val onImagesPicked: (List<PickedImage>) -> Unit,
    private val onRejected: () -> Unit,
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    var picker: PHPickerViewController? = null
    var isPicking = false

    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, null)
        this.picker = null
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isEmpty()) {
            isPicking = false
            return
        }
        val images = MutableList<PickedImage?>(results.size) { null }
        var completed = 0
        var rejected = false
        for ((index, result) in results.withIndex()) {
            val provider = result.itemProvider
            if (!provider.hasItemConformingToTypeIdentifier("public.image")) {
                rejected = true
                completed++
                continue
            }
            provider.loadDataRepresentationForTypeIdentifier("public.image") { data, error ->
                val bytes = data?.let { UIImage(data = it) }?.let { UIImageJPEGRepresentation(it, 0.9) }?.let(::dataBytes)
                dispatch_async(dispatch_get_main_queue()) {
                    if (error != null || bytes == null) {
                        rejected = true
                    } else {
                        images[index] = PickedImage(bytes, "image/jpeg")
                    }
                    completed++
                    if (completed == results.size) {
                        val accepted = images.filterNotNull()
                        if (accepted.isNotEmpty()) onImagesPicked(accepted)
                        if (rejected) onRejected()
                        isPicking = false
                    }
                }
            }
        }
        if (completed == results.size) {
            val accepted = images.filterNotNull()
            if (accepted.isNotEmpty()) onImagesPicked(accepted)
            if (rejected) onRejected()
            isPicking = false
        }
    }
}

private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

@OptIn(ExperimentalForeignApi::class)
private fun dataBytes(data: NSData): ByteArray? {
    if (data.length == 0uL || data.length > MAX_IMAGE_BYTES.toULong()) return null
    return data.bytes?.readBytes(data.length.toInt())
}
