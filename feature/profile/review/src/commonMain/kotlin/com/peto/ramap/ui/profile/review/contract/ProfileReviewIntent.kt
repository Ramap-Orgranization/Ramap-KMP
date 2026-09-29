package com.peto.ramap.ui.profile.review.contract

import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.base.Intent

sealed interface ProfileReviewIntent : Intent {
    data class OpenTarget(
        val target: ProfileReviewTarget,
    ) : ProfileReviewIntent

    data object Load : ProfileReviewIntent

    data object LoadMore : ProfileReviewIntent

    data class ReportReview(
        val review: Review,
    ) : ProfileReviewIntent

    data class ToggleLike(
        val review: Review,
    ) : ProfileReviewIntent

    data class ConfirmDelete(
        val review: Review,
    ) : ProfileReviewIntent

    data object DeleteReview : ProfileReviewIntent

    data object ReportProfile : ProfileReviewIntent

    data class SendReport(
        val reason: ReportReason,
        val details: String,
    ) : ProfileReviewIntent

    data class ConfirmBlock(
        val profile: PublicProfile,
    ) : ProfileReviewIntent

    data object ToggleBlock : ProfileReviewIntent

    data object DismissAction : ProfileReviewIntent
}
