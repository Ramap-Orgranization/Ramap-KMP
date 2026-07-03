package com.peto.ramap.domain.repository

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

interface LoginRepository {
    val sessionStatus: StateFlow<SessionStatus>

    suspend fun awaitInitialization()

    fun hasSession(): Boolean

    fun currentUserEmail(): String?

    suspend fun signInWithKakao()

    suspend fun signOut()
}
