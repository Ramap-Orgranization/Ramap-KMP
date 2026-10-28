package com.peto.ramap.ui.main.map.archive.model

import com.peto.ramap.domain.model.review.ArchiveShops

internal data class ArchiveMapSnapshot(
    val visits: ArchiveShops,
    val userId: String?,
    val revision: Long,
)
