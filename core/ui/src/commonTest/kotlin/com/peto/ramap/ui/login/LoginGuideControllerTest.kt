package com.peto.ramap.ui.login

import com.peto.ramap.domain.model.auth.LoginType
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_bookmark_description
import ramap.shared.generated.resources.login_required_profile_description
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class LoginGuideControllerTest {
    private val controller = LoginGuideController()

    @Test
    fun `로그인 선택은 안내를 먼저 닫고 요청한 화면에 한 번만 전달한다`() {
        val selected = mutableListOf<LoginType>()
        var dismissed = 0
        controller.show(
            owner = Any(),
            description = Res.string.login_required_bookmark_description,
            onLoginTypeSelected = {
                assertNull(controller.request.value)
                selected += it
            },
            onDismiss = { dismissed++ },
        )
        val request = assertNotNull(controller.request.value)
        assertEquals(Res.string.login_required_bookmark_description, request.description)

        controller.select(request, LoginType.KAKAO)
        controller.select(request, LoginType.APPLE)
        controller.dismiss(request)

        assertEquals(listOf(LoginType.KAKAO), selected)
        assertEquals(0, dismissed)
    }

    @Test
    fun `안내를 닫으면 로그인 실행 없이 취소 콜백만 한 번 호출한다`() {
        var dismissed = 0
        val selected = mutableListOf<LoginType>()
        controller.show(
            owner = Any(),
            description = Res.string.login_required_profile_description,
            onLoginTypeSelected = { selected += it },
            onDismiss = { dismissed++ },
        )
        val request = assertNotNull(controller.request.value)

        controller.dismiss(request)
        controller.dismiss(request)
        controller.select(request, LoginType.KAKAO)

        assertNull(controller.request.value)
        assertEquals(1, dismissed)
        assertEquals(emptyList(), selected)
    }

    @Test
    fun `다른 화면의 요청으로 교체하면 이전 대기 동작을 취소하고 새 요청만 유지한다`() {
        val previousOwner = Any()
        val nextOwner = Any()
        var previousDismissed = 0
        val selected = mutableListOf<LoginType>()
        controller.show(
            owner = previousOwner,
            description = Res.string.login_required_bookmark_description,
            onLoginTypeSelected = { error("이전 화면의 로그인은 실행하면 안 된다") },
            onDismiss = { previousDismissed++ },
        )
        val previous = assertNotNull(controller.request.value)
        controller.show(
            owner = nextOwner,
            description = Res.string.login_required_profile_description,
            onLoginTypeSelected = { selected += it },
            onDismiss = {},
        )
        val next = assertNotNull(controller.request.value)

        controller.cancel(previousOwner)
        controller.dismiss(previous)
        controller.select(previous, LoginType.APPLE)

        assertSame(next, controller.request.value)
        assertEquals(1, previousDismissed)
        controller.select(next, LoginType.KAKAO)
        assertEquals(listOf(LoginType.KAKAO), selected)
    }

    @Test
    fun `같은 화면에서 다시 요청해도 최신 대기 동작은 취소하지 않는다`() {
        val owner = Any()
        var dismissed = 0
        controller.show(
            owner = owner,
            description = Res.string.login_required_bookmark_description,
            onLoginTypeSelected = {},
            onDismiss = { dismissed++ },
        )
        val previous = assertNotNull(controller.request.value)
        controller.show(
            owner = owner,
            description = Res.string.login_required_profile_description,
            onLoginTypeSelected = {},
            onDismiss = { dismissed++ },
        )

        controller.dismiss(previous)

        assertEquals(0, dismissed)
        assertEquals(Res.string.login_required_profile_description, controller.request.value?.description)
        controller.cancel(owner)
        controller.cancel(owner)
        assertNull(controller.request.value)
        assertEquals(1, dismissed)
    }

    @Test
    fun `로그인 콜백이 새 안내를 요청해도 이전 요청의 정리가 새 안내를 지우지 않는다`() {
        val owner = Any()
        controller.show(
            owner = owner,
            description = Res.string.login_required_bookmark_description,
            onLoginTypeSelected = {
                controller.show(
                    owner = owner,
                    description = Res.string.login_required_profile_description,
                    onLoginTypeSelected = {},
                    onDismiss = {},
                )
            },
            onDismiss = {},
        )
        val previous = assertNotNull(controller.request.value)

        controller.select(previous, LoginType.KAKAO)
        controller.dismiss(previous)

        assertEquals(Res.string.login_required_profile_description, controller.request.value?.description)
    }
}
