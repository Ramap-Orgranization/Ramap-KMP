package com.peto.ramap.platform.image

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

@Composable
actual fun rememberImagesPicker(
    maxSelectionCount: Int,
    onImagesPicked: (List<PickedImage>) -> Unit,
    onRejected: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onImagesPicked)
    val rejected by rememberUpdatedState(onRejected)
    val remaining by rememberUpdatedState(maxSelectionCount.coerceIn(0, 3))
    val isPicking = remember { mutableStateOf(false) }

    fun handleSelection(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) {
            isPicking.value = false
            return
        }
        scope.launch {
            val selected = uris.take(remaining)
            try {
                val (images, hasInvalid) =
                    withContext(Dispatchers.IO) {
                        val accepted = mutableListOf<PickedImage>()
                        var invalid = uris.size > selected.size
                        for (uri in selected) {
                            val mimeType = context.contentResolver.getType(uri)
                            val bytes =
                                try {
                                    context.contentResolver.openInputStream(uri)?.use(::readAtMostFiveMiB)
                                } catch (_: Exception) {
                                    null
                                }
                            if (mimeType != null && bytes != null && hasExpectedSignature(bytes, mimeType)) {
                                accepted += PickedImage(bytes, mimeType)
                            } else {
                                invalid = true
                            }
                        }
                        accepted to invalid
                    }
                if (images.isNotEmpty()) picked(images)
                if (hasInvalid) rejected()
            } finally {
                isPicking.value = false
            }
        }
    }

    val singleLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) isPicking.value = false else handleSelection(listOf(uri))
        }
    val multipleLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxSelectionCount.coerceAtLeast(2)),
        ) { uris ->
            handleSelection(uris)
        }
    return remember(singleLauncher, multipleLauncher, maxSelectionCount) {
        {
            val request =
                PickVisualMediaRequest(
                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                    maxItems = maxSelectionCount.coerceAtLeast(2),
                    isOrderedSelection = true,
                )
            when {
                isPicking.value || maxSelectionCount <= 0 -> Unit
                maxSelectionCount == 1 -> {
                    isPicking.value = true
                    singleLauncher.launch(request)
                }
                else -> {
                    isPicking.value = true
                    multipleLauncher.launch(request)
                }
            }
        }
    }
}

private val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png")
private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024

private fun hasExpectedSignature(
    bytes: ByteArray,
    mimeType: String,
): Boolean =
    when (mimeType) {
        "image/jpeg" ->
            bytes.size >= 3 &&
                bytes[0] == 0xFF.toByte() &&
                bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte()
        "image/png" ->
            bytes.size >= 8 &&
                bytes.copyOfRange(0, 8).contentEquals(
                    byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
                )
        else -> false
    }

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
