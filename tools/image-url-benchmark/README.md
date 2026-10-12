# 이미지 링크 캐시 전후 비교

## 리소스 정리

2026-10-12 기본 배치 발급 채택 후 Redis 실험 리소스를 정리했다.
현재 상태와 확인 범위는 [정리 기록](../../docs/performance/image-url-cache/CLEANUP.md)을 따른다.
실험 소스는 `archived-functions/`에 보존하며 운영 배포 대상에서 제외했다.
아래 네트워크 비교 명령은 과거 재현 절차다. 기존 운영 함수·Redis를 전제로 실행하면 실패한다.
재실험은 별도로 승인된 검증 환경과 접속 설정을 준비한 뒤 수행한다. 원본 JSON의 분석은 계속 가능하다.


## 앱 적용 후 Kotlin 비교

현재 앱은 기본 배치 API를 사용한다. [적용 후 보고서](../../docs/performance/image-url-cache/APPLIED-BATCH.md)를 우선 읽는다.
`data/src/androidHostTest`의 `ImageUrlLiveBenchmarkTest`는 **현재 프로덕션 데이터 소스 자체**와
이전 Redis 호출 코드를 같은 SupabaseClient/OkHttp에서 실행한다. UI/실기기 벤치마크는 아니다.

일반 테스트에서는 건너뛰며 아래처럼 명시적으로 켠 경우에만 실제 네트워크를 사용한다.
출력 파일은 기존 파일과 다른 경로여야 한다. `--rerun-tasks --no-build-cache`는 과거의 건너뜀/성공 결과 재사용을 막는다.

```sh
RAMAP_RUN_IMAGE_BENCHMARK=1 \
RAMAP_BENCHMARK_ROOT="$PWD" \
RAMAP_BENCHMARK_FIXTURE=/private/fixture.json \
RAMAP_BENCHMARK_OUTPUT=/private/new-sdk-result.json \
./gradlew :data:testAndroidHostTest --tests '*ImageUrlLiveBenchmarkTest' \
  --rerun-tasks --no-build-cache --no-configuration-cache --console=plain

node tools/image-url-benchmark/analyze-sdk.mjs /private/new-sdk-result.json
```

동일 41회 등장/22개 고유 이미지 입력으로 준비 각 1회, 본 측정 각 30회, 1.5초 간격, 순서 교대한다.
기존 운영 `image-signed-urls` v2를 기준으로 삼으므로 해당 함수/Redis가 종료됐다면 재현할 수 없다.
서비스 데이터·권한·캐시를 초기화하지 않는다. 실패나 캐시 상태 불일치는 중단하고 부분 원본을 남긴다.
실패로 예외가 난 표본의 `wallMs=0`은 미측정 표시이며 성능 값으로 집계하지 않는다.
현재 공개 원본은 전부 성공한 실행이다. 첫 Redis 준비 요청의 이름은 `redis_warm`, `round=0`이지만 실제로
22개를 신규 발급한 cold 준비였으며 본 warm 통계에서 제외했다.

## 기본 배치 발급을 포함한 후속 통제 비교

`controlled.mjs`는 아래 기존 파일럿과 별개의 실험이다. Redis 필요성 판단에는
[후속 비교 보고서](../../docs/performance/image-url-cache/CONTROLLED-COMPARISON.md)를 우선 읽는다.

| 비교군 | 구현 | 본 표본 |
| --- | --- | ---: |
| `direct_batch` | 중복 제거 후 버킷당 native batch sign 1회, 2버킷 병렬 | 30 |
| `edge_batch` | 같은 `batch-sign.mjs`를 동일 실험 Edge에서 실행 | 30 |
| `redis_warm` | 현재 캐시 핸들러, 22개 모두 캐시 재사용 | 30 |
| `redis_cold` | 별도 빈 namespace slot, 현재 핸들러 | 6 |
| `redis_mixed` | 11개를 먼저 채운 별도 slot, 50% 이미지 적중 | 6 |
| `edge_uncached_control` | 같은 캐시 핸들러에서 Redis 설정만 생략, info+개별 sign 유지 | 10 |

