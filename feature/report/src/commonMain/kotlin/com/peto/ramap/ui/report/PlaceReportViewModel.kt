package com.peto.ramap.ui.report

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.report.PlaceReportTextParser
import com.peto.ramap.domain.model.report.UnregisteredPlaceReport
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ReverseGeocoder
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.platform.location.CurrentLocationProvider
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnCurrentLocationReportSubmit
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnLocationPermissionResult
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnPlaceReportSubmit
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnPlaceUrlChanged
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect.ShowToast
import com.peto.ramap.ui.report.contract.PlaceReportUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.place_report_existing_shop_message
import ramap.shared.generated.resources.place_report_failure_message
import ramap.shared.generated.resources.place_report_invalid_url_message
import ramap.shared.generated.resources.place_report_location_unavailable_message
import ramap.shared.generated.resources.place_report_success_message

class PlaceReportViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val reportRepository: ShopReportRepository,
    private val currentLocationStore: CurrentLocationStore,
    private val reverseGeocoder: ReverseGeocoder? = null,
    private val currentLocationProvider: CurrentLocationProvider,
) : BaseViewModel<PlaceReportUiState, PlaceReportIntent, PlaceReportSideEffect>(PlaceReportUiState()) {
    init {
        viewModelScope.launch { observeCurrentLocation() }
    }

    override suspend fun handleIntent(intent: PlaceReportIntent) {
        when (intent) {
            is OnPlaceUrlChanged -> reduce { copy(placeUrl = intent.value) }
            OnPlaceReportSubmit -> submitPlaceReport()
            OnCurrentLocationReportSubmit -> submitCurrentLocationReport()
            is OnLocationPermissionResult -> handleLocationPermission(intent.status)
        }
    }

    private suspend fun observeCurrentLocation() {
        currentLocationStore.location.collectLatest { location ->
            if (location == currentState.currentLocation) return@collectLatest
            reduce {
                copy(
                    currentLocation = location,
                    currentAddress = null,
                    isAddressRefreshing = false,
                )
            }
            location?.let { loadAddress(it) }
        }
    }

    private suspend fun handleLocationPermission(status: PermissionStatus) {
        when (status) {
            PermissionStatus.Granted -> loadCurrentLocation()
            PermissionStatus.Denied,
            PermissionStatus.Blocked,
            -> showToast(Res.string.location_permission_enable_message, ToastType.ERROR)
        }
    }

    private suspend fun loadCurrentLocation() {
        if (currentState.isLocationLoading) return

        reduce { copy(isLocationLoading = true) }
        val platformLocation =
            try {
                withTimeoutOrNull(LOCATION_REQUEST_TIMEOUT_MILLIS) {
                    currentLocationProvider.fetchCurrentLocation()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            } finally {
                reduce { copy(isLocationLoading = false) }
            }
        if (platformLocation == null) {
            showToast(Res.string.place_report_location_unavailable_message, ToastType.ERROR)
            return
        }

        currentLocationStore.update(
            Location(
                lat = platformLocation.latitude,
                lng = platformLocation.longitude,
            ),
        )
    }

    private suspend fun loadAddress(location: Location) {
        val geocoder = reverseGeocoder ?: return
        if (currentState.isAddressRefreshing) return

        reduce { copy(isAddressRefreshing = true) }
        try {
            handleResult(
                result = geocoder.address(location),
                onSuccess = { address ->
                    if (currentState.currentLocation == location) {
                        reduce {
                            copy(currentAddress = address)
                        }
                    }
                },
            )
        } finally {
            if (currentState.currentLocation == location) {
                reduce { copy(isAddressRefreshing = false) }
            }
        }
    }

    private suspend fun submitPlaceReport() {
        if (currentState.isSubmitting) return
        val placeUrl = currentState.placeUrl
        processPlaceReport(placeUrl)
    }

    private suspend fun processPlaceReport(placeUrl: String) {
        val extractedPlaceUrl = extractSupportedPlaceUrl(placeUrl) ?: return
        if (findExistingShop(placeUrl)) {
            showToast(Res.string.place_report_existing_shop_message)
            return
        }
        submitPlaceUrlReport(extractedPlaceUrl)
    }

    private fun extractSupportedPlaceUrl(placeUrl: String): String? {
        val extractedPlaceUrl = PlaceReportTextParser.extractSupportedUrl(placeUrl)
        if (extractedPlaceUrl == null) {
            showToast(Res.string.place_report_invalid_url_message, ToastType.ERROR)
        }
        return extractedPlaceUrl
    }

    private suspend fun submitPlaceUrlReport(placeUrl: String) {
        submitReport(UnregisteredPlaceReport(placeUrl = placeUrl)) {
            reduce { copy(placeUrl = "") }
            showToast(Res.string.place_report_success_message)
        }
    }

    private suspend fun findExistingShop(placeUrl: String): Boolean {
        val placeName = PlaceReportTextParser.extractSharedPlaceName(placeUrl) ?: return false
        var existingShop = false
        handleResult(
            result =
                ramenShopRepository.searchRamenShops(
                    query = SearchQuery(placeName),
                    limit = SEARCH_RESULT_LIMIT,
                ),
            onSuccess = { shops ->
                existingShop =
                    shops.values.any { PlaceReportTextParser.matchesSharedPlace(placeUrl, it) }
            },
        )
        return existingShop
    }

    private suspend fun submitCurrentLocationReport() {
        if (currentState.isSubmitting) return
        val location = currentState.currentLocation
        if (location == null) {
            showToast(Res.string.place_report_location_unavailable_message, ToastType.ERROR)
            return
        }

        submitReport(UnregisteredPlaceReport(location = location)) {
            showToast(Res.string.place_report_success_message)
        }
    }

    private suspend fun submitReport(
        report: UnregisteredPlaceReport,
        onSuccess: suspend () -> Unit,
    ) {
        reduce { copy(submitState = LoadState.Loading) }
        try {
            handleResult(
                result = reportRepository.submitUnregisteredPlaceReport(report),
                onSuccess = {
                    reduce { copy(submitState = LoadState.Content(Unit)) }
                    onSuccess()
                },
                onError = {
                    reduce { copy(submitState = LoadState.Error) }
                    showToast(Res.string.place_report_failure_message, ToastType.ERROR)
                },
            )
        } finally {
            if (currentState.submitState == LoadState.Loading) {
                reduce { copy(submitState = LoadState.Error) }
            }
        }
    }

    private fun showToast(
        messageResource: StringResource,
        type: ToastType = ToastType.DEFAULT,
    ) {
        viewModelScope.launch {
            postSideEffect(ShowToast(ToastData(messageResource, type)))
        }
    }

    companion object {
        private const val SEARCH_RESULT_LIMIT = 10
        private const val LOCATION_REQUEST_TIMEOUT_MILLIS = 10_000L
    }
}
