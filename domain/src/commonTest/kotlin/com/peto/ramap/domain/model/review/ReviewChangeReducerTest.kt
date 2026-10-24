package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import com.peto.ramap.domain.model.review.change.ReviewSnapshot
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class ReviewChangeReducerTest {
    private val reducer = ReviewChangeReducer()

    @Test
    fun `처음 등록한 매장의 내 리뷰에는 사진과 주소와 카테고리가 포함된다`() {
        val image =
            MyReviewPhoto(
                url = "retained-url",
                reference = "path",
            )
        val existing =
            createMyReview(
                id = "existing",
                created = "01",
                shop = createShop("existing-shop"),
            ).copy(images = listOf(image))
        val created =
            createReview(
                id = "new",
                created = "02",
                shop = "new-shop",
            ).copy(isPublic = false)
        val newShop = createShop("new-shop")
        val change =
            createWrittenChange(
                before = null,
                after = created,
                published = 1,
            ).let { it.copy(after = it.after?.copy(shop = newShop)) }

        val result =
            reducer.updateOwnerRows(
                rows = listOf(existing),
                change = change,
                filter = MyReviewVisibility.ALL,
                hasMore = false,
            )

        assertEquals(listOf("new", "existing"), result.map { it.id })
        assertSame(newShop, result.first().shop)
        assertEquals("서울 마포구", result.first().shop?.address)
        assertEquals("shop-image", result.first().shop?.instagramProfileImageUrl)
        assertEquals(
            listOf(Category.SHOYU),
            result
                .first()
                .shop
                ?.menuCategories
                ?.toList(),
        )
        assertSame(existing.shop, result.last().shop)
        assertSame(image, result.last().images.single())
    }

    @Test
    fun `매장 정보 보완은 기존 내 리뷰의 사진과 본문을 유지한다`() {
        val current =
            createMyReview(
                id = "review",
                created = "01",
                shop = null,
            ).copy(
                body = "현재 본문",
                images =
                    listOf(
                        MyReviewPhoto(
                            url = "stable",
                            reference = "path",
                        ),
                    ),
            )
        val hydrated =
            createReview(
                id = "review",
                created = "01",
            ).copy(
                body = "이전 본문",
                images =
                    listOf(
                        MyReviewPhoto(
                            url = "rotated",
                            reference = "path",
                        ),
                    ),
            )
        val shop = createShop("shop")

        val result =
            reducer
                .updateOwnerRows(
                    rows = listOf(current),
                    change =
                        ReviewChange.Hydrated(
                            ReviewSnapshot(
                                review = hydrated,
                                shopName = "매장",
                                shop = shop,
                            ),
                        ),
                    filter = MyReviewVisibility.ALL,
                    hasMore = false,
                ).single()

        assertSame(shop, result.shop)
        assertEquals("현재 본문", result.body)
        assertSame(current.images.single(), result.images.single())
    }

    @Test
    fun `매장 정보가 없는 쓰기와 보완은 기존 내 리뷰의 매장을 유지한다`() {
        val shop = createShop("shop")
        val current =
            createMyReview(
                id = "review",
                created = "01",
                shop = shop,
            )
        val edited =
            createReview(
                id = "review",
                created = "01",
            ).copy(body = "수정 본문")
        val written =
            reducer.updateOwnerRows(
                rows = listOf(current),
                change =
                    createWrittenChange(
                        before =
                            createReview(
                                id = "review",
                                created = "01",
                            ),
                        after = edited,
                        published = 1,
                    ),
                filter = MyReviewVisibility.ALL,
                hasMore = false,
            )
        val hydrated =
            reducer.updateOwnerRows(
                rows = written,
                change =
                    ReviewChange.Hydrated(
                        ReviewSnapshot(
                            review = edited,
                            shopName = "매장",
                        ),
                    ),
                filter = MyReviewVisibility.ALL,
                hasMore = false,
            )

        assertSame(shop, written.single().shop)
        assertSame(shop, hydrated.single().shop)
    }

    @Test
    fun `리뷰 삭제는 사진과 이후 방문 번호를 유지하고 작성자의 공개 리뷰 개수만 갱신한다`() {
        val earlier =
            createReview(
                id = "earlier",
                created = "01",
                visit = 1,
            )
        val deleted =
            createReview(
                id = "deleted",
                created = "02",
                visit = 2,
            )
        val later =
            createReview(
                id = "later",
                created = "03",
                visit = 3,
            )
        val image =
            MyReviewPhoto(
                url = "retained-url",
                reference = "path",
            )
        val unrelated =
            createReview(
                id = "unrelated",
                created = "04",
                shop = "other",
            ).copy(
                images = listOf(image),
                imageUrls = listOf("retained-url"),
            )
        val result =
            reducer.patch(
                rows = listOf(unrelated, later, deleted, earlier),
                change =
                    createWrittenChange(
                        before = deleted,
                        after = null,
                        published = 2,
                    ),
                hasMore = true,
            ) { _, _ -> true }
        assertEquals(listOf("unrelated", "later", "earlier"), result.map { it.id })
        assertEquals(3, result.first { it.id == "later" }.visitNumber)
        assertEquals(1, result.last().visitNumber)
        assertEquals(listOf(2, 2, 2), result.map { it.author.reviewCount })
        assertSame(image, result.first().images.single())
    }

    @Test
    fun `비공개 리뷰 등록과 본문 및 공개 범위 수정은 방문 번호와 유효한 아바타를 유지한다`() {
        val later =
            createReview(
                id = "later",
                created = "03",
                visit = 2,
            )
        val avatarExpiresAt = kotlin.time.Instant.parse("2099-01-01T00:00:00Z")
        val private =
            createReview(
                id = "private",
                created = "02",
            ).copy(
                isPublic = false,
                author =
                    ReviewAuthor(
                        userId = "owner",
                        nickname = "작성자",
                        avatarUrl = "retained-avatar",
                        avatarReference = "avatar",
                        avatarExpiresAt = avatarExpiresAt,
                    ),
            )
        val created =
            reducer.patch(
                rows = listOf(later),
                change =
                    createWrittenChange(
                        before = null,
                        after = private,
                        published = 2,
                    ),
                hasMore = false,
            ) { _, _ -> true }
        assertEquals(2, created.first().visitNumber)
        val edited =
            private.copy(
                body = "새로운 본문",
                author =
                    private.author.copy(
                        avatarUrl = null,
                        avatarExpiresAt = null,
                    ),
            )
        val updated =
            reducer.patch(
                rows = created,
                change =
                    createWrittenChange(
                        before = private,
                        after = edited,
                        published = 2,
                    ),
                hasMore = true,
            ) { _, _ -> true }
        assertEquals(2, updated.first().visitNumber)
        assertEquals(
            expected = "retained-avatar",
            actual = updated.first { it.id == "private" }.author.avatarUrl,
        )
        assertEquals(
            expected = avatarExpiresAt,
            actual = updated.first { it.id == "private" }.author.avatarExpiresAt,
        )
        val published = edited.copy(isPublic = true)
        val visible =
            reducer.patch(
                rows = updated,
                change =
                    createWrittenChange(
                        before = edited,
                        after = published,
                        published = 3,
                    ),
                hasMore = true,
            ) { _, _ -> true }
        assertEquals(2, visible.first().visitNumber)
    }

    @Test
    fun `조회 범위 밖의 리뷰 등록은 다음 페이지의 리뷰를 건너뛰지 않는다`() {
        val rows =
            (40 downTo 1).map {
                createReview(
                    id = "id-$it",
                    created = it.toString().padStart(2, '0'),
                )
            }
        val old =
            createReview(
                id = "unloaded",
                created = "00",
            )
        assertEquals(
            rows.map { it.id },
            reducer
                .patch(
                    rows = rows,
                    change =
                        createWrittenChange(
                            before = null,
                            after = old,
                            published = 0,
                        ),
                    hasMore = true,
                ) { _, _ -> true }
                .map { it.id },
        )
        val newest =
            createReview(
                id = "newest",
                created = "41",
            )
        assertEquals(
            41,
            reducer
                .patch(
                    rows = rows,
                    change =
                        createWrittenChange(
                            before = null,
                            after = newest,
                            published = 0,
                        ),
                    hasMore = true,
                ) { _, _ -> true }
                .size,
        )
    }

    @Test
    fun `사진 보완은 기존 경로를 유지하고 이후 본문 수정을 덮어쓰지 않는다`() {
        val current =
            createReview(
                id = "review",
                created = "01",
            ).copy(
                body = "현재 본문",
                images =
                    listOf(
                        MyReviewPhoto(
                            url = "stable",
                            reference = "keep",
                        ),
                        MyReviewPhoto(
                            url = null,
                            reference = "new",
                        ),
                    ),
            )
        val hydrated =
            current.copy(
                body = "이전 본문",
                images =
                    listOf(
                        MyReviewPhoto(
                            url = "rotated",
                            reference = "keep",
                        ),
                        MyReviewPhoto(
                            url = "hydrated",
                            reference = "new",
                        ),
                    ),
            )
        val result =
            reducer
                .patch(
                    rows = listOf(current),
                    change =
                        ReviewChange.Hydrated(
                            ReviewSnapshot(
                                review = hydrated,
                                shopName = "shop",
                            ),
                        ),
                    hasMore = false,
                ) { _, _ -> true }
                .single()
        assertEquals("현재 본문", result.body)
        assertEquals(listOf("stable", "hydrated"), result.imageUrls)
        val replaced =
            current.copy(
                images =
                    listOf(
                        MyReviewPhoto(
                            url = null,
                            reference = "replacement",
                        ),
                    ),
            )
        assertEquals(
            replaced,
            reducer
                .patch(
                    rows = listOf(replaced),
                    change =
                        ReviewChange.Hydrated(
                            ReviewSnapshot(
                                review = hydrated,
                                shopName = "shop",
                            ),
                        ),
                    hasMore = false,
                ) { _, _ -> true }
                .single(),
        )
    }

    @Test
    fun `소유자의 변경 응답은 비공개나 미게시 리뷰를 다른 사용자에게 노출하지 않는다`() {
        val private =
            createReview(
                id = "private",
                created = "01",
            ).copy(isPublic = false)
        val pending =
            createReview(
                id = "pending",
                created = "01",
            ).copy(moderationStatus = ReviewModerationStatus.PENDING)
        assertFalse(
            reducer.isShopVisible(
                change =
                    createWrittenChange(
                        before = null,
                        after = private,
                        published = 0,
                    ),
                review = private,
                viewerId = "other",
            ),
        )
        assertFalse(
            reducer.isShopVisible(
                change =
                    createWrittenChange(
                        before = null,
                        after = pending,
                        published = 0,
                    ),
                review = pending,
                viewerId = "other",
            ),
        )
    }

    @Test
    fun `만료된 사진 URL과 경로가 바뀐 아바타는 새로운 값을 사용한다`() {
        val expired = kotlin.time.Instant.parse("2020-01-01T00:00:00Z")
        val valid = kotlin.time.Instant.parse("2099-01-01T00:00:00Z")
        val previous =
            createReview(
                id = "review",
                created = "01",
            ).copy(
                images =
                    listOf(
                        MyReviewPhoto(
                            url = "expired",
                            reference = "path",
                            expiresAt = expired,
                        ),
                    ),
                author =
                    ReviewAuthor(
                        userId = "owner",
                        nickname = "작성자",
                        avatarUrl = "old-avatar",
                        avatarReference = "old",
                        avatarExpiresAt = valid,
                    ),
            )
        val latest =
            previous.copy(
                images =
                    listOf(
                        MyReviewPhoto(
                            url = "renewed",
                            reference = "path",
                            expiresAt = valid,
                        ),
                    ),
                author =
                    previous.author.copy(
                        avatarUrl = "new-avatar",
                        avatarReference = "new",
                    ),
            )
        val merged =
            reducer.retainMedia(
                next = latest,
                previous = previous,
            )
        assertEquals("renewed", merged.images.single().url)
        assertEquals("new-avatar", merged.author.avatarUrl)
    }

    private fun createReview(
        id: String,
        created: String,
        shop: String = "shop",
        visit: Int = 1,
    ): Review =
        Review(
            id = id,
            shopId = shop,
            body = "리뷰 본문",
            createdAt = created,
            author =
                ReviewAuthor(
                    userId = "owner",
                    nickname = "작성자",
                    reviewCount = 3,
                ),
            visitNumber = visit,
        )

    private fun createMyReview(
        id: String,
        created: String,
        shop: RamenShop?,
    ): MyReview =
        MyReview(
            id = id,
            shopId = shop?.id ?: "shop",
            shopName = "매장",
            body = "리뷰 본문",
            createdAt = created,
            images = emptyList(),
            isPublic = true,
            moderationStatus = ReviewModerationStatus.PUBLISHED,
            shop = shop,
            author =
                ReviewAuthor(
                    userId = "owner",
                    nickname = "작성자",
                ),
        )

    private fun createShop(id: String): RamenShop =
        RamenShop(
            id = id,
            name = "매장",
            address = "서울 마포구",
            location =
                Location(
                    lat = 37.0,
                    lng = 127.0,
                ),
            kakaoPlaceUrl = null,
            instagramUrl = null,
            menuCategories = MenuCategories(listOf(Category.SHOYU)),
            isVisible = true,
            createdAt = "2026-10-13",
            updatedAt = "2026-10-13",
            instagramProfileImageUrl = "shop-image",
        )

    private fun createWrittenChange(
        before: Review?,
        after: Review?,
        published: Int,
    ): ReviewChange.Written {
        val review = requireNotNull(after ?: before)
        return ReviewChange.Written(
            reviewId = review.id,
            userId = "owner",
            shopId = review.shopId,
            before =
                before?.let {
                    ReviewSnapshot(
                        review = it,
                        shopName = "shop",
                    )
                },
            after =
                after?.let {
                    ReviewSnapshot(
                        review = it,
                        shopName = "shop",
                    )
                },
            counts =
                ReviewOwnerCounts(
                    totalCount = 2,
                    publicCount = 2,
                    privateCount = 0,
                    publishedCount = published,
                    profileIsPublic = true,
                ),
        )
    }
}
