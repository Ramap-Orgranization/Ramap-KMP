package com.peto.ramap.network.auth

import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal class AndroidSecureSessionStorage(
    private val file: File,
    private val cipher: AndroidSessionCipher,
) : SecureSessionStorage {
    private val atomicFile = AtomicFile(file)

    override suspend fun read(): String? =
        withContext(Dispatchers.IO) {
            val encrypted =
                try {
                    atomicFile.readFully()
                } catch (exception: FileNotFoundException) {
                    if (file.exists() || File("${file.path}.bak").exists()) throw exception
                    return@withContext null
                }
            cipher.decrypt(encrypted).decodeToString()
        }

    override suspend fun write(value: String) {
        withContext(Dispatchers.IO) {
            val encrypted = cipher.encrypt(value.encodeToByteArray())
            val output = atomicFile.startWrite()
            try {
                output.write(encrypted)
                output.fd.sync()
                atomicFile.finishWrite(output)
            } catch (exception: Exception) {
                atomicFile.failWrite(output)
                throw exception
            }
            // AtomicFile은 일부 완료 오류를 로그로만 남기므로 교체 결과도 확인합니다.
            if (!atomicFile.readFully().contentEquals(encrypted)) throw IOException(ERROR_WRITE)
        }
    }

    private companion object {
        const val ERROR_WRITE = "Secure session write did not complete"
    }
}
