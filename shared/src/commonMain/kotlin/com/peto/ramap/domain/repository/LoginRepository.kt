package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

interface LoginRepository {
    val sessionStatus: StateFlow<SessionStatus>

    suspend fun awaitInitialization()

    fun hasSession(): Boolean

    fun currentUserEmail(): String?

    suspend fun signInWithKakao(): RamapResult<Unit>

    suspend fun signOut(): RamapResult<Unit>

    suspend fun deleteAccount(): RamapResult<Unit>
}
