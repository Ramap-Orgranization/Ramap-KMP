---
authority: reference
status: active
last_verified: 2026-10-11
source_ref: 33a0d49c2731e42144f2c79704748d7c7e0cf68b
---

# 기능 지도

화면에서 domain 계약과 data 구현으로 이동하는 탐색표입니다. 정확한 route 인자와 앱 연결은 [ScreenRoutes](../../core/navigation/src/commonMain/kotlin/com/peto/ramap/navigation/ScreenRoutes.kt), [NavigationGraph](../../shared/src/commonMain/kotlin/com/peto/ramap/NavigationGraph.kt)에 있습니다.


| 기능 | 화면 진입과 상태 | 계약과 데이터 구현 |
| --- | --- | --- |
| 계정·로그인 | [AccountViewModel](../../feature/account/src/commonMain/kotlin/com/peto/ramap/ui/account/AccountViewModel.kt) | [LoginRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/LoginRepository.kt) → [DefaultLoginRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultLoginRepository.kt) |
| 마이·프로필 편집 | [MyTabViewModel](../../feature/main/my/src/commonMain/kotlin/com/peto/ramap/ui/main/my/MyTabViewModel.kt), [ProfileEditViewModel](../../feature/profile/edit/src/commonMain/kotlin/com/peto/ramap/ui/profile/edit/ProfileEditViewModel.kt) | [ProfileRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/ProfileRepository.kt) → [DefaultProfileRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultProfileRepository.kt) |
| 리뷰 작성·수정·내 리뷰 | [ReviewWriteViewModel](../../feature/review/write/src/commonMain/kotlin/com/peto/ramap/ui/review/write/ReviewWriteViewModel.kt), [MyReviewsViewModel](../../feature/review/my/src/commonMain/kotlin/com/peto/ramap/ui/review/my/MyReviewsViewModel.kt) | [ReviewRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/ReviewRepository.kt) → [DefaultReviewRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultReviewRepository.kt) |
| 다른 사용자 프로필·리뷰·차단 | [OtherReviewsViewModel](../../feature/review/other/src/commonMain/kotlin/com/peto/ramap/ui/review/other/OtherReviewsViewModel.kt) | [CommunityRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/CommunityRepository.kt) → [DefaultCommunityRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultCommunityRepository.kt) |
| 북마크 목록·가져오기 | [BookmarkedShopListViewModel](../../feature/bookmark/list/src/commonMain/kotlin/com/peto/ramap/ui/bookmark/list/BookmarkedShopListViewModel.kt), [ImportationViewModel](../../feature/bookmark/importation/src/commonMain/kotlin/com/peto/ramap/ui/bookmark/importation/ImportationViewModel.kt) | [BookmarkRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/BookmarkRepository.kt) → [DefaultBookmarkRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultBookmarkRepository.kt); [ImportationRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/ImportationRepository.kt) → [DefaultImportationRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultImportationRepository.kt) |
| 이벤트 목록·상세 | [EventsViewModel](../../feature/main/event/list/src/commonMain/kotlin/com/peto/ramap/ui/main/event/list/EventsViewModel.kt), [EventDetailViewModel](../../feature/main/event/detail/src/commonMain/kotlin/com/peto/ramap/ui/main/event/detail/EventDetailViewModel.kt) | [RamenShopRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/RamenShopRepository.kt) → [DefaultRamenShopRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultRamenShopRepository.kt) |
| 영업 공지 | [OperatingNoticeViewModel](../../feature/main/notice/src/commonMain/kotlin/com/peto/ramap/ui/main/notice/OperatingNoticeViewModel.kt) | [OperatingNoticeRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/OperatingNoticeRepository.kt) → [DefaultOperatingNoticeRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultOperatingNoticeRepository.kt) |
| 랭킹 | [RankingViewModel](../../feature/main/ranking/src/commonMain/kotlin/com/peto/ramap/ui/main/ranking/RankingViewModel.kt) | [ShopRankingRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/ShopRankingRepository.kt) → [DefaultShopRankingRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultShopRankingRepository.kt) |
| 알림 설정·푸시 | [NotificationSettingsViewModel](../../feature/notification/src/commonMain/kotlin/com/peto/ramap/ui/notification/NotificationSettingsViewModel.kt) | [NotificationSettingsRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/NotificationSettingsRepository.kt), [PushRegistrationRepository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository/PushRegistrationRepository.kt) → [DefaultNotificationSettingsRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultNotificationSettingsRepository.kt), [DefaultPushRegistrationRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultPushRegistrationRepository.kt) |

지도·검색은 [앱 실행 흐름](app-flows.md)을 읽습니다. 숨김·구독·신고·설정의 모듈 위치는 [모듈 구조](module-structure.md)에 있습니다.


## 기능 사이를 연결하는 상태

[PersonalizationStore](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/store/PersonalizationStore.kt)는 북마크·숨김 등 개인화의 공유 진입점입니다. 북마크 목록과 지도 등은 이 상태를 구독합니다. 프로필 관련 화면은 `ProfileRepository.sessionUserIds` 전환 때 진행 작업과 화면 상태를 초기화하며 이전 계정의 늦은 결과가 새 계정에 반영되지 않도록 확인합니다.

리뷰와 차단 변경은 [ReviewChangeNotifier](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/ReviewChangeNotifier.kt)를 거쳐 관련 화면의 재조회로 이어집니다. 이벤트 알림 설정과 OS 알림 권한은 별개의 상태입니다. 푸시 등록 수명은 [NotificationRegistry](../../core/notification/src/commonMain/kotlin/com/peto/ramap/notification/NotificationRegistry.kt)에서 확인합니다.


## 대표 검증 경로

| 경계 | 테스트 |
| --- | --- |
| 로그인 초기화·세션 상태 | [LoginSessionStateTest](../../data/src/commonTest/kotlin/com/peto/ramap/data/repository/LoginSessionStateTest.kt) |
| 프로필 편집·계정 전환 | [ProfileEditViewModelTest](../../feature/profile/edit/src/commonTest/kotlin/com/peto/ramap/ui/profile/edit/ProfileEditViewModelTest.kt) |
| 리뷰 입력·저장·취소 | [ReviewWriteViewModelTest](../../feature/review/write/src/commonTest/kotlin/com/peto/ramap/ui/review/write/ReviewWriteViewModelTest.kt) |
| 개인화·가져오기 | [DefaultPersonalizationStoreTest](../../data/src/commonTest/kotlin/com/peto/ramap/data/store/DefaultPersonalizationStoreTest.kt), [ImportationViewModelTest](../../feature/bookmark/importation/src/commonTest/kotlin/com/peto/ramap/ui/bookmark/importation/ImportationViewModelTest.kt) |
| 이벤트 상세·알림 권한 | [EventDetailViewModelTest](../../feature/main/event/detail/src/commonTest/kotlin/com/peto/ramap/ui/main/event/detail/EventDetailViewModelTest.kt) |
| 랭킹 페이지·순위 | [RankingViewModelTest](../../feature/main/ranking/src/commonTest/kotlin/com/peto/ramap/ui/main/ranking/RankingViewModelTest.kt), [RankedShopsTest](../../domain/src/commonTest/kotlin/com/peto/ramap/domain/model/rank/RankedShopsTest.kt) |
| 푸시 세션 등록 | [NotificationRegistryTest](../../core/notification/src/commonTest/kotlin/com/peto/ramap/notification/NotificationRegistryTest.kt) |

테스트 실행 명령은 [검증 안내](testing.md)에 있습니다. 링크된 파일이 바뀌면 페이지의 기준 ref 이후 diff도 확인합니다.
