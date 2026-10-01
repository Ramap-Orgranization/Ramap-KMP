package com.peto.ramap.ui.retry

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkRetryGeneratorTest {
    @Test
    fun `재시도 대기 중 취소해도 다음 복구에서 요청을 실행한다`() =
        runTest {
            val owner = Any()
            var requestCount = 0
            try {
                NetworkRetryGenerator.enqueue(owner, "read") { requestCount++ }
                val retry = backgroundScope.launch { NetworkRetryGenerator.retryPending() }
                runCurrent()
                advanceTimeBy(500)
                assertEquals(0, requestCount)
                retry.cancel()
                runCurrent()

                NetworkRetryGenerator.retryPending()
                assertEquals(1, requestCount)
            } finally {
                NetworkRetryGenerator.clear()
            }
        }

    @Test
    fun `로그아웃으로 대기 요청을 비우면 지연된 재시도를 실행하지 않는다`() =
        runTest {
            var requestCount = 0
            try {
                NetworkRetryGenerator.enqueue(Any(), "read") { requestCount++ }
                val retry = backgroundScope.launch { NetworkRetryGenerator.retryPending() }
                runCurrent()
                NetworkRetryGenerator.clear()
                advanceTimeBy(1_250)
                runCurrent()
                retry.join()

                assertEquals(0, requestCount)
            } finally {
                NetworkRetryGenerator.clear()
            }
        }

    @Test
    fun `동시에 복구 이벤트가 발생해도 같은 요청은 한 번만 실행한다`() =
        runTest {
            var requestCount = 0
            val response = CompletableDeferred<Unit>()
            try {
                NetworkRetryGenerator.enqueue(Any(), "read") {
                    requestCount++
                    response.await()
                }
                val first = backgroundScope.launch { NetworkRetryGenerator.retryPending() }
                runCurrent()
                advanceTimeBy(1_250)
                runCurrent()
                NetworkRetryGenerator.retryPending()

                assertEquals(1, requestCount)
                response.complete(Unit)
                first.join()
            } finally {
                NetworkRetryGenerator.clear()
            }
        }
}
