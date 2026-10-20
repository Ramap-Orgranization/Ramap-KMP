package com.peto.ramap.network.auth

import kotlinx.coroutines.test.runTest
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosSessionMigrationStorageTest {
    @Test
    fun `NSUserDefaults의 현재 키와 과거 키를 읽고 세션만 삭제한다`() =
        runTest {
            val suite = "com.peto.ramap.test.${NSUUID().UUIDString}"
            val defaults = NSUserDefaults(suiteName = suite)
            val keys = SessionStorageKeys("https://test.supabase.co")
            val storage = IosKeychainSessionStorage(account = keys.session, defaults = defaults)
            try {
                defaults.setObject("current-session", keys.session)
                defaults.setObject("old-session", SessionStorageKeys.LEGACY_SESSION)
                defaults.setObject("preserved", "unrelated-setting")
                assertEquals("current-session", storage.readLegacy())
                defaults.removeObjectForKey(keys.session)
                assertEquals("old-session", storage.readLegacy())
                storage.deleteLegacy()
                assertNull(storage.readLegacy())
                assertEquals("preserved", defaults.stringForKey("unrelated-setting"))
            } finally {
                defaults.removePersistentDomainForName(suite)
            }
        }
}
