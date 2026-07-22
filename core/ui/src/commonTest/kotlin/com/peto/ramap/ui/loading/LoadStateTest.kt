package com.peto.ramap.ui.loading

import com.peto.ramap.ui.base.TestLoadKey
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoadStateTest {
    @Test
    fun `loading은 해당 키의 작업 하나를 활성화한다`() {
        val loadState = LoadState.loading(TestLoadKey.Request)

        assertTrue(loadState.isAnyLoading)
        assertTrue(loadState.isLoading(TestLoadKey.Request))
        assertFalse((loadState - TestLoadKey.Request).isAnyLoading)
    }
}
