package com.peto.ramap.ui.hidden

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.LaduckLoadingContent
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSearchResultList
import com.peto.ramap.designsystem.component.ShopListEmptyContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.extension.stringResource
import com.peto.ramap.ui.hidden.contract.HiddenShopListIntent
import com.peto.ramap.ui.hidden.contract.HiddenShopListSideEffect
import com.peto.ramap.ui.hidden.contract.HiddenShopListUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.hidden_shops_empty_title
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.notification_removal_dismiss_action
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.unhide_shop_confirm_action
import ramap.shared.generated.resources.unhide_shop_confirm_title

@Composable
fun HiddenShopListRoute(
    onBackClick: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: HiddenShopListViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var unhideTargetShopId by remember { mutableStateOf<String?>(null) }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is HiddenShopListSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    HiddenShopListScreen(
        uiState = uiState,
        onBack = onBackClick,
        onShopClick = { unhideTargetShopId = it.id },
    )
    CommonDialog(
        visible = unhideTargetShopId != null,
        confirmText = stringResource(Res.string.unhide_shop_confirm_action),
        dismissText = stringResource(Res.string.notification_removal_dismiss_action),
        onDismissRequest = { unhideTargetShopId = null },
        content = {
            AppText(
                text = stringResource(Res.string.unhide_shop_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = {
            unhideTargetShopId?.let { shopId ->
                viewModel.dispatch(HiddenShopListIntent.OnUnhideConfirmed(shopId))
            }
            unhideTargetShopId = null
        },
        onDismiss = { unhideTargetShopId = null },
    )
}

@Composable
fun HiddenShopListScreen(
    uiState: HiddenShopListUiState,
    onBack: () -> Unit,
    onShopClick: (RamenShop) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                ).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CommonTopBar(
            title = stringResource(Res.string.settings_hidden_shops_menu),
            left = {
                Image(
                    painter = painterResource(Res.drawable.ic_arrow3_left),
                    contentDescription = stringResource(Res.string.navigation_back),
                    modifier =
                        Modifier
                            .padding(18.dp)
                            .size(24.dp)
                            .noRippleClickable(onClick = onBack),
                )
            },
        )

        when (val shopsState = uiState.shopsState) {
            LoadState.Idle, LoadState.Loading -> LaduckLoadingContent()
            LoadState.Error ->
                LoadErrorContent(
                    image = Res.drawable.laduck_error_confused,
                    title = stringResource(Res.string.settings_hidden_shops_menu),
                    description = stringResource(Res.string.data_load_failure_message),
                    modifier = Modifier.fillMaxSize(),
                )

            is LoadState.Content -> {
                val shops = shopsState.data
                if (shops.isEmpty()) {
                    ShopListEmptyContent(
                        title = stringResource(Res.string.hidden_shops_empty_title),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    RamenShopSearchResultList(
                        shops = shops,
                        onShopClick = onShopClick,
                        categoryLabel = { category -> stringResource(category.stringResource) },
                        itemModifier = {
                            Modifier
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                                .border(1.dp, GrayColor.C200, RoundedCornerShape(16.dp))
                        },
                    )
                }
            }
        }
    }
}
