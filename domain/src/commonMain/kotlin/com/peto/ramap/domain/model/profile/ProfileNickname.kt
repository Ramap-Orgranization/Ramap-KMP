package com.peto.ramap.domain.model.profile

object ProfileNickname {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 10

    private val allowed = Regex("^[A-Za-z0-9가-힣_]{2,10}$")

    fun isValid(value: String): Boolean = allowed.matches(value)
}
