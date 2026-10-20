package com.peto.ramap.network.auth

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AndroidSessionCipherTest {
    @Test
    fun `같은 세션도 서로 다른 IV로 암호화하고 원문을 복원한다`() {
        val cipher = AndroidSessionCipher(FakeSessionEncryptionKeyProvider())
        val plaintext = "test-access-and-refresh".encodeToByteArray()
        val first = cipher.encrypt(plaintext)
        val second = cipher.encrypt(plaintext)

        assertFalse(first.contentEquals(second))
        assertFalse(first.decodeToString().contains("test-access-and-refresh"))
        assertContentEquals(plaintext, cipher.decrypt(first))
        assertContentEquals(plaintext, cipher.decrypt(second))
    }

    @Test
    fun `변조된 암호문은 인증 태그 검증에서 거부한다`() {
        val cipher = AndroidSessionCipher(FakeSessionEncryptionKeyProvider())
        val encrypted = cipher.encrypt("test-session".encodeToByteArray())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()

        assertFailsWith<InvalidStoredSessionException> { cipher.decrypt(encrypted) }
    }

    @Test
    fun `키가 사라지면 복호화 중 새 키를 만들지 않는다`() {
        val provider = FakeSessionEncryptionKeyProvider()
        val cipher = AndroidSessionCipher(provider)
        val encrypted = cipher.encrypt("test-session".encodeToByteArray())
        provider.delete()

        assertFailsWith<InvalidStoredSessionException> { cipher.decrypt(encrypted) }
        assertNull(provider.key)
    }

    @Test
    fun `지원하지 않는 형식과 잘린 암호문을 거부한다`() {
        val cipher = AndroidSessionCipher(FakeSessionEncryptionKeyProvider())
        val encrypted = cipher.encrypt("test-session".encodeToByteArray())
        encrypted[0] = 2

        assertFailsWith<InvalidStoredSessionException> { cipher.decrypt(encrypted) }
        assertFailsWith<InvalidStoredSessionException> { cipher.decrypt(byteArrayOf(1)) }
    }
}
