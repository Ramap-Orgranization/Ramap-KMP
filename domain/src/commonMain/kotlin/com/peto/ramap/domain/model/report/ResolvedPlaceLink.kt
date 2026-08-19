package com.peto.ramap.domain.model.report

data class ResolvedPlaceLink(
    val provider: PlaceLinkProvider,
    val placeId: String? = null,
    val name: String? = null,
)
