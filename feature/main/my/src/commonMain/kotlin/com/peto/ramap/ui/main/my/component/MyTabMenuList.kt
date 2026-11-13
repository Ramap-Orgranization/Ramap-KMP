package com.peto.ramap.ui.main.my.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SurfaceColor.Background
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.announcement_title
import ramap.shared.generated.resources.follow_manage
import ramap.shared.generated.resources.follow_new
import ramap.shared.generated.resources.follow_requests
import ramap.shared.generated.resources.ic_news
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_person
import ramap.shared.generated.resources.ic_profile_blocked
import ramap.shared.generated.resources.ic_profile_report
import ramap.shared.generated.resources.ic_review
import ramap.shared.generated.resources.ic_star_filled
import ramap.shared.generated.resources.my_reviews
import ramap.shared.generated.resources.review_blocked_users
import ramap.shared.generated.resources.settings_bookmarked_shops_menu
import ramap.shared.generated.resources.settings_report_menu
import ramap.shared.generated.resources.settings_subscribed_shops_menu

@Composable
internal fun MyTabMenuList(
    isGuest: Boolean,
    hasUnreadAnnouncements: Boolean,
    hasPendingFollowRequests: Boolean,
    hasUnreadFollowConnections: Boolean,
    reviewCount: Int?,
    loadingReviewCount: Boolean,
    blockedUserCount: Int?,
    loadingBlockedUserCount: Boolean,
    bookmarkedCount: Int?,
    notificationCount: Int?,
    isLoading: Boolean,
    onAnnouncementsClick: () -> Unit,
    onFollowClick: () -> Unit,
    onMyReviewsClick: () -> Unit,
    onBlockedUsersClick: () -> Unit,
    onBookmarkedShopsClick: () -> Unit,
    onSubscribedShopsClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Background),
    ) {
        MyMenuRow(
            icon = Res.drawable.ic_news,
            title = Res.string.announcement_title,
            iconBackground = ChromaticColor.Blue050,
            iconTint = ChromaticColor.Blue400,
            hasCount = false,
            isNew = hasUnreadAnnouncements,
            onClick = onAnnouncementsClick,
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = GrayColor.C100,
        )
        if (!isGuest) {
            MyMenuRow(
                icon = Res.drawable.ic_person,
                title = Res.string.follow_manage,
                count = null,
                iconBackground = ChromaticColor.Green050,
                iconTint = ChromaticColor.Green500,
                hasCount = false,
                notification = if (hasPendingFollowRequests) Res.string.follow_requests else null,
                isNew = hasUnreadFollowConnections,
                newBadgeDescription = Res.string.follow_new,
                onClick = onFollowClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 1.dp,
                color = GrayColor.C100,
            )
            MyMenuRow(
                icon = Res.drawable.ic_review,
                title = Res.string.my_reviews,
                count = reviewCount,
                iconBackground = ChromaticColor.Purple050,
                iconTint = ChromaticColor.Purple300,
                isLoading = loadingReviewCount,
                onClick = onMyReviewsClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 1.dp,
                color = GrayColor.C100,
            )
            MyMenuRow(
                icon = Res.drawable.ic_profile_blocked,
                title = Res.string.review_blocked_users,
                count = blockedUserCount,
                iconBackground = GrayColor.C050,
                iconTint = GrayColor.C300,
                isLoading = loadingBlockedUserCount,
                onClick = onBlockedUsersClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 1.dp,
                color = GrayColor.C100,
            )
        }
        MyMenuRow(
            icon = Res.drawable.ic_star_filled,
            title = Res.string.settings_bookmarked_shops_menu,
            count = bookmarkedCount,
            iconBackground = BrandColor.SubtleFill,
            iconTint = BrandColor.SecondPrimary,
            isLoading = isLoading,
            onClick = onBookmarkedShopsClick,
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 1.dp,
            color = GrayColor.C100,
        )
        if (!isGuest) {
            MyMenuRow(
                icon = Res.drawable.ic_notification,
                title = Res.string.settings_subscribed_shops_menu,
                count = notificationCount,
                iconBackground = ChromaticColor.Magenta050,
                iconTint = ChromaticColor.Magenta500,
                isLoading = isLoading,
                onClick = onSubscribedShopsClick,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 1.dp,
                color = GrayColor.C100,
            )
        }
        MyMenuRow(
            icon = Res.drawable.ic_profile_report,
            title = Res.string.settings_report_menu,
            count = null,
            iconBackground = ChromaticColor.Red050,
            iconTint = ChromaticColor.Red300,
            hasCount = false,
            onClick = onReportClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MyTabMenuListPreview() {
    RamapTheme {
        MyTabMenuList(
            isGuest = false,
            hasUnreadAnnouncements = true,
            hasPendingFollowRequests = true,
            hasUnreadFollowConnections = false,
            reviewCount = 5,
            loadingReviewCount = false,
            blockedUserCount = 1,
            loadingBlockedUserCount = false,
            bookmarkedCount = 12,
            notificationCount = 3,
            isLoading = false,
            onAnnouncementsClick = {},
            onFollowClick = {},
            onMyReviewsClick = {},
            onBlockedUsersClick = {},
            onBookmarkedShopsClick = {},
            onSubscribedShopsClick = {},
            onReportClick = {},
        )
    }
}
