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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultOnboardingStorageTest {
    @Test
    fun `권한 확인과 매장 선택 기록을 다시 열어도 유지하고 온보딩 완료 시 선택 기록을 지운다`() =
        runTest {
            val directory = Files.createTempDirectory("onboarding-storage-test").toFile()
            val file = directory.resolve("onboarding.preferences_pb")
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val storage = DefaultOnboardingStorage(PreferenceDataStoreFactory.create(scope = firstScope) { file })
                assertFalse(storage.isPermissionGuideConfirmed())
                assertFalse(storage.isOnboardingCompleted())
                assertNull(storage.savedShopCount())

                storage.confirmPermissionGuide()
                storage.confirmPermissionGuide()
                assertTrue(storage.isPermissionGuideConfirmed())
                assertFalse(storage.isOnboardingCompleted())
                storage.saveShopSelection(3)
                firstScope.coroutineContext[Job]!!.cancelAndJoin()

                val reopened = DefaultOnboardingStorage(PreferenceDataStoreFactory.create(scope = secondScope) { file })
                assertTrue(reopened.isPermissionGuideConfirmed())
                assertFalse(reopened.isOnboardingCompleted())
                assertEquals(3, reopened.savedShopCount())
                reopened.completeOnboarding()
                assertTrue(reopened.isOnboardingCompleted())
                assertNull(reopened.savedShopCount())
            } finally {
                firstScope.coroutineContext[Job]!!.cancelAndJoin()
                secondScope.coroutineContext[Job]!!.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
}
