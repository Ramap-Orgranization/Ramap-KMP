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
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.common.CurrentLocationStore
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnCurrentAddressRefresh
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnCurrentLocationReportSubmit
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnPlaceReportSubmit
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnPlaceUrlChanged
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect.ShowToast
import com.peto.ramap.ui.report.contract.PlaceReportUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
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
) : BaseViewModel<PlaceReportUiState, PlaceReportIntent, PlaceReportSideEffect>(PlaceReportUiState()) {
    private var placeReportJob: Job? = null
    private var addressRequestJob: Job? = null
    private var addressRequestGeneration = 0

    init {
        viewModelScope.launch { observeCurrentLocation() }
    }

    override suspend fun handleIntent(intent: PlaceReportIntent) {
        when (intent) {
            is OnPlaceUrlChanged -> reduce { copy(placeUrl = intent.value) }
            OnPlaceReportSubmit -> submitPlaceReport()
            OnCurrentLocationReportSubmit -> submitCurrentLocationReport()
            OnCurrentAddressRefresh -> refreshCurrentAddress()
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
        currentState.currentLocation?.let(::startAddressRequest)
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

    private fun submitPlaceReport() {
        val placeUrl = currentState.placeUrl
        startPlaceReport {
            val extractedPlaceUrl = PlaceReportTextParser.extractSupportedUrl(placeUrl)
            if (extractedPlaceUrl == null) {
                showToast(Res.string.place_report_invalid_url_message, ToastType.ERROR)
                return@startPlaceReport
            }

            if (findExistingShop(placeUrl)) {
                showToast(Res.string.place_report_existing_shop_message)
                return@startPlaceReport
            }

            handleResult(
                result =
                    reportRepository.submitUnregisteredPlaceReport(
                        UnregisteredPlaceReport(placeUrl = extractedPlaceUrl),
                    ),
                onSuccess = {
                    reduce { copy(placeUrl = "") }
                    showToast(Res.string.place_report_success_message)
                },
                onError = { showToast(Res.string.place_report_failure_message, ToastType.ERROR) },
            )
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
                existingShop = shops.values.any { PlaceReportTextParser.matchesSharedPlace(placeUrl, it) }
            },
        )
        return existingShop
    }

    private fun submitCurrentLocationReport() {
        startPlaceReport {
            val location = currentState.currentLocation
            if (location == null) {
                showToast(Res.string.place_report_location_unavailable_message, ToastType.ERROR)
                return@startPlaceReport
            }

            handleResult(
                result = reportRepository.submitUnregisteredPlaceReport(UnregisteredPlaceReport(location = location)),
                onSuccess = { showToast(Res.string.place_report_success_message) },
                onError = { showToast(Res.string.place_report_failure_message, ToastType.ERROR) },
            )
        }
    }

    private fun startPlaceReport(block: suspend () -> Unit) {
        if (placeReportJob?.isActive == true) return
        placeReportJob = viewModelScope.launch { block() }
    }

    private fun showToast(
        messageResource: StringResource,
        type: ToastType = ToastType.DEFAULT,
    ) {
        trySideEffect(ShowToast(ToastData(messageResource, type)))
    }

    companion object {
        private const val SEARCH_RESULT_LIMIT = 10
    }
}
