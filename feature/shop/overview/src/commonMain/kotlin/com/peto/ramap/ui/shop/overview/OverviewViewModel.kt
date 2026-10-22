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
import com.peto.ramap.domain.model.personalization.VisibilityStatus
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.ShopOverView
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
import ramap.shared.generated.resources.login_required_bookmark_description
import ramap.shared.generated.resources.login_required_hide_shop_description
import ramap.shared.generated.resources.login_required_notification_description
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
        initialState(loginRepository, personalizationStore),
    ) {
    private var pendingShopAction: PendingShopAction? = null
    private var hidingSelectedShopId: String? = null

    init {
        val isNotLoggedIn = !currentState.isLoggedIn
        if (isNotLoggedIn) viewModelScope.launch { awaitLogin() }
        viewModelScope.launch { observePersonalization() }
    }

    override suspend fun handleIntent(intent: ShopOverviewIntent) {
        when {
            handleShopIntent(intent) -> Unit
            handlePersonalizationIntent(intent) -> Unit
            else -> handleAccountIntent(intent)
        }
    }

    private fun handleAccountIntent(intent: ShopOverviewIntent): Boolean =
        when (intent) {
            is OnLoginTypeSelected -> {
                login(intent.type)
                true
            }
            ShopOverviewIntent.OnLoginSelectionDismissed -> {
                pendingShopAction = null
                true
            }
            else -> false
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

    private fun selectShop(shop: RamenShop) {
        val isAlreadySelectedWithOverview = currentState.selectedShop?.id == shop.id && currentState.shopOverview != null
        if (isAlreadySelectedWithOverview) return
        viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(shop.id) }
        openShopOverview(shop.id, shop)
    }

    private fun selectShopId(shopId: String) {
        if (shopId.isBlank()) return
        val isAlreadySelectedWithOverview = currentState.selectedShop?.id == shopId && currentState.shopOverview != null
        if (isAlreadySelectedWithOverview) return
        val cachedOverview = (fetchShopOverviewUseCase.findCached(shopId) as? ShopOverviewCacheLookup.Hit)?.overview
        val initialShop = currentState.selectedShop?.takeIf { it.id == shopId } ?: cachedOverview?.shop
        openShopOverview(shopId, initialShop)
    }

    private fun openShopOverview(
        shopId: String,
        initialShop: RamenShop?,
    ) {
        val cachedOverview = (fetchShopOverviewUseCase.findCached(shopId) as? ShopOverviewCacheLookup.Hit)?.overview
        val hasCachedOverview = cachedOverview != null
        val nextSheet =
            if (hasCachedOverview) {
                ShopOverviewUiState.Content(cachedOverview)
            } else {
                ShopOverviewUiState.Loading(shopId, initialShop)
            }
        reduce { copy(overviewState = nextSheet) }
        if (hasCachedOverview) {
            viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(cachedOverview.shop.id) }
            viewModelScope.launch { postSideEffect(ShopOverviewSideEffect.ShopViewed(cachedOverview.shop)) }
        }
        loadOverview(shopId)
    }

    private fun loadOverview(shopId: String) {
        val cached = (fetchShopOverviewUseCase.findCached(shopId) as? ShopOverviewCacheLookup.Hit)?.overview
        launchResultTask(
            taskKey = OVERVIEW_TASK_KEY,
            loadKey = ShopOverviewLoadKey.Overview,
            policy = TaskPolicy.CancelPrevious,
            onStart = {
                val isSameShopAlreadyLoaded = shopOverview?.shop?.id == shopId
                val hasCachedOverview = cached != null
                val initialOverviewState =
                    when {
                        isSameShopAlreadyLoaded -> overviewState
                        hasCachedOverview -> ShopOverviewUiState.Content(cached)
                        else -> ShopOverviewUiState.Loading(shopId, selectedShop?.takeIf { it.id == shopId })
                    }
                copy(
                    overviewState = initialOverviewState,
                    hasLoadFailure = false,
                    isShopUnavailable = false,
                )
            },
            request = { fetchShopOverviewUseCase(shopId) },
            onSuccess = { overview -> handleOverviewLoadSuccess(shopId, overview) },
            onError = { error -> handleOverviewLoadError(shopId, error) },
        )
    }

    private suspend fun handleOverviewLoadSuccess(
        shopId: String,
        overview: ShopOverView,
    ) {
        val isInactive = !currentCoroutineContext().isActive
        val isSheetShopChanged = currentSheetShopId() != shopId
        val isOverviewShopMismatch = overview.shop.id != shopId
        if (isInactive || isSheetShopChanged || isOverviewShopMismatch) return

        val mapped = fetchShopOverviewUseCase.reconcileBookmarkCount(overview)
        val isVisible = currentState.selectedShop?.isVisible ?: mapped.shop.isVisible
        val finalOverview = mapped.copy(shop = mapped.shop.copy(isVisible = isVisible))
        reduce {
            copy(
                overviewState = ShopOverviewUiState.Content(finalOverview),
                shopWaiting = shopWaiting + (shopId to finalOverview.waitingSystem),
            )
        }
        viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(shopId) }
        trySideEffect(ShopOverviewSideEffect.ShopViewed(finalOverview.shop))
    }

    private suspend fun handleOverviewLoadError(
        shopId: String,
        error: RamapError,
    ) {
        val isInactive = !currentCoroutineContext().isActive
        val isSheetShopChanged = currentSheetShopId() != shopId
        if (isInactive || isSheetShopChanged) return

        val isNotFound = error is RamapError.Http && error.status == SHOP_NOT_FOUND
        val isRetainingPreviousOverview = currentState.shopOverview != null && !isNotFound
        val nextOverviewState =
            if (isRetainingPreviousOverview) {
                currentState.overviewState
            } else {
                ShopOverviewUiState.Error(shopId, currentState.selectedShop)
            }
        reduce {
            copy(
                hasLoadFailure = true,
                isShopUnavailable = isNotFound,
                overviewState = nextOverviewState,
            )
        }
        showErrorToast(error)
    }

    private fun dismissRequestedShop() {
        val isNotLoadingState = currentState.overviewState !is ShopOverviewUiState.Loading
        if (isNotLoadingState) return
        cancelTask(OVERVIEW_TASK_KEY)
        reduce { copy(overviewState = ShopOverviewUiState.Closed) }
    }

    private fun dismissOverview() {
        cancelTask(OVERVIEW_TASK_KEY)
        reduce { copy(overviewState = ShopOverviewUiState.Closed) }
    }

    private fun retryOverview() {
        val shopId = currentSheetShopId() ?: currentState.selectedShop?.id ?: return
        loadOverview(shopId)
    }

    private fun shareShop(shop: RamenShop) {
        mapAnalytics.logShopShared(shop)
        viewModelScope.launch {
            postSideEffect(
                ShopOverviewSideEffect.Interaction(
                    ShareShop(shopId = shop.id, shopName = shop.name),
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
        val isBookmarkStatusUnchanged = currentState.personalization.hasBookmark(shop.id) == enabled
        if (isBookmarkStatusUnchanged) return
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
        val toggle = currentState.personalization.notificationToggle(shop.id)
        val isNotificationHidden = toggle is NotificationStatus.Hidden
        if (isNotificationHidden) {
            showToast(Res.string.hidden_shop_notification_unavailable_message)
            return
        }
        val allowed = toggle as NotificationStatus.Allowed
        launchResultTask(
            taskKey = NOTIFICATION_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = {
                mapAnalytics.logShopSubscribed(shop, allowed.enabled, source)
                personalizationStore.updateShopNotification(shop.id, allowed.enabled)
            },
            onError = { showToast(Res.string.personalization_update_failure_message, ToastType.ERROR) },
        )
    }

    private fun toggleHidden(
        shop: RamenShop,
        source: AnalyticsSource,
    ) {
        if (requireLogin(PendingShopAction.ToggleHidden(shop, source))) return
        val hidden = !currentState.personalization.nextHiddenEnabled(shop.id)
        val isHidingCurrentSelectedShop = !hidden && currentState.selectedShop?.id == shop.id
        if (isHidingCurrentSelectedShop) hidingSelectedShopId = shop.id
        launchResultTask(
            taskKey = HIDDEN_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = {
                mapAnalytics.logHiddenToggled(shop, !hidden, source)
                if (hidden) personalizationStore.unhideShop(shop.id) else personalizationStore.hideShop(shop.id)
            },
            onSuccess = { handleHiddenSuccess(shop.id, hidden) },
            onError = { handleHiddenError() },
        )
    }

    private fun handleHiddenSuccess(
        shopId: String,
        hidden: Boolean,
    ) {
        val isShopUnhidden = !hidden
        if (isShopUnhidden) showToast(Res.string.hide_shop_success_message)
        val isTargetSelectedShop = hidingSelectedShopId == shopId
        if (isTargetSelectedShop) {
            hidingSelectedShopId = null
            trySideEffect(ShopOverviewSideEffect.SelectedShopHidden)
        }
    }

    private fun handleHiddenError() {
        hidingSelectedShopId = null
        showToast(Res.string.personalization_update_failure_message, ToastType.ERROR)
    }

    private suspend fun awaitLogin() {
        loginRepository.session.first { it == LoginSessionState.AUTHENTICATED }
        reduce { copy(isLoggedIn = true) }
        currentSheetShopId()?.let(::loadOverview)
    }

    private suspend fun observePersonalization() {
        personalizationStore.state.collect(::handlePersonalizationStateChange)
    }

    private fun handlePersonalizationStateChange(state: PersonalizationBootstrapState) {
        val personalization = (state as? PersonalizationBootstrapState.Success)?.value ?: return
        val isLoggedOut = !loginRepository.hasSession()
        if (isLoggedOut) return
        val previousBookmarkedShopIds = currentState.bookmarkedShopIds.takeIf { currentState.hasReceivedPersonalization }
        reduce { copy(isLoggedIn = true) }
        applyPersonalization(personalization)
        synchronizeOverviewLikeCounts(previousBookmarkedShopIds)
        resumePendingAction()
    }

    private fun synchronizeOverviewLikeCounts(previousBookmarkedShopIds: Set<String>?) {
        val changes = currentState.personalization.bookmarkChangesSince(previousBookmarkedShopIds)
        for ((shopId, type) in changes) {
            val isBookmarked = type == BookmarkChangeStatus.ADDED
            updateOverviewBookmarkCount(shopId, isBookmarked)
        }
    }

    private fun updateOverviewBookmarkCount(
        shopId: String,
        enabled: Boolean,
    ) {
        fetchShopOverviewUseCase.updateCachedLikeCount(shopId, enabled)
        val overviewContent = currentState.overviewState as? ShopOverviewUiState.Content ?: return
        val isDifferentShop = overviewContent.overview.shop.id != shopId
        if (isDifferentShop) return
        val updatedOverview = overviewContent.overview.updateBookmarkCount(enabled)
        reduce { copy(overviewState = overviewContent.copy(overview = updatedOverview)) }
    }

    private fun applyPersonalization(personalization: Personalization) {
        val selectedShopId = currentState.selectedShop?.id
        val transition = personalization.visibilityChange(selectedShopId, currentState.personalization)
        val nextOverviewState =
            when (transition) {
                VisibilityStatus.Hidden -> ShopOverviewUiState.Closed
                VisibilityStatus.Visible -> overviewStateWithVisibleShop(currentState.overviewState)
                VisibilityStatus.Unchanged -> currentState.overviewState
            }
        reduce {
            copy(
                overviewState = nextOverviewState,
                personalization = personalization,
                hasReceivedPersonalization = true,
            )
        }
        val isHiddenSelectedShop = transition == VisibilityStatus.Hidden && hidingSelectedShopId != selectedShopId
        if (isHiddenSelectedShop) {
            trySideEffect(ShopOverviewSideEffect.SelectedShopHidden)
        }
    }

    private fun login(type: LoginType) {
        val failureMessage =
            when (type) {
                LoginType.KAKAO -> Res.string.kakao_login_failure_message
                LoginType.APPLE -> Res.string.apple_login_failure_message
            }
        launchResultTask(
            taskKey = ACCOUNT_TASK_KEY,
            policy = TaskPolicy.CancelPrevious,
            request = { signInUseCase(type, AnalyticsSource.MAP) },
            onSuccess = { showToast(Res.string.login_success_message) },
            onError = { error -> showErrorToast(error, failureMessage) },
        )
    }

    private fun requireLogin(action: PendingShopAction): Boolean {
        if (currentState.isLoggedIn) return false
        pendingShopAction = action
        requestLoginGuide(action)
        return true
    }

    private fun resumePendingAction() {
        val isPersonalizationLoaded = personalizationStore.state.value is PersonalizationBootstrapState.Success
        val isUserNotReady = !currentState.isLoggedIn || !isPersonalizationLoaded
        if (isUserNotReady) return
        val action = pendingShopAction ?: return
        pendingShopAction = null
        executePendingAction(action)
    }

    private fun executePendingAction(action: PendingShopAction) {
        when (action) {
            is PendingShopAction.ToggleBookmark -> updateBookmark(action.shop, action.source, action.enabled)
            is PendingShopAction.ToggleShopNotification -> toggleShopNotification(action.shop, action.source)
            is PendingShopAction.ToggleHidden -> toggleHidden(action.shop, action.source)
        }
    }

    private fun requestLoginGuide(action: PendingShopAction) {
        val description =
            when (action) {
                is PendingShopAction.ToggleBookmark -> Res.string.login_required_bookmark_description
                is PendingShopAction.ToggleShopNotification -> Res.string.login_required_notification_description
                is PendingShopAction.ToggleHidden -> Res.string.login_required_hide_shop_description
            }
        viewModelScope.launch {
            postSideEffect(ShopOverviewSideEffect.Interaction(ShowLoginGuide(description)))
        }
    }

    private fun currentSheetShopId(): String? =
        when (val sheet = currentState.overviewState) {
            is ShopOverviewUiState.Loading -> sheet.shopId
            is ShopOverviewUiState.Content -> sheet.overview.shop.id
            is ShopOverviewUiState.Error -> sheet.shopId
            ShopOverviewUiState.Closed -> null
        }

    private fun overviewStateWithVisibleShop(state: ShopOverviewUiState): ShopOverviewUiState =
        when (state) {
            ShopOverviewUiState.Closed -> state
            is ShopOverviewUiState.Loading -> state.copy(shop = state.shop?.copy(isVisible = true))
            is ShopOverviewUiState.Content -> visibleContentState(state)
            is ShopOverviewUiState.Error -> state.copy(shop = state.shop?.copy(isVisible = true))
        }

    private fun visibleContentState(state: ShopOverviewUiState.Content): ShopOverviewUiState.Content {
        val visibleShop = state.overview.shop.copy(isVisible = true)
        return state.copy(overview = state.overview.copy(shop = visibleShop))
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

        private fun initialState(
            loginRepository: LoginRepository,
            personalizationStore: PersonalizationStore,
        ): ShopOverviewState {
            val hasSession = loginRepository.hasSession()
            val personalizationSuccess = personalizationStore.state.value as? PersonalizationBootstrapState.Success
            val personalization =
                if (hasSession) {
                    personalizationSuccess?.value ?: Personalization()
                } else {
                    Personalization()
                }
            return ShopOverviewState(
                isLoggedIn = hasSession,
                personalization = personalization,
            )
        }
    }
}
