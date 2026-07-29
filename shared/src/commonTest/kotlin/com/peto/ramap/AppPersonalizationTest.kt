package com.peto.ramap

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppPersonalizationTest {
    @Test
    fun `인증 후 초기 동기화 실패 상태를 유지하고 사용자 재시도를 받는다`() =
        runTest {
            var refreshCount = 0
            val store =
                object : ShopPersonalizationStore by FakePersonalizationRepository() {
                    override suspend fun refresh(): RamapResult<Unit> {
                        refreshCount += 1
                        return if (refreshCount == 1) {
                            RamapResult.Error(
                                RamapError.Unknown(IllegalStateException("failure")),
                            )
                        } else {
                            RamapResult.Success(Unit)
                        }
                    }
                }
            val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

            backgroundScope.launch {
                observeSessionPersonalization(
                    loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                    personalizationStore = store,
                    retryRequests = retryRequests,
                )
            }
            runCurrent()

            assertEquals(1, refreshCount)

            retryRequests.emit(Unit)
            runCurrent()

            assertEquals(2, refreshCount)
        }

    @Test
    fun `초기 동기화 예외 후에도 사용자 재시도를 받는다`() =
        runTest {
            var refreshCount = 0
            val store =
                object : ShopPersonalizationStore by FakePersonalizationRepository() {
                    override suspend fun refresh(): RamapResult<Unit> {
                        refreshCount += 1
                        if (refreshCount == 1) {
                            throw IllegalStateException("failure")
                        }
                        return RamapResult.Success(Unit)
                    }
                }
            val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

            backgroundScope.launch {
                observeSessionPersonalization(
                    loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                    personalizationStore = store,
                    retryRequests = retryRequests,
                )
            }
            runCurrent()

            assertEquals(1, refreshCount)

            retryRequests.emit(Unit)
            runCurrent()

            assertEquals(2, refreshCount)
        }

    @Test
    fun `로그아웃 전이는 진행 중인 초기 동기화를 취소하고 상태를 비운다`() =
        runTest {
            val refreshStarted = CompletableDeferred<Unit>()
            var clearCount = 0
            val store =
                object : ShopPersonalizationStore by FakePersonalizationRepository() {
                    override suspend fun refresh(): RamapResult<Unit> {
                        refreshStarted.complete(Unit)
                        awaitCancellation()
                    }

                    override suspend fun clear() {
                        clearCount += 1
                    }
                }
            val loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED)

            backgroundScope.launch {
                observeSessionPersonalization(loginRepository, store)
            }
            refreshStarted.await()

            loginRepository.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            runCurrent()

            assertEquals(1, clearCount)
        }
}
