package com.peto.ramap

import com.peto.ramap.domain.model.update.AppUpdatePolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateGateTest {
    @Test
    fun `최소 빌드보다 낮고 유효한 스토어 URL일 때만 업데이트를 요구한다`() {
        val policy = AppUpdatePolicy(minimumBuildNumber = 10, storeUrl = "https://example.com")

        assertTrue(shouldRequireAppUpdate(policy, buildNumber = 9, isStoreUrlSupported = true))
        assertFalse(shouldRequireAppUpdate(policy, buildNumber = 10, isStoreUrlSupported = true))
        assertFalse(shouldRequireAppUpdate(policy, buildNumber = 9, isStoreUrlSupported = false))
        assertFalse(shouldRequireAppUpdate(policy = null, buildNumber = 9, isStoreUrlSupported = true))
    }
}
