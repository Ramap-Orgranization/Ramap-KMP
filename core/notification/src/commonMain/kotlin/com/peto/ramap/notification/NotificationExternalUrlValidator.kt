package com.peto.ramap.notification

class NotificationExternalUrlValidator {
    fun isValid(value: String): Boolean {
        if (value.length !in 1..MAX_URL_LENGTH || !value.startsWith(HTTPS_PREFIX)) return false
        if (value.any { it.isWhitespace() || it.code < 32 || it.code in 127..159 || it == '\\' || it == '"' || it == '<' || it == '>' } || '#' in value) return false
        val authority = value.removePrefix(HTTPS_PREFIX).takeWhile { it != '/' && it != '?' }
        if ('@' in authority || '%' in authority) return false
        val host = authority.removeSuffix(":443").lowercase()
        if (':' in host || host.length > MAX_HOST_LENGTH || !isPublicDnsHost(host)) return false
        return hasValidEncoding(value)
    }

    private fun isPublicDnsHost(host: String): Boolean {
        val labels = host.split('.')
        if (labels.size < 2 || labels.any { !DNS_LABEL.matches(it) }) return false
        if (labels.all { label -> label.all(Char::isDigit) || HEX_ADDRESS_PART.matches(label) }) return false
        if (labels.last() in RESERVED_SUFFIXES) return false
        return labels.last().any(Char::isLetter)
    }

    private fun hasValidEncoding(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            if (value[index] != '%') {
                index++
                continue
            }
            val encodedBytes = mutableListOf<Byte>()
            while (index < value.length && value[index] == '%') {
                if (index + 2 >= value.length) return false
                val byte = value.substring(index + 1, index + 3).toIntOrNull(16) ?: return false
                if (byte < 32 || byte == 127 || byte == 92 || byte == 37) return false
                encodedBytes += byte.toByte()
                index += 3
            }
            if (runCatching { encodedBytes.toByteArray().decodeToString(throwOnInvalidSequence = true) }.isFailure) return false
        }
        return true
    }

    private companion object {
        const val HTTPS_PREFIX = "https://"
        const val MAX_URL_LENGTH = 2048
        const val MAX_HOST_LENGTH = 253
        val HEX_ADDRESS_PART = Regex("0x[0-9a-f]+")
        val DNS_LABEL = Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")
        val RESERVED_SUFFIXES = setOf("local", "internal", "localhost", "test", "invalid", "example", "onion", "lan", "home", "intranet", "corp")
    }
}
