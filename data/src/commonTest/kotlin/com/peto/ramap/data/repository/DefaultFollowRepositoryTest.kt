package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.follow.FollowDataSource
import com.peto.ramap.data.model.FollowCountsResponse
import com.peto.ramap.data.model.PublicProfileResponse
import com.peto.ramap.data.testReviewChanges
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.review.change.ReviewChange
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultFollowRepositoryTest {
    @Test
    fun `팔로우 변경 성공 알림은 차단이나 공개 범위 변경과 구분할 수 있다`() =
        runTest {
            val changes = testReviewChanges()
            val source =
                object : FollowDataSource {
                    override suspend fun fetchCounts(): FollowCountsResponse = error("사용하지 않는 조회")

                    override suspend fun fetchConnections(
                        list: String,
                        offset: Long,
                    ): List<PublicProfileResponse> = error("사용하지 않는 조회")

                    override suspend fun changeFollow(
                        userId: String,
                        action: String,
                    ) = Unit
                }
            val repository = DefaultFollowRepository(source, changes)

            for (action in FollowAction.entries) {
                assertIs<RamapResult.Success<Unit>>(repository.changeFollow("author", action))
                assertEquals(
                    ReviewChange.AccessChanged("author", followAction = action),
                    changes.journal.value.changes
                        .last()
                        .change,
                )
            }
        }

    @Test
    fun `팔로우 변경 실패는 접근 변경 알림을 발행하지 않는다`() =
        runTest {
            val changes = testReviewChanges()
            val source =
                object : FollowDataSource {
                    override suspend fun fetchCounts(): FollowCountsResponse = error("사용하지 않는 조회")

                    override suspend fun fetchConnections(
                        list: String,
                        offset: Long,
                    ): List<PublicProfileResponse> = error("사용하지 않는 조회")

                    override suspend fun changeFollow(
                        userId: String,
                        action: String,
                    ): Unit = error("팔로우 변경 실패")
                }

            assertIs<RamapResult.Error>(DefaultFollowRepository(source, changes).changeFollow("author", FollowAction.FOLLOW))
            assertTrue(
                changes.journal.value.changes
                    .isEmpty(),
            )
        }
}
