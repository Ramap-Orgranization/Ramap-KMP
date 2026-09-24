package com.peto.ramap.ui.main.my.profile

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_saved
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {
    @Test
    fun `프로필 조회 중 로그아웃하면 조회 로딩을 종료한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { fetchPending = CompletableDeferred() }
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            assertTrue(model.uiState.value.loading)

            repository.sessionUserIds.value = null
            runCurrent()
            assertFalse(model.uiState.value.loading)
        }

    @Test
    fun `프로필을 불러오면 즉시 수정 상태가 되고 인스타그램 링크를 아이디로 저장한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            assertTrue(model.uiState.value.editing)
            model.dispatch(ProfileIntent.ChangeInstagram("https://www.instagram.com/Ramap_Official/?igsh=shared"))
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertEquals(
                "ramap_official",
                model.uiState.value.profile
                    ?.instagramUsername,
            )
            assertEquals(1, repository.saveCalls)
            assertEquals(ProfileSideEffect.Toast(Res.string.profile_saved), model.sideEffect.first())
            assertEquals(ProfileSideEffect.NavigateBack, model.sideEffect.first())
        }

    @Test
    fun `잘못된 인스타그램 주소는 저장하지 않고 취소한 초안을 버린다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            for (invalid in listOf("https://instagram.com.evil.test/ramap", "https://instagram.com/p/example/")) {
                model.dispatch(ProfileIntent.ChangeInstagram(invalid))
                model.dispatch(ProfileIntent.Save)
                runCurrent()
                assertTrue(model.uiState.value.instagramInvalid)
                assertFalse(model.uiState.value.canSave)
            }
            assertEquals(0, repository.saveCalls)
            model.dispatch(ProfileIntent.Back)
            runCurrent()
            assertTrue(model.uiState.value.confirmDiscard)
            model.dispatch(ProfileIntent.Discard)
            runCurrent()
            assertEquals("", model.uiState.value.instagram)
            assertFalse(model.uiState.value.instagramInvalid)
        }

    @Test
    fun `자기소개만 바꾸어 저장한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeBio("  담백한 라멘을 좋아해요 🍜  "))
            runCurrent()
            assertTrue(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertEquals(
                "담백한 라멘을 좋아해요 🍜",
                model.uiState.value.profile
                    ?.bio,
            )
            assertEquals(1, repository.saveCalls)
        }

    @Test
    fun `긴 자기소개나 줄바꿈은 저장하지 않고 버린 초안은 복원하지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            for (invalid in listOf("가".repeat(51), "라멘\n좋아요")) {
                model.dispatch(ProfileIntent.ChangeBio(invalid))
                model.dispatch(ProfileIntent.Save)
                runCurrent()
                assertTrue(model.uiState.value.bioInvalid)
                assertFalse(model.uiState.value.canSave)
            }
            assertEquals(0, repository.saveCalls)
            model.dispatch(ProfileIntent.Back)
            runCurrent()
            assertTrue(model.uiState.value.confirmDiscard)
            model.dispatch(ProfileIntent.Discard)
            runCurrent()
            assertEquals("", model.uiState.value.bio)
            assertEquals(
                "",
                model.uiState.value.profile
                    ?.bio,
            )
        }

    @Test
    fun `변경 없는 닉네임과 유효하지 않은 닉네임은 저장하지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertFalse(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.ChangeNickname("!"))
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertTrue(model.uiState.value.nicknameInvalid)
            assertEquals(0, repository.saveCalls)
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertEquals(
                "새로운차슈",
                model.uiState.value.profile
                    ?.nickname,
            )
            assertFalse(model.uiState.value.editing)
        }

    @Test
    fun `변경한 닉네임은 중복 확인 전후 상태에 따라 저장을 허용한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { nicknameAvailable = false }
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            runCurrent()
            assertFalse(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            assertFalse(model.uiState.value.canSave)
            assertEquals(false, model.uiState.value.nicknameAvailable)
            repository.nicknameAvailable = true
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            assertTrue(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.ChangeNickname("다른차슈"))
            runCurrent()
            assertFalse(model.uiState.value.canSave)
            assertNull(model.uiState.value.nicknameAvailable)
        }

    @Test
    fun `중복 확인 응답 뒤에 닉네임을 바꾸면 오래된 결과를 무시한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { nicknameCheckResult = CompletableDeferred() }
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            assertTrue(model.uiState.value.checkingNickname)
            model.dispatch(ProfileIntent.ChangeNickname("다른차슈"))
            runCurrent()
            assertFalse(model.uiState.value.checkingNickname)
            repository.nicknameCheckResult?.complete(RamapResult.Success(true))
            runCurrent()
            assertNull(model.uiState.value.nicknameAvailable)
            assertFalse(model.uiState.value.canSave)
        }

    @Test
    fun `변경한 초안에서 돌아가면 확인 후 취소하고 마이 탭으로 돌아간다`() =
        coroutinesTest {
            val model = MyProfileViewModel(FakeProfileRepository(), FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.Back)
            runCurrent()
            assertTrue(model.uiState.value.confirmDiscard)
            model.dispatch(ProfileIntent.KeepEditing)
            runCurrent()
            assertTrue(model.uiState.value.editing)
            model.dispatch(ProfileIntent.Discard)
            runCurrent()
            assertFalse(model.uiState.value.editing)
            assertEquals(
                "느긋한차슈",
                model.uiState.value.profile
                    ?.nickname,
            )
            assertEquals(ProfileSideEffect.NavigateBack, model.sideEffect.first())
        }

    @Test
    fun `변경 없는 초안에서 돌아가면 마이 탭으로 돌아간다`() =
        coroutinesTest {
            val model = MyProfileViewModel(FakeProfileRepository(), FakeLoginRepository())
            runCurrent()

            model.dispatch(ProfileIntent.Back)
            runCurrent()

            assertEquals(ProfileSideEffect.NavigateBack, model.sideEffect.first())
        }

    @Test
    fun `저장 중 돌아가기는 저장과 편집을 취소하지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { saveResult = CompletableDeferred() }
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            model.dispatch(ProfileIntent.Back)
            runCurrent()
            assertTrue(model.uiState.value.saving)
            assertTrue(model.uiState.value.editing)
            assertFalse(model.uiState.value.confirmDiscard)
            repository.saveResult?.complete(RamapResult.Success(AccountProfile("first", "새로운차슈")))
            runCurrent()
            assertFalse(model.uiState.value.editing)
            assertFalse(model.uiState.value.saving)
        }

    @Test
    fun `계정 전환 후 이전 저장 결과가 새 계정을 덮어쓰지 않는다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    saveResult = CompletableDeferred()
                    ignoreCancellation = true
                }
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            model.dispatch(ProfileIntent.ChangeBio("첫 계정의 자기소개"))
            model.dispatch(ProfileIntent.ChangeInstagram("@first_account"))
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            repository.sessionUserIds.value = "second"
            runCurrent()
            repository.saveResult?.complete(RamapResult.Success(AccountProfile("first", "새로운차슈")))
            runCurrent()
            assertEquals(
                "second",
                model.uiState.value.profile
                    ?.userId,
            )
            assertEquals(
                "느긋한차슈",
                model.uiState.value.profile
                    ?.nickname,
            )
            assertTrue(model.uiState.value.editing)
            assertFalse(model.uiState.value.saving)
            assertNull(model.uiState.value.image)
            assertEquals("", model.uiState.value.bio)
            assertEquals(
                "",
                model.uiState.value.profile
                    ?.bio,
            )
        }

    @Test
    fun `로그아웃과 이전 사진 선택 결과는 초안을 복원하지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            runCurrent()
            val generation = model.uiState.value.draftGeneration
            repository.sessionUserIds.value = null
            runCurrent()
            model.dispatch(ProfileIntent.PickImage(ProfileImage(byteArrayOf(1), "image/jpeg"), generation))
            runCurrent()
            assertNull(model.uiState.value.profile)
            assertNull(model.uiState.value.image)
            assertFalse(model.uiState.value.loading)
            assertFalse(model.uiState.value.editing)
        }

    @Test
    fun `불러오기 실패 후 다시 시도하면 프로필을 표시한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { fetchResult = RamapResult.Error(RamapError.Http(500)) }
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            assertTrue(model.uiState.value.failed)
            repository.fetchResult = null
            model.dispatch(ProfileIntent.Retry)
            runCurrent()
            assertFalse(model.uiState.value.failed)
            assertTrue(model.uiState.value.editing)
            assertEquals(
                "first",
                model.uiState.value.profile
                    ?.userId,
            )
        }
}
