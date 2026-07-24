package com.peto.ramap.ui.main.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.rank.RankedShops
import com.peto.ramap.domain.model.rank.ShopRanking
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.ranking.component.AreaSheetContent
import com.peto.ramap.ui.main.ranking.component.RankingFilters
import com.peto.ramap.ui.main.ranking.contract.RankingIntent
import com.peto.ramap.ui.main.ranking.contract.RankingSideEffect
import com.peto.ramap.ui.main.ranking.contract.RankingUiState
import com.peto.ramap.ui.preview.RamenShopPreviewParameterProvider
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmark_removal_confirm_action
import ramap.shared.generated.resources.bookmark_removal_confirm_title
import ramap.shared.generated.resources.notification_removal_dismiss_action

@Composable
fun RankingRoute(
    onShopClick: (String) -> Unit,
    onFindShopClick: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: RankingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLoginGuideDialog by remember { mutableStateOf(false) }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            RankingSideEffect.ShowLoginGuide -> {
                showLoginGuideDialog = true
            }

            is RankingSideEffect.ShowToast -> {
                toastManager.show(sideEffect.data)
            }
        }
    }

    RankingScreen(
        uiState = uiState,
        onShopClick = { shopId ->
            viewModel.dispatch(
                RankingIntent.OnShopClicked(shopId),
            )
            onShopClick(shopId)
        },
        onFindShopClick = onFindShopClick,
        onRefresh = {
            viewModel.dispatch(RankingIntent.OnRefreshed)
        },
        onRetry = {
            viewModel.dispatch(RankingIntent.OnRetried)
        },
        onLoadNext = {
            viewModel.dispatch(RankingIntent.OnNextPageRequested)
        },
        onRetryNext = {
            viewModel.dispatch(RankingIntent.OnNextPageRetried)
        },
        onAreaFilterSelected = { areaFilter ->
            viewModel.dispatch(
                RankingIntent.OnAreaFilterSelected(areaFilter),
            )
        },
        onCategoryToggled = { category ->
            viewModel.dispatch(
                RankingIntent.OnCategoryToggled(category),
            )
        },
        onAllCategoriesSelected = {
            viewModel.dispatch(
                RankingIntent.OnAllCategoriesSelected,
            )
        },
        onBookmarkChange = { shopId, enabled ->
            viewModel.dispatch(
                RankingIntent.OnBookmarkChanged(
                    shopId = shopId,
                    enabled = enabled,
                ),
            )
        },
    )

    LoginGuideDialog(
        visible = showLoginGuideDialog,
        onDismiss = {
            showLoginGuideDialog = false
        },
        onConfirm = {
            showLoginGuideDialog = false
            viewModel.dispatch(RankingIntent.OnKakaoLoginClicked)
        },
    )
}

@Composable
internal fun RankingScreen(
    uiState: RankingUiState,
    onShopClick: (String) -> Unit,
    onFindShopClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadNext: () -> Unit,
    onRetryNext: () -> Unit,
    onAreaFilterSelected: (AreaFilter) -> Unit,
    onCategoryToggled: (Category) -> Unit,
    onAllCategoriesSelected: () -> Unit,
    onBookmarkChange: (String, Boolean) -> Unit,
) {
    var isAreaSheetVisible by remember {
        mutableStateOf(false)
    }

    var removalTargetShopId by remember {
        mutableStateOf<String?>(null)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
        ) {
            AppText(
                text = "전국 인기 라멘 랭킹",
                style = AppTextStyle.L1,
                color = GrayColor.C500,
                modifier =
                    Modifier
                        .padding(
                            top = 20.dp,
                            bottom = 10.dp,
                        ).padding(start = 15.dp),
            )

            RankingFilters(
                uiState = uiState,
                onAreaClick = {
                    isAreaSheetVisible = true
                },
                onCategoryToggled = onCategoryToggled,
                onAllCategoriesSelected = onAllCategoriesSelected,
            )

            RankingContent(
                uiState = uiState,
                onShopClick = onShopClick,
                onFindShopClick = onFindShopClick,
                onRefresh = onRefresh,
                onRetry = onRetry,
                onLoadNext = onLoadNext,
                onRetryNext = onRetryNext,
                onBookmarkClick = { shopId, isBookmarked ->
                    if (isBookmarked) {
                        removalTargetShopId = shopId
                    } else {
                        onBookmarkChange(
                            shopId,
                            true,
                        )
                    }
                },
            )
        }

        CommonBottomSheet(
            visible = isAreaSheetVisible,
            onDismissRequest = {
                isAreaSheetVisible = false
            },
        ) {
            AreaSheetContent(
                areaFilter = uiState.areaFilter,
                onAreaFilterSelected = { areaFilter ->
                    onAreaFilterSelected(areaFilter)
                    isAreaSheetVisible = false
                },
            )
        }

        CommonDialog(
            visible = removalTargetShopId != null,
            confirmText =
                stringResource(
                    Res.string.bookmark_removal_confirm_action,
                ),
            dismissText =
                stringResource(
                    Res.string.notification_removal_dismiss_action,
                ),
            onDismissRequest = {
                removalTargetShopId = null
            },
            content = {
                AppText(
                    text =
                        stringResource(
                            Res.string.bookmark_removal_confirm_title,
                        ),
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                    textAlign = TextAlign.Center,
                )
            },
            onConfirm = {
                val shopId = removalTargetShopId

                if (shopId != null) {
                    onBookmarkChange(
                        shopId,
                        false,
                    )
                }

                removalTargetShopId = null
            },
            onDismiss = {
                removalTargetShopId = null
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingRoutePreview() {
    val shops =
        RamenShopPreviewParameterProvider()
            .ramenShopPreviewSamples

    val rankings =
        shops.mapIndexed { index, shop ->
            ShopRanking(
                shop = shop,
                likeCount = 128L - (index * 33L),
            )
        }

    RamapTheme {
        RankingScreen(
            uiState =
                RankingUiState(
                    shops =
                        RankedShops(
                            ShopRankings(rankings),
                        ),
                    bookmarkedShopIds =
                        setOf(shops.first().id),
                ),
            onShopClick = {},
            onFindShopClick = {},
            onRefresh = {},
            onRetry = {},
            onLoadNext = {},
            onRetryNext = {},
            onAreaFilterSelected = {},
            onCategoryToggled = {},
            onAllCategoriesSelected = {},
            onBookmarkChange = { _, _ -> },
        )
    }
}
