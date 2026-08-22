package com.peto.ramap.ui.main.event.list.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.peto.ramap.domain.model.report.NewsReportEvidence

@Composable
internal actual fun rememberNewsReportImagePicker(onImagePicked: (NewsReportEvidence) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnImagePicked by rememberUpdatedState(onImagePicked)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val mimeType = context.contentResolver.getType(uri) ?: return@rememberLauncherForActivityResult
            if (mimeType !in SUPPORTED_MIME_TYPES) return@rememberLauncherForActivityResult
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
            val evidence = NewsReportEvidence(bytes, mimeType)
            if (evidence.isValid()) currentOnImagePicked(evidence)
        }

    return remember(launcher) { { launcher.launch("image/*") } }
}

private val SUPPORTED_MIME_TYPES =
    setOf(
        NewsReportEvidence.JPEG_MIME_TYPE,
        NewsReportEvidence.PNG_MIME_TYPE,
    )
