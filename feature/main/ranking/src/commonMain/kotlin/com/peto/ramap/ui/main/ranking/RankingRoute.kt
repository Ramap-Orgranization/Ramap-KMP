package com.peto.ramap.ui.main.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.peto.ramap.designsystem.component.LoginButton
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.domain.model.rank.RankedShops
import com.peto.ramap.domain.model.rank.ShopRanking
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
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
    val listState = rememberLazyListState()

    var showLoginGuideDialog by remember {
        mutableStateOf(false)
    }
    val loginTypes = supportedLoginTypes()

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            RankingSideEffect.ShowLoginGuide -> {
                if (loginTypes.size == 1) {
                    viewModel.dispatch(RankingIntent.OnLoginTypeSelected(loginTypes.single()))
                } else {
                    showLoginGuideDialog = true
                }
            }

            is RankingSideEffect.ShowToast -> {
                toastManager.show(sideEffect.data)
            }
        }
    }

    RankingScreen(
        uiState = uiState,
        listState = listState,
        onShopClick = { shop ->
            viewModel.dispatch(
                RankingIntent.OnShopClicked(
                    shop = shop,
                ),
            )

            onShopClick(shop.id)
        },
        onFindShopClick = onFindShopClick,
        onRefresh = {
            viewModel.dispatch(
                RankingIntent.OnRefreshed,
            )
        },
        onRetry = {
            viewModel.dispatch(
                RankingIntent.OnRefreshed,
            )
        },
        onLoadNext = {
            viewModel.dispatch(
                RankingIntent.OnNextPageRequested,
            )
        },
        onRetryNext = {
            viewModel.dispatch(
                RankingIntent.OnNextPageRetried,
            )
        },
        onAreaFilterSelected = { areaFilter ->
            viewModel.dispatch(
                RankingIntent.OnAreaFilterSelected(
                    areaFilter = areaFilter,
                ),
            )
        },
        onAreaSheetOpened = {
            viewModel.dispatch(RankingIntent.OnAreaSheetOpened)
        },
        onAdministrativeAreaSelected = { area ->
            viewModel.dispatch(
                RankingIntent.OnAdministrativeAreaSelected(area),
            )
        },
        onAreaSelectionBack = {
            viewModel.dispatch(RankingIntent.OnAreaSelectionBack)
        },
        onCategoryToggled = { category ->
            viewModel.dispatch(
                RankingIntent.OnCategoryToggled(
                    category = category,
                ),
            )
        },
        onAllCategoriesSelected = {
            viewModel.dispatch(
                RankingIntent.OnAllCategoriesSelected,
            )
        },
        onBookmarkChange = { shop, enabled ->
            viewModel.dispatch(
                RankingIntent.OnBookmarkChanged(
                    shop = shop,
                    enabled = enabled,
                ),
            )
        },
    )

    LoginGuideDialog(
        visible = showLoginGuideDialog,
        onDismiss = {
            showLoginGuideDialog = false
            viewModel.dispatch(RankingIntent.OnLoginSelectionDismissed)
        },
        onLoginTypeSelected = { type ->
            showLoginGuideDialog = false
            viewModel.dispatch(RankingIntent.OnLoginTypeSelected(type))
        },
        loginButton = { type, onClick -> LoginButton(type, onClick) },
    )
}

@Composable
internal fun RankingScreen(
    uiState: RankingUiState,
    listState: LazyListState,
    onShopClick: (RamenShop) -> Unit,
    onFindShopClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadNext: () -> Unit,
    onRetryNext: () -> Unit,
    onAreaFilterSelected: (AreaFilter) -> Unit,
    onAreaSheetOpened: () -> Unit,
    onAdministrativeAreaSelected: (AdministrativeArea) -> Unit,
    onAreaSelectionBack: () -> Unit,
    onCategoryToggled: (Category) -> Unit,
    onAllCategoriesSelected: () -> Unit,
    onBookmarkChange: (RamenShop, Boolean) -> Unit,
) {
    var isAreaSheetVisible by remember {
        mutableStateOf(false)
    }

    var removalTargetShop by remember {
        mutableStateOf<RamenShop?>(null)
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
            RankingFilters(
                uiState = uiState,
                onAreaClick = {
                    onAreaSheetOpened()
                    isAreaSheetVisible = true
                },
                onCategoryToggled = onCategoryToggled,
                onAllCategoriesSelected = onAllCategoriesSelected,
                modifier = Modifier.padding(top = 20.dp),
            )

            RankingContent(
                uiState = uiState,
                listState = listState,
                onShopClick = { shop ->
                    isAreaSheetVisible = false
                    onShopClick(shop)
                },
                onFindShopClick = onFindShopClick,
                onRefresh = onRefresh,
                onRetry = onRetry,
                onLoadNext = onLoadNext,
                onRetryNext = onRetryNext,
                onBookmarkClick = { shop, isBookmarked ->
                    if (isBookmarked) {
                        removalTargetShop = shop
                    } else {
                        onBookmarkChange(
                            shop,
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
                onAreaSelectionBack()
            },
        ) {
            AreaSheetContent(
                areaFilter = uiState.areaFilter,
                areaSelectionArea = uiState.areaSelectionArea,
                administrativeDistricts = uiState.administrativeDistricts,
                isLoadingDistricts = uiState.isLoadingDistricts,
                onAdministrativeAreaSelected = { area ->
                    onAdministrativeAreaSelected(area)
                    if (area == AdministrativeArea.SEJONG) {
                        isAreaSheetVisible = false
                        onAreaSelectionBack()
                    }
                },
                onAreaFilterSelected = { areaFilter ->
                    onAreaFilterSelected(areaFilter)
                    isAreaSheetVisible = false
                    onAreaSelectionBack()
                },
            )
        }

        CommonDialog(
            visible = removalTargetShop != null,
            confirmText =
                stringResource(
                    Res.string.bookmark_removal_confirm_action,
                ),
            dismissText =
                stringResource(
                    Res.string.notification_removal_dismiss_action,
                ),
            onDismissRequest = {
                removalTargetShop = null
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
                val shop = removalTargetShop

                if (shop != null) {
                    onBookmarkChange(
                        shop,
                        false,
                    )
                }

                removalTargetShop = null
            },
            onDismiss = {
                removalTargetShop = null
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingRoutePreview() {
    val shops = RamenShopPreviewParameterProvider().ramenShopPreviewSamples

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
            listState = rememberLazyListState(),
            onFindShopClick = {},
            onRefresh = {},
            onRetry = {},
            onLoadNext = {},
            onRetryNext = {},
            onAreaFilterSelected = {},
            onAreaSheetOpened = {},
            onAdministrativeAreaSelected = {},
            onAreaSelectionBack = {},
            onCategoryToggled = {},
            onAllCategoriesSelected = {},
            onBookmarkChange = { _, _ -> },
        )
    }
}
