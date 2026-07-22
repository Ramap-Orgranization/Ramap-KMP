package com.peto.ramap.ui.loading

import com.peto.ramap.ui.base.State

/**
 * 키 기반 로딩 상태를 포함하는 UI 상태의 계약.
 *
 * @param S [loadState]를 교체한 뒤 반환할 구체 UI 상태 타입
 */
interface LoadableState<S : State> : State {
    /** 현재 화면의 키별 로딩 카운트. */
    val loadState: LoadState

    /**
     * 나머지 UI 값을 유지하면서 [loadState]만 교체한 새 상태를 반환한다.
     *
     * @param loadState 적용할 불변 로딩 상태
     */
    fun withLoadingState(loadState: LoadState): S
}
