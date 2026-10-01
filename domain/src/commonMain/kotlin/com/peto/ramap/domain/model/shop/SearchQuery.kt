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
     * `ilike` 연산에서 사용할 공백 무시 포함 검색 패턴을 생성한다.
     *
     * 공백을 제외한 각 문자를 이스케이프하고 그 사이에 `%` 와일드카드를 넣어,
     * 저장된 상호에 포함된 공백과 관계없이 매칭될 수 있는 패턴을 반환한다.
     */
    fun ilikePattern(): String =
        buildString {
            append('%')
            value.forEach { character ->
                if (!character.isWhitespace()) {
                    when (character) {
                        '\\' -> append("\\\\")
                        '%' -> append("\\%")
                        '_' -> append("\\_")
                        else -> append(character)
                    }
                    append('%')
                }
            }
        }
}
