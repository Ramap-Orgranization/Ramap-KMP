package com.peto.ramap.ui.main.event.list.platform

import androidx.compose.runtime.Composable
import com.peto.ramap.domain.model.report.NewsReportEvidence

@Composable
internal expect fun rememberNewsReportImagePicker(onImagePicked: (NewsReportEvidence) -> Unit): () -> Unit
