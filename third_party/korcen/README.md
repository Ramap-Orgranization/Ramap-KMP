# 한국어 비속어 필터

`domain`의 `KoreanProfanityFilter`는 Android와 iOS에서 공통으로 사용할 수 있는
동기식 로컬 필터다. 네트워크 요청이나 플랫폼 라이브러리 의존성이 없다.

```kotlin
val filter = KoreanProfanityFilter()
val containsProfanity = filter.containsProfanity(input)
```

## 자료와 라이선스

- 탐색에 사용한 자료 목록: https://github.com/Tanat05/korean-profanity-resources
- 실제 단어 출처: https://github.com/Tanat05/korcen
- 고정한 원본 revision: `eecd9763dbdccce3dc96ddb578ef0b6396058fa9`
- 원본 파일: `korcen/korcen.py`의 한국어 비속어 목록과 예외 표현 목록
- 라이선스: 원본 revision의 MIT. 저작권 및 허가 고지는
  [`LICENSE`](LICENSE)에 보관했다.
  동일한 고지를 `core/designsystem/src/commonMain/composeResources/files/licenses/korcen.txt`에
  포함하여 Android와 iOS의 공통 Compose 리소스로 배포한다.

자료 목록의 라이선스 표는 현재 원본과 다를 수 있다. 실제 사용한 revision의
`LICENSE`를 기준으로 한다. 목록 저장소의 `slang.csv`와 LoL 필터링 목록은 가져오지 않았다.

## Ramap에 맞춘 변경

원본 라이브러리 전체를 이식하지 않고 명확한 욕설, 자음 약어, 변형 표기, 일부 비하 표현을
선별했다. `씨발`, `개새끼`, `씨1발`, `시1발`, `븅신`, `니미랄`, `씹새끼`는
원본의 변형 표기 및 부분 단어를 보완한 Ramap 표기다.
`미친`, `보지`, `자지`, `세끼`, `새끼`, `시바`, 숫자 `18`, 단독 `ㅗ`와 같이
일상 문장이나 음식 리뷰에서 쉽게 오탐이 나는 표현은 기본 차단 목록에 넣지 않았다.
정치인 이름, 외국어 전체 목록, 문맥을 분류하는 ML 모델도 포함하지 않았다.

검사 시 공백, 구두점, 제로 너비 문자 등을 제거하고 영문 대소문자와 전각 ASCII를 통일한다.
현대 한글의 분해 자모 및 호환 자모를 조합하여 `씨발`, `ㅆㅣㅂㅏㄹ`도 검사한다.
숫자는 유지한다. 모든 숫자를 지우거나 비슷하게 생긴 문자로 바꾸지 않는다.

`시발점`, `시발역` 등의 예외는 해당 구간만 제외한다. 예외가 있는 문장에 다른 욕설이
있으면 감지하며, 예외 구간 양옆의 글자를 합쳐 새로운 금지어를 만들지 않는다.

추가 정책이 필요하면 생성자에 `blockedWords`와 `allowedExpressions`를 전달할 수 있다.
전달한 목록은 기본 목록을 대체하며 생성 시 복사된다. 원본 단어 목록과 서비스 보완 규칙은
`KoreanProfanityDictionary`에서 관리한다. 단어를 늘릴 때 정상 리뷰와 우회 표기 테스트도
함께 검증한다.

## 적용 범위와 한계

`ProfileNickname.isValid`와 `Review.isValidBody`에서 비속어를 검사한다. 닉네임 중복 확인과
프로필 저장, 리뷰 신규 등록과 수정은 기존 저장 직전 검증을 통해 서버 요청 및 사진 업로드
전에 차단한다. 닉네임·리뷰 입력란에는 비속어 안내를 표시하며, 수정해 정상 입력으로 돌아오면
기존 중복 확인·등록 흐름을 다시 사용할 수 있다. 자기소개와 기존 표시 콘텐츠는 검사하지 않는다.

Supabase 서버 정책은 변경하지 않았다. 클라이언트 필터만으로는 직접 API 요청을 통한
우회를 막을 수 없으므로 서비스 전체 차단이 필요하면 서버에서도 같은 정책을 검증해야 한다.

단어 기반 검사이므로 문맥을 이해하지 못하고, 모든 신조어·우회 표기·혐오 표현을
탐지하지는 못한다. 기본 목록은 긍정·부정 평가를 분류하는 용도가 아니다.

## 검증

`KoreanProfanityFilterTest`에서 직접 욕설, 구분자 우회, 영문 대소문자·전각 표기,
분해 한글, 정상 음식 리뷰, 예외 구간, 사용자 정의 목록을 검사한다.

```shell
./gradlew :domain:testAndroidHostTest :domain:iosSimulatorArm64Test :domain:ktlintCheck
```
