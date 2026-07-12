package com.peto.ramap.core.result

sealed interface RamapError {
    val cause: Throwable?

    data class Network(
        override val cause: Throwable,
    ) : RamapError

    data class Timeout(
        override val cause: Throwable,
    ) : RamapError

    data class Http(
        val status: Int,
        override val cause: Throwable? = null,
    ) : RamapError

    data class Serialization(
        override val cause: Throwable,
    ) : RamapError

    data class Unknown(
        override val cause: Throwable,
    ) : RamapError
}
