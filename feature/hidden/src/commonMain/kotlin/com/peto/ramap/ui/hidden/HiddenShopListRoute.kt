package com.peto.ramap.ui.hidden

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.LaduckLoadingContent
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSearchResultList
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.extension.stringResource
import com.peto.ramap.ui.hidden.contract.HiddenShopListIntent
import com.peto.ramap.ui.hidden.contract.HiddenShopListUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.hidden_shops_empty_title
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.top_level_tab_my

@Composable
fun HiddenShopListRoute(
    onBackClick: () -> Unit,
    onShopClick: (String) -> Unit,
    viewModel: HiddenShopListViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    HiddenShopListScreen(
        uiState = uiState,
        onBack = onBackClick,
        onShopClick = { onShopClick(it.id) },
        onRetryClick = { viewModel.dispatch(HiddenShopListIntent.OnHiddenShopListRetried) },
    )
}

@Composable
fun HiddenShopListScreen(
    uiState: HiddenShopListUiState,
    onBack: () -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onRetryClick: () -> Unit,
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
                    onRetry = onRetryClick,
                )

            is LoadState.Content -> {
                val shops = shopsState.data
                if (shops.isEmpty()) {
                    HiddenShopEmptyContent(onBack = onBack)
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

@Composable
fun HiddenShopEmptyContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.laduck_error_crying),
            contentDescription = null,
            modifier = Modifier.size(220.dp).align(androidx.compose.ui.Alignment.CenterHorizontally),
        )
        AppText(
            text = stringResource(Res.string.hidden_shops_empty_title),
            style = AppTextStyle.H3,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        AppButton(
            text = stringResource(Res.string.top_level_tab_my),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            onClick = onBack,
        )
    }
}
