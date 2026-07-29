<img src="https://github.com/user-attachments/assets/3ac49158-b4c9-47c1-8cef-264d782058af" alt="라맵 히어로 이미지" width="100%" />

<p align="center">
<img src="https://github.com/user-attachments/assets/b232948c-5377-4577-acd5-bf7b324edd78" width="30%"/>
<img src="https://github.com/user-attachments/assets/a6185d53-64fe-45c9-ab91-18c40a2c717d" width="30%"/>
<img src="https://github.com/user-attachments/assets/28eeb61e-3ca1-44c5-9640-ba786e7998fc" width="30%"/>
</p>
<p align="center">
<img src="https://github.com/user-attachments/assets/6c7c7c5b-b185-40dc-b675-55062313675c" width="30%"/>
<img src="https://github.com/user-attachments/assets/fcf54bea-4bc2-45ff-8bf1-0919ce93633f" width="30%"/>
<img src="https://github.com/user-attachments/assets/9c17f64c-9478-4cf4-a52a-d84e6be66656" width="30%"/>
</p>

## 기술 스택

| 구분 | 기술 |
|---|---|
| 멀티플랫폼 | Kotlin Multiplatform, Android, iOS |
| UI | Compose Multiplatform, Jetpack Compose, Material 3 |
| 아키텍처 | Clean Architecture, Feature-based Modularization |
| 상태 관리 | Kotlin Coroutines, Flow, StateFlow, MVI 스타일 단방향 상태 관리 |
| 의존성 주입 | Koin |
| 네트워크 | Ktor Client, Kotlinx Serialization |
| 백엔드 | Supabase Auth, PostgREST, Storage |
| 내비게이션 | AndroidX Navigation 3, Navigation Event, 딥링크, Universal Link |
| 지도·위치 | Naver Map SDK, Google Play Services Location |
| 로그인 | Kakao SDK |
| 분석·모니터링 | Firebase Analytics, Firebase Crashlytics |
| 푸시 알림 | Firebase Cloud Messaging |
| 이미지 | Coil 3 |
| 테스트 | Kotlin Test, Coroutines Test, Turbine, Compose UI Test, Robolectric |
| 코드 품질 | Ktlint |
| 빌드 | Gradle Convention Plugin, BuildKonfig |
| 개발 환경 | Kotlin `2.4.10`, AGP `9.3.0`, Android SDK `37`, JDK 17+ |

## 프로젝트 구조

| 계층 | 모듈 | 역할 |
|---|---|---|
| Application | `androidApp` | Android 앱 진입점, Firebase·Kakao·Naver SDK 초기화 |
| Application | `iosApp` | iOS 앱 진입점, SwiftUI 및 플랫폼 이벤트 처리 |
| Composition Root | `shared` | 공통 앱 조립, Koin 초기화, 루트 UI 및 라우팅 |
| Feature | `feature/*` | 지도, 랭킹, 이벤트, 마이, 북마크 등 화면별 기능 |
| Data | `data` | DataSource, DTO, Repository 구현, Supabase 연동 |
| Domain | `domain` | 도메인 모델, Repository 계약, Use Case |
| Core | `core/network` | Ktor·Supabase 네트워크 계층 |
| Core | `core/ui` | 공통 ViewModel, UI 상태, 로딩·에러 처리 |
| Core | `core/designsystem` | 공통 Compose 컴포넌트와 디자인 리소스 |
| Core | `core/navigation` | Navigation 3, 탭, 백스택, 딥링크 |
| Core | `core/platform` | Android·iOS 플랫폼 기능 추상화 |
| Core | `core/analytics` | Analytics·Crashlytics 추상화 |
| Core | `core/notification` | 푸시 알림 및 알림 딥링크 처리 |
| Core | `core/testing` | 테스트용 Fake와 Fixture |
