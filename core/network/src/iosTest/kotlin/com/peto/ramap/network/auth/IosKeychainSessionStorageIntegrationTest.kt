@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.peto.ramap.network.auth

import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.test.runTest
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSUUID
import platform.Security.SecItemDelete
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosKeychainSessionStorageIntegrationTest {
    @Test
    fun `Keychain 저장과 교체 및 로그아웃 상태를 새 객체에서 읽는다`() =
        runTest {
            val service = "com.peto.ramap.test.${NSUUID().UUIDString}"
            val store = IosKeychainSessionStorage(account = "test-account", service = service)
            try {
                assertNull(store.read())
                store.write("test-first-session-한글🍜")
                assertEquals("test-first-session-한글🍜", IosKeychainSessionStorage("test-account", service).read())
                store.write("test-refreshed-session")
                assertEquals("test-refreshed-session", IosKeychainSessionStorage("test-account", service).read())
                store.write("")
                assertEquals("", IosKeychainSessionStorage("test-account", service).read())
                assertNull(IosKeychainSessionStorage("different-account", service).read())
            } finally {
                deleteTestKeychain(service)
            }
        }

    private fun deleteTestKeychain(service: String) {
        val retainedService = CFBridgingRetain(service)
        try {
            memScoped {
                val keys = allocArrayOf(kSecClass, kSecAttrService)
                val values = allocArrayOf(kSecClassGenericPassword, retainedService)
                val query =
                    CFDictionaryCreate(
                        kCFAllocatorDefault,
                        keys.reinterpret(),
                        values.reinterpret(),
                        2.convert(),
                        null,
                        null,
                    )
                try {
                    SecItemDelete(query)
                } finally {
                    CFRelease(query)
                }
            }
        } finally {
            CFRelease(retainedService)
        }
    }
}
