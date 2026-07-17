package com.peto.ramap.network

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.Location

fun interface ReverseGeocoder {
    suspend fun address(location: Location): RamapResult<String?>
}
