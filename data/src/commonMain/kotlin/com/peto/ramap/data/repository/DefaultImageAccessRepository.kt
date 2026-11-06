package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.image.ExpiredImageUrlParser
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.domain.model.image.AuthorizedImage
import com.peto.ramap.domain.model.image.ImageAccessScope
import com.peto.ramap.domain.repository.ImageAccessRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal class DefaultImageAccessRepository(
    private val parser: ExpiredImageUrlParser,
    private val images: ImageUrlDataSource,
    private val changes: ReviewChangeStore,
    sessionUserIds: Flow<String?>,
    private val currentUserId: () -> String?,
) : ImageAccessRepository {
    override val scopes = combine(sessionUserIds, changes.journal) { _, _ -> snapshot() }.distinctUntilChanged()

    override fun snapshot(): ImageAccessScope {
        val journal = changes.journal.value
        return ImageAccessScope(currentUserId(), journal.epoch, journal.imageRevision)
    }

    override fun isProtected(url: String): Boolean = parser.isProtected(url)

    override suspend fun authorize(url: String): AuthorizedImage? {
        val image = parser.parse(url) ?: return null
        val before = snapshot()
        val resolved = images.fetchUrls(listOf(image))[image] ?: return null
        if (snapshot() != before) return null
        return AuthorizedImage(resolved.url, "${image.bucket}/${image.path}", before)
    }
}
