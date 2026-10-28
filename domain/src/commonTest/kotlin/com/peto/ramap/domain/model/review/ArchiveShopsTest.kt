package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchiveShopsTest {
    @Test
    fun `최신 방문에 사진이 없어도 사진이 있는 최신 리뷰의 첫 사진을 선택한다`() {
        val rows =
            listOf(
                review("newest", "2026-10-26", emptyList()),
                review("photo", "2026-10-25", listOf("first", "second")),
                review("old", "2026-10-24", listOf("older")),
            )
        assertEquals("first", visits(rows).representativePhotos()["shop"]?.reference)
    }

    @Test
    fun `작성 시각이 같으면 리뷰 ID 내림차순으로 대표를 선택한다`() {
        val rows = listOf(review("a", "2026-10-25", listOf("a")), review("b", "2026-10-25", listOf("b")))
        assertEquals("b", visits(rows).representativePhotos()["shop"]?.reference)
    }

    @Test
    fun `대표 리뷰 삭제 시 이전 사진을 선택하고 마지막 리뷰 삭제 시 사진이 없다`() {
        val rows = listOf(review("new", "2026-10-26", listOf("new")), review("old", "2026-10-25", listOf("old")))
        assertEquals("old", visits(rows.drop(1)).representativePhotos()["shop"]?.reference)
        assertTrue(visits(emptyList()).representativePhotos().isEmpty())
    }

    @Test
    fun `선택 매장 기록은 작성자 ID가 비어 있어도 시각과 ID 내림차순으로 정렬한다`() {
        val rows = listOf(review("a", "2026-10-25", emptyList()), review("b", "2026-10-25", emptyList()), review("new", "2026-10-26", emptyList()))
        assertEquals(listOf("new", "b", "a"), visits(rows).reviewsForShop("shop").map { it.review.id })
        assertTrue(visits(rows).reviewsForShop("other").isEmpty())
    }

    private fun review(
        id: String,
        createdAt: String,
        references: List<String>,
    ) = MyReview(
        Review(id, "shop", "방문 후기입니다", createdAt, images = references.map { MyReviewPhoto(null, it) }),
        "매장",
    )

    private fun visits(rows: List<MyReview>) =
        ArchiveShops(
            rows,
            RamenShops(listOf(RamenShop("shop", "매장", "서울", Location(37.0, 127.0), null, instagramUrl = null, menuCategories = MenuCategories(emptyList()), isVisible = true, createdAt = "", updatedAt = ""))),
        )
}
