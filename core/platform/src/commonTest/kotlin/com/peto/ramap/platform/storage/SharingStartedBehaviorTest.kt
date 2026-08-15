package com.peto.ramap.platform.storage

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SharingStartedBehaviorTest {
    @Test
    fun `Eagerly는 구독자가 없어도 저장된 값을 읽기 시작하고 WhileSubscribed는 구독 후 시작한다`() =
        runTest {
            val storedSearches = listOf("저장된 검색어")
            val source = MutableStateFlow(storedSearches)
            val eagerly = source.stateIn(backgroundScope, SharingStarted.Eagerly, emptyList())
            val whileSubscribed = source.stateIn(backgroundScope, SharingStarted.WhileSubscribed(), emptyList())

            runCurrent()

            assertEquals(storedSearches, eagerly.value)
            assertEquals(emptyList(), whileSubscribed.value)

            val collection = backgroundScope.launch { whileSubscribed.collect() }
            runCurrent()

            assertEquals(storedSearches, whileSubscribed.value)
            collection.cancel()
        }

    @Test
    fun `WhileSubscribed StateFlow의 first는 upstream 값이 아닌 초기값을 즉시 반환할 수 있다`() =
        runTest {
            val state =
                MutableStateFlow(listOf("저장된 검색어")).stateIn(
                    backgroundScope,
                    SharingStarted.WhileSubscribed(),
                    emptyList(),
                )

            assertEquals(emptyList(), state.first())
        }
}
