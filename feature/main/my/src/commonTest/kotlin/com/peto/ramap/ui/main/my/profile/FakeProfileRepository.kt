package com.peto.ramap.ui.main.my.profile

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
    var saveResult: CompletableDeferred<RamapResult<AccountProfile>>? = null
    var fetchResult: RamapResult<AccountProfile>? = null
    var fetchPending: CompletableDeferred<RamapResult<AccountProfile>>? = null
    var ignoreFetchCancellation = false
    var ignoreCancellation = false
    var saveCalls = 0
    var nicknameAvailable = true
    var nicknameCheckResult: CompletableDeferred<RamapResult<Boolean>>? = null

    override suspend fun fetchMyProfile(): RamapResult<AccountProfile> {
        val pending = fetchPending
        if (pending != null) return if (ignoreFetchCancellation) withContext(NonCancellable) { pending.await() } else pending.await()
        return fetchResult ?: RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), "느긋한차슈"))
    }

    override suspend fun isNicknameAvailable(nickname: String): RamapResult<Boolean> = nicknameCheckResult?.await() ?: RamapResult.Success(nicknameAvailable)

    override suspend fun updateMyProfile(draft: ProfileDraft): RamapResult<AccountProfile> {
        saveCalls++
        val pending = saveResult
        if (pending == null) return RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), draft.nickname.value, bio = draft.bio?.value.orEmpty(), instagramUsername = draft.normalizedInstagramUsername.orEmpty()))
        return if (ignoreCancellation) withContext(NonCancellable) { pending.await() } else pending.await()
    }
}
