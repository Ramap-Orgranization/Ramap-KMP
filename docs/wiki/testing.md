---
authority: reference
status: active
last_verified: 2026-10-11
source_ref: 009f67d2924a285353213d1aa91ade5406e87324 + working-tree
---

# 검증 안내

이 문서는 실행할 검사를 고르는 안내입니다. 테스트 파일의 존재나 아래 명령 목록은 테스트가 이미 통과했다는 뜻이 아닙니다. 검증 결과에는 실행 명령·결과·환경 제약을 남깁니다.

테스트 이름·설명·검증 메시지의 작성 기준은 [코딩 규칙](../../.agents/skills/ramap-coding-conventions/SKILL.md#text-tests-and-safety)이 소유합니다. 한국어 작성 기준의 확정 이유는 [DEC-0012](decisions.md#dec-0012-한국어-테스트-이름과-설명)에서 확인합니다.

## 변경 범위별 명령

| 변경 범위 | 시작할 검사 |
| --- | --- |
| 위키 문서 | `python3 scripts/check_wiki.py`, `git diff --check` |
| 위키 검사기 | `python3 -m unittest discover -s scripts/tests -v` 및 위키 검사 |
| 모듈의 공통 로직·ViewModel | `./gradlew :<module>:testAndroidHostTest` |
| 지도 흐름 | `./gradlew :feature:main:map:testAndroidHostTest` |
| 도메인·데이터 경계 | `./gradlew :domain:testAndroidHostTest :data:testAndroidHostTest` |
| 관리자 모델·상태 | `./gradlew :debug:admin:testAndroidHostTest` |
| 관리자 화면 조작·연속 날짜 등록 | `./gradlew :debug:admin:connectedAndroidDeviceTest` — [기기 테스트](../../debug/admin/src/androidDeviceTest/kotlin/com/peto/ramap/debug/admin/AdminRegistrationFlowTest.kt)는 테스트 데이터로 실행하며 운영 데이터에 쓰지 않습니다. |
| 관리자 등록 API·날짜 검증 | `node --test supabase/functions/register-event/handler.test.mjs supabase/functions/_shared/operating-notice-schedule.test.mjs` |
| 영업 상태 worker | `npm --prefix supabase/workers/naver-operating-status test` |
| 앱 공통 조립·내비게이션 | `./gradlew :shared:testAndroidHostTest :core:navigation:testAndroidHostTest` |
| Android 앱 연결 | `./gradlew :androidApp:assembleDebug` |
| iOS 공통 조립·Compose 리소스 | `./gradlew :shared:iosSimulatorArm64Test` |
| iOS framework 연결 | `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` |
| 모든 KMP Android 호스트 테스트 | `./gradlew testAndroidHostTest` |
| 모든 iOS 시뮬레이터 테스트 | `./gradlew iosSimulatorArm64Test` |
| Kotlin 서식 | `./gradlew ktlintCheck` 또는 해당 모듈의 `ktlintCheck` |

iOS 앱 실행은 `iosApp`을 Xcode에서 엽니다. 플랫폼별 빌드는 해당 SDK·설정 파일이 필요합니다. 로컬에 구성된 비밀 값을 문서나 로그로 출력하지 않습니다.

## CI와 테스트 기반

[Android CI](../../.github/workflows/ci.yml)는 JDK 17에서 ktlint, 전체 Android 호스트 테스트, debug APK와 release bundle 빌드를 실행합니다. iOS 검증은 이 workflow에 포함되어 있지 않습니다. [위키 CI](../../.github/workflows/wiki.yml)는 Python만 사용하며 Android 빌드와 독립된 job입니다.

공통 테스트 의존성과 Android host target 설정은 [KmpTestConventionPlugin](../../build-logic/convention/src/main/kotlin/com/peto/ramap/convention/KmpTestConventionPlugin.kt), 공용 fake·fixture는 [core/testing](../../core/testing)에서 확인합니다. 데이터 소유 fake는 [data/commonTest](../../data/src/commonTest)에 있습니다.

코드 변경에는 소유 스킬의 검증 기준을 적용합니다. 문서와 검사기만 바뀐 작업에는 앱 빌드를 문서 검증으로 대신하지 않습니다. 링크 검사 성공도 제품 의미나 코드 동작을 검증하지 않으므로 근거를 읽는 검토가 필요합니다.
