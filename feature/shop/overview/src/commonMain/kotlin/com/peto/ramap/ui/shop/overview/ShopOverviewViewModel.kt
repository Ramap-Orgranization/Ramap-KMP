package com.peto.ramap.ui.shop.overview

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.personalization.ShopNotificationToggle
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.platform.storage.SearchHistoryStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect.ShareShop
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect.ShowLoginGuide
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect.ShowToast
import com.peto.ramap.ui.shop.log.ShopAnalytics
import com.peto.ramap.ui.shop.model.PendingShopAction
import com.peto.ramap.ui.shop.model.ShopDetailEntryContext
import com.peto.ramap.ui.shop.model.ShopDetailSheetUiState
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnBookmarkToggled
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnHiddenToggled
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnLoginTypeSelected
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnRequestedShopDismissed
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopDetailDismissed
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopDetailRetry
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopIdSelected
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopMapLinkClicked
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopNotificationToggled
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopReportSubmitted
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopSelected
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent.OnShopShareClicked
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewLoadKey
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewSideEffect
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collect
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

class ShopOverviewViewModel(
    private val loginRepository: LoginRepository,
    private val shopReportRepository: ShopReportRepository,
    private val personalizationStore: PersonalizationStore,
    private val fetchShopDetailUseCase: FetchShopDetailUseCase,
    private val mapSearchHistoryStorage: SearchHistoryStorage,
    private val mapAnalytics: ShopAnalytics,
    private val signInUseCase: SignInUseCase,
    private val profileRepository: ProfileRepository,
) : BaseViewModel<ShopOverviewState, ShopOverviewIntent, ShopOverviewSideEffect>(ShopOverviewState()) {
    val entryContext = ShopDetailEntryContext()
    private var pendingShopAction: PendingShopAction? = null
    private var pendingActionEntryGeneration = entryContext.generation
    private val resumedActionTaskKeys = mutableSetOf<String>()
    private var observedBookmarkedShopIds: Set<String>? = null
    private var requestContext = 0L
    private var sessionUserId: String? = null
    private var hidingSelectedShopId: String? = null

    init {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { observeShopSession() }
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { observeSessionState() }
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { observePersonalization() }
    }

    override suspend fun handleIntent(intent: ShopOverviewIntent) {
        when {
            handleShopIntent(intent) -> Unit
            handlePersonalizationIntent(intent) -> Unit
            else -> handleAccountIntent(intent)
        }
    }

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collect { sessionState ->
            val authenticated = sessionState == LoginSessionState.AUTHENTICATED
            if (currentState.isLoggedIn && !authenticated) clearPendingAction()
            if (!authenticated) observedBookmarkedShopIds = null
            reduce { copy(isLoggedIn = authenticated, personalization = if (authenticated) personalization else ShopPersonalization()) }
        }
    }

    private suspend fun observeShopSession() {
        profileRepository.sessionUserIds.collect { userId ->
            if (sessionUserId == userId) return@collect
            if (sessionUserId != null) clearPendingAction()
            sessionUserId = userId
            observedBookmarkedShopIds = null
            requestContext++
            cancelTask(OVERVIEW_TASK_KEY)
            fetchShopDetailUseCase.clearCache()
            currentSheetShopId()?.let(::loadShopDetail)
        }
    }

    private fun loadShopDetail(shopId: String) {
        val context = ++requestContext
        val cached = (fetchShopDetailUseCase.findCached(shopId) as? ShopDetailCacheLookup.Hit)?.detail
        launchResultTask(
            taskKey = OVERVIEW_TASK_KEY,
            loadKey = ShopOverviewLoadKey.Overview,
            policy = TaskPolicy.CancelPrevious,
            onStart = {
                copy(
                    shopDetailState =
                        when {
                            shopDetail?.shop?.id == shopId -> shopDetailState
                            cached != null -> ShopDetailSheetUiState.Content(cached)
                            else -> ShopDetailSheetUiState.Loading(shopId, selectedShop?.takeIf { it.id == shopId })
                        },
                    hasLoadFailure = false,
                    isShopUnavailable = false,
                )
            },
            request = { fetchShopDetailUseCase(shopId) },
            onSuccess = { overview ->
                if (!currentCoroutineContext().isActive || requestContext != context || currentSheetShopId() != shopId || overview.shop.id != shopId) return@launchResultTask
                val mapped = fetchShopDetailUseCase.reconcileBookmarkCount(overview)
                val detail = mapped.copy(shop = mapped.shop.copy(isVisible = currentState.selectedShop?.isVisible ?: mapped.shop.isVisible))
                reduce { copy(shopDetailState = ShopDetailSheetUiState.Content(detail), shopWaiting = shopWaiting + (shopId to detail.waitingSystem)) }
                viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(shopId) }
                trySideEffect(ShopOverviewSideEffect.ShopViewed(detail.shop))
            },
            onError = { error ->
                if (!currentCoroutineContext().isActive || requestContext != context || currentSheetShopId() != shopId) return@launchResultTask
                reduce { copy(hasLoadFailure = true, isShopUnavailable = error is RamapError.Http && error.status == SHOP_NOT_FOUND, shopDetailState = if (shopDetail != null && !(error is RamapError.Http && error.status == SHOP_NOT_FOUND)) shopDetailState else ShopDetailSheetUiState.Error(shopId, selectedShop)) }
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
            synchronizeShopDetailLikeCounts(personalization.bookmarkedShopIds)
            resumePendingAction()
        }
    }

    private fun handleAccountIntent(intent: ShopOverviewIntent): Boolean {
        when (intent) {
            ShopOverviewIntent.OnLoginRequested -> requestBookmarkedViewLogin()
            is OnLoginTypeSelected -> login(intent.type)
            ShopOverviewIntent.OnLoginSelectionDismissed -> dismissLoginGuide()
            ShopOverviewIntent.OnPendingLoginActionCancelled -> clearPendingAction()
            is ShopOverviewIntent.OnMapEntryChanged -> {
                if (intent.generation > currentState.entryGeneration) {
                    entryContext.advanceTo(intent.generation)
                    clearPendingAction()
                    reduce { copy(entryGeneration = intent.generation) }
                }
            }
            is ShopOverviewIntent.OnLoginRequestedForEntry -> {
                if (entryContext.includes(intent.generation)) requestBookmarkedViewLogin()
            }
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
            OnShopDetailDismissed -> dismissShopDetail()
            OnShopDetailRetry -> retryShopDetail()
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
        when (val sheet = currentState.shopDetailState) {
            is ShopDetailSheetUiState.Loading -> sheet.shopId
            is ShopDetailSheetUiState.Content -> sheet.detail.shop.id
            is ShopDetailSheetUiState.Error -> sheet.shopId
            ShopDetailSheetUiState.Closed -> null
        }

    private fun selectShop(shop: RamenShop) {
        val selectedShop = currentState.selectedShop
        if (selectedShop?.id == shop.id && currentState.shopDetail != null) return
        val peekDetail = (fetchShopDetailUseCase.findCached(shop.id) as? ShopDetailCacheLookup.Hit)?.detail
        val nextSheet =
            when {
                peekDetail != null -> ShopDetailSheetUiState.Content(peekDetail)
                else -> ShopDetailSheetUiState.Loading(shop.id, shop)
            }
        reduce {
            copy(
                shopDetailState = nextSheet,
            )
        }
        viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(shop.id) }
        if (peekDetail != null) {
            viewModelScope.launch { postSideEffect(ShopOverviewSideEffect.ShopViewed(peekDetail.shop)) }
        }
        loadShopDetail(shop.id)
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
        if (currentState.selectedShop?.id == shopId && currentState.shopDetail != null) return
        val peekDetail = (fetchShopDetailUseCase.findCached(shopId) as? ShopDetailCacheLookup.Hit)?.detail
        val selectedShop = currentState.selectedShop?.takeIf { it.id == shopId } ?: peekDetail?.shop
        val nextSheet =
            when {
                peekDetail != null -> ShopDetailSheetUiState.Content(peekDetail)
                else -> ShopDetailSheetUiState.Loading(shopId, selectedShop)
            }
        reduce {
            copy(
                shopDetailState = nextSheet,
            )
        }
        if (peekDetail != null) {
            viewModelScope.launch { mapSearchHistoryStorage.addRecentlyViewedShop(peekDetail.shop.id) }
            viewModelScope.launch { postSideEffect(ShopOverviewSideEffect.ShopViewed(peekDetail.shop)) }
        }
        loadShopDetail(shopId)
    }

    private fun dismissRequestedShop() {
        if (currentState.shopDetailState is ShopDetailSheetUiState.Loading) {
            cancelTask(OVERVIEW_TASK_KEY)
            reduce { copy(shopDetailState = ShopDetailSheetUiState.Closed) }
        }
    }

    private fun dismissShopDetail() {
        requestContext++
        cancelTask(OVERVIEW_TASK_KEY)
        reduce { copy(shopDetailState = ShopDetailSheetUiState.Closed) }
    }

    private fun retryShopDetail() {
        val shopId =
            when (val state = currentState.shopDetailState) {
                is ShopDetailSheetUiState.Error -> state.shopId
                is ShopDetailSheetUiState.Loading -> state.shopId
                is ShopDetailSheetUiState.Content -> state.detail.shop.id
                ShopDetailSheetUiState.Closed -> currentState.selectedShop?.id
            } ?: return
        loadShopDetail(shopId)
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
        pendingEntry: Long? = null,
    ) {
        if (currentState.personalization.hasBookmark(shop.id) == enabled) return
        launchPersonalizationTask(
            taskKey = "$PERSONALIZATION_TASK_KEY:bookmark:${shop.id}",
            pendingEntry = pendingEntry,
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
        pendingEntry: Long? = null,
    ) {
        if (requireLogin(PendingShopAction.ToggleShopNotification(shop, source))) return
        when (val toggle = currentState.personalization.notificationToggle(shop.id)) {
            ShopNotificationToggle.HiddenShop -> showToast(Res.string.hidden_shop_notification_unavailable_message)
            is ShopNotificationToggle.Allowed ->
                launchPersonalizationTask(
                    taskKey = "$PERSONALIZATION_TASK_KEY:notification:${shop.id}",
                    pendingEntry = pendingEntry,
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
        pendingEntry: Long? = null,
    ) {
        if (requireLogin(PendingShopAction.ToggleHidden(shop, source))) return
        val hidden = !currentState.personalization.nextHiddenEnabled(shop.id)
        if (!hidden && currentState.selectedShop?.id == shop.id) hidingSelectedShopId = shop.id
        launchPersonalizationTask(
            taskKey = "$PERSONALIZATION_TASK_KEY:hidden:${shop.id}",
            pendingEntry = pendingEntry,
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

    private fun launchPersonalizationTask(
        taskKey: String,
        pendingEntry: Long?,
        request: suspend () -> RamapResult<Unit>,
        onSuccess: suspend (Unit) -> Unit = {},
        onError: suspend (RamapError) -> Unit,
    ) {
        val key = pendingEntry?.let { "$taskKey:entry:$it" } ?: taskKey
        if (pendingEntry != null) resumedActionTaskKeys += key
        launchResultTask(
            taskKey = key,
            policy = TaskPolicy.IgnoreNew,
            onFinish = {
                resumedActionTaskKeys -= key
                this
            },
            request = {
                if (pendingEntry != null && !entryContext.includes(pendingEntry)) {
                    throw CancellationException(STALE_ENTRY_MESSAGE)
                }
                request()
            },
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    private fun synchronizeShopDetailLikeCounts(bookmarkedShopIds: Set<String>) {
        val changes = currentState.personalization.bookmarkChangesSince(observedBookmarkedShopIds)
        observedBookmarkedShopIds = bookmarkedShopIds
        for (change in changes) updateShopDetailLikeCount(change.shopId, change.enabled)
    }

    private fun updateShopDetailLikeCount(
        shopId: String,
        enabled: Boolean,
    ) {
        fetchShopDetailUseCase.updateCachedLikeCount(shopId, enabled)
        val detailState = currentState.shopDetailState as? ShopDetailSheetUiState.Content ?: return
        if (detailState.detail.shop.id != shopId) return

        reduce {
            copy(
                shopDetailState =
                    detailState.copy(
                        detail =
                            detailState.detail.updateBookmarkCount(enabled),
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

    private fun applyPersonalization(personalization: ShopPersonalization) {
        val selectedShopId = currentState.selectedShop?.id
        val transition = personalization.visibilityChange(selectedShopId, currentState.personalization)
        reduce {
            copy(
                shopDetailState =
                    when (transition) {
                        com.peto.ramap.domain.model.personalization.ShopVisibilityChange.Hidden -> ShopDetailSheetUiState.Closed
                        com.peto.ramap.domain.model.personalization.ShopVisibilityChange.Visible -> detailStateWithVisibleShop(shopDetailState)
                        com.peto.ramap.domain.model.personalization.ShopVisibilityChange.Unchanged -> shopDetailState
                    },
                personalization = personalization,
            )
        }
        if (transition == com.peto.ramap.domain.model.personalization.ShopVisibilityChange.Hidden && hidingSelectedShopId != selectedShopId) trySideEffect(ShopOverviewSideEffect.SelectedShopHidden)
    }

    private fun detailStateWithVisibleShop(state: ShopDetailSheetUiState): ShopDetailSheetUiState =
        when (state) {
            ShopDetailSheetUiState.Closed -> state
            is ShopDetailSheetUiState.Loading -> state.copy(shop = state.shop?.copy(isVisible = true))
            is ShopDetailSheetUiState.Content -> visibleContentState(state)
            is ShopDetailSheetUiState.Error -> state.copy(shop = state.shop?.copy(isVisible = true))
        }

    private fun visibleContentState(state: ShopDetailSheetUiState.Content): ShopDetailSheetUiState.Content =
        state.copy(
            detail = state.detail.copy(shop = state.detail.shop.copy(isVisible = true)),
        )

    private fun requestLoginGuide() {
        viewModelScope.launch { postSideEffect(ShopOverviewSideEffect.Interaction(ShowLoginGuide)) }
    }

    private fun dismissLoginGuide() {
        clearPendingAction()
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

    private fun requestBookmarkedViewLogin() {
        pendingShopAction = PendingShopAction.LoginRequest
        pendingActionEntryGeneration = entryContext.generation
        if (currentState.isLoggedIn) resumePendingAction() else requestLoginGuide()
    }

    private fun requireLogin(action: PendingShopAction): Boolean {
        if (currentState.isLoggedIn) return false
        pendingShopAction = action
        pendingActionEntryGeneration = entryContext.generation
        requestLoginGuide()
        return true
    }

    private fun resumePendingAction() {
        if (!currentState.isLoggedIn || personalizationStore.state.value !is PersonalizationBootstrapState.Success) return
        val action = pendingShopAction ?: return
        val generation = pendingActionEntryGeneration
        pendingShopAction = null
        if (!entryContext.includes(generation)) return
        when (action) {
            PendingShopAction.LoginRequest -> trySideEffect(ShopOverviewSideEffect.LoginCompleted(generation))
            is PendingShopAction.ToggleBookmark -> updateBookmark(action.shop, action.source, action.enabled, generation)
            is PendingShopAction.ToggleShopNotification -> toggleShopNotification(action.shop, action.source, generation)
            is PendingShopAction.ToggleHidden -> toggleHidden(action.shop, action.source, generation)
        }
    }

    private fun clearPendingAction() {
        pendingShopAction = null
        for (key in resumedActionTaskKeys.toList()) cancelTask(key)
        resumedActionTaskKeys.clear()
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
        private const val STALE_ENTRY_MESSAGE = "Shop detail entry changed before pending action"
        private const val OVERVIEW_TASK_KEY = "shop_overview"
        private const val PERSONALIZATION_TASK_KEY = "shop_personalization"
        private const val SHOP_REPORT_TASK_KEY = "shop_report"
        private const val ACCOUNT_TASK_KEY = "shop_account"
    }
}
