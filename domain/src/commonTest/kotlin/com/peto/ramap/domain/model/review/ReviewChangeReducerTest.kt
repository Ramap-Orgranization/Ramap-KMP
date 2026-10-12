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
    @Test
    fun `new owner row includes shop image address and categories for a previously unloaded shop`() {
        val image = MyReviewPhoto("retained-url", "path")
        val existing = ownerRow("existing", "01", shop("existing-shop")).copy(images = listOf(image))
        val created = row("new", "02", shop = "new-shop").copy(isPublic = false)
        val newShop = shop("new-shop")
        val change = write(null, created, 1).let { it.copy(after = it.after?.copy(shop = newShop)) }

        val result = ReviewChangeReducer.ownerRows(listOf(existing), change, MyReviewVisibility.ALL, false)

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
    fun `shop hydration repairs an owner row without replacing existing photo or body`() {
        val current = ownerRow("review", "01", null).copy(body = "현재 본문", images = listOf(MyReviewPhoto("stable", "path")))
        val hydrated = row("review", "01").copy(body = "이전 본문", images = listOf(MyReviewPhoto("rotated", "path")))
        val shop = shop("shop")

        val result =
            ReviewChangeReducer
                .ownerRows(
                    listOf(current),
                    ReviewChange.Hydrated(ReviewSnapshot(hydrated, "매장", shop = shop)),
                    MyReviewVisibility.ALL,
                    false,
                ).single()

        assertSame(shop, result.shop)
        assertEquals("현재 본문", result.body)
        assertSame(current.images.single(), result.images.single())
    }

    @Test
    fun `write and hydration without shop retain loaded owner row shop`() {
        val shop = shop("shop")
        val current = ownerRow("review", "01", shop)
        val edited = row("review", "01").copy(body = "수정 본문")
        val written = ReviewChangeReducer.ownerRows(listOf(current), write(row("review", "01"), edited, 1), MyReviewVisibility.ALL, false)
        val hydrated = ReviewChangeReducer.ownerRows(written, ReviewChange.Hydrated(ReviewSnapshot(edited, "매장")), MyReviewVisibility.ALL, false)

        assertSame(shop, written.single().shop)
        assertSame(shop, hydrated.single().shop)
    }

    @Test
    fun `delete patches subsequent visits and every sibling public count without changing image identities`() {
        val earlier = row("earlier", "01", visit = 1)
        val deleted = row("deleted", "02", visit = 2)
        val later = row("later", "03", visit = 3)
        val image = MyReviewPhoto("retained-url", "path")
        val unrelated = row("unrelated", "04", shop = "other").copy(images = listOf(image), imageUrls = listOf("retained-url"))
        val result = ReviewChangeReducer.patch(listOf(unrelated, later, deleted, earlier), write(deleted, null, 2), true) { _, _ -> true }
        assertEquals(listOf("unrelated", "later", "earlier"), result.map { it.id })
        assertEquals(2, result.first { it.id == "later" }.visitNumber)
        assertEquals(1, result.last().visitNumber)
        assertEquals(listOf(2, 2, 2), result.map { it.author.reviewCount })
        assertSame(image, result.first().images.single())
    }

    @Test
    fun `private create and body edit do not increment subsequent public visits`() {
        val later = row("later", "03", visit = 2)
        val private = row("private", "02").copy(isPublic = false)
        val created = ReviewChangeReducer.patch(listOf(later), write(null, private, 2), true) { _, _ -> true }
        assertEquals(2, created.first().visitNumber)
        val edited = private.copy(body = "새로운 본문")
        val updated = ReviewChangeReducer.patch(created, write(private, edited, 2), true) { _, _ -> true }
        assertEquals(2, updated.first().visitNumber)
        val published = edited.copy(isPublic = true)
        val visible = ReviewChangeReducer.patch(updated, write(edited, published, 3), true) { _, _ -> true }
        assertEquals(3, visible.first().visitNumber)
    }

    @Test
    fun `insertion outside incomplete loaded boundary does not skip later pagination`() {
        val rows = (40 downTo 1).map { row("id-$it", it.toString().padStart(2, '0')) }
        val old = row("unloaded", "00")
        assertEquals(rows.map { it.id }, ReviewChangeReducer.patch(rows, write(null, old, 0), true) { _, _ -> true }.map { it.id })
        val newest = row("newest", "41")
        assertEquals(41, ReviewChangeReducer.patch(rows, write(null, newest, 0), true) { _, _ -> true }.size)
    }

    @Test
    fun `hydration preserves retained paths and cannot overwrite later edits`() {
        val current = row("review", "01").copy(body = "현재 본문", images = listOf(MyReviewPhoto("stable", "keep"), MyReviewPhoto(null, "new")))
        val hydrated = current.copy(body = "이전 본문", images = listOf(MyReviewPhoto("rotated", "keep"), MyReviewPhoto("hydrated", "new")))
        val result = ReviewChangeReducer.patch(listOf(current), ReviewChange.Hydrated(ReviewSnapshot(hydrated, "shop")), false) { _, _ -> true }.single()
        assertEquals("현재 본문", result.body)
        assertEquals(listOf("stable", "hydrated"), result.imageUrls)
        val replaced = current.copy(images = listOf(MyReviewPhoto(null, "replacement")))
        assertEquals(replaced, ReviewChangeReducer.patch(listOf(replaced), ReviewChange.Hydrated(ReviewSnapshot(hydrated, "shop")), false) { _, _ -> true }.single())
    }

    @Test
    fun `owner receipt never exposes private or unpublished review to another viewer`() {
        val private = row("private", "01").copy(isPublic = false)
        val pending = row("pending", "01").copy(moderationStatus = ReviewModerationStatus.PENDING)
        assertFalse(ReviewChangeReducer.shopVisible(write(null, private, 0), private, "other"))
        assertFalse(ReviewChangeReducer.shopVisible(write(null, pending, 0), pending, "other"))
    }

    @Test
    fun `expired retained image uses new signed URL and changed avatar path uses new avatar`() {
        val expired = kotlin.time.Instant.parse("2020-01-01T00:00:00Z")
        val valid = kotlin.time.Instant.parse("2099-01-01T00:00:00Z")
        val previous = row("review", "01").copy(images = listOf(MyReviewPhoto("expired", "path", expired)), author = ReviewAuthor("owner", "작성자", "old-avatar", avatarReference = "old", avatarExpiresAt = valid))
        val latest = previous.copy(images = listOf(MyReviewPhoto("renewed", "path", valid)), author = previous.author.copy(avatarUrl = "new-avatar", avatarReference = "new"))
        val merged = ReviewChangeReducer.mergeImages(latest, previous)
        assertEquals("renewed", merged.images.single().url)
        assertEquals("new-avatar", merged.author.avatarUrl)
    }

    private fun row(
        id: String,
        created: String,
        shop: String = "shop",
        visit: Int = 1,
    ) = Review(id, shop, "리뷰 본문", created, author = ReviewAuthor("owner", "작성자", reviewCount = 3), visitNumber = visit)

    private fun ownerRow(
        id: String,
        created: String,
        shop: RamenShop?,
    ) = MyReview(id, shop?.id ?: "shop", "매장", "리뷰 본문", created, emptyList(), true, ReviewModerationStatus.PUBLISHED, shop = shop, author = ReviewAuthor("owner", "작성자"))

    private fun shop(id: String) =
        RamenShop(
            id = id,
            name = "매장",
            address = "서울 마포구",
            location = Location(37.0, 127.0),
            kakaoPlaceUrl = null,
            instagramUrl = null,
            menuCategories = MenuCategories(listOf(Category.SHOYU)),
            isVisible = true,
            createdAt = "2026-10-13",
            updatedAt = "2026-10-13",
            instagramProfileImageUrl = "shop-image",
        )

    private fun write(
        before: Review?,
        after: Review?,
        published: Int,
    ) = ReviewChange.Written((after ?: before)!!.id, "owner", (after ?: before)!!.shopId, before?.let { ReviewSnapshot(it, "shop") }, after?.let { ReviewSnapshot(it, "shop") }, ReviewOwnerCounts(2, 2, 0, published, true))
}
