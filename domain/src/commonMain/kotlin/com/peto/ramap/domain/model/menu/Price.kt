package com.peto.ramap.domain.model.menu

@JvmInline
value class Price(
    val krw: Int,
) {
    val formatted: String
        get() =
            krw
                .toString()
                .reversed()
                .chunked(3)
                .joinToString(",")
                .reversed()
}
