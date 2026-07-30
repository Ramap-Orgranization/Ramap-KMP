package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginSessionState
import kotlinx.coroutines.flow.Flow

interface LoginRepository {
    val sessionState: Flow<LoginSessionState>

    suspend fun awaitInitialization()

    fun hasSession(): Boolean

    fun currentUserEmail(): String?

    suspend fun signInWithKakao(): RamapResult<Unit>

    suspend fun signOut(): RamapResult<Unit>

    suspend fun deleteAccount(): RamapResult<Unit>
}
