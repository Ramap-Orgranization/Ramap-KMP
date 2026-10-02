package com.peto.ramap.domain.usecase

import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import kotlinx.coroutines.flow.Flow

class LoginSessionUseCase(
    private val loginRepository: LoginRepository,
) {
    operator fun invoke(): Flow<LoginSessionState> = loginRepository.sessionState

    fun hasSession(): Boolean = loginRepository.hasSession()

    fun currentUserEmail(): String? = loginRepository.currentUserEmail()
}
