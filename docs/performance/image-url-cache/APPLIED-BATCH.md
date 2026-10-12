# 앱 기본 배치 발급 적용 및 Kotlin 재측정

2026-10-12. 사용자가 기존 권한 검증을 유지하며 Redis를 제외하고 기본 배치 발급으로 앱을 수정하도록 승인했다.
**공통 데이터 계층에 적용 완료. 앱 배포 및 실기기 화면 성능 측정은 아직 하지 않았다.**

## 적용 내용

[RemoteImageUrlDataSource](../../../data/src/commonMain/kotlin/com/peto/ramap/data/datasource/image/RemoteImageUrlDataSource.kt)가
`image-signed-urls` Edge 대신 Supabase의 `POST /storage/v1/object/sign/{bucket}`를 호출한다.

- 요청 내 동일 버킷·경로 중복 제거 → 버킷별 묶음 → 64개씩 분할 → 최대 2개 배치 동시 실행.
- 프로필/리뷰 버킷과 기존 UUID 이미지 경로만 허용한다. 지원하지 않는 입력은 네트워크 호출 없이 null이다.
- 경로로 응답을 대응시킨다. 반환 순서에 의존하지 않고 누락·중복 응답·개별 실패는 해당 이미지만 null로 처리한다.
- 버킷 하나의 실패가 다른 버킷의 성공을 지우지 않는다. 취소는 전파하고, 요청 중 계정이 변경되면 결과를 반환하지 않는다.
- 현재 5분 링크 수명, 발급 요청 전 시각을 기준으로 한 보수적인 만료 시각, 기존 내 프로필 메모리 캐시의 1분 여유를 유지한다.
- URL 경로·토큰·fragment·남은 유효 시간을 검사한다. 공유 캐시나 새 인앱 영속 캐시는 추가하지 않았다.
- 기존 DI와 `ImageUrlDataSource` 계약을 유지하므로 내 프로필·커뮤니티·리뷰 호출부가 새 경로를 공통으로 사용한다.
- Edge 응답 DTO 및 이전 데이터 소스는 비교용 `androidHostTest`로 옮겼다. 앱 공통 소스에는 Redis API 호출이 없다.

### Kotlin SDK 어댑터가 필요한 이유

사용 버전은 supabase-kt **3.6.0**이다. 이 버전의 `SignedUrl.signedURL`은 필수 non-null String이고,
`createSignedUrls`는 성공·실패 항목을 함께 역직렬화한 뒤 URL을 변환한다. 서버의 이미지별 실패에서
`signedURL: null`이 오면 성공한 이미지까지 전체 배치 역직렬화에 실패할 수 있다.

따라서 동일한 **기본 배치 HTTP API**를 SDK의 `authenticatedSupabaseApi(client.storage)`로 호출하고,
[nullable 응답 DTO](../../../data/src/commonMain/kotlin/com/peto/ramap/data/model/StorageSignedImageResponse.kt)로 처리한다.
직접 JWT 문자열을 조립하거나 service-role로 발급하지 않는다. 이 SDK 내부 어댑터 사용은
`@OptIn(SupabaseInternal::class)`로 한 데이터 소스에 한정했다. SDK 업그레이드 때 이 경계와 회귀 테스트를 확인해야 한다.

## 기존 권한 검증 유지 근거

