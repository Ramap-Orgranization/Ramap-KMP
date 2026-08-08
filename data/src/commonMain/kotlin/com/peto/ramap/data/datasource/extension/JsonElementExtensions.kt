package com.peto.ramap.data.datasource.extension

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** JSON 문자열 값을 공백 제거 후 반환한다. */
internal fun JsonPrimitive?.asText(): String? = this?.content?.trim()?.takeIf(String::isNotEmpty)

/** JSON 요소가 원시 값일 때 문자열로 반환한다. */
internal fun JsonElement?.asText(): String? = (this as? JsonPrimitive).asText()

/** JSON 요소가 원시 값일 때 Double로 변환한다. */
internal fun JsonElement?.asDouble(): Double? = (this as? JsonPrimitive)?.content?.toDoubleOrNull()
