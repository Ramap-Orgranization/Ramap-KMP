package com.peto.ramap

import androidx.compose.runtime.Composable
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.navigation.NavigationRouter
import com.peto.ramap.navigation.NavigationSource
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.navigation.TabStatus
import com.peto.ramap.ui.account.AccountSettingsRoute
import com.peto.ramap.ui.account.InformationRoute
import com.peto.ramap.ui.bookmark.importation.ImportationGuideRoute
import com.peto.ramap.ui.bookmark.importation.ImportationRoute
import com.peto.ramap.ui.bookmark.list.BookmarkedShopListRoute
import com.peto.ramap.ui.hidden.HiddenShopListRoute
import com.peto.ramap.ui.main.event.detail.EventDetailRoute
import com.peto.ramap.ui.main.event.list.EventsRoute
import com.peto.ramap.ui.main.map.MapRoute
import com.peto.ramap.ui.main.map.ShopDetailHost
import com.peto.ramap.ui.main.my.MyTabRoute
import com.peto.ramap.ui.main.notice.OperatingNoticeRoute
import com.peto.ramap.ui.main.ranking.RankingRoute
import com.peto.ramap.ui.notification.NotificationSettingsRoute
import com.peto.ramap.ui.profile.edit.ProfileEditRoute
import com.peto.ramap.ui.profile.follow.FollowRoute
import com.peto.ramap.ui.report.PlaceReportRoute
import com.peto.ramap.ui.review.my.MyReviewsRoute
import com.peto.ramap.ui.review.other.OtherReviewsRoute
import com.peto.ramap.ui.review.write.ShopReviewWriteRoute
import com.peto.ramap.ui.settings.SettingsRoute
import com.peto.ramap.ui.subscribed.SubscribedShopListRoute
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_not_found_message

