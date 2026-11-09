package com.peto.ramap.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ScreenRoutes : NavKey {
    @Serializable
    data class MapRoutes(
        val shopId: String? = null,
        val returnTab: TabStatus? = null,
        val showShopDetail: Boolean = true,
        val showReviews: Boolean = false,
        val source: NavigationSource? = null,
        val instanceId: Int = 0,
    ) : ScreenRoutes

    @Serializable
    data object ArchiveMapRoutes : ScreenRoutes

    @Serializable
    data object ReviewShopSearchRoutes : ScreenRoutes

    @Serializable
    data class EventTabRoutes(
        val instanceId: Int = 0,
        val noticeId: String? = null,
    ) : ScreenRoutes

    // 이전 버전에서 저장한 새소식 스택을 복원할 때 사용한다.
    @Serializable
    data object OperatingNoticeRoutes : ScreenRoutes

    @Serializable
    data object RankingTabRoutes : ScreenRoutes

    @Serializable
    @SerialName("com.peto.ramap.navigation.ScreenRoutes.CommunityTabRoutes")
    data object FeedTabRoutes : ScreenRoutes

    @Serializable
    data object MyTabRoutes : ScreenRoutes

    @Serializable
    data object SettingsRoutes : ScreenRoutes

    @Serializable
    data object AccountSettingsRoutes : ScreenRoutes

    @Serializable
    data object ProfileEditRoutes : ScreenRoutes

    @Serializable
    data object FollowRoutes : ScreenRoutes

    @Serializable
    data object MyReviewsRoutes : ScreenRoutes

    @Serializable
    data class OtherReviewsRoutes(
        val userId: String,
    ) : ScreenRoutes

    @Serializable
    data class ReviewWriteRoutes(
        val shopId: String,
        val reviewId: String? = null,
    ) : ScreenRoutes

    @Serializable
    data object InformationRoutes : ScreenRoutes

    @Serializable
    data object PlaceReportRoutes : ScreenRoutes

    // 이전 버전에서 저장한 마이 탭 스택을 복원할 때 사용한다.
    @Serializable
    data object HiddenShopListRoutes : ScreenRoutes

    @Serializable
    data object NotificationSettingsRoutes : ScreenRoutes

    @Serializable
    data object SubscribedShopListRoutes : ScreenRoutes

    @Serializable
    data object BookmarkedShopListRoutes : ScreenRoutes

    @Serializable
    data object ImportationRoutes : ScreenRoutes

    @Serializable
    data object ImportationGuideRoutes : ScreenRoutes

    @Serializable
    data class EventDetailRoutes(
        val eventId: String,
    ) : ScreenRoutes
}
