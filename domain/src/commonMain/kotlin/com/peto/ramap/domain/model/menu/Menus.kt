package com.peto.ramap.domain.model.menu

data class Menus(
    private val values: List<Menu>,
) : List<Menu> by values
