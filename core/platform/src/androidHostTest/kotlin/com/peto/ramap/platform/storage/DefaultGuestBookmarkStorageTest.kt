package com.peto.ramap.platform.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.peto.ramap.platform.storage.bookmark.DefaultGuestBookmarkStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultGuestBookmarkStorageTest {
    @Test
    fun `기기 저장과 계정별 이전 대기는 저장소를 다시 열어도 소유자를 유지한다`() =
        runTest {
            val directory = Files.createTempDirectory("guest-bookmark-test").toFile()
            val file = directory.resolve("guest-bookmark.preferences_pb")
            val firstScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val storage = DefaultGuestBookmarkStorage(PreferenceDataStoreFactory.create(scope = firstScope) { file })
                storage.addGuestBookmarks(setOf("first", "second", "removed"))
                storage.removeGuestBookmark("removed")
                storage.claimGuestBookmarks("account-a")
                assertEquals(setOf("first", "second"), storage.fetchPendingBookmarks("account-a"))
                storage.forgetPendingBookmarks("account-a", setOf("first"))
                storage.addGuestBookmarks(setOf("guest"))
                firstScope.coroutineContext[Job]!!.cancelAndJoin()

                val reopened = DefaultGuestBookmarkStorage(PreferenceDataStoreFactory.create(scope = secondScope) { file })
                assertEquals(setOf("guest"), reopened.fetchGuestBookmarks())
                reopened.claimGuestBookmarks("account-b")
                assertEquals(setOf("guest"), reopened.fetchPendingBookmarks("account-b"))
                reopened.claimGuestBookmarks("account-a")
                assertEquals(setOf("second"), reopened.fetchPendingBookmarks("account-a"))
                assertEquals(emptySet(), reopened.fetchGuestBookmarks())
            } finally {
                firstScope.coroutineContext[Job]!!.cancelAndJoin()
                secondScope.coroutineContext[Job]!!.cancelAndJoin()
                directory.deleteRecursively()
            }
        }
}
