package com.peto.ramap.ui.onboarding

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.fake.FakePersonalizationRepository
import kotlinx.coroutines.CompletableDeferred

internal class ControllablePersonalizationStore(
    val delegate: FakePersonalizationRepository,
) : PersonalizationStore by delegate {
    var pendingUpdate: CompletableDeferred<Unit>? = null
    var refreshError: RamapError? = null

    override suspend fun refresh(): RamapResult<Unit> = refreshError?.let { RamapResult.Error(it) } ?: delegate.refresh()

    override suspend fun updateBookmark(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        pendingUpdate?.await()
        return delegate.updateBookmark(shopId, enabled)
    }
}
