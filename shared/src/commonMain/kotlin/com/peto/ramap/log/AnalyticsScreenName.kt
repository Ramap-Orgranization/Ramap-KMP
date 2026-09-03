package com.peto.ramap.log

import com.peto.ramap.navigation.ScreenRoutes

/**
 * [ScreenRoutes]를 Firebase Analytics screen_name 문자열로 매핑한다.
 *
 * [com.peto.ramap.AppRoute]의 화면 전환 추적에서 사용한다.
 */
val ScreenRoutes.analyticsScreenName: String
    get() =
        when (this) {
            is ScreenRoutes.MapRoutes -> "map"
            is ScreenRoutes.EventTabRoutes -> "event_list"
            is ScreenRoutes.OperatingNoticeRoutes -> "operating_notice"
            is ScreenRoutes.RankingTabRoutes -> "ranking"
            is ScreenRoutes.MyTabRoutes -> "my"
            is ScreenRoutes.AccountSettingsRoutes -> "account_settings"
            is ScreenRoutes.InformationRoutes -> "information"
            is ScreenRoutes.PlaceReportRoutes -> "place_report"
            is ScreenRoutes.HiddenShopListRoutes -> "hidden_shop_list"
            is ScreenRoutes.NotificationSettingsRoutes -> "notification_settings"
            is ScreenRoutes.SubscribedShopListRoutes -> "subscribed_shop_list"
            is ScreenRoutes.BookmarkedShopListRoutes -> "bookmarked_shop_list"
            is ScreenRoutes.ImportationRoutes -> "importation"
            is ScreenRoutes.ImportationGuideRoutes -> "importation_guide"
            is ScreenRoutes.EventDetailRoutes -> "event_detail"
        }
