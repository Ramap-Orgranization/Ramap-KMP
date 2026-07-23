package com.peto.ramap.ui.subscribed

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSearchResultList
import com.peto.ramap.designsystem.component.ShopListEmptyContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.component.eventDateText
import com.peto.ramap.ui.resource.category.label
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListSideEffect
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListUiState
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.notification_removal_confirm_action
import ramap.shared.generated.resources.notification_removal_confirm_title
import ramap.shared.generated.resources.notification_removal_dismiss_action
import ramap.shared.generated.resources.settings_subscribed_shops_menu
import ramap.shared.generated.resources.subscribed_shops_empty_title
import ramap.shared.generated.resources.top_level_tab_event

@Composable
fun SubscribedShopListRoute(
    onBack: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: SubscribedShopListViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var removalTarget by remember { mutableStateOf<SubscribedRemovalTarget?>(null) }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is SubscribedShopListSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    SubscribedShopListScreen(
        uiState = uiState,
        onBack = onBack,
        onRemovalRequested = { removalTarget = it },
    )
    CommonDialog(
        visible = removalTarget != null,
        confirmText = stringResource(Res.string.notification_removal_confirm_action),
        dismissText = stringResource(Res.string.notification_removal_dismiss_action),
        onDismissRequest = { removalTarget = null },
        content = {
            AppText(
                text = stringResource(Res.string.notification_removal_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = {
            removalTarget?.let { target ->
                viewModel.dispatch(SubscribedShopListIntent.OnRemovalConfirmed(target))
            }
            removalTarget = null
        },
        onDismiss = { removalTarget = null },
    )
}

@Composable
fun SubscribedShopListScreen(
    uiState: SubscribedShopListUiState,
    onBack: () -> Unit,
    onRemovalRequested: (SubscribedRemovalTarget) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CommonTopBar(
            title = stringResource(Res.string.settings_subscribed_shops_menu),
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

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.showError ->
                    LoadErrorContent(
                        image = Res.drawable.laduck_error_confused,
                        title = stringResource(Res.string.settings_subscribed_shops_menu),
                        description = stringResource(Res.string.data_load_failure_message),
                        modifier = Modifier.fillMaxSize(),
                    )

                uiState.isOnlyLoading ->
                    RamenLoadingIndicator(modifier = Modifier.fillMaxSize())

                else -> SubscribedShopListContent(uiState, onRemovalRequested)
            }
        }
    }
}

@Composable
private fun SubscribedShopListContent(
    uiState: SubscribedShopListUiState,
    onRemovalRequested: (SubscribedRemovalTarget) -> Unit,
) {
    if (uiState.shops.isEmpty() && uiState.subscribedEvents.isEmpty()) {
        ShopListEmptyContent(
            title = stringResource(Res.string.subscribed_shops_empty_title),
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            if (uiState.subscribedEvents.isNotEmpty()) {
                SectionCard(
                    title = stringResource(Res.string.top_level_tab_event),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    uiState.subscribedEvents.forEach { event ->
                        EventCard(
                            event = event,
                            dateText = eventDateText(event.startDate, event.endDate),
                            onClick = {
                                onRemovalRequested(SubscribedRemovalTarget.EventOverride(event.id))
                            },
                            modifier =
                                Modifier
                                    .padding(horizontal = 20.dp)
                                    .padding(vertical = 10.dp),
                        )
                    }
                }
            }

            if (uiState.shops.isNotEmpty()) {
                RamenShopSearchResultList(
                    shops = uiState.shops,
                    onShopClick = {
                        onRemovalRequested(SubscribedRemovalTarget.Shop(it.id))
                    },
                    categoryLabel = { category -> stringResource(category.label()) },
                    itemModifier = {
                        Modifier
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                            .border(1.dp, GrayColor.C200, RoundedCornerShape(16.dp))
                    },
                    showDividers = false,
                )
            }
        }

        if (uiState.isOverlayLoading) {
            RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
        }
    }
}
