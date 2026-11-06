package com.peto.ramap.domain.repository

import com.peto.ramap.domain.model.image.AuthorizedImage
import com.peto.ramap.domain.model.image.ImageAccessScope
import kotlinx.coroutines.flow.Flow

interface ImageAccessRepository {
    val scopes: Flow<ImageAccessScope>

    fun snapshot(): ImageAccessScope

    fun isProtected(url: String): Boolean

    suspend fun authorize(url: String): AuthorizedImage?
}
