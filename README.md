# Ramap KMP

Ramap은 Android와 iOS를 함께 지원하는 Kotlin Multiplatform 앱입니다. 공통 UI는 Compose Multiplatform으로 작성하고, `shared`가 각 기능과 인프라 모듈을 조립해 플랫폼 앱에 제공합니다.

## 모듈 구조

| 모듈 | 책임 |
| --- | --- |
| `:androidApp` | Android 애플리케이션 진입점·리소스와 로그인/푸시 알림 플랫폼 연결 |
| `:shared` | 앱 진입 UI, 루트 내비게이션, Koin 조립, 플랫폼 연결부 |
| `:core:common` | 공통 결과 타입과 범용 확장 |
| `:core:ui` | 공통 ViewModel 계약, UI 상태 저장소, 도메인 결합 공용 UI |
| `:core:designsystem` | 도메인을 모르는 테마·시각 컴포넌트와 Compose 리소스 |
| `:core:network` | Supabase/Ktor 클라이언트, 네트워크 실행과 BuildKonfig 설정 |
| `:core:platform` | 권한, 외부 URI, 시간 등 플랫폼별 구현 |
| `:core:notification` | 알림 딥링크 처리와 Supabase 푸시 대상 등록 |
| `:core:navigation` | 화면 경로, 내비게이션 상태와 공용 내비게이션 UI |
| `:core:testing` | domain repository fake와 공용 테스트 fixture |
| `:domain` | 도메인 모델과 repository 계약 |
| `:data` | datasource, DTO, repository 구현, 로그인 플랫폼 구현 |
| `:feature:main:map` | 지도 화면과 플랫폼별 지도 렌더링 |
| `:feature:main:events` | 이벤트 목록과 상세 화면 |
| `:feature:main:my` | 마이 화면과 매장 제보 흐름 |
| `:feature:main` | 메인 하위 feature를 묶는 집계 모듈 |
| `:feature:hidden` | 숨긴 매장 목록 |
| `:feature:notification` | 알림 설정 화면 |

의존성은 아래 방향을 따릅니다.

```text
androidApp ---> shared <--- iosApp
                / | \
         feature data core:*
             \    /
              domain <--- core:ui / core:navigation / core:network
                |
           core:common

feature:main ---> events / map / my   집계만 담당
```

- leaf feature는 필요한 `domain`과 `core` 모듈에만 직접 의존합니다. `feature:main`만 하위 feature를 묶는 집계 역할로 feature에 의존합니다.
- `data`는 `domain` 계약을 구현하고 `core:network` 같은 인프라를 사용합니다.
- `core:ui`, `core:navigation`, `core:network`만 공개 계약에 필요한 domain 타입을 사용하며, `core:designsystem`, `core:platform`, `core:notification`은 domain에 의존하지 않습니다.
- `shared`는 기능·데이터·인프라의 Koin 모듈을 한 번씩 조립합니다.
- `androidApp`은 `shared`에 더해 Android 로그인·푸시 알림 서비스가 사용하는 data/core 모듈을 직접 연결합니다.
- 테스트 전용 코드는 production 모듈에서 참조하지 않습니다. `core:testing`은 data 타입에 의존하지 않으며, data 전용 fake는 `data/src/commonTest`가 소유합니다.

## 빌드와 실행

Android 디버그 앱을 빌드합니다.

```shell
./gradlew :androidApp:assembleDebug
```

iOS 앱은 `iosApp`을 Xcode로 열어 실행합니다. 공용 framework 연결은 다음 명령으로 확인할 수 있습니다.

```shell
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## 테스트와 정적 검사

분리된 모든 KMP 모듈의 Android host 테스트를 실행합니다.

```shell
./gradlew testAndroidHostTest
```

모든 KMP 모듈의 iOS Simulator 테스트를 실행합니다.

```shell
./gradlew iosSimulatorArm64Test
```

모듈 하나만 검증할 때는 Gradle 경로를 지정합니다.

```shell
./gradlew :domain:testAndroidHostTest
./gradlew :data:testAndroidHostTest
./gradlew :feature:main:map:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test
```

코드 스타일을 확인합니다.

```shell
./gradlew ktlintCheck
```

## 로컬 설정

Supabase와 플랫폼 API 값은 환경 변수 또는 커밋되지 않는 `local.properties`로 주입합니다.

```properties
supabase.url=...
supabase.anon_key=...
kakao_native_app_key=...
naver_map_ncp_key_id=...
naver_client_secret=...
```

비밀 값과 개인 환경 설정은 저장소에 커밋하지 않습니다.

## 커밋 규칙

커밋은 의존성/빌드 설정, 파일 이동, 기능 동작, 플랫폼 코드, 테스트처럼 독립적으로 검토하고 되돌릴 수 있는 작업 단위로 나눕니다.
