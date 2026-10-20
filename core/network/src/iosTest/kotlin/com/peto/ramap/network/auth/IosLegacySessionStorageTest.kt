package com.peto.ramap.network.auth

import kotlinx.coroutines.test.runTest
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosLegacySessionStorageTest {
    @Test
    fun `NSUserDefaults의 현재 키와 과거 키를 읽고 세션만 삭제한다`() =
        runTest {
            val suite = "com.peto.ramap.test.${NSUUID().UUIDString}"
            val defaults = NSUserDefaults(suiteName = suite)
            val keys = SessionStorageKeys("https://test.supabase.co")
            val legacy = IosLegacySessionStorage(defaults, keys)
            try {
                defaults.setObject("current-session", keys.session)
                defaults.setObject("old-session", SessionStorageKeys.LEGACY_SESSION)
                defaults.setObject("preserved", "unrelated-setting")
                assertEquals("current-session", legacy.read())
                defaults.removeObjectForKey(keys.session)
                assertEquals("old-session", legacy.read())
                legacy.delete()
                assertNull(legacy.read())
                assertEquals("preserved", defaults.stringForKey("unrelated-setting"))
            } finally {
                defaults.removePersistentDomainForName(suite)
            }
        }
}
