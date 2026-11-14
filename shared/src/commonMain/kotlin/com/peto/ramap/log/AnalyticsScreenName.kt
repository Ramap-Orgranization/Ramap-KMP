package com.peto.ramap.log

import com.peto.ramap.navigation.NewsTab
import com.peto.ramap.navigation.ScreenRoutes

/**
 * [ScreenRoutes]를 Firebase Analytics screen_name 문자열로 매핑한다.
 *
 * [com.peto.ramap.AppRoute]의 화면 전환 추적에서 사용한다.
 */
internal fun ScreenRoutes.analyticsScreenName(newsTab: NewsTab): String =
    when (this) {
        is ScreenRoutes.MapRoutes -> "map"
        ScreenRoutes.ArchiveMapRoutes -> "archive_map"
        ScreenRoutes.ReviewShopSearchRoutes -> "review_shop_search"
        is ScreenRoutes.EventTabRoutes ->
            when (newsTab) {
                NewsTab.EVENTS -> "event_list"
                NewsTab.OPERATING_NOTICES -> "operating_notice"
                NewsTab.NEW_SHOPS -> "new_shops"
            }
        ScreenRoutes.OperatingNoticeRoutes -> "operating_notice"
        is ScreenRoutes.RankingTabRoutes -> "ranking"
        is ScreenRoutes.FeedTabRoutes -> "feed"
        ScreenRoutes.MyTabRoutes, ScreenRoutes.HiddenShopListRoutes -> "my"
        is ScreenRoutes.SettingsRoutes -> "settings"
        is ScreenRoutes.AccountSettingsRoutes -> "account_settings"
        ScreenRoutes.AnnouncementRoutes -> "announcements"
        is ScreenRoutes.FollowRoutes -> "my_follows"
        is ScreenRoutes.ProfileEditRoutes -> "my_profile"
        ScreenRoutes.MyReviewsRoutes -> "my_reviews"
        is ScreenRoutes.OtherReviewsRoutes -> "other_reviews"
        is ScreenRoutes.ReviewDetailRoutes -> "review_detail"
        is ScreenRoutes.ReviewWriteRoutes -> "review_write"
        is ScreenRoutes.InformationRoutes -> "information"
        is ScreenRoutes.PlaceReportRoutes -> "place_report"
        is ScreenRoutes.NotificationSettingsRoutes -> "notification_settings"
        is ScreenRoutes.SubscribedShopListRoutes -> "subscribed_shop_list"
        is ScreenRoutes.BookmarkedShopListRoutes -> "bookmarked_shop_list"
        is ScreenRoutes.ImportationRoutes -> "importation"
        is ScreenRoutes.ImportationGuideRoutes -> "importation_guide"
        is ScreenRoutes.EventDetailRoutes -> "event_detail"
    }
