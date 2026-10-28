package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.shop.RamenShops

interface ArchiveRepository {
    suspend fun fetchArchiveShops(): RamapResult<ArchiveShops>

    suspend fun fetchShops(shopIds: Set<String>): RamapResult<RamenShops>

    suspend fun resolvePhotos(references: Set<String>): RamapResult<Map<String, MyReviewPhoto>>
}
