package com.peto.ramap.network.auth

internal interface SecureSessionStorage {
    /** null은 아직 이관되지 않은 저장소, 빈 문자열은 명시적으로 삭제된 세션입니다. */
    suspend fun read(): String?

    /** 호출이 끝나기 전에 새 값을 영속적으로 저장합니다. */
    suspend fun write(value: String)
}
