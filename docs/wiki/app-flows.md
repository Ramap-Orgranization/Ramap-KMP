---
authority: reference
status: active
last_verified: 2026-10-11
source_ref: 33a0d49c2731e42144f2c79704748d7c7e0cf68b
---

# 앱 실행 흐름

플랫폼 진입점에서 공통 앱, 내비게이션, 지도 요청까지 따라가는 경로입니다. 사용자에게 보이는 동작의 경계는 [제품 정책과 구현 근거](product-policy.md)에서 함께 확인합니다.

## 플랫폼 시작과 공통 앱

| 단계 | Android | iOS |
| --- | --- | --- |
| SDK·Koin 초기화 | [MainApplication](../../androidApp/src/main/kotlin/com/peto/ramap/MainApplication.kt) | [iOSApp.swift](../../iosApp/iosApp/iOSApp.swift) |
| 화면 진입 | [MainActivity](../../androidApp/src/main/kotlin/com/peto/ramap/MainActivity.kt) | [ContentView.swift](../../iosApp/iosApp/ContentView.swift) → [MainViewController](../../shared/src/iosMain/kotlin/com/peto/ramap/MainViewController.kt) |
| 공통 조립 | [KoinInitializer](../../shared/src/commonMain/kotlin/com/peto/ramap/di/KoinInitializer.kt) | 같은 공통 초기화 함수 |
| 공통 UI | [App](../../shared/src/commonMain/kotlin/com/peto/ramap/App.kt) | 같은 공통 App |

`App`은 테마와 업데이트 게이트를 거쳐 공지, 개인화 초기화, `AppRoute`를 연결합니다. 업데이트 게이트는 최소 빌드 미달이고 지원하는 스토어 URL이 있을 때 진입을 막습니다. 조회 실패·시간 초과는 진입을 허용합니다. 앱 공지는 콘텐츠 위에 표시하며 콘텐츠 진입을 기다리게 하지 않습니다.

세션 전환은 개인화 재조회·초기화와 재시도 대기열에 영향을 줍니다. 첫 진입에는 개인화 로딩·오류 상태를 처리하지만 이미 열린 route는 이후 새로고침에도 유지합니다. 근거: [AppPersonalizationTest](../../shared/src/commonTest/kotlin/com/peto/ramap/AppPersonalizationTest.kt), [AppUpdateGateViewModelTest](../../shared/src/commonTest/kotlin/com/peto/ramap/AppUpdateGateViewModelTest.kt), [AppNoticeGateTest](../../shared/src/commonTest/kotlin/com/peto/ramap/AppNoticeGateTest.kt).

## 내비게이션과 딥링크

[ScreenRoutes](../../core/navigation/src/commonMain/kotlin/com/peto/ramap/navigation/ScreenRoutes.kt)가 typed route를 정의하고 [RememberNavigationState](../../core/navigation/src/commonMain/kotlin/com/peto/ramap/navigation/RememberNavigationState.kt)가 탭별 스택을 만듭니다. 초기 탭은 지도이며 지도·랭킹·이벤트·마이는 별도 스택을 가집니다. 이벤트 탭 이탈 시 이벤트 스택 초기화 등 전이 규칙은 [NavigationState](../../core/navigation/src/commonMain/kotlin/com/peto/ramap/navigation/NavigationState.kt)와 [테스트](../../core/navigation/src/commonTest/kotlin/com/peto/ramap/navigation/NavigationStateTest.kt)에서 확인합니다.

[NavigationGraph](../../shared/src/commonMain/kotlin/com/peto/ramap/NavigationGraph.kt)가 feature route를 연결합니다. 화면에 겹쳐 여는 경로는 [AppRoute](../../shared/src/commonMain/kotlin/com/peto/ramap/AppRoute.kt)의 overlay 연결도 확인해야 합니다.

가게 링크는 [ShopDeepLinkParser](../../core/navigation/src/commonMain/kotlin/com/peto/ramap/navigation/deeplink/ShopDeepLinkParser.kt) → [ShopDeepLinkDispatcher](../../core/navigation/src/commonMain/kotlin/com/peto/ramap/navigation/deeplink/ShopDeepLinkDispatcher.kt) → [AppRouteObservers](../../shared/src/commonMain/kotlin/com/peto/ramap/AppRouteObservers.kt) → 지도 선택으로 이어집니다. Android는 `DeepLinkEntryPoint`를 거치며 iOS는 [ShopDeepLinkBridge](../../shared/src/iosMain/kotlin/com/peto/ramap/ShopDeepLinkBridge.kt)에서 dispatcher를 직접 호출합니다. 두 플랫폼의 인증·가게 링크 진입 경로를 동일하다고 가정하지 않습니다.

가게 링크 소비는 이전 링크를 처리하는 동안 새 링크가 들어와도 새 pending 값을 지우지 않도록 구성되어 있습니다. 근거: [ShopDeepLinkDispatcherTest](../../core/navigation/src/commonTest/kotlin/com/peto/ramap/navigation/ShopDeepLinkDispatcherTest.kt), [ShopDeepLinkParserTest](../../core/navigation/src/commonTest/kotlin/com/peto/ramap/navigation/ShopDeepLinkParserTest.kt).

## 지도와 가게 상세

[MapRoute](../../feature/main/map/src/commonMain/kotlin/com/peto/ramap/ui/main/map/MapRoute.kt)는 상태·효과를 구독하고 입력을 intent로 전달합니다. [MapViewModel](../../feature/main/map/src/commonMain/kotlin/com/peto/ramap/ui/main/map/MapViewModel.kt)은 viewport, 검색, 가게 선택, 리뷰, 개인화, 계정 이벤트를 연결합니다.

- 영역 조회: [ViewportShopLoader](../../feature/main/map/src/commonMain/kotlin/com/peto/ramap/ui/main/map/viewport/ViewportShopLoader.kt)가 변경을 지연 처리하고 마지막 성공 영역과 요청 ID로 재조회·오래된 응답 반영을 제어합니다.
- 검색: 정규화한 입력으로 요청을 교체합니다. 새 검색은 상세와 일부 필터를 초기화하고, 결과 수와 가시성에 따라 상세 또는 목록으로 연결합니다. 외부 장소 검색 fallback은 없습니다.
- 상세: 캐시가 있으면 먼저 보여주고 갱신합니다. 캐시를 보여주는 중 갱신이 실패하면 기존 내용을 유지합니다. 선택 변경과 탭 이탈은 진행 중인 작업과 상세 상태에 영향을 줍니다.
- 플랫폼 지도: 공통 [RamapMapView](../../feature/main/map/src/commonMain/kotlin/com/peto/ramap/ui/main/map/RamapMapView.kt)와 각 플랫폼 소스 세트를 함께 확인합니다.

상태 전이의 중심 근거는 [MapViewModelTest](../../feature/main/map/src/commonTest/kotlin/com/peto/ramap/ui/main/MapViewModelTest.kt)입니다. 숫자 설정이나 취소 정책을 바꾸면 ViewModel뿐 아니라 loader·상태·관련 테스트까지 확인합니다.
