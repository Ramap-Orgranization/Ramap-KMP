package com.peto.ramap.image

import coil3.PlatformContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ImageCachePolicyTest {
    @Test
    fun `이전 기본 캐시는 실제 로그아웃 처리 후에도 디스크 바이트를 보존한다`() = runTest { LegacyImageCacheLogoutScenario(PlatformContext.INSTANCE).verifyDiskBytesRemainAfterSignOut() }

    @Test
    fun `서명이 바뀌어도 같은 사진은 권한 확인 후 메모리에서 재사용한다`() = runTest { ImageCacheScenarios(PlatformContext.INSTANCE).reuseSignedImageInMemory() }

    @Test
    fun `권한 사진은 새 로더에서 디스크 대신 다시 내려받는다`() = runTest { ImageCacheScenarios(PlatformContext.INSTANCE).keepPrivateImagesOffDisk() }

    @Test
    fun `공개 사진은 새 로더에서도 디스크 캐시를 재사용한다`() = runTest { ImageCacheScenarios(PlatformContext.INSTANCE).reusePublicImagesFromDisk() }

    @Test
    fun `계정 변경과 접근 거절은 메모리 적중보다 먼저 처리한다`() = runTest { ImageCacheScenarios(PlatformContext.INSTANCE).denyRevokedAccessBeforeMemoryLookup() }

    @Test
    fun `다운로드 중 권한이 바뀌면 결과와 메모리 항목을 버린다`() = runTest { ImageCacheScenarios(PlatformContext.INSTANCE).discardDownloadAfterAccessChanges() }

    @Test
    fun `이전 버전의 권한 사진 디스크 항목을 읽지 않는다`() = runTest { ImageCacheScenarios(PlatformContext.INSTANCE).ignoreOldPrivateDiskEntry() }

    @Test
    fun `이전 캐시는 한 번만 정리하고 새 공개 캐시는 보존한다`() = runTest { ImageCacheScenarios(PlatformContext.INSTANCE).migrateOldCacheOnlyOnce() }
}
