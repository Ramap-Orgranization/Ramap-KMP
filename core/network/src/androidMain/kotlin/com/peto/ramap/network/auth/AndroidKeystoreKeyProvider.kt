package com.peto.ramap.network.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class AndroidKeystoreKeyProvider(
    private val alias: String,
) : SessionEncryptionKeyProvider {
    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    override fun find(): SecretKey? = keyStore().getKey(alias, null) as? SecretKey

    override fun create(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec
                    .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build(),
            )
            generateKey()
        }

    override fun delete() {
        keyStore().deleteEntry(alias)
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_SIZE_BITS = 256
    }
}
