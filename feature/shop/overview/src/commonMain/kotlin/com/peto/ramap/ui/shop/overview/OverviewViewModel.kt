package com.peto.ramap.ui.shop.overview

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.personalization.BookmarkChangeStatus
import com.peto.ramap.domain.model.personalization.NotificationStatus
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.FetchShopOverviewUseCase
import com.peto.ramap.domain.usecase.ShopOverviewCacheLookup
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.platform.storage.SearchHistoryStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect.ShareShop
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect.ShowLoginGuide
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect.ShowToast
import com.peto.ramap.ui.shop.model.ShopOverviewUiState
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnBookmarkToggled
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnDismissed
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnHiddenToggled
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnLoginTypeSelected
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnOverviewRetry
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnRequestedShopDismissed
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopIdSelected
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopMapLinkClicked
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopNotificationToggled
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopReportSubmitted
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopSelected
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopShareClicked
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewLoadKey
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewSideEffect
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.overview.log.ShopAnalytics
import com.peto.ramap.ui.shop.overview.model.PendingShopAction
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_login_failure_message
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.hidden_shop_notification_unavailable_message
import ramap.shared.generated.resources.hide_shop_success_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.login_success_message
import ramap.shared.generated.resources.personalization_update_failure_message
import ramap.shared.generated.resources.shop_information_report_failure_message
import ramap.shared.generated.resources.shop_information_report_success_message

