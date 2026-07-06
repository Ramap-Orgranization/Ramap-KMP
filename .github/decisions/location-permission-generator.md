# 위치 권한 책임 공통화 결정 기록

## 배경

`KakaoMapView`의 `expect/actual` 구현을 상태 객체 중심으로 정리하는 과정에서 위치 권한 요청 책임이 지도 화면 안에 섞여 있었다. Android는 `rememberLauncherForActivityResult`, 권한 결과 판단, 차단 상태 판단, 마지막 위치 이동이 `KakaoMapView.android.kt`와 `LocationProvider`에 나뉘어 있었고, iOS는 `IosKakaoMapController`가 지도 제어와 권한 상태 판단을 함께 담당했다.

이 구조에서는 두 플랫폼의 지도 화면이 비슷한 책임을 갖는지 비교하기 어렵고, 권한 요청 정책을 바꾸려면 지도 렌더링 코드까지 같이 읽어야 했다.

## 문제

- `KakaoMapView`가 지도 표시, 마커 렌더링, 카메라 이동, 위치 권한 요청 결과 처리까지 함께 담당했다.
- Android와 iOS의 권한 흐름이 서로 다른 파일에 묻혀 있어 같은 책임인지 리뷰하기 어려웠다.
- Android에서는 권한 차단 판단을 위해 `SharedPreferences`로 요청 이력을 저장하는 구현이 들어갔지만, 앱의 기존 개발 방식과 맞지 않고 이 작업의 핵심 요구도 아니었다.
- 처음 작성한 `LocationPermission` expect 함수명은 함수가 권한 객체 자체처럼 보이게 만들어, common 계약의 의도가 잘 드러나지 않았다.

## 결정

위치 권한 요청 책임을 `platform.permission` 패키지로 분리하고, common에는 플랫폼 중립적인 계약만 둔다.

- `LocationPermissionGenerator`: 권한 보유 여부 확인과 권한 요청을 담당하는 common 인터페이스
- `PermissionStatus`: 권한 요청 결과를 `Granted`, `Denied`, `Blocked`로 표현하는 common 상태
- `rememberLocationPermissionGenerator`: Compose 안에서 플랫폼별 권한 generator를 생성하는 expect 함수
- Android actual: `rememberLauncherForActivityResult`와 `ActivityCompat.shouldShowRequestPermissionRationale` 기반 구현
- iOS actual: `CLLocationManager`와 delegate 기반 구현

`KakaoMapView`는 권한 요청을 직접 수행하지 않고 `LocationPermissionGenerator.requestPermission()`만 호출한다. 권한 결과를 받은 뒤 지도 위치 이동 또는 차단 콜백을 실행하는 화면 정책만 남긴다.

## 주요 의사 결정

### expect 함수에 `@Composable`을 유지한 이유

`expect`는 플랫폼별 구현이 맞춰야 하는 함수의 계약이다. 이 함수는 Android actual에서 `rememberLauncherForActivityResult`, `LocalContext`, `remember`, `rememberUpdatedState`를 사용해야 하므로 Compose 호출 컨텍스트가 필요하다. 따라서 common 선언에도 `@Composable`을 붙여 호출 제약을 계약으로 드러내는 것이 맞다.

다만 함수명이 `LocationPermission`이면 객체 또는 컴포넌트처럼 보이므로, Compose의 remember 계열 함수라는 의미가 드러나는 `rememberLocationPermissionGenerator`로 변경했다.

### common에는 인터페이스를 두고 플랫폼 actual은 함수로 둔 이유

common은 "무엇을 할 수 있는가"만 정의한다. `LocationPermissionGenerator`는 권한 상태 확인과 요청이라는 행위를 표현하고, `rememberLocationPermissionGenerator`는 각 플랫폼에서 그 인터페이스 구현체를 만들어 반환한다.

즉, expect 함수가 인터페이스를 "구현"하는 구조가 아니라, expect/actual 함수가 플랫폼 구현 객체를 "생성해서 반환"하는 구조다.

### SharedPreferences를 제거한 이유

Android에서 `shouldShowRequestPermissionRationale`만으로 최초 요청 전 상태와 "다시 묻지 않음" 상태를 완전히 구분할 수는 없다. 둘 다 `false`가 될 수 있기 때문이다.

하지만 이번 흐름에서는 `requestPermission()`을 실제로 호출한 뒤 결과 콜백에서 판단한다. 이 시점에는 사용자가 권한 요청에 응답한 뒤이므로, 권한이 없고 rationale도 표시되지 않는 상태를 `Blocked`로 다루는 단순한 정책을 사용할 수 있다. 별도 요청 이력 저장은 요구사항보다 무겁고 기존 앱 패턴에도 맞지 않아 제거했다.

### 지도 controller에서 권한 판단을 제거한 이유

지도 controller는 지도 view, marker, camera, 현재 위치 이동 같은 지도 제어에 집중하는 편이 자연스럽다. 권한 상태 판단까지 포함하면 Android/iOS의 책임 경계가 달라지고, 권한 정책 변경이 지도 제어 코드에 영향을 준다.

그래서 iOS의 `IosKakaoMapController`에서도 권한 분기를 제거하고, 권한이 허용된 뒤 현재 위치로 이동하는 책임만 남겼다.

## 결과 구조

```text
shared/src/commonMain/kotlin/com/peto/ramap/platform/permission
  LocationPermissionGenerator.kt
  PermissionStatus.kt
  RememberLocationPermissionGenerator.kt

shared/src/androidMain/kotlin/com/peto/ramap/platform/permission
  AndroidLocationPermissionGenerator.kt
  RememberLocationPermissionGenerator.android.kt

shared/src/iosMain/kotlin/com/peto/ramap/platform/permission
  IosLocationPermissionGenerator.kt
  IosLocationPermissionDelegate.kt
  RememberLocationPermissionGenerator.ios.kt
```

## 검증

다음 명령으로 Android host test, iOS simulator test, Android debug build를 확인했다.

```bash
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug
```

결과는 `BUILD SUCCESSFUL`이다. 남은 경고는 기존 Gradle/AGP/expect-actual Beta 경고이며, 이번 변경의 컴파일 실패와는 무관하다.

## 후속 검토 포인트

- Android에서 최초 요청 전 `shouldShowRequestPermissionRationale == false`와 차단 상태를 더 엄밀히 구분해야 하는 UX가 생기면 요청 이력 저장을 다시 검토한다.
- `PermissionStatus.Denied`를 화면에서 무시하는 현재 정책이 충분한지, snackbar 또는 안내 UI가 필요한지 별도로 판단한다.
- 다른 권한도 같은 패턴으로 확장할 경우 `LocationPermissionGenerator`를 일반 권한 인터페이스로 넓힐지 검토한다.
