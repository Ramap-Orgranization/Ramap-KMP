package com.peto.ramap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.ui.review.other.OtherReviewsRoute
import com.peto.ramap.ui.review.write.ShopReviewWriteRoute

@Composable
internal fun OverlayHost(
    overlayState: OverlayState,
    navigationState: NavigationState,
    onLoginTypeSelected: (LoginType) -> Unit,
) {
    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )

    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = overlayState.isOverlayActive,
        onBackCompleted = {
            if (overlayState.overlayProfileUserId != null) {
                overlayState.overlayProfileUserId = null
            } else if (overlayState.overlayReviewWriteArgs != null) {
                overlayState.overlayReviewWriteArgs = null
            }
        },
    )

    if (overlayState.overlayProfileUserId != null) {
        FullScreenOverlay {
            OtherReviewsRoute(
                userId = overlayState.overlayProfileUserId!!,
                onBack = { overlayState.overlayProfileUserId = null },
                onLoginTypeSelected = onLoginTypeSelected,
                onShowShop = { shopId ->
                    overlayState.overlayProfileUserId = null
                    navigationState.showShopOnMap(shopId)
                },
            )
        }
    }

    if (overlayState.overlayReviewWriteArgs != null) {
        val (shopId, reviewId) = overlayState.overlayReviewWriteArgs!!
        FullScreenOverlay {
            ShopReviewWriteRoute(
                shopId = shopId,
                reviewId = reviewId,
                onBack = { overlayState.overlayReviewWriteArgs = null },
                onSubmitted = {
                    overlayState.overlayReviewWriteArgs = null
                },
                onLogin = navigationState::showMyRoot,
            )
        }
    }
}

@Composable
private fun FullScreenOverlay(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White),
    ) {
        content()
    }
}
