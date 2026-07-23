package com.peto.ramap.ui.main.map.viewport

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.repository.RamenShopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 현재 지도 화면 영역에 필요한 라멘 매장을 지연 조회하고 중복 요청을 줄인다.
 *
 * 지도 영역이 연속으로 변경되면 마지막 요청만 실행하도록 debounce한다. 실제 조회 시에는
 * 화면보다 넓은 영역을 미리 불러오며, 이후 요청 영역이 마지막 조회 영역에 포함되면 저장소를
 * 다시 호출하지 않는다. 이전 요청이 취소에 협조하지 않고 완료되더라도 요청 ID를 비교해 오래된
 * 결과가 전달되지 않도록 한다.
 *
 * @property repository 지도 영역에 포함된 라멘 매장을 조회하는 저장소
 * @property coroutineScope 지연 조회 작업을 실행하는 코루틴 스코프
 */
internal class ViewportShopLoader(
    private val repository: RamenShopRepository,
    private val coroutineScope: CoroutineScope,
) {
    /** 현재 예약되었거나 실행 중인 영역 조회 작업. */
    private var job: Job? = null

    /** 최신 영역 조회 요청을 식별하는 단조 증가 ID. */
    private var requestId = 0L

    /** 마지막으로 조회에 성공한, 미리 불러오기 영역이 반영된 지도 범위. */
    private var lastLoadedBounds: MapBounds? = null

    /**
     * [bounds]를 기준으로 라멘 매장 조회한다.
     *
     * [bounds]가 마지막으로 조회한 영역에 포함되면 별도 결과를 전달하지 않는다.
     * 저장소 조회가 필요한 경우 확장된 영역을 사용하며,
     * 최신 요청의 성공 또는 실패 결과만 [onResult]에 전달한다.
     *
     * @param bounds 사용자가 현재 보고 있는 지도 영역
     * @param onResult 최신 영역 조회의 성공 또는 실패 결과를 전달받는 콜백
     */
    fun schedule(
        bounds: MapBounds,
        onResult: suspend (ViewportLoadResult) -> Unit,
    ) {
        cancelPendingLoad()
        val currentRequestId = requestId
        job =
            coroutineScope.launch {
                loadAfterDebounce(bounds, currentRequestId, onResult)
            }
    }

    /**
     * 예약되었거나 실행 중인 조회를 취소하고 해당 요청의 결과를 무효화한다.
     *
     * 저장소가 코루틴 취소에 협조하지 않더라도 결과를 폐기할 수 있도록 [requestId]를 증가시킨다.
     */
    private fun cancelPendingLoad() {
        job?.cancel()
        requestId += 1
    }

    /**
     * 연속된 지도 이동이 끝날 때까지 기다린 뒤 필요한 영역을 조회한다.
     *
     * [bounds]가 마지막 성공 영역에 포함되면 저장소를 호출하지 않는다.
     */
    private suspend fun loadAfterDebounce(
        bounds: MapBounds,
        currentRequestId: Long,
        onResult: suspend (ViewportLoadResult) -> Unit,
    ) {
        delay(LOAD_DEBOUNCE_MILLIS.milliseconds)
        if (isAlreadyLoaded(bounds)) return

        loadExpandedBounds(bounds, currentRequestId, onResult)
    }

    /** [bounds]가 마지막으로 조회에 성공한 미리 불러오기 영역 안에 포함되는지 확인한다. */
    private fun isAlreadyLoaded(bounds: MapBounds): Boolean = lastLoadedBounds?.contains(bounds) == true

    /**
     * [bounds]에 미리 불러오기 범위를 적용해 저장소에서 매장을 조회한다.
     */
    private suspend fun loadExpandedBounds(
        bounds: MapBounds,
        currentRequestId: Long,
        onResult: suspend (ViewportLoadResult) -> Unit,
    ) {
        val expandedBounds = bounds.expandBy(PREFETCH_RATIO)
        val result = repository.fetchRamenShops(expandedBounds)
        deliverIfCurrent(currentRequestId, expandedBounds, result, onResult)
    }

    /**
     * [currentRequestId]가 최신 요청일 때만 조회 결과를 반영한다.
     *
     * 성공한 경우에만 [lastLoadedBounds]를 갱신하고, 성공과 실패 모두 최신 요청의 콜백으로 전달한다.
     */
    private suspend fun deliverIfCurrent(
        currentRequestId: Long,
        expandedBounds: MapBounds,
        result: RamapResult<RamenShops>,
        onResult: suspend (ViewportLoadResult) -> Unit,
    ) {
        if (!isCurrentRequest(currentRequestId)) return

        when (result) {
            is RamapResult.Success -> {
                lastLoadedBounds = expandedBounds
                onResult(ViewportLoadResult.Loaded(result.data))
            }

            is RamapResult.Error ->
                onResult(ViewportLoadResult.Failed(result.error))
        }
    }

    /** [currentRequestId]가 현재 최신 영역 조회 요청의 ID인지 확인한다. */
    private fun isCurrentRequest(currentRequestId: Long): Boolean = currentRequestId == requestId

    companion object {
        /** 현재 지도 영역에 추가 적용할 미리 불러오기 비율. */
        private const val PREFETCH_RATIO = 0.5

        /** 연속된 지도 이동이 끝날 때까지 조회를 지연하는 시간. */
        private const val LOAD_DEBOUNCE_MILLIS = 350L
    }
}
