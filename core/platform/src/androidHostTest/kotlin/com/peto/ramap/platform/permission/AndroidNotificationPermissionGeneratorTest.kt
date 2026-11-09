package com.peto.ramap.platform.permission

import android.Manifest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class AndroidNotificationPermissionGeneratorTest {
    @Test
    fun `안드로이드 13 이상에서 미허용 알림 권한만 요청하고 응답을 기다린다`() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val launcher = RecordingNotificationPermissionLauncher()
        val results = mutableListOf<NotificationPermissionOutcome>()
        val generator = AndroidNotificationPermissionGenerator(application, launcher, results::add)

        generator.requestPermission()

        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), launcher.requestedPermissions)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `알림 권한이 이미 있으면 팝업 없이 결과를 전달한다`() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val launcher = RecordingNotificationPermissionLauncher()
        val results = mutableListOf<NotificationPermissionOutcome>()
        val generator = AndroidNotificationPermissionGenerator(application, launcher, results::add)

        generator.requestPermission()

        assertTrue(launcher.requestedPermissions.isEmpty())
        assertEquals(listOf(NotificationPermissionOutcome.AlreadyGranted), results)
    }

    @Test
    @Config(sdk = [32])
    fun `안드로이드 12 이하는 런타임 알림 권한을 요청하지 않는다`() {
        val application = RuntimeEnvironment.getApplication()
        val launcher = RecordingNotificationPermissionLauncher()
        val results = mutableListOf<NotificationPermissionOutcome>()
        val generator = AndroidNotificationPermissionGenerator(application, launcher, results::add)

        generator.requestPermission()

        assertTrue(launcher.requestedPermissions.isEmpty())
        assertEquals(listOf(NotificationPermissionOutcome.EnabledWithoutRuntimeRequest), results)
    }
}
