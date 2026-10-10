---
authority: reference
status: active
last_verified: 2026-10-11
source_ref: 33a0d49c2731e42144f2c79704748d7c7e0cf68b
---

# 설계 패턴 탐색

새 구현에 적용할 규칙은 로컬 `.agents/skills/ramap-design-patterns/SKILL.md`, 이름·서식·문자열·테스트 규칙은 `ramap-coding-conventions/SKILL.md`에서 읽습니다. 아래 링크는 패턴의 실제 API와 검증 사례입니다.

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