@Composable
internal fun NavigationGraph(
    navigationState: NavigationState,
    toastManager: ToastManager,
    hasActiveOverlay: Boolean,
    openProfile: (userId: String, asOverlay: Boolean) -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    onOpenReviewWriteOverlay: (shopId: String) -> Unit,
    onOpenReviewEditOverlay: (shopId: String, reviewId: String?) -> Unit,
) {
    NavigationRouter(
        navigationState = navigationState,
        mapScreen = { route ->
            MapRoute(
                isUncovered = !hasActiveOverlay,
                onReviewNavigate = { shopId -> onOpenReviewWriteOverlay(shopId) },
                onEditReview = { shopId, reviewId -> onOpenReviewEditOverlay(shopId, reviewId) },
                onOpenProfile = { userId -> openProfile(userId, true) },
                isBackEnabled = route.returnTab == null && !hasActiveOverlay,
                onDetailDismissed = navigationState::consumeMapReturnOrigin,
                onEventNavigate = { event -> navigationState.showEvent(event.id) },
                onOperatingNoticeNavigate = { notice -> navigationState.showShopOnMap(notice.shop.id) },
                requestedShopId = route.shopId,
                showShopDetail = route.showShopDetail,
                showReviewsOnOpen = route.showReviews,
                originSource =
                    when (route.source) {
                        NavigationSource.BOOKMARKED_SHOPS -> AnalyticsSource.BOOKMARKED_SHOPS
                        NavigationSource.EVENT_DETAIL -> AnalyticsSource.EVENT_DETAIL
                        NavigationSource.HIDDEN_SHOPS -> AnalyticsSource.HIDDEN_SHOPS
                        NavigationSource.RANKING -> AnalyticsSource.RANKING
                        NavigationSource.SUBSCRIBED_SHOPS -> AnalyticsSource.SUBSCRIBED_SHOPS
                        NavigationSource.OPERATING_NOTICE -> AnalyticsSource.OPERATING_NOTICE
                        NavigationSource.SHARED_LINK, null -> AnalyticsSource.MAP
                    },
            )
        },
        rankingScreen = {
            RankingRoute(
                onFindShopClick = navigationState::showMap,
                onShowShopOnMap = { shopId ->
                    navigationState.showShopOnMap(
                        shopId = shopId,
                        source = NavigationSource.RANKING,
                        showShopDetail = false,
                    )
                },
                onEventNavigate = { event -> navigationState.showEvent(event.id) },
                shopDetailContent = { shopId, onDismiss, onShowOnMap, onEventNavigate ->
                    ShopDetailHost(
                        isUncovered = !hasActiveOverlay,
                        shopId = shopId,
                        onDismiss = onDismiss,
                        onShowOnMap = onShowOnMap,
                        onReviewNavigate = { shopId -> onOpenReviewWriteOverlay(shopId) },
                        onEditReview = { shopId, reviewId -> onOpenReviewEditOverlay(shopId, reviewId) },
                        onOpenProfile = { userId -> openProfile(userId, true) },
                        onEventNavigate = { event -> onEventNavigate(event) },
                        originSource = AnalyticsSource.RANKING,
                    )
                },
            )
        },
        myScreen = {
            MyTabRoute(
                onProfileNavigate = navigationState::showProfileEdit,
                onFollowNavigate = navigationState::showFollows,
                onMyReviewsNavigate = navigationState::showMyReviews,
                onOpenProfile = { userId -> openProfile(userId, false) },
                onSettingsNavigate = navigationState::showSettings,
                onReportNavigate = navigationState::showPlaceReport,
                onHiddenShopsNavigate = navigationState::showHiddenShops,
                onSubscribedShopsNavigate = navigationState::showSubscribedShops,
                onBookmarkedShopsNavigate = navigationState::showBookmarkedShops,
                onLoginClick = onLoginTypeSelected,
            )
        },
        settingsScreen = {
            SettingsRoute(
                onBack = navigationState::pop,
                onAccountNavigate = navigationState::showAccountSettings,
                onNotificationSettingsNavigate = navigationState::showNotificationSettings,
            )
        },
        accountSettingsScreen = {
            AccountSettingsRoute(
                onBack = navigationState::pop,
            )
        },
        followScreen = {
            FollowRoute(
                isUncovered = !hasActiveOverlay,
                onBack = navigationState::pop,
                onOpenProfile = { userId -> openProfile(userId, true) },
            )
        },
        profileEditScreen = {
            ProfileEditRoute(
                onBack = navigationState::pop,
                onLoginClick = onLoginTypeSelected,
            )
        },
        myReviewsScreen = {
            MyReviewsRoute(
                onBack = navigationState::pop,
                onShowShop = navigationState::showReviewShopOnMap,
                onEditReview = navigationState::showReviewEdit,
            )
        },
        otherReviewsScreen = { route ->
            OtherReviewsRoute(
                userId = route.userId,
                onBack = navigationState::pop,
                onLoginTypeSelected = onLoginTypeSelected,
                onShowShop = navigationState::showReviewShopOnMap,
                onEventNavigate = { event -> navigationState.showEvent(event.id) },
                shopDetailContent = { shopId, onDismiss, onShowOnMap, onEventNavigate ->
                    ShopDetailHost(
                        isUncovered = !hasActiveOverlay,
                        shopId = shopId,
                        onDismiss = onDismiss,
                        onShowOnMap = onShowOnMap,
                        onReviewNavigate = onOpenReviewWriteOverlay,
                        onEditReview = onOpenReviewEditOverlay,
                        onOpenProfile = { userId -> openProfile(userId, true) },
                        onEventNavigate = onEventNavigate,
                        isNavigationBarPadded = true,
                        originSource = AnalyticsSource.REVIEW_PROFILE,
                    )
                },
            )
        },
        reviewWriteScreen = { route ->
            ShopReviewWriteRoute(
                shopId = route.shopId,
                reviewId = route.reviewId,
                onBack = navigationState::pop,
                onSubmitted = {
                    if (route.reviewId == null) {
                        navigationState.showShopOnMap(shopId = route.shopId, showReviews = true)
                    } else {
                        navigationState.pop()
                    }
                },
                onLogin = navigationState::showMyRoot,
            )
        },
        informationScreen = {
            InformationRoute(
                onBack = navigationState::pop,
            )
        },
        placeReportScreen = {
            PlaceReportRoute(
                onBack = navigationState::pop,
            )
        },
        eventListScreen = {
            EventsRoute(
                onClickEvent = { event ->
                    navigationState.showEvent(event.id)
                },
                onClickNotice = navigationState::showOperatingNotice,
            )
        },
        operatingNoticeScreen = {
            OperatingNoticeRoute(
                onBack = navigationState::pop,
                onEventListClick = navigationState::pop,
                onShopClick = { shopId ->
                    navigationState.showShopOnMap(
                        shopId = shopId,
                        source = NavigationSource.OPERATING_NOTICE,
                    )
                },
            )
        },
        hiddenScreen = {
            HiddenShopListRoute(
                onBackClick = navigationState::pop,
                onShopOpen = { shopId ->
                    navigationState.showShopOnMap(
                        shopId,
                        source = NavigationSource.HIDDEN_SHOPS,
                        returnTab = TabStatus.MY,
                        showShopDetail = false,
                    )
                },
            )
        },
        notificationSettingsScreen = {
            NotificationSettingsRoute(
                onBack = navigationState::pop,
            )
        },
        subscribedShopsScreen = {
            SubscribedShopListRoute(
                onBack = navigationState::pop,
                onShopOpen = { shopId ->
                    navigationState.showShopOnMap(
                        shopId,
                        source = NavigationSource.SUBSCRIBED_SHOPS,
                        returnTab = TabStatus.MY,
                        showShopDetail = false,
                    )
                },
                onEventOpen = navigationState::showEvent,
            )
        },
        bookmarkedShopsScreen = {
            BookmarkedShopListRoute(
                onBack = navigationState::pop,
                onImportationNavigate = navigationState::showImportation,
                onShopOpen = { shopId ->
                    navigationState.showShopOnMap(
                        shopId,
                        source = NavigationSource.BOOKMARKED_SHOPS,
                        returnTab = TabStatus.MY,
                        showShopDetail = false,
                    )
                },
            )
        },
        importationScreen = {
            ImportationRoute(
                onBack = navigationState::pop,
                onGuideNavigate = navigationState::showImportationGuide,
                onImportCompleted = navigationState::pop,
            )
        },
        importationGuideScreen = {
            ImportationGuideRoute(onBack = navigationState::pop)
        },
        eventScreen = { route ->
            EventDetailRoute(
                eventId = route.eventId,
                onBack = navigationState::pop,
                onUnavailable = {
                    navigationState.showEventRoot()

                    toastManager.tryShow(
                        ToastData(
                            message = Res.string.event_not_found_message,
                            type = ToastType.DEFAULT,
                        ),
                    )
                },
                onShopClick = { shopId ->
                    navigationState.showShopOnMap(
                        shopId = shopId,
                        source = NavigationSource.EVENT_DETAIL,
                        showShopDetail = false,
                    )
                },
                onEventNavigate = { event -> navigationState.showEvent(event.id) },
                shopDetailContent = { shopId, onDismiss, onShowOnMap, onEventNavigate ->
                    ShopDetailHost(
                        isUncovered = !hasActiveOverlay,
                        shopId = shopId,
                        onDismiss = onDismiss,
                        onReviewNavigate = navigationState::showReviewWrite,
                        onOpenProfile = { userId -> openProfile(userId, false) },
                        onEditReview = navigationState::showReviewEdit,
                        isNavigationBarPadded = true,
                        onShowOnMap = onShowOnMap,
                        onEventNavigate = onEventNavigate,
                        originSource = AnalyticsSource.EVENT_DETAIL,
                    )
                },
            )
        },
    )
}
