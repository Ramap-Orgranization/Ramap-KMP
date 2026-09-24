package com.peto.ramap.platform.image

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
    onRejected: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onImagePicked)
    val rejected by rememberUpdatedState(onRejected)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType == null || mimeType !in SUPPORTED_MIME_TYPES) {
                rejected()
                return@rememberLauncherForActivityResult
            }
            val bytes =
                try {
                    context.contentResolver.openInputStream(uri)?.use(::readAtMostFiveMiB)
                } catch (_: Exception) {
                    null
                }
            if (bytes == null) {
                rejected()
                return@rememberLauncherForActivityResult
            }
            callback(PickedImage(bytes, mimeType))
        }
    return remember(launcher) { { launcher.launch("image/*") } }
}

private val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png")
private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

private fun readAtMostFiveMiB(input: java.io.InputStream): ByteArray? {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return output.toByteArray()
        if (output.size() + read > MAX_IMAGE_BYTES) return null
        output.write(buffer, 0, read)
    }
}
