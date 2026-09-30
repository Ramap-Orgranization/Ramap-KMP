package com.peto.ramap.domain.model.moderation

class KoreanProfanityFilter(
    blockedWords: Collection<String> = KoreanProfanityDictionary.blockedWords,
    allowedExpressions: Collection<String> = KoreanProfanityDictionary.allowedExpressions,
) {
    private val blockedWords = normalizeWords(blockedWords)
    private val allowedExpressions = normalizeWords(allowedExpressions).sortedByDescending { it.length }

    fun containsProfanity(text: String): Boolean {
        var normalized = KoreanProfanityNormalizer.normalize(text)
        for (expression in allowedExpressions) {
            normalized = normalized.replace(expression, EXCEPTION_BOUNDARY)
        }
        return blockedWords.any { normalized.contains(it) }
    }

    private fun normalizeWords(words: Collection<String>): List<String> = words.map(KoreanProfanityNormalizer::normalize).filter { it.isNotEmpty() }.distinct()

    private companion object {
        const val EXCEPTION_BOUNDARY = "\u0000"
    }
}
