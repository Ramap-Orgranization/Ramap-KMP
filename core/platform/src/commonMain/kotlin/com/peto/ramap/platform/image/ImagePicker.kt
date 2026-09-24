package com.peto.ramap.platform.image

import androidx.compose.runtime.Composable

data class PickedImage(
    val bytes: ByteArray,
    val mimeType: String,
)

@Composable
expect fun rememberImagePicker(
    onImagePicked: (PickedImage) -> Unit,
    onRejected: () -> Unit,
): () -> Unit
