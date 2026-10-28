package com.peto.ramap.ui.main.map.archive

import coil3.request.allowHardware
import coil3.size.Size
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.ui.main.map.archive.model.ArchiveMarkerImageRequestFactory
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ArchiveMarkerImageRequestFactoryTest {
    @Test
    fun `마커 사진 요청은 software bitmap을 요구하며 reference 캐시와 썸네일 크기를 유지한다`() =
        runTest {
            val photo = MyReviewPhoto(url = "https://example.com/signed-photo", reference = "owner/photo.jpg")
            val request =
                ArchiveMarkerImageRequestFactory().create(
                    context = RuntimeEnvironment.getApplication(),
                    photo = photo,
                    sizePx = 174,
                )

            assertFalse(request.allowHardware)
            assertEquals(photo.url, assertIs<String>(request.data))
            assertEquals(photo.reference, request.memoryCacheKey)
            assertEquals(photo.reference, request.diskCacheKey)
            assertEquals(Size(174, 174), request.sizeResolver.size())
        }
}
