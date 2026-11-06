package com.peto.ramap.image

import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.peto.ramap.domain.model.image.ImageAccessScope
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ImageCacheScenarios(
    private val context: PlatformContext,
) {
    suspend fun reuseSignedImageInMemory() =
        fixture { f ->
            val loader = f.loader()
            assertEquals(DataSource.NETWORK, assertIs<SuccessResult>(f.load(loader, PRIVATE_URL)).dataSource)
            val second = assertIs<SuccessResult>(f.load(loader, "$PRIVATE_URL?token=renewed"))
            assertEquals(DataSource.MEMORY_CACHE, second.dataSource)
            assertEquals(1, f.downloads)
            assertEquals(2, f.authorizations)
        }

    suspend fun keepPrivateImagesOffDisk() =
        fixture { f ->
            assertIs<SuccessResult>(f.load(f.loader(), PRIVATE_URL))
            val result = assertIs<SuccessResult>(f.load(f.loader(), PRIVATE_URL))
            assertEquals(DataSource.NETWORK, result.dataSource)
            assertEquals(2, f.downloads)
            assertNull(f.cache.openSnapshot(PRIVATE_URL))
        }

    suspend fun reusePublicImagesFromDisk() =
        fixture { f ->
            assertIs<SuccessResult>(f.load(f.loader(), PUBLIC_URL))
            val result = assertIs<SuccessResult>(f.load(f.loader(), PUBLIC_URL))
            assertEquals(DataSource.DISK, result.dataSource)
            assertEquals(1, f.downloads)
        }

    suspend fun denyRevokedAccessBeforeMemoryLookup() =
        fixture { f ->
            val loader = f.loader()
            assertIs<SuccessResult>(f.load(loader, PRIVATE_URL))
            f.allowed = false
            f.scopes.value = ImageAccessScope("other", 1, 0)
            assertIs<ErrorResult>(f.load(loader, PRIVATE_URL))
            assertEquals(1, f.downloads)
        }

    suspend fun discardDownloadAfterAccessChanges() =
        fixture { f ->
            val loader = f.loader()
            f.onDownload = { f.scopes.value = ImageAccessScope(null, 1, 0) }
            assertIs<ErrorResult>(f.load(loader, PRIVATE_URL))
            assertEquals(0, loader.memoryCache?.keys?.size)
        }

    suspend fun ignoreOldPrivateDiskEntry() =
        fixture { f ->
            f.protected = false
            assertIs<SuccessResult>(f.load(f.loader(respectHeaders = false), PRIVATE_URL))
            f.protected = true
            val result = assertIs<SuccessResult>(f.load(f.loader(), PRIVATE_URL))
            assertEquals(DataSource.NETWORK, result.dataSource)
            assertEquals(2, f.downloads)
        }

    suspend fun migrateOldCacheOnlyOnce() =
        fixture { f ->
            assertIs<SuccessResult>(f.load(f.loader(), PUBLIC_URL))
            assertIs<SuccessResult>(f.load(f.loader(migrate = true), PUBLIC_URL))
            assertEquals(2, f.downloads)
            assertEquals(DataSource.DISK, assertIs<SuccessResult>(f.load(f.loader(migrate = true), PUBLIC_URL)).dataSource)
            assertEquals(2, f.downloads)
        }

    private suspend fun fixture(block: suspend (ImageCacheFixture) -> Unit) {
        val f = ImageCacheFixture(context)
        try {
            block(f)
        } finally {
            f.close()
        }
    }

    companion object {
        const val PRIVATE_URL = "https://fixture.invalid/private/photo.jpg"
        const val PUBLIC_URL = "https://fixture.invalid/public/photo.jpg"
    }
}
