package com.peto.ramap.platform.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.peto.ramap.platform.storage.onboarding.DefaultOnboardingStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultOnboardingStorageTest {
    @Test
    fun `첫 실행에는 미확인이며 확인 기록은 저장소를 다시 열어도 유지한다`() =
        runTest {
            val directory = Files.createTempDirectory("onboarding-storage-test").toFile()
            val file = directory.resolve("onboarding.preferences_pb")
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val storage = DefaultOnboardingStorage(PreferenceDataStoreFactory.create(scope = firstScope) { file })
                assertFalse(storage.isPermissionGuideConfirmed())
                assertFalse(storage.isOnboardingCompleted())

                storage.confirmPermissionGuide()
                storage.confirmPermissionGuide()
                assertTrue(storage.isPermissionGuideConfirmed())
                assertFalse(storage.isOnboardingCompleted())
                storage.completeOnboarding()
                firstScope.coroutineContext[Job]!!.cancelAndJoin()

                val reopened = DefaultOnboardingStorage(PreferenceDataStoreFactory.create(scope = secondScope) { file })
                assertTrue(reopened.isPermissionGuideConfirmed())
                assertTrue(reopened.isOnboardingCompleted())
            } finally {
                firstScope.coroutineContext[Job]!!.cancelAndJoin()
                secondScope.coroutineContext[Job]!!.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
}
