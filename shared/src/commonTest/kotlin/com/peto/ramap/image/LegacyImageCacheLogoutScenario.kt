package com.peto.ramap.image

import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.peto.ramap.core.result.RamapResult
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class LegacyImageCacheLogoutScenario(
    private val context: PlatformContext,
    private val decoder: Decoder.Factory? = null,
) {
    suspend fun verifyDiskBytesRemainAfterSignOut() {
        val fixture = ImageLogoutCacheFixture(context, decoder)
        try {
            fixture.authenticate()
            assertTrue(fixture.login.hasSession())
            assertNull(fixture.cache.openSnapshot(ImageLogoutCacheFixture.IMAGE_URL))

            val loader = fixture.legacyLoader()
            val downloaded = fixture.load(loader)
            assertEquals(DataSource.NETWORK, assertIs<SuccessResult>(downloaded, (downloaded as? ErrorResult)?.throwable?.stackTraceToString()).dataSource)
            val before =
                assertNotNull(fixture.cache.openSnapshot(ImageLogoutCacheFixture.IMAGE_URL)).use { snapshot ->
                    assertTrue(fixture.cache.fileSystem.exists(snapshot.data))
                    fixture.cache.fileSystem.read(snapshot.data) { readByteString() }
                }
            assertEquals(fixture.imageBytes, before)

            assertIs<RamapResult.Success<Unit>>(fixture.login.signOut())
            assertFalse(fixture.login.hasSession())
            assertTrue("/auth/v1/logout" in fixture.authRequests)
            assertTrue("/rest/v1/rpc/unregister_push_registrations" in fixture.authRequests)
            val after =
                assertNotNull(fixture.cache.openSnapshot(ImageLogoutCacheFixture.IMAGE_URL)).use { snapshot ->
                    assertTrue(fixture.cache.fileSystem.exists(snapshot.data))
                    fixture.cache.fileSystem.read(snapshot.data) { readByteString() }
                }
            assertEquals(before, after)
            assertEquals(before.sha256(), after.sha256())

            loader.memoryCache?.clear()
            fixture.reopenCache()
            val restored = fixture.legacyLoader()
            assertTrue(restored.memoryCache?.keys.isNullOrEmpty())
            val reopened = fixture.load(restored)
            assertEquals(DataSource.DISK, assertIs<SuccessResult>(reopened, (reopened as? ErrorResult)?.throwable?.stackTraceToString()).dataSource)
            assertEquals(1, fixture.imageRequests)

            println("legacy_cache_logout: authenticated_after=false, bytes_before=${before.size}, bytes_after=${after.size}, sha256=${after.sha256().hex()}, reopened_source=DISK, image_requests=${fixture.imageRequests}")
        } finally {
            fixture.close()
        }
    }
}
