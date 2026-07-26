package com.peto.ramap.navigation.deeplink

class ShopDeepLinkParser(
    private val config: ShopLinkConfig,
) {
    fun parse(rawUrl: String?): ShopDeepLink? {
        val value = rawUrl?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return parseWebLink(value) ?: parseCustomLink(value)
    }

    private fun parseWebLink(value: String): ShopDeepLink? =
        parseShopId(
            value = value,
            prefix = "https://${config.webHost}/shops/",
            ignoreCase = true,
        )

    private fun parseCustomLink(value: String): ShopDeepLink? =
        parseShopId(
            value = value,
            prefix = "${config.customScheme}://${config.customHost}/",
            ignoreCase = true,
        )

    private fun parseShopId(
        value: String,
        prefix: String,
        ignoreCase: Boolean,
    ): ShopDeepLink? {
        if (!value.startsWith(prefix, ignoreCase = ignoreCase)) return null
        val pathAndSuffix = value.substring(prefix.length)
        val shopId = pathAndSuffix.substringBefore('?').substringBefore('#')
        if (shopId.isBlank() || '/' in shopId) return null
        return ShopDeepLink.Shop(shopId = decodePathSegment(shopId) ?: return null)
    }

    private fun decodePathSegment(value: String): String? {
        val output = mutableListOf<Byte>()
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character != '%') {
                character.toString().encodeToByteArray().forEach(output::add)
                index++
                continue
            }
            if (index + 2 >= value.length) return null
            val high = value[index + 1].digitToIntOrNull(16) ?: return null
            val low = value[index + 2].digitToIntOrNull(16) ?: return null
            output.add(((high shl 4) or low).toByte())
            index += 3
        }
        return output.toByteArray().decodeToString().takeIf(String::isNotBlank)
    }
}
