---
authority: reference
status: active
last_verified: 2026-10-13
source_ref: afe2511050f20a60dfdefa369151ec456d2b6c1c
---

# 설계 패턴 탐색

새 구현에 적용할 규칙은 `.agents/skills/ramap-design-patterns/SKILL.md`, 이름·서식·문자열·테스트 규칙은 `ramap-coding-conventions/SKILL.md`에서 읽습니다. 아래 링크는 패턴의 실제 API와 검증 사례입니다.

| 찾는 동작 | 구현 | 검증 |
| --- | --- | --- |
| Intent 처리, 상태 갱신, 일회성 효과와 작업 수명 | [BaseViewModel](../../core/ui/src/commonMain/kotlin/com/peto/ramap/ui/base/BaseViewModel.kt) | [BaseViewModelTaskTest](../../core/ui/src/commonTest/kotlin/com/peto/ramap/ui/base/BaseViewModelTaskTest.kt) |
| 같은 키의 작업 교체·중복 무시 | [TaskPolicy](../../core/ui/src/commonMain/kotlin/com/peto/ramap/ui/task/TaskPolicy.kt) | [결과·재시도 테스트](../../core/ui/src/commonTest/kotlin/com/peto/ramap/ui/base/BaseViewModelResultTaskTest.kt) |
| 키별 로딩 표현 | [LoadableState](../../core/ui/src/commonMain/kotlin/com/peto/ramap/ui/loading/LoadableState.kt), [LoadKey](../../core/ui/src/commonMain/kotlin/com/peto/ramap/ui/loading/LoadKey.kt) | [LoadStateTest](../../core/ui/src/commonTest/kotlin/com/peto/ramap/ui/loading/LoadStateTest.kt) |
| 네트워크 성공·실패 변환 | [InvokeRequest](../../core/network/src/commonMain/kotlin/com/peto/ramap/network/execute/InvokeRequest.kt) | [RamapResultCallTest](../../core/network/src/commonTest/kotlin/com/peto/ramap/core/result/RamapResultCallTest.kt) |
| 공유 개인화 상태 | [PersonalizationStore](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/store/PersonalizationStore.kt), [구현](../../data/src/commonMain/kotlin/com/peto/ramap/data/store/DefaultPersonalizationStore.kt) | [DefaultPersonalizationStoreTest](../../data/src/commonTest/kotlin/com/peto/ramap/data/store/DefaultPersonalizationStoreTest.kt) |
| 화면의 Route·ViewModel·contract 연결 | [지도 화면](../../feature/main/map/src/commonMain/kotlin/com/peto/ramap/ui/main/map) | [MapViewModelTest](../../feature/main/map/src/commonTest/kotlin/com/peto/ramap/ui/main/MapViewModelTest.kt) |

지도 ViewModel은 `dispatch → handleIntent → reduce/postSideEffect` 흐름을 사용합니다. 비동기 작업의 교체·중복 처리와 로딩은 공통 API에 연결되어 있습니다. 작업 취소와 늦게 도착한 결과를 다루는 방법은 호출부와 테스트를 같이 읽어야 합니다.

Koin 등록 위치는 [앱 조립](architecture.md), typed route와 탭 상태는 [실행 흐름](app-flows.md), 공통 컴포넌트와 mapper의 현재 위치는 [디자인시스템](design-system.md)에 연결했습니다. 문서에 짧은 코드 예제를 복제하는 대신 실제 API를 기준으로 사용합니다.

## 리뷰 변경 기록과 화면 구독

이번 변경에서 확인한 범위는 리뷰 변경 전파입니다. [ReviewChangeStore](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/store/ReviewChangeStore.kt)의 읽기 계약과 [DefaultReviewChangeStore](../../data/src/commonMain/kotlin/com/peto/ramap/data/store/DefaultReviewChangeStore.kt)의 단일 인스턴스는 세션 epoch, revision, 최대 128개 변경 기록을 유지합니다. StateFlow가 중간 방출을 합쳐도 화면 cursor 이후의 기록을 차례로 적용합니다. 보관 범위를 넘으면 해당 화면은 명시적 복구 조회를 수행합니다.

[ReviewChangeReducer](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/review/change/ReviewChangeReducer.kt)는 before/after를 이용해 현재 카드, 작성자 공개·게시 리뷰 수, 같은 매장의 이후 방문 번호와 정렬된 조회 범위를 반영합니다. 쓰기 결과와 이미지 hydration은 다른 변경 타입이므로 URL 준비가 개수와 방문 번호를 다시 갱신하지 않습니다. [DefaultReviewRepository](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/DefaultReviewRepository.kt)는 쓰기를 직렬화하고 저장 결과를 이미지 서명·삭제 정리 전에 발행합니다. 후속 실패는 저장된 리뷰를 실패로 보고하지 않습니다.

Reducer는 일반 클래스이며 [useCaseModule](../../data/src/commonMain/kotlin/com/peto/ramap/data/usecase/di/UseCaseModule.kt)의 `singleOf` 등록으로 수명을 관리합니다. 매장 상세 use case와 Map·OtherReviews·MyReviews ViewModel은 생성자로 같은 반영기를 주입받습니다. 목록 변경, 작성자 집계, 조회 범위 판단, 이미지·아바타 보존을 각각의 함수에서 처리하고 내 리뷰 변환은 `MyReview.toReview()`를 재사용합니다. 클래스·주입에 관한 팀 규칙의 소유 위치는 [코딩 규칙](../../.agents/skills/ramap-coding-conventions/SKILL.md#kotlin-style)이며 변경 이유는 [DEC-0011](decisions.md#dec-0011-비즈니스-규칙의-일반-클래스와-생성자-주입)에 기록합니다.

화면은 조회를 시작할 때 epoch, revision과 요청 세대를 기록합니다. 진행 중인 관련 조회와 변경이 겹치면 그 조회를 다시 시작하고, 정상 변경은 보유 중인 페이지에 적용합니다. 프로필·팔로우·차단 변경은 해당 작성자의 접근을 재확인하는 별도 변경으로 발행합니다. 근거: [순수 reducer 테스트](../../domain/src/commonTest/kotlin/com/peto/ramap/domain/model/review/ReviewChangeReducerTest.kt), [journal 테스트](../../data/src/commonTest/kotlin/com/peto/ramap/data/store/DefaultReviewChangeStoreTest.kt), [내 리뷰 테스트](../../feature/review/my/src/commonTest/kotlin/com/peto/ramap/ui/review/my/MyReviewsViewModelTest.kt).
