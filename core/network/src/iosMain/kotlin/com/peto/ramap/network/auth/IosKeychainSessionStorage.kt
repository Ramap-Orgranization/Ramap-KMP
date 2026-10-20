@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.peto.ramap.network.auth

import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecAttrSynchronizable
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

internal class IosKeychainSessionStorage(
    private val account: String,
    private val service: String = "com.peto.ramap.auth.session",
) : SecureSessionStorage {
    override suspend fun read(): String? =
        withContext(Dispatchers.IO) {
            memScoped {
                val result = alloc<CFTypeRefVar>()
                result.value = null
                val status =
                    withQuery(
                        mapOf(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne),
                    ) { SecItemCopyMatching(it, result.ptr) }
                if (status == errSecItemNotFound) return@memScoped null
                check(status == errSecSuccess) { "$ERROR_READ ($status)" }
                val data = CFBridgingRelease(result.value) as? NSData ?: throw InvalidStoredSessionException()
                @Suppress("CAST_NEVER_SUCCEEDS")
                (NSString.create(data, NSUTF8StringEncoding) as? String) ?: throw InvalidStoredSessionException()
            }
        }

    override suspend fun write(value: String) {
        withContext(Dispatchers.IO) {
            val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)
            val retainedData = CFBridgingRetain(data)
            try {
                val attributes =
                    mapOf(
                        kSecValueData to retainedData,
                        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                    )
                val status =
                    withQuery { query ->
                        withDictionary(attributes) { SecItemUpdate(query, it) }
                    }
                if (status == errSecItemNotFound) {
                    val added = withQuery(attributes) { SecItemAdd(it, null) }
                    check(added == errSecSuccess) { "$ERROR_WRITE ($added)" }
                } else {
                    check(status == errSecSuccess) { "$ERROR_WRITE ($status)" }
                }
            } finally {
                CFRelease(retainedData)
            }
        }
    }

    private fun <T> withQuery(
        attributes: Map<CFStringRef?, CFTypeRef?> = emptyMap(),
        operation: (CFDictionaryRef?) -> T,
    ): T {
        val retainedService = CFBridgingRetain(service)
        val retainedAccount = CFBridgingRetain(account)
        return try {
            withDictionary(
                mapOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to retainedService,
                    kSecAttrAccount to retainedAccount,
                    kSecAttrSynchronizable to kCFBooleanFalse,
                ) + attributes,
                operation,
            )
        } finally {
            CFRelease(retainedService)
            CFRelease(retainedAccount)
        }
    }

    private fun <T> withDictionary(
        attributes: Map<CFStringRef?, CFTypeRef?>,
        operation: (CFDictionaryRef?) -> T,
    ): T =
        memScoped {
            val keys = allocArrayOf(*attributes.keys.toTypedArray())
            val values = allocArrayOf(*attributes.values.toTypedArray())
            val dictionary =
                CFDictionaryCreate(
                    kCFAllocatorDefault,
                    keys.reinterpret(),
                    values.reinterpret(),
                    attributes.size.convert(),
                    null,
                    null,
                )
            try {
                operation(dictionary)
            } finally {
                CFRelease(dictionary)
            }
        }

    private companion object {
        const val ERROR_READ = "Keychain session read failed"
        const val ERROR_WRITE = "Keychain session write failed"
    }
}
