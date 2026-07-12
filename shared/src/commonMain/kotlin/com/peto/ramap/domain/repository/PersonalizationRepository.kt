package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.Personalization

interface PersonalizationRepository {
    suspend fun fetchPersonalization(): RamapResult<Personalization>

    suspend fun addBookmark(shopId: String): RamapResult<Unit>

    suspend fun removeBookmark(shopId: String): RamapResult<Unit>

    suspend fun hideShop(
        shopId: String,
        removeBookmark: Boolean = false,
    ): RamapResult<Unit>

    suspend fun unhideShop(shopId: String): RamapResult<Unit>
}
