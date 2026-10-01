package com.peto.ramap.ui.report

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.report.PlaceReportTextParser
import com.peto.ramap.domain.model.report.UnregisteredPlaceReport
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.PlaceLinkResolver
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.report.contract.PlaceReportIntent
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnPlaceReportSubmit
import com.peto.ramap.ui.report.contract.PlaceReportIntent.OnPlaceUrlChanged
import com.peto.ramap.ui.report.contract.PlaceReportLoadKey
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect
import com.peto.ramap.ui.report.contract.PlaceReportSideEffect.ShowToast
import com.peto.ramap.ui.report.contract.PlaceReportUiState
import com.peto.ramap.ui.task.TaskPolicy
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.place_report_existing_shop_message
import ramap.shared.generated.resources.place_report_failure_message
import ramap.shared.generated.resources.place_report_invalid_url_message
import ramap.shared.generated.resources.place_report_success_message

class PlaceReportViewModel(
    private val ramenShopRepository: RamenShopRepository,
    private val placeLinkResolver: PlaceLinkResolver,
    private val reportRepository: ShopReportRepository,
) : BaseViewModel<PlaceReportUiState, PlaceReportIntent, PlaceReportSideEffect>(PlaceReportUiState()) {
    override suspend fun handleIntent(intent: PlaceReportIntent) {
        when (intent) {
            is OnPlaceUrlChanged -> reduce { copy(placeUrl = intent.value) }
            OnPlaceReportSubmit -> submitPlaceReport()
        }
    }

    private fun submitPlaceReport() {
        val placeUrl = currentState.placeUrl
        launchSubmission { processPlaceReport(placeUrl) }
    }

    private suspend fun processPlaceReport(placeUrl: String) {
        val extractedPlaceUrl = extractSupportedPlaceUrl(placeUrl) ?: return
        val existingShop = findExistingShop(placeUrl, extractedPlaceUrl) ?: return
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

    private suspend fun findExistingShop(
        placeUrl: String,
        extractedPlaceUrl: String,
    ): Boolean? {
        val placeName = PlaceReportTextParser.extractSharedPlaceName(placeUrl)
        val resolvedPlace =
            if (placeName == null) {
                placeLinkResolver.resolve(extractedPlaceUrl)
            } else {
                null
            }
        val searchQuery = placeName ?: resolvedPlace?.name ?: resolvedPlace?.placeId ?: return false
        var existingShop: Boolean? = null
        handleResult(
            result =
                ramenShopRepository.searchRamenShops(
                    query = SearchQuery(searchQuery),
                    limit = SEARCH_RESULT_LIMIT,
                ),
            onSuccess = { shops ->
                existingShop =
                    shops.values.any { shop ->
                        PlaceReportTextParser.matchesSharedPlace(placeUrl, shop) ||
                            (resolvedPlace != null && PlaceReportTextParser.matchesResolvedPlace(resolvedPlace, shop))
                    }
            },
            onError = { showToast(Res.string.place_report_failure_message, ToastType.ERROR) },
        )
        return existingShop
    }

    /** URL 제보 제출 작업을 실행하고 실행 중 추가 제출은 무시한다. */
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

        private const val SUBMIT_TASK_KEY = "submit-place-report"
    }
}
