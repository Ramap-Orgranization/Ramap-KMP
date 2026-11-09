package com.peto.ramap.platform.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultAnnouncementReadStorageTest {
    @Test
    fun `소유자를 알 수 없는 이전 읽음 기록을 비로그인 기록에 합치지 않는다`() =
        runTest {
            val directory = Files.createTempDirectory("announcement-legacy-test").toFile()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val dataStore = PreferenceDataStoreFactory.create(scope = scope) { directory.resolve("read.preferences_pb") }
                dataStore.edit { it[stringSetPreferencesKey("read_announcement_ids")] = setOf("legacy") }
                val storage = DefaultAnnouncementReadStorage(dataStore)
                assertTrue(storage.readIds.first().isEmpty())
                storage.markRead("guest")
                assertEquals(setOf("guest"), storage.readIds.first())
            } finally {
                scope.coroutineContext[Job]!!.cancelAndJoin()
                directory.deleteRecursively()
            }
        }

    @Test
    fun `여러 공지의 읽음 기록을 저장하고 저장소를 다시 열어도 유지한다`() =
        runTest {
            val directory = Files.createTempDirectory("announcement-read-test").toFile()
            val file = directory.resolve("read.preferences_pb")
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val storage = DefaultAnnouncementReadStorage(PreferenceDataStoreFactory.create(scope = firstScope) { file })
                assertTrue(storage.readIds.first().isEmpty())
                val first = launch { storage.markRead("first") }
                val second = launch { storage.markRead("second") }
                first.join()
                second.join()
                storage.markRead("first")
                storage.markRead(" ")
                assertEquals(setOf("first", "second"), storage.readIds.first())
                firstScope.coroutineContext[Job]!!.cancelAndJoin()

                val reopened = DefaultAnnouncementReadStorage(PreferenceDataStoreFactory.create(scope = secondScope) { file })
                assertEquals(setOf("first", "second"), reopened.readIds.first())
            } finally {
                firstScope.coroutineContext[Job]!!.cancelAndJoin()
                secondScope.coroutineContext[Job]!!.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
}
