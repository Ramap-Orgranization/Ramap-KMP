package com.peto.ramap.domain.model.shop

enum class Category(
    val id: String,
) {
    TONKOTSU("tonkotsu"),
    SHOYU("shoyu"),
    SHIO("shio"),
    MISO("miso"),
    TORI("tori"),
    TSUKEMEN("tsukemen"),
    MAZESOBA("mazesoba"),
    ABURASOBA("aburasoba"),
    JIRO("jiro"),
    NIBOSHI_GYOKAI("niboshi_gyokai"),
    IEKEI("iekei"),
    HIYASHI("hiyashi"),
    CHUKASOBA("chukasoba"),
    TOMATO("tomato"),
    ;

    companion object {
        private val categoriesById: Map<String, Category> = entries.associateBy(Category::id)

        fun fromId(id: String): Category? = categoriesById[id]
    }
}
