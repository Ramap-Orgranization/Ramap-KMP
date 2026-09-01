package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.menu.Menu
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.menu.Menus
import com.peto.ramap.domain.model.menu.Price

class MenuSectionsPreviewParameterProvider : PreviewParameterProvider<List<MenuSection>> {
    val defaultMenuSections =
        listOf(
            MenuSection(
                "1",
                "대표 메뉴",
                "가장 인기 있는 메뉴입니다.",
                0,
                Menus(
                    listOf(
                        Menu(
                            "1",
                            "시오라멘",
                            Price(10000),
                            description = "깔끔한 소금 베이스의 라멘",
                            imageUrl = null,
                            displayOrder = 0,
                            isRepresentative = true,
                        ),
                        Menu("2", "쇼유라멘", Price(11000), description = "깊은 맛의 간장 베이스 라멘", imageUrl = null, displayOrder = 1),
                    ),
                ),
            ),
            MenuSection(
                "2",
                "사이드 메뉴",
                displayOrder = 1,
                items =
                    Menus(
                        listOf(
                            Menu("3", "가라아게", Price(5000), description = "바삭한 닭튀김", imageUrl = null, displayOrder = 0),
                        ),
                    ),
            ),
        )

    val defaultTitleMenuSection = defaultMenuSections[1].copy(title = "상시메뉴")

    val highlightedMenuSections =
        listOf(
            MenuSection(
                "limited",
                "한정메뉴",
                "화·수요일 점심한정",
                0,
                Menus(
                    listOf(
                        Menu("limited-1", "니보시 이에케", Price(13000), description = null, imageUrl = null, displayOrder = 0),
                        Menu(
                            "limited-2",
                            "이에케라멘",
                            Price(11000),
                            description = null,
                            imageUrl = "https://ldb-phinf.pstatic.net/20240817_167/1723888449228rqsuN_JPEG/20240813_132233.jpg",
                            displayOrder = 1,
                        ),
                    ),
                ),
            ),
            MenuSection(
                "event",
                "8월 이벤트 메뉴",
                displayOrder = 1,
                items =
                    Menus(
                        listOf(
                            Menu(
                                "event-1",
                                "냉 게우 츠케멘",
                                Price(12000),
                                description = "전복내장의 깊고 진한 바다 풍미",
                                imageUrl = null,
                                isRepresentative = true,
                                displayOrder = 0,
                            ),
                        ),
                    ),
            ),
        )

    val sourceMenuSections =
        listOf(
            MenuSection(
                "source",
                "상시메뉴",
                displayOrder = 0,
                items =
                    Menus(
                        listOf(
                            Menu(
                                "source-1",
                                "네이버 소식 / 인스타그램 참고",
                                priceText = "매일 변동",
                                description = "매일 메뉴가 바뀝니다.",
                                imageUrl = "https://search.pstatic.net/common/?autoRotate=true&quality=95&type=f320_320&src=https%3A%2F%2Fldb-phinf.pstatic.net%2F20260112_220%2F1768183556400nkALp_JPEG%2FIMG_4441.jpeg",
                                sourceUrl = "https://pcmap.place.naver.com/restaurant/2030397790/feed",
                                isRepresentative = true,
                                displayOrder = 0,
                            ),
                        ),
                    ),
            ),
        )

    override val values: Sequence<List<MenuSection>> =
        sequenceOf(
            defaultMenuSections,
            highlightedMenuSections,
            sourceMenuSections,
        )
}
