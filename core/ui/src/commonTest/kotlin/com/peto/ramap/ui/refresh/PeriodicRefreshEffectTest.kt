package com.peto.ramap.ui.refresh

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodicRefreshEffectTest {
    @Test
    fun `비활성 상태에서는 갱신을 중단하고 복귀 후 간격을 다시 계산한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner =
                object : LifecycleOwner {
                    override val lifecycle = LifecycleRegistry.createUnsafe(this)
                }
            var refreshCount = 0
            try {
                owner.lifecycle.currentState = Lifecycle.State.STARTED
                val polling = backgroundScope.launch { runPeriodicRefresh(owner.lifecycle, 1_000) { refreshCount++ } }
                runCurrent()
                advanceTimeBy(5_000)
                runCurrent()
                assertEquals(0, refreshCount)

                owner.lifecycle.currentState = Lifecycle.State.RESUMED
                runCurrent()
                advanceTimeBy(1_000)
                runCurrent()
                assertEquals(1, refreshCount)

                owner.lifecycle.currentState = Lifecycle.State.CREATED
                runCurrent()
                advanceTimeBy(5_000)
                runCurrent()
                assertEquals(1, refreshCount)

                owner.lifecycle.currentState = Lifecycle.State.RESUMED
                runCurrent()
                advanceTimeBy(999)
                runCurrent()
                assertEquals(1, refreshCount)
                advanceTimeBy(1)
                runCurrent()
                assertEquals(2, refreshCount)

                owner.lifecycle.currentState = Lifecycle.State.DESTROYED
                runCurrent()
                polling.join()
                advanceTimeBy(5_000)
                runCurrent()
                assertEquals(2, refreshCount)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
