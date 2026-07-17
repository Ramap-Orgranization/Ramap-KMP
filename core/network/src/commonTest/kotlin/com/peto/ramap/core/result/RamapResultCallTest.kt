package com.peto.ramap.core.result

import com.peto.ramap.network.execute.invokeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RamapResultCallTest {
    @Test
    fun `IO 예외를 Network 오류로 변환한다`() =
        runTest {
            val result = invokeRequest<String> { throw IOException("offline") }

            assertIs<RamapError.Network>((result as RamapResult.Error).error)
        }

    @Test
    fun `취소 예외는 결과로 변환하지 않는다`() =
        runTest {
            assertFailsWith<CancellationException> {
                invokeRequest<String> { throw CancellationException("cancelled") }
            }
        }

    @Test
    fun `실패하면 한 번만 재시도한다`() =
        runTest {
            var calls = 0

            val result =
                retryOnce {
                    calls += 1
                    if (calls == 1) {
                        RamapResult.Error(RamapError.Unknown(IllegalStateException()))
                    } else {
                        RamapResult.Success("success")
                    }
                }

            assertEquals(2, calls)
            assertEquals(RamapResult.Success("success"), result)
        }
}
