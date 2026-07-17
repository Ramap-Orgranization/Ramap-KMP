package com.peto.ramap.ui.main.my

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.report.PlaceReportTextParser
import com.peto.ramap.domain.model.report.UnregisteredPlaceReport
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ReverseGeocoder
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.CurrentLocationStore
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnAccountDeleteClick
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnAccountDeleteConfirm
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnAccountDeleteDismiss
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnCurrentAddressRefresh
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnCurrentLocationReportSubmit
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnHiddenShopsClick
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnKakaoLoginClick
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnLoginGuideDismiss
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnLogoutClick
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnPlaceReportSubmit
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnPlaceUrlChanged
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect.NavigateToHiddenShops
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect.ShowMyToast
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_failure_message
import ramap.shared.generated.resources.account_delete_success_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.place_report_existing_shop_message
import ramap.shared.generated.resources.place_report_failure_message
import ramap.shared.generated.resources.place_report_invalid_url_message
import ramap.shared.generated.resources.place_report_location_unavailable_message
import ramap.shared.generated.resources.place_report_success_message

class MyTabViewModel(
    private val loginRepository: LoginRepository,
    private val ramenShopRepository: RamenShopRepository,
    private val reportRepository: ShopReportRepository,
    private val currentLocationStore: CurrentLocationStore,
    private val reverseGeocoder: ReverseGeocoder? = null,
) : BaseViewModel<MyTabUiState, MyTabIntent, MyTabSideEffect>(initialState = MyTabUiState()) {
    private var placeReportJob: Job? = null
    private var addressRequestJob: Job? = null
    private var addressRequestGeneration = 0

    init {
        viewModelScope.launch { observeSessionState() }
        viewModelScope.launch { observeCurrentLocation() }
    }

    override suspend fun handleIntent(intent: MyTabIntent) {
        when (intent) {
            OnKakaoLoginClick -> signInWithKakao()
            OnLogoutClick -> signOut()
            OnAccountDeleteClick -> showAccountDeleteConfirmDialog()
            OnAccountDeleteDismiss -> hideAccountDeleteConfirmDialog()
            OnAccountDeleteConfirm -> deleteAccount()
            is OnPlaceUrlChanged -> updatePlaceUrl(intent.value)
            OnPlaceReportSubmit -> submitPlaceReport()
            OnCurrentLocationReportSubmit -> submitCurrentLocationReport()
            OnCurrentAddressRefresh -> refreshCurrentAddress()
            OnHiddenShopsClick -> openHiddenShops()
            OnLoginGuideDismiss -> hideLoginGuideDialog()
        }
    }

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collectLatest { sessionState ->
            val isAuthenticated = sessionState == LoginSessionState.AUTHENTICATED
            reduce {
                copy(
                    isLoggedIn = isAuthenticated,
                    accountLabel = if (isAuthenticated) loginRepository.currentUserEmail() else null,
                    isDeletingAccount = if (isAuthenticated) isDeletingAccount else false,
                    showAccountDeleteConfirmDialog = if (isAuthenticated) showAccountDeleteConfirmDialog else false,
                )
            }
        }
    }

    private suspend fun observeCurrentLocation() {
        currentLocationStore.location.collectLatest { location ->
            if (location == currentState.currentLocation) return@collectLatest
            addressRequestJob?.cancel()
            reduce { copy(currentLocation = location, currentAddress = null, isAddressRefreshing = false) }
            location?.let(::startAddressRequest)
        }
    }

    private fun refreshCurrentAddress() {
        val location = currentState.currentLocation ?: return
        startAddressRequest(location)
    }

    private fun startAddressRequest(location: Location) {
        val geocoder = reverseGeocoder ?: return
        if (addressRequestJob?.isActive == true) return
        val generation = ++addressRequestGeneration

        reduce { copy(isAddressRefreshing = true) }
        addressRequestJob =
            viewModelScope.launch {
                try {
                    handleResult(
                        result = geocoder.address(location),
                        onSuccess = { address ->
                            if (currentState.currentLocation == location) reduce { copy(currentAddress = address) }
                        },
                    )
                } finally {
                    if (generation == addressRequestGeneration) reduce { copy(isAddressRefreshing = false) }
                }
            }
    }

    private fun updatePlaceUrl(value: String) {
        reduce { copy(placeUrl = value) }
    }

    private fun showAccountDeleteConfirmDialog() {
        reduce { copy(showAccountDeleteConfirmDialog = true) }
    }

    private fun hideAccountDeleteConfirmDialog() {
        reduce { copy(showAccountDeleteConfirmDialog = false) }
    }

    private fun hideLoginGuideDialog() {
        reduce { copy(showLoginGuideDialog = false) }
    }

    private fun openHiddenShops() {
        if (!currentState.isLoggedIn) {
            reduce { copy(showLoginGuideDialog = true) }
            return
        }

        trySideEffect(NavigateToHiddenShops)
    }

    private fun submitPlaceReport() {
        val placeUrl = currentState.placeUrl
        startPlaceReport {
            val extractedPlaceUrl = PlaceReportTextParser.extractSupportedUrl(placeUrl)
            if (extractedPlaceUrl == null) {
                showToast(Res.string.place_report_invalid_url_message, ToastType.ERROR)
                return@startPlaceReport
            }

            val existingShop = findExistingShop(placeUrl)
            if (existingShop) {
                showToast(Res.string.place_report_existing_shop_message)
            } else {
                handleResult(
                    result =
                        reportRepository.submitUnregisteredPlaceReport(
                            UnregisteredPlaceReport(placeUrl = extractedPlaceUrl),
                        ),
                    onSuccess = {
                        reduce { copy(placeUrl = "") }
                        showToast(Res.string.place_report_success_message)
                    },
                    onError = {
                        showToast(
                            Res.string.place_report_failure_message,
                            ToastType.ERROR,
                        )
                    },
                )
            }
        }
    }

    private suspend fun findExistingShop(placeUrl: String): Boolean {
        val placeName = PlaceReportTextParser.extractSharedPlaceName(placeUrl) ?: return false
        return handleResultForValue(
            defaultValue = false,
            onSuccess = { shops ->
                shops.values.any { shop ->
                    PlaceReportTextParser.matchesSharedPlace(placeUrl, shop)
                }
            },
            block = {
                ramenShopRepository.searchRamenShops(
                    query = SearchQuery(placeName),
                    limit = SEARCH_RESULT_LIMIT,
                )
            },
        )
    }

    private fun submitCurrentLocationReport() {
        startPlaceReport {
            val location = currentState.currentLocation
            if (location == null) {
                showToast(Res.string.place_report_location_unavailable_message, ToastType.ERROR)
                return@startPlaceReport
            }

            handleResult(
                result =
                    reportRepository.submitUnregisteredPlaceReport(
                        UnregisteredPlaceReport(location = location),
                    ),
                onSuccess = { showToast(Res.string.place_report_success_message) },
                onError = { showToast(Res.string.place_report_failure_message, ToastType.ERROR) },
            )
        }
    }

    private fun startPlaceReport(block: suspend () -> Unit) {
        if (placeReportJob?.isActive == true) return
        placeReportJob = viewModelScope.launch { block() }
    }

    private suspend fun signInWithKakao() {
        reduce { copy(showLoginGuideDialog = false) }
        handleResult(
            result = loginRepository.signInWithKakao(),
            onError = { showToast(Res.string.kakao_login_failure_message, ToastType.ERROR) },
        )
    }

    private suspend fun signOut() {
        handleResult(result = loginRepository.signOut())
    }

    private suspend fun deleteAccount() {
        if (currentState.isDeletingAccount) return

        reduce {
            copy(
                isDeletingAccount = true,
                showAccountDeleteConfirmDialog = false,
            )
        }
        handleResult(
            result = loginRepository.deleteAccount(),
            onSuccess = { showToast(Res.string.account_delete_success_message, ToastType.SUCCESS) },
            onError = {
                reduce { copy(isDeletingAccount = false) }
                showToast(Res.string.account_delete_failure_message, ToastType.ERROR)
            },
        )
    }

    private fun showToast(
        messageResource: StringResource,
        type: ToastType = ToastType.DEFAULT,
    ) {
        trySideEffect(
            ShowMyToast(
                ToastData(
                    message = messageResource,
                    type = type,
                ),
            ),
        )
    }

    private suspend fun <T, R> handleResultForValue(
        defaultValue: R,
        onSuccess: suspend (T) -> R,
        block: suspend () -> RamapResult<T>,
    ): R {
        var output = defaultValue
        handleResult(
            result = block(),
            onSuccess = { output = onSuccess(it) },
        )
        return output
    }

    companion object {
        private const val SEARCH_RESULT_LIMIT = 10
    }
}
