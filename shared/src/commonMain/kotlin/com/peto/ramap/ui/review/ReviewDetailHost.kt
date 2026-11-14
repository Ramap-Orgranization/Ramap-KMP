package com.peto.ramap.ui.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.ShareLauncher
import com.peto.ramap.ui.review.detail.ReviewDetailRoute
import com.peto.ramap.ui.shop.ShopDetailHost
import org.koin.compose.koinInject

@Composable
internal fun ReviewDetailHost(
    reviewId: String,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onShowShopOnMap: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    onWriteReview: (String) -> Unit,
    onEditShopReview: (String, String) -> Unit,
    onOpenReviewDetail: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    isUncovered: Boolean,
) {
    val shareLinks = koinInject<ShopShareLinkFactory>()
    var selectedShopId by rememberSaveable(reviewId) { mutableStateOf<String?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        ReviewDetailRoute(
            reviewId = reviewId,
            onBack = onBack,
            onOpenProfile = onOpenProfile,
            onOpenShop = { selectedShopId = it },
            onEditReview = onEditReview,
            onLoginTypeSelected = onLoginTypeSelected,
            onShareReview = { shopId -> ShareLauncher.share(shareLinks.createReview(shopId, reviewId)) },
        )
        selectedShopId?.let { shopId ->
            ShopDetailHost(
                shopId = shopId,
                onDismiss = { selectedShopId = null },
                onShowOnMap = {
                    selectedShopId = null
                    onShowShopOnMap(it)
                },
                onReviewNavigate = onWriteReview,
                onOpenProfile = onOpenProfile,
                onEditReview = onEditShopReview,
                onEventNavigate = {
                    selectedShopId = null
                    onOpenEvent(it.id)
                },
                onOpenReviewDetail = {
                    if (it == reviewId) {
                        selectedShopId = null
                    } else {
                        onOpenReviewDetail(it)
                    }
                },
                originSource = AnalyticsSource.REVIEW_PROFILE,
                isNavigationBarPadded = true,
                isUncovered = isUncovered,
                isBackEnabled = isUncovered,
            )
        }
    }
}
