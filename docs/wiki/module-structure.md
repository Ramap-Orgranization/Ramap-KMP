---
authority: reference
status: active
last_verified: 2026-10-11
source_ref: 33a0d49c2731e42144f2c79704748d7c7e0cf68b
---

# 모듈 구조

포함된 Gradle 모듈의 원천은 [settings.gradle.kts](../../settings.gradle.kts)입니다. 파일 배치 규칙은 `.agents/skills/ramap-module-structure/SKILL.md`에서 확인합니다.

작업 트리 반영 범위는 `debug/admin`의 device test 설정과 공용 테스트 지원 소스 세트입니다. 포함 모듈 목록은 기준 커밋과 같습니다.

| 그룹 | 코드와 설정 |
| --- | --- |
| 진입점과 앱 조립 | [androidApp](../../androidApp), [iosApp](../../iosApp), [shared](../../shared) |
| 도메인·데이터 | [domain](../../domain), [data](../../data) |
| 공통 기반 | [core](../../core): common, analytics, ui, preview, designsystem, network, platform, notification, navigation, testing |
| 메인 탭과 하위 화면 | [feature/main](../../feature/main): map, ranking, event/list, event/detail, notice, my |
| 계정·프로필·리뷰 | [account](../../feature/account), [profile/edit](../../feature/profile/edit), [review](../../feature/review) |
| 저장·설정·운영 사용자 기능 | [bookmark](../../feature/bookmark), [hidden](../../feature/hidden), [notification](../../feature/notification), [report](../../feature/report), [settings](../../feature/settings), [subscribed](../../feature/subscribed) |
| 디버그 도구 | [debug/admin](../../debug/admin) |

`iosApp`은 Xcode 프로젝트이며 settings에 포함되는 Gradle 모듈이 아닙니다. `feature/main`은 메인 탭 모듈을 묶는 기존 집계 지점입니다. 프로필 기능의 포함 모듈은 `feature/profile/edit`이며 소유 스킬의 목록도 현재 settings와 일치합니다([DEC-0002](decisions.md#dec-0002-현재-구현을-기준으로-개발-규칙-정합성-확정)).

## 소스 세트와 빌드 근거

- 공통 코드와 테스트: 각 모듈의 `src/commonMain`, `src/commonTest`.
- 플랫폼 구현: `src/androidMain`, `src/iosMain`. Android 앱 자체는 `androidApp/src/main`.
- Android 호스트 테스트: `src/androidHostTest`. iOS 전용 테스트는 `src/iosTest`.
- 공통 설정: [convention plugin](../../build-logic/convention/src/main/kotlin/com/peto/ramap/convention), [버전 카탈로그](../../gradle/libs.versions.toml), [루트 build](../../build.gradle.kts).
- Compose 리소스: [디자인시스템](../../core/designsystem/src/commonMain/composeResources), [shared 빌드와 리소스 조립](../../shared/build.gradle.kts).
- 테스트 태스크 선택: [검증 안내](testing.md).

모듈 목록을 바꿀 때는 이 표만 고치지 않고 settings, 소비자 build 파일, DI 조립과 소유 스킬을 함께 확인합니다.
