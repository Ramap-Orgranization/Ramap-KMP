package com.peto.ramap.network.auth

import javax.crypto.SecretKey

internal interface SessionEncryptionKeyProvider {
    fun find(): SecretKey?

    fun create(): SecretKey

    fun delete()
}
