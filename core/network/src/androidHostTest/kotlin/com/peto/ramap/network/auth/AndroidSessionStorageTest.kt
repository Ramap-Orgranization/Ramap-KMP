package com.peto.ramap.network.auth

import android.content.Context
import android.util.AtomicFile
import io.github.jan.supabase.auth.SettingsSessionManager
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidSessionStorageTest {
    @Test
    fun `암호화 파일을 새 저장소 객체에서 복원하고 삭제 상태도 유지한다`() =
        runTest {
            val file = File(RuntimeEnvironment.getApplication().noBackupFilesDir, "auth/test-session.enc")
            val provider = FakeSessionEncryptionKeyProvider()
            val store = AndroidSecureSessionStorage(file, AndroidSessionCipher(provider))

            store.write("test-session")

            assertFalse(file.readText().contains("test-session"))
            assertEquals("test-session", AndroidSecureSessionStorage(file, AndroidSessionCipher(provider)).read())
            store.write("")
            assertEquals("", AndroidSecureSessionStorage(file, AndroidSessionCipher(provider)).read())
        }

    @Test
    fun `파일 교체가 중단되면 마지막 완료된 세션을 복원한다`() =
        runTest {
            val file = File(RuntimeEnvironment.getApplication().noBackupFilesDir, "auth/interrupted-session.enc")
            val provider = FakeSessionEncryptionKeyProvider()
            val store = AndroidSecureSessionStorage(file, AndroidSessionCipher(provider))
            store.write("previous-session")
            AtomicFile(file).startWrite().use { it.write(byteArrayOf(1, 2, 3)) }

            assertEquals("previous-session", AndroidSecureSessionStorage(file, AndroidSessionCipher(provider)).read())
        }

    @Test
    fun `SDK의 현재 키와 예전 키를 모두 읽고 세션만 삭제한다`() =
        runTest {
            val context = RuntimeEnvironment.getApplication()
            val preferences = context.getSharedPreferences("legacy-test", Context.MODE_PRIVATE)
            val keys = SessionStorageKeys("https://test.supabase.co")
            val legacy = AndroidLegacySessionStorage(preferences, keys)
            preferences
                .edit()
                .putString(keys.session, "current-session")
                .putString(SettingsSessionManager.SETTINGS_KEY, "old-session")
                .putString("unrelated-setting", "preserved")
                .commit()

            assertEquals("current-session", legacy.read())
            preferences.edit().remove(keys.session).commit()
            assertEquals("old-session", legacy.read())
            legacy.delete()
            assertNull(legacy.read())
            assertEquals("preserved", preferences.getString("unrelated-setting", null))
        }
}
