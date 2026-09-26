package com.peto.ramap.domain.model.profile

data class ProfileDraft(
    val nickname: ProfileNickname,
    val image: ProfileImage? = null,
    val removePhoto: Boolean = false,
    val bio: ProfileBio? = null,
) {
    val hasConflictingImageChanges: Boolean
        get() = image != null && removePhoto

    val isValid: Boolean
        get() =
            nickname.isValid &&
                (bio == null || bio.isValid) &&
                (image == null || image.isValid()) &&
                !hasConflictingImageChanges

    fun validate() {
        require(nickname.isValid) { "Invalid profile nickname" }
        require(bio == null || bio.isValid) { "Invalid profile bio" }
        require(image == null || image.isValid()) { "Invalid profile image" }
        require(!hasConflictingImageChanges) { "Conflicting profile image changes" }
    }

    companion object {
        fun of(
            nickname: String,
            image: ProfileImage? = null,
            removePhoto: Boolean = false,
            bio: String? = null,
        ): ProfileDraft =
            ProfileDraft(
                nickname = ProfileNickname(nickname),
                image = image,
                removePhoto = removePhoto,
                bio = bio?.let { ProfileBio(it) },
            )
    }
}
