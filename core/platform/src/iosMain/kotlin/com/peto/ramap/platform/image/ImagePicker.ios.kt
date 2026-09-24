package com.peto.ramap.platform.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIModalPresentationPopover
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
    onRejected: () -> Unit,
): () -> Unit {
    val controller = LocalUIViewController.current
    val callback by rememberUpdatedState(onImagePicked)
    val rejection by rememberUpdatedState(onRejected)
    val delegate = remember { ImagePickerDelegate({ callback(it) }, { rejection() }) }
    return remember(controller) {
        {
            val picker = UIImagePickerController()
            picker.sourceType = platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            picker.modalPresentationStyle = UIModalPresentationPopover
            picker.popoverPresentationController?.sourceView = controller.view
            picker.popoverPresentationController?.sourceRect = controller.view.bounds
            delegate.picker = picker
            picker.delegate = delegate
            controller.presentViewController(picker, true, null)
        }
    }
}

class ImagePickerDelegate(
    private val onImagePicked: (PickedImage) -> Unit,
    private val onRejected: () -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    var picker: UIImagePickerController? = null

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val bytes = image?.let { UIImageJPEGRepresentation(it, 0.9) }?.let(::dataBytes)
        if (bytes == null || bytes.size > MAX_IMAGE_BYTES) onRejected() else onImagePicked(PickedImage(bytes, "image/jpeg"))
        this.picker?.dismissViewControllerAnimated(true, null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        this.picker?.dismissViewControllerAnimated(true, null)
    }
}

private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

@OptIn(ExperimentalForeignApi::class)
private fun dataBytes(data: NSData): ByteArray? {
    if (data.length == 0uL || data.length > MAX_IMAGE_BYTES.toULong()) return null
    return data.bytes?.readBytes(data.length.toInt())
}
