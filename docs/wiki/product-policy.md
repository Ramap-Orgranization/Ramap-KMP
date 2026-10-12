---
authority: reference
status: active
last_verified: 2026-10-13
source_ref: c559e52c3248076dacab2f2928622d9d787bc1e5
---

# 제품 정책과 구현 근거

아래는 확인한 현재 소스의 제품 동작입니다. 새 요구사항으로 동작을 바꾸면 사용자 확정 근거, 적용 상태, [결정 ID](decisions.md)를 함께 갱신합니다.

| 영역 | 확인한 동작 | 구현과 검증 근거 |
| --- | --- | --- |
| 지도 검색 | 정규화 후 2~15자 입력을 사용하고 최대 50개를 조회합니다. 새 검색은 이전 검색을 교체합니다. | [MapViewModel](../../feature/main/map/src/commonMain/kotlin/com/peto/ramap/ui/main/map/MapViewModel.kt), [검색·상태 테스트](../../feature/main/map/src/commonTest/kotlin/com/peto/ramap/ui/main/MapViewModelTest.kt) |
| 리뷰 목록의 가게 선택 | 내 리뷰와 다른 사람 리뷰는 같은 가게 리뷰 카드를 사용합니다. 내 리뷰 선택 시 현재 목록 위에 가게 상세 바텀시트를 표시하고 상세의 지도 보기에서 지도 화면으로 이동합니다. 변경 없이 상세를 닫을 때는 목록을 다시 조회하지 않고, 본인 리뷰 삭제 시 해당 항목과 집계를 목록 상태에 반영하며, 리뷰 작성·수정 성공은 공통 변경 기록으로 목록을 갱신하고 단순 복귀는 조회하지 않습니다. [DEC-0006](decisions.md#dec-0006-리뷰-목록의-가게-카드와-상세-표시-통합), [DEC-0007](decisions.md#dec-0007-내-리뷰-상세-닫기에서-목록-재조회-제외), [DEC-0008](decisions.md#dec-0008-리뷰-변경-기록으로-화면-동기화) | [MyReviewsRoute](../../feature/review/my/src/commonMain/kotlin/com/peto/ramap/ui/review/my/MyReviewsRoute.kt), [OtherReviewsRoute](../../feature/review/other/src/commonMain/kotlin/com/peto/ramap/ui/review/other/OtherReviewsRoute.kt), [ShopReviewCard](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/review/ShopReviewCard.kt) |
| 리뷰 입력 | 본문 trim 후 Unicode code point 5~30자와 비속어를 검사하며 이미지는 최대 3개입니다. | [Review](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/review/Review.kt), [ReviewImage](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/review/ReviewImage.kt), [작성 상태 테스트](../../feature/review/write/src/commonTest/kotlin/com/peto/ramap/ui/review/write/ReviewWriteUiStateTest.kt) |
| 리뷰 공개 | 비공개 프로필에서 공개 리뷰를 제출하면 프로필을 비공개로 유지할지 공개로 바꿀지 확인합니다. 저장 실패 시 초안을 유지합니다. | [ReviewWriteViewModel](../../feature/review/write/src/commonMain/kotlin/com/peto/ramap/ui/review/write/ReviewWriteViewModel.kt), [테스트](../../feature/review/write/src/commonTest/kotlin/com/peto/ramap/ui/review/write/ReviewWriteViewModelTest.kt) |
| 다른 사용자 접근 | 프로필 접근 결과에 따라 공개·비공개·차단·이용 불가 상태를 처리합니다. | [ProfileAccess](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/community/ProfileAccess.kt), [OtherReviewsViewModel](../../feature/review/other/src/commonMain/kotlin/com/peto/ramap/ui/review/other/OtherReviewsViewModel.kt) |
| 가져오기 | 이미 저장했거나 숨긴 매장을 후보에서 제외하고 사용자가 남긴 후보를 저장합니다. | [ImportationCandidatesTest](../../domain/src/commonTest/kotlin/com/peto/ramap/domain/model/importation/ImportationCandidatesTest.kt), [ImportationViewModel](../../feature/bookmark/importation/src/commonMain/kotlin/com/peto/ramap/ui/bookmark/importation/ImportationViewModel.kt) |
| 이벤트 상세 | 개별 event ID로 조회합니다. 결과 없음과 요청 실패를 구별해 이용 불가 처리 또는 재시도를 제공합니다. | [EventDetailViewModel](../../feature/main/event/detail/src/commonMain/kotlin/com/peto/ramap/ui/main/event/detail/EventDetailViewModel.kt), [테스트](../../feature/main/event/detail/src/commonTest/kotlin/com/peto/ramap/ui/main/event/detail/EventDetailViewModelTest.kt) |
| 이벤트 알림 설정 창 | KST 전날 21시 전에는 전날·당일, 이후에는 당일만 설정 가능하며 당일 09시부터 닫힙니다. | [EventNotificationWindow](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/notification/EventNotificationWindow.kt), [경계 테스트](../../domain/src/commonTest/kotlin/com/peto/ramap/domain/model/notification/EventNotificationWindowTest.kt) |
| 날짜별 영업 변경 | 휴무가 우선하고, 명시된 일정 override가 있으면 사용하며, 없으면 해당 요일의 정규 영업시간을 참조합니다. | [OperatingNoticeDay](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/notice/OperatingNoticeDay.kt), [MonthlyOperatingNoticeTest](../../domain/src/commonTest/kotlin/com/peto/ramap/domain/model/notice/MonthlyOperatingNoticeTest.kt) |
| 랭킹 | 페이지 간 중복을 제거하고 dense rank를 유지합니다. 북마크 표시 변화는 이미 받은 순서를 재정렬하지 않습니다. | [RankedShopsTest](../../domain/src/commonTest/kotlin/com/peto/ramap/domain/model/rank/RankedShopsTest.kt), [RankingViewModel](../../feature/main/ranking/src/commonMain/kotlin/com/peto/ramap/ui/main/ranking/RankingViewModel.kt) |

이벤트 알림 표는 **설정 가능한 시간 창**을 설명합니다. 실제 발송은 알림 함수와 서버 실행 상태가 담당합니다. 메뉴·매장·이벤트 등록 정책의 원천은 백엔드 문서에 적힌 전용 로컬 스킬과 정책 파일입니다.

2026-10-12 작업 트리 확인에 리뷰 목록의 공통 카드와 가게 상세 선택 경로를 추가했습니다. 내 리뷰의 가게 정보는 페이지의 가게 ID를 중복 제거해 일괄 조회하며, 조회 실패나 누락은 리뷰 목록 실패로 처리하지 않습니다. 근거: [DefaultReviewRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultReviewRepository.kt), [회귀 테스트](../../data/src/commonTest/kotlin/com/peto/ramap/data/repository/DefaultReviewRepositoryTest.kt).

리뷰 변경 전파는 사용자 승인에 따라 공통 기록 방식으로 통합합니다. 현재 필터·조회 범위 밖의 삭제도 서버가 반환한 작성자 전체·공개·비공개 개수에 반영하며, 현재 페이지와 스크롤 키를 유지합니다. 공개·게시 여부가 변할 때 같은 작성자의 이후 방문 번호를 조정하고 본문·이미지 수정만으로 방문 번호를 바꾸지 않습니다. 다른 사용자의 목록에 소유자용 비공개 결과를 삽입하지 않습니다. [ReviewChangeReducer](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/review/change/ReviewChangeReducer.kt), [내 리뷰 ViewModel](../../feature/review/my/src/commonMain/kotlin/com/peto/ramap/ui/review/my/MyReviewsViewModel.kt), [DEC-0008](decisions.md#dec-0008-리뷰-변경-기록으로-화면-동기화)의 앱 코드는 검증된 PR 코드 커밋 `c559e52c3248076dacab2f2928622d9d787bc1e5`에 반영됐습니다. v2 RPC 서버 변경은 2026-10-13에 운영에 배포됐으며 앱 스토어 배포는 아닙니다.