공식 배치 발급 API는 `storage.objects` SELECT 정책을 요구한다.
[Supabase Kotlin 배치 API](https://supabase.com/docs/reference/kotlin/storage-from-createsignedurls).

운영 설정을 읽기 전용으로 확인했다.

- `profile-avatars`, `shop-review-images` 모두 `public=false`.
- 리뷰 이미지: `Owners or visible published review images`, `anon/authenticated`, `community_private.can_read_review_image(name)`.
- 프로필: `Owners or visible community profiles read avatars`가 소유자/유효 사용자 또는 볼 수 있는 공개 프로필을 확인.
- 기존 추가 정책인 `Blockers read blocked profile avatars`, `follow_header_avatar_read`도 유지.
- 확인한 SELECT 정책에는 이번에 생략한 info 호출을 별도 필수 단계로 만드는 operation 조건이 없다.
- 마이그레이션·버킷 공개 설정·RLS·함수 권한·계정 정책을 변경하지 않았다.

캐시 전의 info 확인은 파일 버전 및 현재 권한을 알아보려는 단계였다. 새 경로에서는 **발급 요청 자체에
현재 사용자 JWT를 보내 Storage가 같은 SELECT 정책으로 판단한다.** 비로그인은 anon으로 검사한다.
이미 받은 임시 URL의 즉시 회수까지 보장하는 변경은 아니다.

토큰 전달·부분 거부는 모의 네트워크 회귀 테스트로 확인했다. 실제 네트워크 성능 시험은 anon이 볼 수 있는
공개 이미지로 실행했다. 다른 회원의 비공개·차단 관계 전체를 실제 계정으로 통합 검증했다고 주장하지 않는다.

## 실제 Kotlin 코드 적용 후 비교

[실행한 테스트](../../../data/src/androidHostTest/kotlin/com/peto/ramap/data/datasource/image/ImageUrlLiveBenchmarkTest.kt)는
변경된 프로덕션 `RemoteImageUrlDataSource`와 변경 전 코드를 보존한
[기준 데이터 소스](../../../data/src/androidHostTest/kotlin/com/peto/ramap/data/datasource/image/LegacyImageUrlBenchmarkDataSource.kt)를
동일 SupabaseClient·OkHttp 엔진·anon 권한·입력으로 실행한다.

- 시각: **2026-10-12 10:55:08–10:57:15 KST**.
- 리뷰 20개, 이미지 등장 41회, 고유 22개. 기존 비교와 fixture SHA-256 동일.
- 공통 Kotlin 프로덕션 코드를 Android host JVM/macOS에서 실행. 에뮬레이터·실기기·UI 측정이 아니다.
- 각 30회, 홀수/짝수 라운드마다 순서 교대, 표본 사이 1.5초, 페이지 동시 실행 1, 재시도 0.
- 기존 Redis 경로는 시드니를 지정하며 매 본 표본에서 `cacheHits=22, issued=0`을 확인했다.
- 준비 요청은 각 1회 별도 기록. 기본 배치 최초 1,544.849ms, 기존 Redis 최초 1,513.424ms.
  최초 연결·서버/캐시 준비 비용을 정상 반복 요청과 섞지 않는다. 첫 실행부터 71.3% 빠르다는 뜻은 아니다.

| 지표 | 변경 전 Edge + Redis 적중 | 적용 후 기본 배치 |
| --- | ---: | ---: |
| 본 측정 횟수 | 30 | 30 |
| 링크 준비 시간 p50 | 669.756ms | **192.056ms** |
| p95 (선형 보간) | 1,144.839ms | **548.643ms** |
| 평균 | 726.105ms | 283.976ms |
| 실패 이미지 | 0 | 0 |
| 페이지당 클라이언트 HTTP | 1 | 2 |
| 페이지당 Storage HTTP | 22 info | 2 batch sign |
| 페이지당 새로 발급한 링크 | 0 | 22 |
| 페이지당 Redis REST | 22 | **0** |
| 페이지당 Edge 호출 | 1 | **0** |

- p50 **71.3% 감소**, p95 **52.1% 감소**. 같은 라운드 30쌍 중 28쌍에서 기본 배치가 빨랐다.
- Storage HTTP 요청 수 **90.9% 감소**(22 → 2). 실제 DB 쿼리·물리 Disk IO 감소율은 아니다.
- 클라이언트 HTTP 요청은 1 → 2로 증가한다. 모든 종류의 요청이 줄었다고 표현하면 안 된다.
- 캐시가 없어 새 링크 발급 수는 증가하지만 전체 시간과 별도 캐시 운영 의존성은 줄었다.
- 준비 포함 Storage 호출 시도 766회, Redis REST 748회. 새 임시 함수를 배포하거나 데이터/RLS를 바꾸지 않았다.
- 응답의 request ID를 서버 `function_logs`와 대조했다. 기존 경로 31개 로그, 발급 22개, 적중 660개가
  헤더 집계와 일치했다. 동일 계측 코드의 두 출력 대조이며 Storage 내부 쿼리나 청구서의 별도 검증은 아니다.

[원본 62개 표본](sdk-comparison-20261012.json), [요약](sdk-summary-20261012.json),
[계산 스크립트](../../../tools/image-url-benchmark/analyze-sdk.mjs).
프로덕션·기준·측정 코드 및 fixture 해시를 보관했고 최종 Kotlin 파일과 일치하는지 확인했다.
원본 이미지 경로·URL·토큰은 산출물에서 제외했다.

## 검증 결과와 남는 한계

- `:data:testAndroidHostTest`: **130개 통과**, 실패 0, 명시적 실행이 필요한 네트워크 벤치마크 1개 건너뜀.
- 별도 명시적 네트워크 벤치마크: **1개 통과**, 위 60회 비교 + 준비 2회 완료.
- 새 회귀 테스트 10개: 중복/두 버킷, nullable 부분 실패, 사용자 JWT, 버킷 거부 격리, 잘못된 응답,
  빈/잘못된 입력, 64개 분할·최대 동시 2개, 계정 변경, 취소, 토큰 없는/fragment URL 거부.
- `:data:compileKotlinIosSimulatorArm64`, `:data:ktlintCheck`: 통과.
- 기존 `DefaultProfileRepositoryTest`의 ExperimentalCoroutinesApi opt-in 경고는 남아 있다.
- 앱 바이너리를 릴리스하거나 실기기에서 측정하지 않았다. 실제 사용자 전체의 적중률·UI·다운로드·Disk IO 개선은 미측정이다.
- 측정 직후에는 `image-signed-urls` 운영 함수와 Upstash 리소스를 비교 기준용으로 남겼다.
  이후 2026-10-12 사용자 요청으로 관련 함수·Redis·전용 Secrets를 삭제했다([정리 기록](CLEANUP.md)).
  **변경된 앱의 이미지 요청 경로에서는 호출하지 않는다.** 리소스 폐기와 운영 앱 전환 완료를 혼동하지 않는다.

## 포트폴리오 표현

> 이미지 임시 링크의 Redis 공유 캐시와 Supabase 기본 배치 API를 비교하고, 기존 RLS를 유지하며
> KMP 공통 데이터 계층을 버킷별 배치 발급으로 전환했다. 변경 전후 Kotlin 데이터 소스를 동일 입력으로
> 각 30회 실행한 Android host HTTP 벤치마크에서 링크 준비 시간 중앙값을 670ms에서 192ms로
> 71.3% 단축하고, 페이지당 Storage HTTP 요청을 22회에서 2회로 줄였다.
> 새 앱 경로의 Redis·Edge 의존성을 제거하면서 부분 실패·세션 변경·취소에 대한 회귀 검증을 추가했다.

면접에서는 측정 환경·준비 요청·표본 수·실기기와 Disk IO 미측정 범위를 함께 설명한다.
앞선 [Node 대안 비교](CONTROLLED-COMPARISON.md)의 69.7%와 이번 Kotlin 적용 후 71.3%를 서로 다른 실험으로 구분한다.
