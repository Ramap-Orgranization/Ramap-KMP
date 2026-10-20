package com.peto.ramap.network.auth

import android.security.keystore.KeyPermanentlyInvalidatedException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

internal class AndroidSessionCipher(
    private val keyProvider: SessionEncryptionKeyProvider,
) {
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, keyProvider.find() ?: keyProvider.create())
        } catch (_: KeyPermanentlyInvalidatedException) {
            keyProvider.delete()
            cipher.init(Cipher.ENCRYPT_MODE, keyProvider.create())
        }
        check(cipher.iv.size == IV_SIZE) { ERROR_IV_SIZE }
        cipher.updateAAD(byteArrayOf(FORMAT_VERSION))
        return byteArrayOf(FORMAT_VERSION) + cipher.iv + cipher.doFinal(plaintext)
    }

    fun decrypt(encrypted: ByteArray): ByteArray {
        if (encrypted.size < HEADER_SIZE + TAG_SIZE_BYTES || encrypted[0] != FORMAT_VERSION) {
            throw InvalidStoredSessionException()
        }
        val key = keyProvider.find() ?: throw InvalidStoredSessionException()
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameters = GCMParameterSpec(TAG_SIZE_BITS, encrypted.copyOfRange(1, HEADER_SIZE))
            cipher.init(Cipher.DECRYPT_MODE, key, parameters)
            cipher.updateAAD(byteArrayOf(FORMAT_VERSION))
            cipher.doFinal(encrypted, HEADER_SIZE, encrypted.size - HEADER_SIZE)
        } catch (_: AEADBadTagException) {
            throw InvalidStoredSessionException()
        } catch (_: KeyPermanentlyInvalidatedException) {
            throw InvalidStoredSessionException()
        }
    }

    private companion object {
        const val FORMAT_VERSION: Byte = 1
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val HEADER_SIZE = 1 + IV_SIZE
        const val TAG_SIZE_BITS = 128
        const val TAG_SIZE_BYTES = TAG_SIZE_BITS / 8
        const val ERROR_IV_SIZE = "Unexpected session encryption IV size"
    }
}
