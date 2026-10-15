package com.peto.ramap.ui.shop.overview.model

/** 다른 화면에서 지도에 진입한 순간을 상세의 비동기 작업과 공유한다. */
class ShopDetailEntryContext {
    var generation: Long = 0
        private set

    fun advance(): Long = ++generation

    fun advanceTo(generation: Long) {
        if (generation > this.generation) this.generation = generation
    }

    fun includes(generation: Long): Boolean = this.generation == generation
}
