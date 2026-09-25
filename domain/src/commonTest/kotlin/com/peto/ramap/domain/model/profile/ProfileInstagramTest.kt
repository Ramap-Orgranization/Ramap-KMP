package com.peto.ramap.domain.model.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileInstagramTest {
    @Test
    fun normalizesHandlesAndExactProfileUrls() {
        for (input in listOf("RaMap.official", " @RaMap.official ", "https://instagram.com/RaMap.official", "HTTP://WWW.INSTAGRAM.COM/RaMap.official/", "instagram.com/RaMap.official/?igsh=123#profile", "www.instagram.com/RaMap.official?igsh=123", "https://instagram.com/RaMap.official/#profile")) {
            assertEquals("ramap.official", ProfileInstagram.normalize(input), input)
        }
        assertEquals("", ProfileInstagram.normalize(" \n "))
        assertEquals("_", ProfileInstagram.normalize("_"))
        assertEquals("a".repeat(30), ProfileInstagram.normalize("A".repeat(30)))
    }

    @Test
    fun rejectsWrongHostsReservedRoutesAndInvalidHandles() {
        for (input in listOf("@", "@@ramap", "a".repeat(31), "라맵", "ra map", "ra-map", ".ramap", "ramap.", "ra..map", ".", "..", "https://evil.test/ramap", "https://instagram.com.evil.test/ramap", "https://instagram.com@evil.test/ramap", "https://evil.test@instagram.com/ramap", "https://instagram.com:443/ramap", "ftp://instagram.com/ramap", "https://instagram.com/", "https://instagram.com/ramap/extra", "https://instagram.com/p/post", "https://instagram.com/reel/post", "https://instagram.com/%72amap", "https://instagram.com/ramap/?x=one\ntwo", "https://instagram.com/../", "https://instagram.com/ramap//")) {
            assertNull(ProfileInstagram.normalize(input), input)
        }
        for (route in listOf("about", "accounts", "api", "challenge", "developer", "developers", "direct", "emails", "explore", "legal", "nametag", "oauth", "p", "privacy", "reel", "reels", "share", "stories", "terms", "tv", "web")) {
            assertNull(ProfileInstagram.normalize(route), route)
            assertNull(ProfileInstagram.normalize("https://instagram.com/${route.uppercase()}/"), route)
        }
    }

    @Test
    fun generatesUrlsOnlyFromCanonicalUsernames() {
        assertEquals("https://www.instagram.com/ramap.official/", ProfileInstagram.url("ramap.official"))
        for (input in listOf("", "@ramap", "RaMap", "https://instagram.com/ramap", "..", "p")) {
            assertNull(ProfileInstagram.url(input), input)
        }
    }
}
