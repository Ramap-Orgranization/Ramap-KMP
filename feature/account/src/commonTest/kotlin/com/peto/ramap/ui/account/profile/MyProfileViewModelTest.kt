package com.peto.ramap.ui.account.profile

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyProfileViewModelTest {
    @Test
    fun `인스타그램 링크를 아이디로 저장하고 같은 계정 표현은 변경으로 보지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.Edit)
            model.dispatch(ProfileIntent.ChangeInstagram("https://www.instagram.com/Ramap_Official/?igsh=shared"))
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertEquals(
                "ramap_official",
                model.uiState.value.profile
                    ?.instagramUsername,
            )
            model.dispatch(ProfileIntent.Edit)
            runCurrent()
            assertEquals("ramap_official", model.uiState.value.instagram)
            model.dispatch(ProfileIntent.ChangeInstagram("@RAMAP_OFFICIAL"))
            runCurrent()
            assertFalse(model.uiState.value.changed)
            assertFalse(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.ChangeInstagram(""))
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertEquals(
                "",
                model.uiState.value.profile
                    ?.instagramUsername,
            )
            assertEquals(2, repository.saveCalls)
        }

    @Test
    fun `잘못된 인스타그램 주소는 저장하지 않고 취소한 초안을 버린다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.Edit)
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
            model.dispatch(ProfileIntent.Edit)
            runCurrent()
            assertEquals("", model.uiState.value.instagram)
            assertFalse(model.uiState.value.instagramInvalid)
        }

    @Test
    fun `자기소개만 바꾸어 저장하고 다시 편집하거나 비울 수 있다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.Edit)
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
            model.dispatch(ProfileIntent.Edit)
            runCurrent()
            assertEquals("담백한 라멘을 좋아해요 🍜", model.uiState.value.bio)
            assertFalse(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.ChangeBio(""))
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertEquals(
                "",
                model.uiState.value.profile
                    ?.bio,
            )
            assertEquals(2, repository.saveCalls)
        }

    @Test
    fun `긴 자기소개나 줄바꿈은 저장하지 않고 버린 초안은 복원하지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.Edit)
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
            model.dispatch(ProfileIntent.Edit)
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
            model.dispatch(ProfileIntent.Edit)
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertFalse(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.ChangeNickname("!"))
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertTrue(model.uiState.value.nicknameInvalid)
            assertEquals(0, repository.saveCalls)
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
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
    fun `변경한 초안에서 돌아가면 확인 후 취소한다`() =
        coroutinesTest {
            val model = MyProfileViewModel(FakeProfileRepository(), FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.Edit)
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
        }

    @Test
    fun `저장 중 돌아가기는 저장과 편집을 취소하지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { saveResult = CompletableDeferred() }
            val model = MyProfileViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.Edit)
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
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
            model.dispatch(ProfileIntent.Edit)
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
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
            assertFalse(model.uiState.value.editing)
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
            model.dispatch(ProfileIntent.Edit)
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
            assertEquals(
                "first",
                model.uiState.value.profile
                    ?.userId,
            )
        }
}
