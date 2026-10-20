package com.peto.ramap.network.auth

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class FakeSessionEncryptionKeyProvider : SessionEncryptionKeyProvider {
    var key: SecretKey? = null

    override fun find(): SecretKey? = key

    override fun create(): SecretKey =
        KeyGenerator
            .getInstance("AES")
            .apply { init(256) }
            .generateKey()
            .also { key = it }

    override fun delete() {
        key = null
    }
}
