package com.peto.ramap.ui.main.map.archive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapRenderModel

@Composable
internal expect fun ArchiveMapView(
    request: ArchiveMapRenderModel,
    modifier: Modifier = Modifier,
)
