package com.peto.ramap.domain.repository

import com.peto.ramap.domain.model.report.ResolvedPlaceLink

fun interface PlaceLinkResolver {
    suspend fun resolve(url: String): ResolvedPlaceLink?
}
