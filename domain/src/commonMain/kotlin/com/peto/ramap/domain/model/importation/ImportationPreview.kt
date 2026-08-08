package com.peto.ramap.domain.model.importation

data class ImportationPreview(
    val provider: ImportationProvider,
    val totalPlaceCount: Int,
    val matchedShopIds: Set<String>,
    val unmatchedPlaceNames: List<String>,
)
