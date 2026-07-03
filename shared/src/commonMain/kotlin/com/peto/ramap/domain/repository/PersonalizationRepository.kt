package com.peto.ramap.domain.repository

import com.peto.ramap.domain.model.Personalization

interface PersonalizationRepository {
    suspend fun fetchPersonalization(): Personalization

    suspend fun addBookmark(shopId: String)

    suspend fun removeBookmark(shopId: String)

    suspend fun hideShop(shopId: String)

    suspend fun unhideShop(shopId: String)
}