추가 대조군은 **현재 코드에서 Redis만의 효과**를 비교한다. `edge_batch`와 Redis의 차이는
캐시뿐 아니라 배치 API 사용 여부도 포함한다. 기본 배치의 동시 요청은 버킷 수에 따라 2개,
캐시 핸들러의 동시 이미지 처리 상한은 8개다. 이를 동일 알고리즘·동일 하위 요청 수라고 설명하지 않는다.

공통 조건: 동일 22개 이미지, anon JWT/RLS, 링크 수명 300초, 동일 Node 클라이언트,
페이지 동시 실행 1개, 표본 간 2초, 주 비교군 순서 6가지 순환. Edge는 `x-region: ap-southeast-2`를
명시하고 실제 응답의 `x-sb-edge-region`을 검사한다. 캐시가 빈 상태는 DB 버퍼나 Edge 프로세스가
처음 실행되는 상태를 의미하지 않는다. 혼합 적중률 50%는 합성 조건이며 실제 서비스 적중률이 아니다.

### 실험 함수와 격리

1. 기존 비공개 fixture를 사용한다. `prepare-controlled.mjs FIXTURE PRIVATE_NEW_DIRECTORY`로
   0700 디렉터리와 0600 배포 묶음/설정을 만든다. 생성물은 저장소 밖에 둔다.
2. 생성된 `bundle.json`의 파일 목록을 Supabase 배포 도구로 해당 `functionName`에 배포한다.
   `verify_jwt=true`, `index.ts` 진입점이다. 운영 `image-signed-urls`는 변경하지 않는다.
3. 임시 함수는 별도 무작위 토큰, 2시간 종료 시각, 이미지 해시 허용 목록을 검사한다.
   토큰은 함수에 SHA-256만 포함하며 클라이언트 개인 설정 파일에 원문을 보관한다.
4. 운영 서버 모듈을 복제하되 캐시 키의 namespace 인자만 추가한다. 소유자 잠금, TTL,
   권한 조회, 발급/재사용 로직은 동일하다. 원본 4파일과 배포 묶음 해시를 기록한다.
5. 새 실험마다 새 namespace를 만든다. Redis 전체 초기화나 앱의 캐시 키 삭제를 하지 않는다.

```sh
node tools/image-url-benchmark/prepare-controlled.mjs /private/fixture.json /private/new-experiment
node tools/image-url-benchmark/controlled.mjs \
  /private/fixture.json /private/new-experiment/config.json /private/result.json
node tools/image-url-benchmark/analyze-controlled.mjs /private/result.json
```

이 명령은 실제 서버에 요청한다. 읽기 전용 분석 명령은 마지막 `analyze-controlled.mjs`뿐이다.
캐시 값은 약 240초, 잠금은 10초 내 만료한다. 실행 뒤 임시 함수만 삭제하고 목록에서 제거됐는지 확인한다.
생성된 토큰 설정과 배포 소스도 정리한다. 해당 함수를 앱에 연결하지 않는다.
2026-10-12 실행에서는 CLI 삭제 인증이 없어 삭제 대신 410만 반환하는 코드로 닫았다.
함수 항목이 남은 상태와 종료 응답 확인은 후속 보고서의 정리 상태에 기록했다.

### 실패·통계 처리

- Storage 요청 상한 2,500회. 본 표본은 재시도하지 않는다. 연속 3페이지 실패, Redis 우회,
  캐시 준비 실패 또는 요청 예산 소진 시 중단한다. HTTP 200의 부분 실패도 이미지 오류로 센다.
- 10라운드마다 새로운 slot의 cold 표본과 별도 readiness 요청을 기록해 warm 상태를 준비한다.
  첫 cold 요청에 오류가 있으면 readiness에서 빈 항목을 채울 수 있으며 이 비용을 준비 기록에 남긴다.
- 모든 준비·실패·캐시 상태 불일치를 원본에 남긴다. 캐시 적중 수는 실제 응답 계측으로 확인한다.
- p50/p95는 정렬 표본의 `(n-1)*p` 위치를 선형 보간한다. 기존 파일럿의 nearest-rank와 다르다.
  오류 응답도 응답 시간 분포에 포함하고 실패 페이지/이미지 수를 별도 보고한다.
