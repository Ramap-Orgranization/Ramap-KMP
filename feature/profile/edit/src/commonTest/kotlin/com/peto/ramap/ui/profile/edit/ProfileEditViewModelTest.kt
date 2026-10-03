package com.peto.ramap.ui.profile.edit

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.profile.ProfileImage
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_bio_daily_change_limit_reached
import ramap.shared.generated.resources.profile_nickname_check_rate_limited
import ramap.shared.generated.resources.profile_nickname_daily_change_limit_reached
import ramap.shared.generated.resources.profile_save_failed
import ramap.shared.generated.resources.profile_save_rate_limited
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {
    @Test
    fun `비속어 닉네임의 중복 확인과 저장을 막고 수정 후 저장한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("씨_발"))
            model.dispatch(ProfileIntent.CheckNickname)
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertTrue(model.uiState.value.nicknameContainsProfanity)
            assertFalse(model.uiState.value.canCheckNickname)
            assertFalse(model.uiState.value.canSave)
            assertEquals(0, repository.nicknameCheckCalls)
            assertEquals(0, repository.saveCalls)

            model.dispatch(ProfileIntent.ChangeNickname("정상라멘러"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            assertFalse(model.uiState.value.nicknameContainsProfanity)
            assertTrue(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            assertEquals(1, repository.nicknameCheckCalls)
            assertEquals(1, repository.saveCalls)
        }

    @Test
    fun `프로필 조회 중 로그아웃하면 조회 로딩을 종료한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { fetchPending = CompletableDeferred() }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            assertTrue(model.uiState.value.loading)

            repository.sessionUserIds.value = null
            runCurrent()
            assertFalse(model.uiState.value.loading)
        }

    @Test
    fun `자기소개만 바꾸어 저장한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository()
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
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
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            for (invalid in listOf("가".repeat(31), "라멘\n좋아요")) {
                model.dispatch(ProfileIntent.ChangeBio(invalid))
                model.dispatch(ProfileIntent.Save)
                runCurrent()
                assertTrue(model.uiState.value.bioInvalid)
                assertFalse(model.uiState.value.canSave)
            }
            assertEquals(0, repository.saveCalls)
            model.dispatch(ProfileIntent.Back)
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
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
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
    fun `확인한 닉네임은 결과와 관계없이 다시 중복 확인하지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { nicknameAvailable = false }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            runCurrent()
            assertTrue(model.uiState.value.canCheckNickname)
            assertFalse(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            assertFalse(model.uiState.value.canSave)
            assertEquals(false, model.uiState.value.nicknameAvailable)
            assertFalse(model.uiState.value.canCheckNickname)
            repository.nicknameAvailable = true
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            assertEquals(1, repository.nicknameCheckCalls)
            assertFalse(model.uiState.value.canSave)
            model.dispatch(ProfileIntent.ChangeNickname("다른차슈"))
            runCurrent()
            assertFalse(model.uiState.value.canSave)
            assertNull(model.uiState.value.nicknameAvailable)
            assertTrue(model.uiState.value.canCheckNickname)
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            assertEquals(2, repository.nicknameCheckCalls)
            assertTrue(model.uiState.value.canSave)
        }

    @Test
    fun `확인 완료한 닉네임을 같은 공백 정리 값으로 바꾸어도 결과를 유지한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { nicknameAvailable = true }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()

            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("  새로운차슈  "))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()

            assertEquals(true, model.uiState.value.nicknameAvailable)
            assertTrue(model.uiState.value.canSave)
            assertFalse(model.uiState.value.canCheckNickname)
            assertEquals(1, repository.nicknameCheckCalls)
        }

    @Test
    fun `실패한 닉네임 중복 확인은 다시 시도할 수 있다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    nicknameCheckResult = CompletableDeferred()
                }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()

            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            repository.nicknameCheckResult?.complete(RamapResult.Error(RamapError.Http(500)))
            runCurrent()

            assertTrue(model.uiState.value.nicknameCheckFailed)
            assertTrue(model.uiState.value.canCheckNickname)
            repository.nicknameCheckResult = null
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()

            assertEquals(2, repository.nicknameCheckCalls)
            assertEquals(true, model.uiState.value.nicknameAvailable)
            assertFalse(model.uiState.value.canCheckNickname)
        }

    @Test
    fun `중복 확인 응답 뒤에 닉네임을 바꾸면 오래된 결과를 무시한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { nicknameCheckResult = CompletableDeferred() }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
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
    fun `닉네임 상태 메시지 표시 조건은 유효하고 변경되었으며 확인 결과가 존재할 때 true이다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { nicknameAvailable = true }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            assertFalse(model.uiState.value.showNicknameStatus)

            model.dispatch(ProfileIntent.ChangeNickname("!"))
            runCurrent()
            assertFalse(model.uiState.value.showNicknameStatus)

            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            runCurrent()
            assertFalse(model.uiState.value.showNicknameStatus)

            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            assertTrue(model.uiState.value.showNicknameStatus)
        }

    @Test
    fun `변경한 초안에서 돌아가면 마이 탭으로 돌아간다`() =
        coroutinesTest {
            val model = ProfileEditViewModel(FakeProfileRepository(), FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.Back)
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
            val model = ProfileEditViewModel(FakeProfileRepository(), FakeLoginRepository())
            runCurrent()

            model.dispatch(ProfileIntent.Back)
            runCurrent()

            assertEquals(ProfileSideEffect.NavigateBack, model.sideEffect.first())
        }

    @Test
    fun `저장 중 돌아가기는 저장과 편집을 취소하지 않는다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { saveResult = CompletableDeferred() }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
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
            repository.saveResult?.complete(RamapResult.Success(AccountProfile("first", "새로운차슈")))
            runCurrent()
            assertFalse(model.uiState.value.editing)
            assertFalse(model.uiState.value.saving)
        }

    @Test
    fun `일일 변경 제한 거절 뒤에도 편집 초안과 닉네임 확인 결과를 유지한다`() =
        coroutinesTest {
            val repository = FakeProfileRepository().apply { saveResult = CompletableDeferred() }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            model.dispatch(ProfileIntent.ChangeBio("저장하려던 자기소개"))
            runCurrent()
            val image = ProfileImage(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), "image/jpeg")
            model.dispatch(ProfileIntent.PickImage(image))
            model.dispatch(ProfileIntent.Save)
            runCurrent()
            repository.saveResult?.complete(
                RamapResult.Error(
                    RamapError.Http(429, IllegalStateException("opaque SDK message"), serverMessage = "PROFILE_NICKNAME_DAILY_LIMIT"),
                ),
            )
            runCurrent()

            assertEquals("새로운차슈", model.uiState.value.nickname)
            assertEquals("저장하려던 자기소개", model.uiState.value.bio)
            assertEquals(image, model.uiState.value.image)
            assertEquals("새로운차슈", model.uiState.value.checkedNickname)
            assertEquals(
                "느긋한차슈",
                model.uiState.value.profile
                    ?.nickname,
            )
            assertTrue(model.uiState.value.editing)
            assertEquals(
                ProfileSideEffect.ShowToast(Res.string.profile_nickname_daily_change_limit_reached, ToastType.ERROR),
                model.sideEffect.first(),
            )
        }

    @Test
    fun `자기소개 일일 변경 제한은 자기소개 안내를 표시한다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    saveResult =
                        CompletableDeferred(
                            RamapResult.Error(
                                RamapError.Http(
                                    429,
                                    serverMessage = "PROFILE_BIO_DAILY_LIMIT",
                                ),
                            ),
                        )
                }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeBio("저장하려던 소개"))
            model.dispatch(ProfileIntent.Save)
            runCurrent()

            assertEquals(
                ProfileSideEffect.ShowToast(Res.string.profile_bio_daily_change_limit_reached, ToastType.ERROR),
                model.sideEffect.first(),
            )
        }

    @Test
    fun `닉네임 중복 저장 오류는 재확인 요청 없이 사용 불가로 표시한다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    saveResult =
                        CompletableDeferred(
                            RamapResult.Error(
                                RamapError.Http(
                                    409,
                                    serverMessage = "PROFILE_NICKNAME_TAKEN",
                                ),
                            ),
                        )
                }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            model.dispatch(ProfileIntent.Save)
            runCurrent()

            assertEquals(false, model.uiState.value.nicknameAvailable)
            assertEquals("새로운차슈", model.uiState.value.checkedNickname)
            assertEquals(1, repository.nicknameCheckCalls)
        }

    @Test
    fun `사진 삭제 의도는 기존 프로필 사진 제거로 저장된다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    fetchResult = RamapResult.Success(AccountProfile("first", "느긋한차슈", avatarUrl = "signed:first/old.jpg"))
                }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.RemovePhoto)
            runCurrent()

            assertTrue(model.uiState.value.removePhoto)
            assertTrue(model.uiState.value.canSave)
        }

    @Test
    fun `닉네임 확인 요청 제한은 초안을 유지하고 다시 확인할 수 있게 한다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    nicknameCheckResult =
                        CompletableDeferred(
                            RamapResult.Error(RamapError.Http(429, IllegalStateException("PROFILE_NICKNAME_CHECK_RATE_LIMIT"))),
                        )
                }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()

            assertEquals("새로운차슈", model.uiState.value.nickname)
            assertTrue(model.uiState.value.canCheckNickname)
            assertFalse(model.uiState.value.nicknameCheckFailed)
            assertEquals(1, repository.nicknameCheckCalls)
            assertEquals(
                ProfileSideEffect.ShowToast(Res.string.profile_nickname_check_rate_limited, ToastType.ERROR),
                model.sideEffect.first(),
            )
        }

    @Test
    fun `프로필 저장 요청 제한은 일일 변경 제한과 구분하고 초안을 보존한다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    saveResult =
                        CompletableDeferred(
                            RamapResult.Error(RamapError.Http(429, IllegalStateException("opaque SDK message"), serverMessage = "PROFILE_SAVE_RATE_LIMIT")),
                        )
                }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeBio("저장하려던 소개"))
            model.dispatch(ProfileIntent.Save)
            runCurrent()

            assertEquals("저장하려던 소개", model.uiState.value.bio)
            assertTrue(model.uiState.value.canSave)
            assertEquals(1, repository.saveCalls)
            assertEquals(
                ProfileSideEffect.ShowToast(Res.string.profile_save_rate_limited, ToastType.ERROR),
                model.sideEffect.first(),
            )
        }

    @Test
    fun `서버 오류 ID가 없으면 예외 문구에 제한 코드가 있어도 일반 저장 실패로 처리한다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    saveResult =
                        CompletableDeferred(
                            RamapResult.Error(RamapError.Http(429, IllegalStateException("PROFILE_SAVE_RATE_LIMIT"))),
                        )
                }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeBio("저장하려던 소개"))
            model.dispatch(ProfileIntent.Save)
            runCurrent()

            assertEquals(
                ProfileSideEffect.ShowToast(Res.string.profile_save_failed, ToastType.ERROR),
                model.sideEffect.first(),
            )
        }

    @Test
    fun `계정 전환 후 이전 저장 결과가 새 계정을 덮어쓰지 않는다`() =
        coroutinesTest {
            val repository =
                FakeProfileRepository().apply {
                    saveResult = CompletableDeferred()
                    ignoreCancellation = true
                }
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            model.dispatch(ProfileIntent.ChangeNickname("새로운차슈"))
            model.dispatch(ProfileIntent.CheckNickname)
            runCurrent()
            model.dispatch(ProfileIntent.ChangeBio("첫 계정의 자기소개"))
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
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
            runCurrent()
            runCurrent()
            repository.sessionUserIds.value = null
            runCurrent()
            model.dispatch(ProfileIntent.PickImage(ProfileImage(byteArrayOf(1), "image/jpeg")))
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
            val model = ProfileEditViewModel(repository, FakeLoginRepository())
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
