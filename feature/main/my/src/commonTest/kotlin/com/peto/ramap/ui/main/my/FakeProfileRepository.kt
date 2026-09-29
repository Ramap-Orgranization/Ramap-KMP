package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileDraft
import com.peto.ramap.domain.repository.ProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class FakeProfileRepository : ProfileRepository {
    override val sessionUserIds = MutableStateFlow<String?>("first")
    var fetchResult: RamapResult<AccountProfile>? = null
    var fetchPending: CompletableDeferred<RamapResult<AccountProfile>>? = null
    var ignoreFetchCancellation = false
    var visibilityResult: RamapResult<AccountProfile>? = null
    var visibilityPending: CompletableDeferred<RamapResult<AccountProfile>>? = null
    var ignoreVisibilityCancellation = false

    override suspend fun fetchMyProfile(): RamapResult<AccountProfile> {
        val pending = fetchPending
        if (pending != null) return if (ignoreFetchCancellation) withContext(NonCancellable) { pending.await() } else pending.await()
        return fetchResult ?: RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), "느긋한차슈"))
    }

    override suspend fun isNicknameAvailable(nickname: String): RamapResult<Boolean> = RamapResult.Success(true)

    override suspend fun updateMyProfile(draft: ProfileDraft): RamapResult<AccountProfile> = RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), draft.nickname.value, bio = draft.bio?.value.orEmpty()))

    override suspend fun updateProfileVisibility(isPublic: Boolean): RamapResult<AccountProfile> {
        val pending = visibilityPending
        if (pending != null) return if (ignoreVisibilityCancellation) withContext(NonCancellable) { pending.await() } else pending.await()
        return visibilityResult ?: RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), "느긋한차슈", isPublic = isPublic))
    }
}
