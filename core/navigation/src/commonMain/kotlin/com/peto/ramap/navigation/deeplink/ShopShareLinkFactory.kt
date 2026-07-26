package com.peto.ramap.navigation.deeplink

class ShopShareLinkFactory(
    private val config: ShopLinkConfig,
) {
    fun create(shopId: String): String {
        require(shopId.isNotBlank())
        return "${config.baseUrl.trimEnd('/')}/shops/${encodePathSegment(shopId)}"
    }

    private fun encodePathSegment(value: String): String =
        buildString {
            value.encodeToByteArray().forEach { byte ->
                val unsigned = byte.toInt() and 0xFF
                if (isUnreserved(unsigned)) {
                    append(unsigned.toChar())
                } else {
                    append('%')
                    append(HEX_DIGITS[unsigned shr 4])
                    append(HEX_DIGITS[unsigned and 0x0F])
                }
            }
        }

    private fun isUnreserved(value: Int): Boolean =
        value in 'a'.code..'z'.code ||
            value in 'A'.code..'Z'.code ||
            value in '0'.code..'9'.code ||
            value == '-'.code ||
            value == '.'.code ||
            value == '_'.code ||
            value == '~'.code

    private companion object {
        const val HEX_DIGITS = "0123456789ABCDEF"
    }
}
