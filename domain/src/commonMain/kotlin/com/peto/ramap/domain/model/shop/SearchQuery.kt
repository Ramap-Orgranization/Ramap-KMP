package com.peto.ramap.domain.model.shop

import kotlin.jvm.JvmInline

@JvmInline
value class SearchQuery(
    val value: String,
) {
    /**
     * 검색 비교에 사용할 표준 형태의 검색어를 반환한다.
     *
     * 앞뒤 공백을 제거하고, 중간의 연속 공백을 하나로 합친 뒤, 대소문자 차이 없이
     * 비교할 수 있도록 소문자로 변환한다.
     */
    fun normalizeShopSearchQuery(): SearchQuery =
        SearchQuery(
            value
                .trim()
                .replace(Regex("\\s+"), " ")
                .lowercase(),
        )

    /**
     * Supabase/PostgREST `ilike` 검색 패턴에서 특별한 의미를 갖는 문자를 이스케이프한다.
     *
     * 사용자 입력에 포함된 `\`, `%`, `_`가 와일드카드나 escape 문자로 해석되지 않고
     * 일반 문자 그대로 검색되도록 변환한다.
     */
    fun escapeIlikePattern(): String =
        value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    /**
     * `ilike` 연산에서 사용할 포함 검색 패턴을 생성한다.
     *
     * 사용자 입력을 먼저 [escapeIlikePattern]으로 이스케이프한 뒤,
     * 앞뒤에 `%` 와일드카드를 추가하여 문자열 어디에 검색어가 포함되어 있어도
     * 매칭될 수 있는 패턴을 반환한다.
     */
    fun ilikePattern(): String = "%${escapeIlikePattern()}%"
}
