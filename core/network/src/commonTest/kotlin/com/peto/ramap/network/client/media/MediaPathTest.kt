package com.peto.ramap.network.client.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaPathTest {
    @Test
    fun `상대 경로의 미디어 파일을 허용한다`() {
        assertTrue(MediaPath("shops/example.jpg").isValid)
    }

    @Test
    fun `여러 하위 디렉터리를 가진 미디어 경로를 허용한다`() {
        assertTrue(MediaPath("events/2026/01/banner.png").isValid)
    }

    @Test
    fun `빈 문자열은 미디어 경로로 허용하지 않는다`() {
        assertFalse(MediaPath("").isValid)
    }

    @Test
    fun `공백만 있는 문자열은 미디어 경로로 허용하지 않는다`() {
        val blankPaths =
            listOf(
                " ",
                "\t",
                "\n",
                " \t\n ",
            )

        for (path in blankPaths) {
            assertFalse(MediaPath(path).isValid)
        }
    }

    @Test
    fun `앞에 공백이 있는 미디어 경로를 허용하지 않는다`() {
        val pathsWithLeadingWhitespace =
            listOf(
                " shops/example.jpg",
                "\tshops/example.jpg",
                "\nshops/example.jpg",
            )

        for (path in pathsWithLeadingWhitespace) {
            assertFalse(MediaPath(path).isValid)
        }
    }

    @Test
    fun `뒤에 공백이 있는 미디어 경로를 허용하지 않는다`() {
        val pathsWithTrailingWhitespace =
            listOf(
                "shops/example.jpg ",
                "shops/example.jpg\t",
                "shops/example.jpg\n",
            )

        for (path in pathsWithTrailingWhitespace) {
            assertFalse(MediaPath(path).isValid)
        }
    }

    @Test
    fun `슬래시로 시작하는 절대 경로를 허용하지 않는다`() {
        assertFalse(MediaPath("/shops/example.jpg").isValid)
    }

    @Test
    fun `상위 디렉터리 탐색이 포함된 미디어 경로를 허용하지 않는다`() {
        val pathsWithTraversal =
            listOf(
                "../shops/example.jpg",
                "shops/../example.jpg",
                "shops/../../example.jpg",
            )

        for (path in pathsWithTraversal) {
            assertFalse(
                actual = MediaPath(path).isValid,
                message = "상위 디렉터리 탐색을 거절해야 한다: $path",
            )
        }
    }

    @Test
    fun `백슬래시가 포함된 미디어 경로를 허용하지 않는다`() {
        assertFalse(MediaPath("shops\\example.jpg").isValid)
    }

    @Test
    fun `퍼센트 기호가 포함된 미디어 경로를 허용하지 않는다`() {
        val pathsWithPercentSigns =
            listOf(
                "shops/example%20image.jpg",
                "shops/%2e%2e/example.jpg",
            )

        for (path in pathsWithPercentSigns) {
            assertFalse(
                actual = MediaPath(path).isValid,
                message = "퍼센트 기호가 포함된 경로를 거절해야 한다: $path",
            )
        }
    }

    @Test
    fun `쿼리 문자열이 포함된 미디어 경로를 허용하지 않는다`() {
        assertFalse(MediaPath("shops/example.jpg?token=123").isValid)
    }

    @Test
    fun `프래그먼트가 포함된 미디어 경로를 허용하지 않는다`() {
        assertFalse(MediaPath("shops/example.jpg#section").isValid)
    }

    @Test
    fun `URL 스킴이 포함된 미디어 경로를 허용하지 않는다`() {
        val pathsWithUrlSchemes =
            listOf(
                "https://example.com/image.jpg",
                "http://example.com/image.jpg",
                "file://image.jpg",
            )

        for (path in pathsWithUrlSchemes) {
            assertFalse(
                actual = MediaPath(path).isValid,
                message = "URL 스킴이 포함된 경로를 거절해야 한다: $path",
            )
        }
    }

    @Test
    fun `유효한 상대 경로로 미디어 경로를 생성한다`() {
        assertEquals(
            expected = MediaPath("shops/example.jpg"),
            actual = MediaPath.from("shops/example.jpg"),
        )
    }

    @Test
    fun `미디어 경로 생성 시 앞뒤 공백을 제거한다`() {
        assertEquals(
            expected = MediaPath("shops/example.jpg"),
            actual = MediaPath.from(" \tshops/example.jpg\r\n "),
        )
    }

    @Test
    fun `null로는 미디어 경로를 생성하지 않는다`() {
        assertNull(MediaPath.from(null))
    }

    @Test
    fun `빈 문자열로는 미디어 경로를 생성하지 않는다`() {
        assertNull(MediaPath.from(""))
    }

    @Test
    fun `공백만 있는 문자열로는 미디어 경로를 생성하지 않는다`() {
        val blankPaths =
            listOf(
                " ",
                "\t",
                "\n",
                " \t\n ",
            )

        for (path in blankPaths) {
            assertNull(MediaPath.from(path))
        }
    }

    @Test
    fun `형식이 잘못된 경로로는 미디어 경로를 생성하지 않는다`() {
        val invalidPaths =
            listOf(
                "/shops/example.jpg",
                "shops/../example.jpg",
                "shops\\example.jpg",
                "shops/example%20image.jpg",
                "shops/example.jpg?token=123",
                "shops/example.jpg#section",
                "https://example.com/image.jpg",
            )

        for (path in invalidPaths) {
            assertNull(
                actual = MediaPath.from(path),
                message = "형식이 잘못된 경로로는 객체를 생성하지 않아야 한다: $path",
            )
        }
    }

    @Test
    fun `앞뒤 공백을 제거해도 잘못된 경로로는 미디어 경로를 생성하지 않는다`() {
        assertNull(MediaPath.from("  /shops/example.jpg  "))
    }
}
