package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileDraft
import com.peto.ramap.domain.model.profile.ProfileVisibility
import com.peto.ramap.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeProfileRepository(
    userId: String? = "me",
    var isProfilePublic: Boolean = true,
    var followersCanReadReviews: Boolean = false,
) : ProfileRepository {
    override val sessionUserIds = MutableStateFlow(userId)

    override suspend fun fetchMyProfile() = RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), "라멘러", isPublic = isProfilePublic, followersCanReadReviews = followersCanReadReviews))

    override suspend fun isNicknameAvailable(nickname: String) = RamapResult.Success(true)

    override suspend fun updateMyProfile(draft: ProfileDraft) = fetchMyProfile()

    override suspend fun updateProfileVisibility(visibility: ProfileVisibility): RamapResult<AccountProfile> = updateProfileVisibility(visibility.isPublic)

    override suspend fun updateProfileVisibility(isPublic: Boolean): RamapResult<AccountProfile> {
        isProfilePublic = isPublic
        return RamapResult.Success(AccountProfile(sessionUserIds.value.orEmpty(), "라멘러", isPublic = isPublic))
    }
}