class OverviewViewModel(
    private val loginRepository: LoginRepository,
    private val shopReportRepository: ShopReportRepository,
    private val personalizationStore: PersonalizationStore,
    private val fetchShopOverviewUseCase: FetchShopOverviewUseCase,
    private val mapSearchHistoryStorage: SearchHistoryStorage,
    private val mapAnalytics: ShopAnalytics,
    private val signInUseCase: SignInUseCase,
) : BaseViewModel<ShopOverviewState, ShopOverviewIntent, ShopOverviewSideEffect>(
        ShopOverviewState(
            isLoggedIn = loginRepository.hasSession(),
            personalization =
                if (loginRepository.hasSession()) {
                    (personalizationStore.state.value as? PersonalizationBootstrapState.Success)?.value ?: Personalization()
                } else {
                    Personalization()
                },
        ),
    ) {
    private var pendingShopAction: PendingShopAction? = null
    private var observedBookmarkedShopIds: Set<String>? = null
    private var hidingSelectedShopId: String? = null

    init {
        if (!currentState.isLoggedIn) viewModelScope.launch { awaitLogin() }
        viewModelScope.launch { observePersonalization() }
    }

    override suspend fun handleIntent(intent: ShopOverviewIntent) {
        when {
            handleShopIntent(intent) -> Unit
            handlePersonalizationIntent(intent) -> Unit
            else -> handleAccountIntent(intent)
        }
    }

    private suspend fun awaitLogin() {
        loginRepository.session.first { it == LoginSessionState.AUTHENTICATED }
        reduce { copy(isLoggedIn = true) }
        // 공개 상세 캐시를 유지해 재조회 중 발생한 저장 수 변경을 보존한다.
        currentSheetShopId()?.let(::loadOverview)
    }

    private fun loadOverview(shopId: String) {
        val cached = (fetchShopOverviewUseCase.findCached(shopId) as? ShopOverviewCacheLookup.Hit)?.overview
        launchResultTask(
            taskKey = OVERVIEW_TASK_KEY,
            loadKey = ShopOverviewLoadKey.Overview,
            policy = TaskPolicy.CancelPrevious,
            onStart = {
                copy(
                    overviewState =
                        when {
                            shopOverview?.shop?.id == shopId -> overviewState
                            cached != null -> ShopOverviewUiState.Content(cached)
                            else -> ShopOverviewUiState.Loading(shopId, selectedShop?.takeIf { it.id == shopId })
                        },
                    hasLoadFailure = false,
                    isShopUnavailable = false,
                )
            },
            request = { fetchShopOverviewUseCase(shopId) },
            onSuccess = { overview ->
                if (!currentCoroutineContext().isActive || currentSheetShopId() != shopId || overview.shop.id != shopId) return@launchResultTask
                val mapped = fetchShopOverviewUseCase.reconcileBookmarkCount(overview)
                val overview = mapped.copy(shop = mapped.shop.copy(isVisible = currentState.selectedShop?.isVisible ?: mapped.shop.isVisible))
                reduce { copy(overviewState = ShopOverviewUiState.Content(overview), shopWaiting = shopWaiting + (shopId to overview.waitingSystem)) }
                viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(shopId) }
                trySideEffect(ShopOverviewSideEffect.ShopViewed(overview.shop))
            },
            onError = { error ->
                if (!currentCoroutineContext().isActive || currentSheetShopId() != shopId) return@launchResultTask
                reduce { copy(hasLoadFailure = true, isShopUnavailable = error is RamapError.Http && error.status == SHOP_NOT_FOUND, overviewState = if (shopOverview != null && !(error is RamapError.Http && error.status == SHOP_NOT_FOUND)) overviewState else ShopOverviewUiState.Error(shopId, selectedShop)) }
                showErrorToast(error)
            },
        )
    }

    private suspend fun observePersonalization() {
        personalizationStore.state.collect { state ->
            val personalization = (state as? PersonalizationBootstrapState.Success)?.value ?: return@collect
            if (!loginRepository.hasSession()) return@collect
            reduce { copy(isLoggedIn = true) }
            applyPersonalization(personalization)
            synchronizeOverviewLikeCounts(personalization.bookmarkedShopIds)
            resumePendingAction()
        }
    }

    private fun handleAccountIntent(intent: ShopOverviewIntent): Boolean {
        when (intent) {
            is OnLoginTypeSelected -> login(intent.type)
            ShopOverviewIntent.OnLoginSelectionDismissed -> pendingShopAction = null
            else -> return false
        }
        return true
    }

    private fun handleShopIntent(intent: ShopOverviewIntent): Boolean {
        when (intent) {
            is OnShopSelected -> selectShop(intent.shop)
            is OnShopIdSelected -> selectShopId(intent.shopId)
            is OnShopShareClicked -> shareShop(intent.shop)
            is OnShopMapLinkClicked -> openShopMapLink(intent.shop, intent.mapProvider)
            OnRequestedShopDismissed -> dismissRequestedShop()
            OnDismissed -> dismissOverview()
            OnOverviewRetry -> retryOverview()
            is OnShopReportSubmitted -> submitShopReport(intent.wrongFields, intent.description)
            else -> return false
        }
        return true
    }

    private fun handlePersonalizationIntent(intent: ShopOverviewIntent): Boolean {
        when (intent) {
            is OnBookmarkToggled -> toggleBookmark(intent.shop, intent.source)
            is OnShopNotificationToggled -> toggleShopNotification(intent.shop, intent.source)
            is OnHiddenToggled -> toggleHidden(intent.shop, intent.source)
            else -> return false
        }
        return true
    }

    private fun currentSheetShopId(): String? =
        when (val sheet = currentState.overviewState) {
            is ShopOverviewUiState.Loading -> sheet.shopId
            is ShopOverviewUiState.Content -> sheet.overview.shop.id
            is ShopOverviewUiState.Error -> sheet.shopId
            ShopOverviewUiState.Closed -> null
        }

    private fun selectShop(shop: RamenShop) {
        val selectedShop = currentState.selectedShop
        if (selectedShop?.id == shop.id && currentState.shopOverview != null) return
        val cachedOverview = (fetchShopOverviewUseCase.findCached(shop.id) as? ShopOverviewCacheLookup.Hit)?.overview
        val nextSheet =
            when {
                cachedOverview != null -> ShopOverviewUiState.Content(cachedOverview)
                else -> ShopOverviewUiState.Loading(shop.id, shop)
            }
        reduce {
            copy(
                overviewState = nextSheet,
            )
        }
        viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(shop.id) }
        if (cachedOverview != null) {
            viewModelScope.launch { postSideEffect(ShopOverviewSideEffect.ShopViewed(cachedOverview.shop)) }
        }
        loadOverview(shop.id)
    }

    private fun shareShop(shop: RamenShop) {
        mapAnalytics.logShopShared(shop)
        viewModelScope.launch {
            postSideEffect(
                ShopOverviewSideEffect.Interaction(
                    ShareShop(
                        shopId = shop.id,
                        shopName = shop.name,
                    ),
                ),
            )
        }
    }

    private fun openShopMapLink(
        shop: RamenShop,
        mapProvider: String,
    ) {
        mapAnalytics.logShopMapLinkOpened(shop, mapProvider)
    }

    private fun selectShopId(shopId: String) {
        if (shopId.isBlank()) return
        if (currentState.selectedShop?.id == shopId && currentState.shopOverview != null) return
        val cachedOverview = (fetchShopOverviewUseCase.findCached(shopId) as? ShopOverviewCacheLookup.Hit)?.overview
        val selectedShop = currentState.selectedShop?.takeIf { it.id == shopId } ?: cachedOverview?.shop
        val nextSheet =
            when {
                cachedOverview != null -> ShopOverviewUiState.Content(cachedOverview)
                else -> ShopOverviewUiState.Loading(shopId, selectedShop)
            }
        reduce {
            copy(
                overviewState = nextSheet,
            )
        }
        if (cachedOverview != null) {
            viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(cachedOverview.shop.id) }
            viewModelScope.launch { postSideEffect(ShopOverviewSideEffect.ShopViewed(cachedOverview.shop)) }
        }
        loadOverview(shopId)
    }

    private fun dismissRequestedShop() {
        if (currentState.overviewState is ShopOverviewUiState.Loading) {
            cancelTask(OVERVIEW_TASK_KEY)
            reduce { copy(overviewState = ShopOverviewUiState.Closed) }
        }
    }

    private fun dismissOverview() {
        cancelTask(OVERVIEW_TASK_KEY)
        reduce { copy(overviewState = ShopOverviewUiState.Closed) }
    }

    private fun retryOverview() {
        val shopId =
            when (val state = currentState.overviewState) {
                is ShopOverviewUiState.Error -> state.shopId
                is ShopOverviewUiState.Loading -> state.shopId
                is ShopOverviewUiState.Content -> state.overview.shop.id
                ShopOverviewUiState.Closed -> currentState.selectedShop?.id
            } ?: return
        loadOverview(shopId)
    }

    private fun toggleBookmark(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        val enabled = currentState.personalization.nextBookmarkEnabled(shop.id)
        if (requireLogin(PendingShopAction.ToggleBookmark(shop, source, enabled))) return
        updateBookmark(shop, source, enabled)
    }

    private fun updateBookmark(
        shop: RamenShop,
        source: AnalyticsSource,
        enabled: Boolean,
    ) {
        if (currentState.personalization.hasBookmark(shop.id) == enabled) return
        launchResultTask(
            taskKey = BOOKMARK_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = {
                mapAnalytics.logBookmarkToggled(shop, enabled, source)
                personalizationStore.updateBookmark(shop.id, enabled)
            },
            onError = { showToast(Res.string.personalization_update_failure_message, ToastType.ERROR) },
        )
    }

    private fun toggleShopNotification(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        if (requireLogin(PendingShopAction.ToggleShopNotification(shop, source))) return
        when (val toggle = currentState.personalization.notificationToggle(shop.id)) {
            NotificationStatus.Hidden -> showToast(Res.string.hidden_shop_notification_unavailable_message)
            is NotificationStatus.Allowed ->
                launchResultTask(
                    taskKey = NOTIFICATION_TASK_KEY,
                    policy = TaskPolicy.IgnoreNew,
                    request = {
                        mapAnalytics.logShopSubscribed(shop, toggle.enabled, source)
                        personalizationStore.updateShopNotification(shop.id, toggle.enabled)
                    },
                    onError = { showToast(Res.string.personalization_update_failure_message, ToastType.ERROR) },
                )
        }
    }

    private fun toggleHidden(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        if (requireLogin(PendingShopAction.ToggleHidden(shop, source))) return
        val hidden = !currentState.personalization.nextHiddenEnabled(shop.id)
        if (!hidden && currentState.selectedShop?.id == shop.id) hidingSelectedShopId = shop.id
        launchResultTask(
            taskKey = HIDDEN_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = {
                mapAnalytics.logHiddenToggled(shop, !hidden, source)
                if (hidden) personalizationStore.unhideShop(shop.id) else personalizationStore.hideShop(shop.id)
            },
            onSuccess = {
                if (!hidden) showToast(Res.string.hide_shop_success_message)
                if (hidingSelectedShopId == shop.id) {
                    hidingSelectedShopId = null
                    trySideEffect(ShopOverviewSideEffect.SelectedShopHidden)
                }
            },
            onError = {
                hidingSelectedShopId = null
                showToast(Res.string.personalization_update_failure_message, ToastType.ERROR)
            },
        )
    }

    private fun synchronizeOverviewLikeCounts(bookmarkedShopIds: Set<String>) {
        val changes = currentState.personalization.bookmarkChangesSince(observedBookmarkedShopIds)
        observedBookmarkedShopIds = bookmarkedShopIds
        for (change in changes) {
            val isBookmarked = change.type == BookmarkChangeStatus.ADDED
            updateOverviewBookmarkCount(change.shopId, isBookmarked)
        }
    }

    private fun updateOverviewBookmarkCount(
        shopId: String,
        enabled: Boolean,
    ) {
        fetchShopOverviewUseCase.updateCachedLikeCount(shopId, enabled)
        val overviewContent = currentState.overviewState as? ShopOverviewUiState.Content ?: return
        if (overviewContent.overview.shop.id != shopId) return

        reduce {
            copy(
                overviewState =
                    overviewContent.copy(
                        overview =
                            overviewContent.overview.updateBookmarkCount(enabled),
                    ),
            )
        }
    }

    private fun submitShopReport(
        wrongFields: Set<ShopInformationField>,
        description: String,
    ) {
        val shop = currentState.selectedShop ?: return
        val report = ShopInformationReport.create(shop, wrongFields, description) ?: return
        launchResultTask(
            taskKey = SHOP_REPORT_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = { shopReportRepository.submitShopInformationReport(report) },
            onSuccess = { showToast(Res.string.shop_information_report_success_message) },
            onError = { showToast(Res.string.shop_information_report_failure_message, ToastType.ERROR) },
        )
    }

    private fun applyPersonalization(personalization: Personalization) {
        val selectedShopId = currentState.selectedShop?.id
        val transition = personalization.visibilityChange(selectedShopId, currentState.personalization)
        reduce {
            copy(
                overviewState =
                    when (transition) {
                        com.peto.ramap.domain.model.personalization.VisibilityStatus.Hidden -> ShopOverviewUiState.Closed
                        com.peto.ramap.domain.model.personalization.VisibilityStatus.Visible -> overviewStateWithVisibleShop(overviewState)
                        com.peto.ramap.domain.model.personalization.VisibilityStatus.Unchanged -> overviewState
                    },
                personalization = personalization,
            )
        }
        if (transition == com.peto.ramap.domain.model.personalization.VisibilityStatus.Hidden && hidingSelectedShopId != selectedShopId) trySideEffect(ShopOverviewSideEffect.SelectedShopHidden)
    }

    private fun overviewStateWithVisibleShop(state: ShopOverviewUiState): ShopOverviewUiState =
        when (state) {
            ShopOverviewUiState.Closed -> state
            is ShopOverviewUiState.Loading -> state.copy(shop = state.shop?.copy(isVisible = true))
            is ShopOverviewUiState.Content -> visibleContentState(state)
            is ShopOverviewUiState.Error -> state.copy(shop = state.shop?.copy(isVisible = true))
        }

    private fun visibleContentState(state: ShopOverviewUiState.Content): ShopOverviewUiState.Content =
        state.copy(
            overview = state.overview.copy(shop = state.overview.shop.copy(isVisible = true)),
        )

    private fun requestLoginGuide() {
        viewModelScope.launch { postSideEffect(ShopOverviewSideEffect.Interaction(ShowLoginGuide)) }
    }

    private fun login(type: LoginType) {
        launchResultTask(
            taskKey = ACCOUNT_TASK_KEY,
            policy = TaskPolicy.CancelPrevious,
            request = { signInUseCase(type, AnalyticsSource.MAP) },
            onSuccess = {
                showToast(Res.string.login_success_message)
            },
            onError = { error ->
                showErrorToast(
                    error,
                    when (type) {
                        LoginType.KAKAO -> Res.string.kakao_login_failure_message
                        LoginType.APPLE -> Res.string.apple_login_failure_message
                    },
                )
            },
        )
    }

    private fun requireLogin(action: PendingShopAction): Boolean {
        if (currentState.isLoggedIn) return false
        pendingShopAction = action
        requestLoginGuide()
        return true
    }

    private fun resumePendingAction() {
        if (!currentState.isLoggedIn || personalizationStore.state.value !is PersonalizationBootstrapState.Success) return
        val action = pendingShopAction ?: return
        pendingShopAction = null
        when (action) {
            is PendingShopAction.ToggleBookmark ->
                updateBookmark(
                    shop = action.shop,
                    source = action.source,
                    enabled = action.enabled,
                )
            is PendingShopAction.ToggleShopNotification ->
                toggleShopNotification(
                    shop = action.shop,
                    source = action.source,
                )
            is PendingShopAction.ToggleHidden ->
                toggleHidden(
                    shop = action.shop,
                    source = action.source,
                )
        }
    }

    private fun showToast(
        message: StringResource,
        type: ToastType = ToastType.DEFAULT,
    ) {
        trySideEffect(
            ShopOverviewSideEffect.Interaction(
                ShowToast(ToastData(message = message, type = type)),
            ),
        )
    }

    private fun showErrorToast(
        error: RamapError,
        fallbackMessage: StringResource = Res.string.data_load_failure_message,
    ) {
        val message =
            when (error) {
                is RamapError.Http -> fallbackMessage
                else -> fallbackMessage
            }
        showToast(message, ToastType.ERROR)
    }

    companion object {
        private const val SHOP_NOT_FOUND = 404
        private const val OVERVIEW_TASK_KEY = "shop_overview"
        private const val BOOKMARK_TASK_KEY = "shop_bookmark"
        private const val NOTIFICATION_TASK_KEY = "shop_notification"
        private const val HIDDEN_TASK_KEY = "shop_hidden"
        private const val SHOP_REPORT_TASK_KEY = "shop_report"
        private const val ACCOUNT_TASK_KEY = "shop_account"
    }
}
