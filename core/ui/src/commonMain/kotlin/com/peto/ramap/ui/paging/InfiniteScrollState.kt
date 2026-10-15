package com.peto.ramap.ui.paging

/**
 * [itemCount]는 헤더·푸터를 제외한 데이터 개수이며 같은 개수의 중복 요청을 막는 기준이다.
 * [itemKeys]를 지정하면 해당 데이터 항목의 끝부분 노출로 추가 로딩을 판단한다.
 * [requestKey]는 사용자·검색어 등 목록의 정체성이 달라질 때 요청 기록을 초기화한다.
 */
data class InfiniteScrollState(
    val itemCount: Int,
    val hasMore: Boolean,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val itemKeys: List<Any>? = null,
    val requestKey: Any? = null,
)
