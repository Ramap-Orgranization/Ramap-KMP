package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.repository.ProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

internal class FakeProfileRepository : ProfileRepository {
    override val sessionUserIds = MutableStateFlow<String?>("first")
    var fetchResult: RamapResult<AccountProfile>? = null
    var fetchPending: CompletableDeferred<RamapResult<AccountProfile>>? = null
    var ignoreFetchCancellation = false

    override suspend fun fetchMyProfile(): RamapResult<AccountProfile> {
        val pending = fetchPending
        if (pending != null) return if (ignoreFetchCancellation) withContext(NonCancellable) { pending.await() } else pending.await()
        return fetchResult ?: RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), "느긋한차슈"))
    }
}
