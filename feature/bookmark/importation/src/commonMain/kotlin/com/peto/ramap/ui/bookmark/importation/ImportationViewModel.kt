package com.peto.ramap.ui.bookmark.importation

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.importation.ImportationCandidates
import com.peto.ramap.domain.model.importation.ImportationPreview
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.report.PlaceReportTextParser
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.ImportationRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.bookmark.importation.contract.ImportationError
import com.peto.ramap.ui.bookmark.importation.contract.ImportationIntent
import com.peto.ramap.ui.bookmark.importation.contract.ImportationLoadKey
import com.peto.ramap.ui.bookmark.importation.contract.ImportationSideEffect
import com.peto.ramap.ui.bookmark.importation.contract.ImportationUiState
import com.peto.ramap.ui.bookmark.importation.log.ImportationAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.importation_completed

class ImportationViewModel(
    private val personalizationStore: ShopPersonalizationStore,
    private val ramenShopRepository: RamenShopRepository,
    private val importationRepository: ImportationRepository,
    private val importationAnalytics: ImportationAnalytics,
) : BaseViewModel<ImportationUiState, ImportationIntent, ImportationSideEffect>(ImportationUiState()) {
    override suspend fun handleIntent(intent: ImportationIntent) {
        when (intent) {
            is ImportationIntent.UrlChanged -> changeUrl(intent.url)
            is ImportationIntent.CandidateRemoved -> removeCandidate(intent.shopId)
            ImportationIntent.Analyze -> analyze()
            ImportationIntent.Retry -> analyze()
            ImportationIntent.Confirm -> addBookmarks()
            ImportationIntent.Reset -> reset()
        }
    }

    private fun changeUrl(url: String) {
        reduce { copy(url = PlaceReportTextParser.extractSupportedUrl(url) ?: url, error = null) }
    }

    private fun removeCandidate(shopId: String) {
        reduce { copy(candidates = candidates.remove(shopId)) }
    }

    private fun analyze() {
        val url = currentState.url.trim()
        if (url.isEmpty()) return publishError(ImportationError.INVALID_URL)
        val personalization =
            currentPersonalization()
                ?: return publishError(ImportationError.PERSONALIZATION_UNAVAILABLE)
        launchTask(
            taskKey = ANALYZE_TASK_KEY,
            loadKey = ImportationLoadKey.ANALYZE,
            policy = TaskPolicy.IgnoreNew,
            onStart = { clearPreview(this) },
        ) { requestPreview(url, personalization) }
    }

    private suspend fun requestPreview(
        url: String,
        personalization: ShopPersonalization,
    ) {
        when (val result = importationRepository.analyze(url)) {
            is RamapResult.Error -> publishError(ImportationErrorHandler.resolveAnalyzeError(result.error))
            is RamapResult.Success -> {
                importationAnalytics.logMatchFailures(
                    provider = result.data.provider,
                    placeNames = result.data.unmatchedPlaceNames,
                )
                requestCandidates(result.data, personalization)
            }
        }
    }

    private suspend fun requestCandidates(
        preview: ImportationPreview,
        personalization: ShopPersonalization,
    ) {
        val resolved: ImportationCandidates = ImportationCandidates.from(preview, personalization)
        if (resolved.importableShopIds.isEmpty()) {
            publishPreview(preview, resolved, RamenShops(emptyMap()))
            return
        }
        when (val result = ramenShopRepository.fetchRamenShops(resolved.importableShopIds)) {
            is RamapResult.Error -> publishError(ImportationError.ANALYZE_FAILED)
            is RamapResult.Success -> publishPreview(preview, resolved, result.data)
        }
    }

    private fun publishPreview(
        preview: ImportationPreview,
        resolved: ImportationCandidates,
        shops: RamenShops,
    ) {
        reduce {
            copy(
                preview = preview,
                candidates = shops.filterByShopIds(resolved.importableShopIds),
                alreadyBookmarkedCount = resolved.alreadyBookmarkedCount,
                hiddenCount = resolved.hiddenCount,
                error = null,
            )
        }
    }

    private fun addBookmarks() {
        val shopIds = currentState.candidates.keys
        if (shopIds.isEmpty()) return

        launchResultTask(
            taskKey = CONFIRM_TASK_KEY,
            loadKey = ImportationLoadKey.CONFIRM,
            policy = TaskPolicy.IgnoreNew,
            request = { personalizationStore.addBookmarks(shopIds) },
            onSuccess = { completeImport() },
            onError = { error -> publishError(ImportationErrorHandler.resolveConfirmError(error)) },
        )
    }

    private suspend fun completeImport() {
        postSideEffect(
            ImportationSideEffect.ImportCompleted(
                ToastData(Res.string.importation_completed, ToastType.SUCCESS),
            ),
        )
    }

    private fun reset() {
        cancelTask(ANALYZE_TASK_KEY)
        cancelTask(CONFIRM_TASK_KEY)
        reduce { clearPreview(this).copy(url = "") }
    }

    private fun currentPersonalization(): ShopPersonalization? =
        (personalizationStore.state.value as? PersonalizationBootstrapState.Success)?.value

    private fun publishError(error: ImportationError) {
        reduce { copy(error = error) }
        trySideEffect(
            ImportationSideEffect.showToast(
                ToastData(ImportationErrorHandler.resourceFor(error), ToastType.ERROR),
            ),
        )
    }

    private fun clearPreview(state: ImportationUiState): ImportationUiState =
        state.copy(
            preview = null,
            candidates = RamenShops(emptyMap()),
            alreadyBookmarkedCount = 0,
            hiddenCount = 0,
            error = null,
        )

    private companion object {
        const val ANALYZE_TASK_KEY = "analyze-map-list"
        const val CONFIRM_TASK_KEY = "confirm-importation"
    }
}