- 동일 라운드 시간 차이를 짝지어 비교한다. 고정 seed로 10,000회 bootstrap한 평균 차이 구간은
  **한 실행 내 표본 불확실성**이며 다른 사용자·시간대·부하에 대한 보장이 아니다.
- 최초 리전 자동 선택 실행과 시드니 초기 실행은 오류로 중단되어 별도 원본에 보존했다.
  이를 보고 본 실험의 중단 규칙을 위와 같이 조정했다. 모두 무오류였다고 보고하지 않는다.

검증: `node --experimental-transform-types --test tools/image-url-benchmark/archived-functions/image-signed-urls/handler.test.mjs tools/image-url-benchmark/batch-sign.test.mjs`.

## 기존 소규모 파일럿

실제 이미지 파일을 내려받지 않고 **목록의 이미지 링크를 모두 받는 데 걸리는 시간**을 측정한다.
같은 자료를 세 방식으로 재생한다. 앱의 화면 표시 시간, 다운로드 속도, DB Disk IO를 측정하는 도구는 아니다.

| 방식 | 실행 방식 | 분리하려는 효과 |
| --- | --- | --- |
| `legacy` | 리뷰 사진은 최대 4개 동시 발급, 각 리뷰 사진 뒤 작성자 사진 발급 | 변경 전 코드의 요청 순서·중복 |
| `dedup` | 같은 흐름에서 한 목록 내 같은 경로의 요청만 하나로 합침 | 중복 제거만의 효과 |
| `redis` | 중복 제거한 목록을 새 Edge Function에 한 번 전달 | 서버 배치 처리와 Redis 재사용의 합친 효과 |

기존 흐름의 근거는 `33a0d49c`의 `RemoteReviewDataSource.signReviews`, `signedAvatar`,
`ReviewImageSigner`다. Node의 HTTP 연결 관리는 Android/iOS SDK와 다르다.
따라서 `legacy`는 **기존 요청 흐름을 재현한 HTTP 비교군**이며 옛 앱 바이너리의 측정값이 아니다.
`dedup`과 `redis`의 차이를 Redis 하나의 효과로 표현하면 안 된다. 서버 배치 처리와 실행 위치도 달라진다.

## 준비

- Node 24 이상, Git, 현재 저장소가 필요하다. 별도 Node 패키지는 사용하지 않는다.
- `image-signed-urls`의 계측 버전(`metrics.ts`, `X-Image-Metrics`)을 배포한다.
- `local.properties`의 기존 Supabase URL/anon key를 사용한다. 환경 변수 `SUPABASE_URL`,
  `SUPABASE_ANON_KEY`로 덮어쓸 수 있다. 키를 커맨드 인자나 결과 파일에 적지 않는다.
- 기본은 비로그인 권한이다. `BENCHMARK_ACCESS_TOKEN`은 별도로 승인된 테스트 계정에만 쓴다.
  서비스 역할/secret key는 도구에서 거부한다.
- 아래 명령은 실제 서버에 요청한다. 기본은 방식별 6회이며 Storage 요청 예상 상한은 600건이다.
  매 표본 사이 4초를 둔다. 운영 부하 시험을 위한 도구가 아니다.

공개 프로필의 첫 리뷰 페이지를 anon 권한으로 읽어 개인용 입력 파일을 만든다.
사용자 ID·경로가 포함되는 입력 파일은 저장소 밖에 두고 커밋하지 않는다.

```sh
node tools/image-url-benchmark/capture-fixture.mjs PUBLIC_PROFILE_UUID /tmp/image-url-fixture.json
node tools/image-url-benchmark/benchmark.mjs \
  --fixture /tmp/image-url-fixture.json \
  --output /tmp/image-url-benchmark.json \
  --rounds 6 \
  --delay-ms 4000 \
  --max-storage-requests 600
```

`capture-fixture`는 조회만 하고, 닉네임·리뷰 본문을 입력 파일에서 제외한다. 기존 파일을 덮어쓰지 않는다.
고정된 입력 파일 하나를 모든 방식에 사용한다. 새 실행 때 내용이 바뀌었다면 결과의 `fixtureSha256`도 달라진다.
고정 프로필의 데이터는 변경/삭제될 수 있으므로, 같은 이미지 개수·중복률을 가진 검증 환경 자료가 장기 재현에 더 적합하다.

