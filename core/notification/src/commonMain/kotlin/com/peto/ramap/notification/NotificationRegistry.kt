package com.peto.ramap.notification

import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.PushRegistrationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationRegistry(
    private val loginRepository: LoginRepository,
    private val pushRegistrationRepository: PushRegistrationRepository,
    private val scope: CoroutineScope,
) {
    private var registrationJob: Job? = null

    fun track(
        identifier: String,
        platform: String,
        targetType: String,
    ) {
        if (identifier.isBlank()) return
        registrationJob?.cancel()
        registrationJob =
            scope.launch {
                loginRepository.accountId.collectLatest { accountId ->
                    if (accountId == null || loginRepository.currentAccountId() != accountId) return@collectLatest
                    try {
                        pushRegistrationRepository.register(
                            identifier = identifier,
                            platform = platform,
                            targetType = targetType,
                            expectedAccountId = accountId,
                            supportedProtocolVersion = NotificationDeepLinkParser.PROTOCOL_VERSION,
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // A new identity or token will retry registration, never under the previous account.
                    }
                }
            }
    }
}
