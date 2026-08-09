package com.peto.ramap.ui.subscribed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.peto.ramap.designsystem.component.RamenShopSearchResultList
import com.peto.ramap.designsystem.component.SettingsListPage
import com.peto.ramap.designsystem.component.ShopListCount
import com.peto.ramap.designsystem.component.ShopListEmptyContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.component.eventDateText
import com.peto.ramap.ui.resource.category.CategoryResourceMapper
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListIntent
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListSideEffect
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListUiState
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.notification_removal_confirm_action
import ramap.shared.generated.resources.notification_removal_confirm_title
import ramap.shared.generated.resources.notification_removal_dismiss_action
import ramap.shared.generated.resources.settings_subscribed_shops_menu
import ramap.shared.generated.resources.subscribed_shops_empty_title
import ramap.shared.generated.resources.top_level_tab_event

@Composable
fun SubscribedShopListRoute(
    onBack: () -> Unit,
    onShopOpen: (String) -> Unit,
    onEventOpen: (String) -> Unit,
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
        onShopOpen = { onShopOpen(it.id) },
        onEventOpen = { onEventOpen(it.id) },
        onRemovalRequested = { removalTarget = it },
        onRetry = { viewModel.dispatch(SubscribedShopListIntent.OnRetry) },
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
internal fun SubscribedShopListScreen(
    uiState: SubscribedShopListUiState,
    onBack: () -> Unit,
    onShopOpen: (RamenShop) -> Unit,
    onEventOpen: (ShopEvent) -> Unit,
    onRemovalRequested: (SubscribedRemovalTarget) -> Unit,
    onRetry: () -> Unit,
) {
    SettingsListPage(
        title = Res.string.settings_subscribed_shops_menu,
        onBack = onBack,
        showError = uiState.showError,
        showInitialLoading = uiState.isOnlyLoading,
        showOverlayLoading = uiState.isOverlayLoading,
        errorImage = Res.drawable.laduck_error_confused,
        errorDescription = Res.string.data_load_failure_message,
        onRetry = onRetry,
    ) {
        SubscribedShopListContent(uiState, onShopOpen, onEventOpen, onRemovalRequested)
    }
}

@Composable
private fun SubscribedShopListContent(
    uiState: SubscribedShopListUiState,
    onShopOpen: (RamenShop) -> Unit,
    onEventOpen: (ShopEvent) -> Unit,
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
            ShopListCount(count = uiState.shops.size)
            if (uiState.subscribedEvents.isNotEmpty()) {
                SectionCard(
                    title = stringResource(Res.string.top_level_tab_event),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    uiState.subscribedEvents.forEach { event ->
                        EventCard(
                            event = event,
                            dateText = eventDateText(event.startDate, event.endDate),
                            onClick = { onEventOpen(event) },
                            actionLabel = stringResource(Res.string.notification_removal_confirm_action),
                            onAction = {
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
                    onShopClick = onShopOpen,
                    categoryLabel = { category -> stringResource(CategoryResourceMapper.label(category)) },
                    itemActionLabel = { stringResource(Res.string.notification_removal_confirm_action) },
                    onItemAction = {
                        onRemovalRequested(SubscribedRemovalTarget.Shop(it.id))
                    },
                    itemModifier = {
                        Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    },
                )
            }
        }
    }
}
