package com.peto.ramap.ui.main.my.profile

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.domain.repository.ProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class FakeProfileRepository : ProfileRepository {
    override val sessionUserIds = MutableStateFlow<String?>("first")
    var saveResult: CompletableDeferred<RamapResult<AccountProfile>>? = null
    var fetchResult: RamapResult<AccountProfile>? = null
    var ignoreCancellation = false
    var saveCalls = 0

    override suspend fun fetchMyProfile(): RamapResult<AccountProfile> = fetchResult ?: RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), "느긋한차슈"))

    override suspend fun updateMyProfile(
        nickname: String,
        image: ProfileImage?,
        removePhoto: Boolean,
        bio: String?,
        instagramUsername: String?,
    ): RamapResult<AccountProfile> {
        saveCalls++
        val pending = saveResult
        if (pending == null) return RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), nickname, bio = bio.orEmpty(), instagramUsername = instagramUsername.orEmpty()))
        return if (ignoreCancellation) withContext(NonCancellable) { pending.await() } else pending.await()
    }
}
