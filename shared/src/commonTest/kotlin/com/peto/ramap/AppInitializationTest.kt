package com.peto.ramap

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppInitializationTest {
    @Test
    fun sessionInitialization_opensAppBeforePersonalizationRefreshCompletes() =
        runTest {
            val loginRepository =
                FakeLoginRepository(initialSessionState = LoginSessionState.AUTHENTICATED)
            val refreshStarted = CompletableDeferred<Unit>()
            val personalizationStore =
                object : ShopPersonalizationStore by FakePersonalizationRepository() {
                    override suspend fun refresh(): RamapResult<Unit> {
                        refreshStarted.complete(Unit)
                        awaitCancellation()
                    }
                }
            var isInitialized = false

            val observation =
                launch {
                    observeSessionPersonalization(
                        loginRepository = loginRepository,
                        personalizationStore = personalizationStore,
                        onSessionInitialized = { isInitialized = true },
                    )
                }
            runCurrent()

            assertTrue(isInitialized)
            assertTrue(refreshStarted.isCompleted)
            observation.cancel()
        }

    @Test
    fun sessionChange_cancelsRefreshAndClearsPersonalization() =
        runTest {
            val loginRepository =
                FakeLoginRepository(initialSessionState = LoginSessionState.AUTHENTICATED)
            val refreshStarted = CompletableDeferred<Unit>()
            val refreshCancelled = CompletableDeferred<Unit>()
            var clearCallCount = 0
            val personalizationStore =
                object : ShopPersonalizationStore by FakePersonalizationRepository() {
                    override suspend fun refresh(): RamapResult<Unit> {
                        refreshStarted.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            refreshCancelled.complete(Unit)
                        }
                    }

                    override suspend fun clear() {
                        clearCallCount += 1
                    }
                }

            val observation =
                launch {
                    observeSessionPersonalization(
                        loginRepository = loginRepository,
                        personalizationStore = personalizationStore,
                        onSessionInitialized = {},
                    )
                }
            runCurrent()
            loginRepository.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            runCurrent()

            assertTrue(refreshStarted.isCompleted)
            assertTrue(refreshCancelled.isCompleted)
            assertEquals(1, clearCallCount)
            observation.cancel()
        }
}
