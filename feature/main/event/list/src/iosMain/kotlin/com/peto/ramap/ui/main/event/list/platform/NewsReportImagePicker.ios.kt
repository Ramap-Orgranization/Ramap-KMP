package com.peto.ramap.ui.main.event.list.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.interop.LocalUIViewController
import com.peto.ramap.domain.model.report.NewsReportEvidence
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@Composable
internal actual fun rememberNewsReportImagePicker(onImagePicked: (NewsReportEvidence) -> Unit): () -> Unit {
    val viewController = LocalUIViewController.current
    val onImagePickedState = rememberUpdatedState(onImagePicked)
    val delegate = remember { NewsReportImagePickerDelegate { evidence -> onImagePickedState.value(evidence) } }
    return remember(viewController) {
        {
            val picker = UIImagePickerController()
            picker.sourceType = platform.UIKit.UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            delegate.picker = picker
            picker.delegate = delegate
            viewController.presentViewController(picker, true, null)
        }
    }
}

private class NewsReportImagePickerDelegate(
    private val onImagePicked: (NewsReportEvidence) -> Unit,
) : NSObject(),
    UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    var picker: UIImagePickerController? = null

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image =
            didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage
                ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val bytes = image?.let(::jpegBytes)
        if (bytes != null) {
            val evidence = NewsReportEvidence(bytes, NewsReportEvidence.JPEG_MIME_TYPE)
            if (evidence.isValid()) onImagePicked(evidence)
        }
        this.picker?.dismissViewControllerAnimated(true, null)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        this.picker?.dismissViewControllerAnimated(true, null)
    }
}

private fun jpegBytes(image: UIImage): ByteArray? = UIImageJPEGRepresentation(image, compressionQuality = 0.9)?.toByteArray()

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = bytes?.readBytes(length.toInt()) ?: byteArrayOf()
