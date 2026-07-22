package com.peto.ramap.ui.loading

import androidx.compose.runtime.Immutable

@Immutable
@ConsistentCopyVisibility
data class LoadState internal constructor(
    private val value: Map<LoadKey, Int>,
) {
    /** 활성 작업이 없는 빈 로딩 상태를 만든다. */
    constructor() : this(emptyMap())

    /** 하나 이상의 로딩 작업이 활성 상태인지 여부. */
    val isAnyLoading: Boolean
        get() = value.isNotEmpty()

    /**
     * [key]에 해당하는 로딩 작업이 하나 이상 활성 상태인지 확인한다.
     *
     * @param key 확인할 화면 로딩 키
     */
    fun isLoading(key: LoadKey): Boolean = activeCount(key) > 0

    /**
     * [key]를 공유하는 활성 작업 수를 반환한다.
     *
     * 등록된 작업이 없으면 `0`을 반환한다.
     */
    private fun activeCount(key: LoadKey): Int = value[key] ?: 0

    /** 작업 시작과 짝을 이루어 [key]의 활성 개수를 증가시킨다. */
    internal operator fun plus(key: LoadKey): LoadState = copy(value = value + (key to activeCount(key) + 1))

    /** 작업 종료와 짝을 이루어 [key]의 활성 개수를 감소시킨다. */
    internal operator fun minus(key: LoadKey): LoadState {
        val currentCount = activeCount(key)

        val nextCounts =
            if (currentCount == 1) {
                value - key
            } else {
                value + (key to currentCount - 1)
            }
        return copy(value = nextCounts)
    }
}