## 측정 규칙

1. Storage info GET 2회로 연결을 준비하고, 해당 시간도 원본 결과에 따로 보관한다.
2. 새 API의 첫 요청을 `preparation.initial`에 따로 보관한다. 캐시를 지우거나 강제 우회하지 않는다.
   `issued == unique`, `cacheHits == 0`인 경우에만 전체 캐시가 비어 있었다고 판정한다.
   비어 있지 않다면 기다렸다가 다시 측정하거나 혼합 상태로 기록한다.
3. 측정 순서를 여섯 가지로 순환해 항상 특정 방식만 먼저 실행하지 않게 한다.
4. 각 방식의 한 페이지 요청이 모두 끝날 때까지 걸린 시간을 `wallMs`로 기록한다.
   실패를 재시도하지 않으며, 오류나 Redis 우회가 발생하면 중단하고 부분 결과를 보존한다.
5. 캐시 재사용 표본은 `cacheHits == unique`, `issued == 0`, `cacheBypasses == 0`을 확인한다.
   측정 중 TTL이 지나 새로 발급했다면 `redis`라는 이름만 보고 적중 표본으로 분류하지 않는다.
6. 정렬된 표본의 중앙값을 p50으로, nearest-rank 방식을 p95로 기록한다.
   **6회 측정의 p95는 최댓값**이므로 안정적인 꼬리 지연 개선의 근거로 사용하지 않는다.

결과에는 Node 버전·실행 플랫폼·시각·기준 커밋·서버/측정 코드 해시·입력 해시·전체 표본을 보관한다.
경로·사용자 ID·서명 URL·API 키는 결과에 넣지 않는다. 이미지별 성공 여부는 반환 URL의 origin/path/token과
만료 필드를 확인하고, 실제 이미지 다운로드는 수행하지 않는다.

## 지표 읽는 법

- `clientHttpRequests`: 한 목록의 링크를 받기 위해 측정 클라이언트가 보낸 HTTP 요청 수.
- `signRequests`: Storage 서명 API에 보낸 횟수. 실패한 시도도 포함한다.
- `signSuccesses` / 서버 `issued`: 발급 성공 횟수.
- 서버 `operations.authorize.requests`: 캐시가 있더라도 실행한 현재 요청자의 권한/메타데이터 확인.
- 서버 `operations.redis.requests`: Redis REST 호출 수. Lua 내부 명령 수나 청구 단위와 같지 않다.
- 서버 `operations.*.durationSumMs`: 해당 종류의 요청 시간 **합계**. 병렬 작업이 겹치므로 전체 시간보다 클 수 있다.
- 서버 `totalMs`: 함수 핸들러 내부 시간. `wallMs`에는 네트워크·게이트웨이·실행 준비 시간도 포함된다.
- 서버 `requestId`: 함수 로그와 응답 계측을 대조할 때 쓰는 ID. 개인정보를 포함하지 않는다.

`legacy`에는 계측 헤더가 없으므로 클라이언트가 실제 보낸 요청을 센다. `redis`는 서버에서
실제 네트워크 호출 직전에 센 수치를 응답 헤더로 받는다. 동일 JSON을 함수 로그에도 남긴다.
현재 실측 및 한계는 [결과 보고서](../../docs/performance/image-url-cache/RESULTS.md)를 참고한다.

## 로컬 회귀 검증

운영 권한이나 Redis 설정을 바꾸지 않고 모의 네트워크 경계에서 캐시/잠금/권한/장애를 검증한다.

```sh
node --experimental-transform-types --test tools/image-url-benchmark/archived-functions/image-signed-urls/handler.test.mjs
./gradlew :data:testAndroidHostTest --console=plain
```

운영 적용 후의 성과는 같은 길이의 기간별로 `issued / unique`, 캐시 적중률, 실패율,
링크 준비 시간과 사용량을 함께 비교한다. 앱 버전·활성 사용자·화면 조회 수·사진 수가 달라진 효과를
구분한다. Disk IO는 같은 기간의 별도 DB 지표를 확보한 뒤에만 개선 여부를 판단한다.
