package com.peto.ramap.ui.review.other.contract

import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.loading.LoadState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtherReviewsUiStateTest {
    private val sampleProfile = PublicProfile(userId = "user-1", nickname = "라멘마니아")
    private val sampleReview =
        Review(
            id = "review-1",
            shopId = "shop-1",
            body = "맛있는 라멘",
            createdAt = "2026-03-31T00:00:00Z",
            author = ReviewAuthor(userId = "user-1", nickname = "라멘마니아"),
        )

    @Test
    fun `showsProfileHeader is true when profile is present`() {
        val state = OtherReviewsUiState(profileAccess = ProfileAccess.Visible(sampleProfile))

        assertTrue(state.showsProfileHeader)
    }

    @Test
    fun `block action is available to guests and other users but not the profile owner`() {
        val guest = OtherReviewsUiState(profileAccess = ProfileAccess.Visible(sampleProfile))
        val signedIn = guest.copy(currentUserId = "other-user")
        val owner = guest.copy(currentUserId = sampleProfile.userId)
        val unavailable = guest.copy(profileAccess = ProfileAccess.Private)

        assertTrue(guest.showsBlockAction)
        assertTrue(signedIn.showsBlockAction)
        assertFalse(owner.showsBlockAction)
        assertFalse(unavailable.showsBlockAction)
    }

    @Test
    fun `showsSummaryHeader is true when profile is present not blocked and reviews exist`() {
        val validState = OtherReviewsUiState(profileAccess = ProfileAccess.Visible(sampleProfile), reviews = listOf(sampleReview))
        val emptyReviewsState = OtherReviewsUiState(profileAccess = ProfileAccess.Visible(sampleProfile), reviews = emptyList())
        val blockedState = OtherReviewsUiState(profileAccess = ProfileAccess.Blocked(sampleProfile), reviews = listOf(sampleReview))

        assertTrue(validState.showsSummaryHeader)
        assertFalse(emptyReviewsState.showsSummaryHeader)
        assertFalse(blockedState.showsSummaryHeader)
    }

    @Test
    fun `showsInitialLoading is true when page is loading and reviews are empty`() {
        val state =
            OtherReviewsUiState(
                loadState = LoadState.loading(OtherReviewsLoadKey.Page),
                reviews = emptyList(),
            )

        assertTrue(state.showsInitialLoading)
        assertFalse(state.showsAppendLoading)
    }

    @Test
    fun `showsAppendLoading is true when page is loading and reviews exist`() {
        val state =
            OtherReviewsUiState(
                loadState = LoadState.loading(OtherReviewsLoadKey.Page),
                reviews = listOf(sampleReview),
            )

        assertTrue(state.showsAppendLoading)
        assertFalse(state.showsInitialLoading)
    }

    @Test
    fun `showsUnavailableProfile is true when loaded without failure and profile is null`() {
        val state =
            OtherReviewsUiState(
                failed = false,
                profileAccess = ProfileAccess.Unavailable,
            )

        assertTrue(state.showsUnavailableProfile)
    }

    @Test
    fun `showsBlockedContent is true when loaded without failure and user is blocked`() {
        val state =
            OtherReviewsUiState(
                failed = false,
                profileAccess = ProfileAccess.Blocked(sampleProfile),
            )

        assertTrue(state.showsBlockedContent)
    }

    @Test
    fun `showsEmptyReviews is true when loaded without failure with profile not blocked and reviews empty`() {
        val state =
            OtherReviewsUiState(
                failed = false,
                profileAccess = ProfileAccess.Visible(sampleProfile),
                reviews = emptyList(),
            )

        assertTrue(state.showsEmptyReviews)
    }
}
