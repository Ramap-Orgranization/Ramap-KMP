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
import com.peto.ramap.ui.location.CurrentLocationStore
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnCurrentLocationReportSubmit
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnLocationPermissionResult
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnPlaceReportSubmit
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnPlaceUrlChanged
import com.peto.ramap.ui.report.contract.PlaceReportLoadKey
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect.ShowToast
import com.peto.ramap.ui.report.contract.PlaceReportUiState
import com.peto.ramap.ui.task.TaskPolicy
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
import kotlin.time.Duration.Companion.milliseconds

class PlaceReportViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val reportRepository: ShopReportRepository,
    private val currentLocationStore: CurrentLocationStore,
    private val reverseGeocoder: ReverseGeocoder,
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
            cancelTask(ADDRESS_TASK_KEY)
            reduce {
                copy(
                    currentLocation = location,
                    currentAddress = null,
                )
            }
            location?.let { loadAddress(it) }
        }
    }

    private fun handleLocationPermission(status: PermissionStatus) {
        when (status) {
            PermissionStatus.Granted -> loadCurrentLocation()
            PermissionStatus.Denied,
            PermissionStatus.Blocked,
            -> showToast(Res.string.location_permission_enable_message, ToastType.ERROR)
        }
    }

    private fun loadCurrentLocation() {
        launchTask(
            taskKey = CURRENT_LOCATION_TASK_KEY,
            loadKey = PlaceReportLoadKey.CurrentLocation,
            policy = TaskPolicy.IgnoreNew,
        ) {
            val platformLocation =
                try {
                    withTimeoutOrNull(LOCATION_REQUEST_TIMEOUT_MILLIS.milliseconds) {
                        currentLocationProvider.fetchCurrentLocation()
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
            if (platformLocation == null) {
                showToast(Res.string.place_report_location_unavailable_message, ToastType.ERROR)
                return@launchTask
            }

            currentLocationStore.update(
                Location(
                    lat = platformLocation.latitude,
                    lng = platformLocation.longitude,
                ),
            )
        }
    }

    /** 이전 주소 변환을 취소하고 결과가 현재 위치와 일치할 때만 반영한다. */
    private fun loadAddress(location: Location) {
        launchResultTask(
            taskKey = ADDRESS_TASK_KEY,
            loadKey = PlaceReportLoadKey.Address,
            policy = TaskPolicy.CancelPrevious,
            retryOnNetworkError = true,
            request = { reverseGeocoder.address(location) },
            onSuccess = { address ->
                if (currentState.currentLocation == location) {
                    reduce { copy(currentAddress = address) }
                }
            },
        )
    }

    private fun submitPlaceReport() {
        val placeUrl = currentState.placeUrl
        launchSubmission { processPlaceReport(placeUrl) }
    }

    private suspend fun processPlaceReport(placeUrl: String) {
        val extractedPlaceUrl = extractSupportedPlaceUrl(placeUrl) ?: return
        val existingShop = findExistingShop(placeUrl) ?: return
        if (existingShop) {
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

    private suspend fun findExistingShop(placeUrl: String): Boolean? {
        val placeName = PlaceReportTextParser.extractSharedPlaceName(placeUrl) ?: return false
        var existingShop: Boolean? = null
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
            onError = { showToast(Res.string.place_report_failure_message, ToastType.ERROR) },
        )
        return existingShop
    }

    private fun submitCurrentLocationReport() {
        val location = currentState.currentLocation
        if (location == null) {
            showToast(Res.string.place_report_location_unavailable_message, ToastType.ERROR)
            return
        }

        launchSubmission {
            submitReport(UnregisteredPlaceReport(location = location)) {
                showToast(Res.string.place_report_success_message)
            }
        }
    }

    /** 두 제보 경로가 공유하는 제출 작업을 실행하고 실행 중 추가 제출은 무시한다. */
    private fun launchSubmission(block: suspend () -> Unit) {
        launchTask(
            taskKey = SUBMIT_TASK_KEY,
            loadKey = PlaceReportLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
        ) {
            block()
        }
    }

    private suspend fun submitReport(
        report: UnregisteredPlaceReport,
        onSuccess: suspend () -> Unit,
    ) {
        handleResult(
            result = reportRepository.submitUnregisteredPlaceReport(report),
            onSuccess = { onSuccess() },
            onError = {
                showToast(Res.string.place_report_failure_message, ToastType.ERROR)
            },
        )
    }

    private fun showToast(
        messageResource: StringResource,
        type: ToastType = ToastType.DEFAULT,
    ) {
        trySideEffect(ShowToast(ToastData(messageResource, type)))
    }

    companion object {
        private const val SEARCH_RESULT_LIMIT = 10
        private const val LOCATION_REQUEST_TIMEOUT_MILLIS = 10_000L

        private const val SUBMIT_TASK_KEY = "submit-place-report"
        private const val CURRENT_LOCATION_TASK_KEY = "place-report-current-location"
        private const val ADDRESS_TASK_KEY = "place-report-address"
    }
}
