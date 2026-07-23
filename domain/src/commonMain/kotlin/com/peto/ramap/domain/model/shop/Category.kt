package com.peto.ramap.domain.model.shop

enum class Category(
    val id: String,
) {
    TONKOTSU("tonkotsu"),
    SHOYU("shoyu"),
    SHIO("shio"),
    TORI("tori"),
    IEKEI("iekei"),
    JIRO("jiro"),
    TSUKEMEN("tsukemen"),
    MAZESOBA("mazesoba"),
    ABURASOBA("aburasoba"),
    MISO("miso"),
    CHANKE("chanke"),
    NIBOSHI_GYOKAI("niboshi_gyokai"),
    HIYASHI("hiyashi"),
    TOMATO("tomato"),
    ;

    companion object {
        private val categoriesById: Map<String, Category> = entries.associateBy(Category::id)

        fun fromId(id: String): Category? = categoriesById[id]
    }
}
