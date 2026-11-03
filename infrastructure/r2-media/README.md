# 도메인 구매 없는 R2 사진 저장소

DB·로그인·사진 권한은 Supabase, 사진 제어 API와 30분 정리 작업은 기존 서울 Cloud Run을 유지한다. Firebase FCM·Analytics·Crashlytics도 유지한다. 사진 업로드와 비공개 사진 읽기는 앱과 R2 사이에서 직접 처리한다. 공개 사진은 Cloudflare Worker가 R2 파일을 스트리밍한다.

R2 연결 전 기본 동작은 Supabase Storage다. 파일 추가나 로컬 검증만으로 운영 사진 저장소가 전환되지 않는다.

## 주소와 비용

도메인을 구매하지 않는다. Worker 배포 후 Cloudflare가 실제로 발급한 `https://ramap-images.<계정 하위 이름>.workers.dev`를 사용한다. `ramap.com`이나 DNS zone, Cache Purge 토큰이 필요 없다. `r2.dev` 개발 주소는 사용하지 않으며, R2 버킷 두 개 모두 직접 공개 접근을 끈다. 공개 사진 Worker에 공개 사진용 버킷 하나만 연결한다.

Worker는 사진 바이트를 그대로 스트리밍하고 ETag/304와 단말 캐시를 제공한다. 공유·edge 캐시는 사용하지 않아서 새 네트워크 요청마다 R2 존재 여부를 확인한다. R2 삭제 후 별도 CDN purge 없이 404가 되며, 이미 단말에 저장된 사진은 원격으로 회수할 수 없다. 이 방식의 지연을 custom-domain CDN cache hit와 동일하다고 간주하지 않는다.

도메인 비용과 별개로 Workers·R2·Cloud Run 사용량은 발생한다. Workers Free는 계정 내 합산 하루 10만 요청과 요청당 CPU 10ms 한도이며 초과 시 오류가 발생할 수 있다. 자동으로 유료 Workers 플랜에 가입하지 않는다. R2 Standard의 무료 사용량을 초과하면 해당 계정의 R2 구독 조건에 따라 과금될 수 있다. `workers.dev`는 개인·취미 프로젝트 용도이며 Cloudflare는 중요 운영 서비스에 자체 도메인 사용을 권장한다. [Workers 주소](https://developers.cloudflare.com/workers/configuration/routing/workers-dev/), [Workers 요금](https://developers.cloudflare.com/workers/platform/pricing/), [R2 요금](https://developers.cloudflare.com/r2/pricing/)

## 계정과 버킷 준비

Cloudflare 이메일 인증과 R2 구독 활성화가 필요하다. 구독 화면의 약관·사용량 과금 조건은 사용자가 확인한다. `config.example.json`을 Git 외부에 복사해 account ID와 두 버킷 이름을 넣는다. `public_delivery`의 기본값은 `worker`다. 배포 전 `r2_public_base_url`은 비워 둔다.

설정용 API 토큰은 로컬 환경의 `CLOUDFLARE_API_TOKEN`에만 둔다. R2 S3 키는 두 버킷에 한정된 Object Read & Write 권한으로 발급한다. 키를 앱이나 설정 JSON에 넣지 않는다. 두 버킷은 R2 Standard와 APAC 위치 힌트를 사용한다. APAC은 서울 리전 보장이 아니다. [R2 위치](https://developers.cloudflare.com/r2/reference/data-location/)

```bash
python3 infrastructure/r2-media/provision.py --config /absolute/local/r2.json
python3 infrastructure/r2-media/provision.py --config /absolute/local/r2.json --apply
```

## 공개 사진 Worker

```bash
npm --prefix services/public-media-worker ci
npm --prefix services/public-media-worker test
python3 infrastructure/r2-media/deploy_worker.py --config /absolute/local/r2.json
python3 infrastructure/r2-media/deploy_worker.py --config /absolute/local/r2.json --apply --output /absolute/local/r2-connected.json
```

마지막 명령은 로컬 Wrangler 인증 또는 환경 API 토큰을 사용해 배포한다. 실제로 반환된 `workers.dev` URL만 출력 설정에 기록한다. 가상의 계정 이름을 운영 URL로 채우지 않는다. 전용 Worker 이름은 `public_worker_name`으로 바꿀 수 있다. Worker 바인딩은 `PUBLIC_IMAGES` 한 개이며 비공개 버킷·Supabase 키·임의 URL 프록시·업로드·삭제·목록 기능은 제공하지 않는다. 기본 Wrangler 설정은 로컬 개발용이며, 운영 배포에는 계정 연결 설정을 사용한다.

## Cloud Run 배포와 호환성

로컬 환경에 `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`를 설정한 뒤 GCP Secret Manager에 저장한다. 값은 인수·출력에 포함하지 않는다. Workers 방식에는 DNS zone이나 purge secret이 필요 없다.

```bash
python3 infrastructure/r2-media/provision.py --config /absolute/local/r2-connected.json --store-secrets
```

1. SQL migrations `20261102232823_r2_media_storage.sql`, `20261103000013_remove_gcs_media_runtime.sql`, `20261103000708_scope_media_copy_to_used_photos.sql`, `20261103002509_optimize_used_photo_inventory.sql`, `20261103003005_fast_path_media_copy_eligibility.sql`을 순서대로 검증·적용한다. 이전 저장소 전환은 진행 중 업로드·삭제 작업이 없고 지원되는 복사본이 존재할 때만 적용된다. 실제 원본 파일을 삭제하지 않는다. R2 읽기·쓰기는 기본 비활성이다.
2. 서버와 worker를 같은 이미지·secret으로 배포한다. Cloud Run 기본 0개 인스턴스, 최대 2개, 정리 주기 30분을 유지한다.

```bash
python3 infrastructure/r2-media/deploy.py --apply --redeploy-only --r2-config /absolute/local/r2-connected.json --output /absolute/local/media-deployment.json
```

3. R2 연결이 설정된 앱은 `/v2`를 사용하고, 미설정 앱은 Supabase Storage를 직접 사용한다. 구버전 `/v1`에는 Supabase 주소만 반환한다. 앱 BuildKonfig에 공개 정보인 `media.r2_account_id`, `media.r2_private_bucket`을 지정한다. 공개 사진 검증 전 `media.r2_public_base_url`은 비워 Supabase 공개 URL을 사용한다.

앱 빌드에는 `media.api_url`, `media.r2_account_id`, `media.r2_private_bucket`, `media.r2_public_base_url`을 ignored `local.properties`에 지정하거나 각각 `MEDIA_API_URL`, `R2_ACCOUNT_ID`, `R2_PRIVATE_BUCKET`, `R2_PUBLIC_BASE_URL` 환경 변수로 주입한다. Android CI는 같은 이름의 GitHub repository variables를 사용한다. 이 값들은 공개 연결 정보이며 S3 접근 키와 서비스 역할 키는 포함하지 않는다.
4. Edge `preview-event`·`register-event` 환경에도 `R2_ACCOUNT_ID`, `R2_PRIVATE_BUCKET`을 지정하고 `_shared/media-storage.ts`를 함께 배포한다. 계정 탈퇴는 기존 API로 모든 provider 정리를 확인한다.
5. 새 업로드는 prepare 시 provider와 필요한 호환 복사를 고정한다. R2 업로드는 필요한 Supabase 호환 복사가 끝나야 ready다. v1 업로드는 Supabase를 사용하므로 신규분을 inventory에 포함해 복사한다. 운영 수동 등록은 기존 `infrastructure/r2-media/upload.py`를 사용한다. Storage 직접 쓰기를 계속 사용하는 공개 writer가 있으면 사진 주소 전환을 완료할 수 없다.

공개 사진 주소는 `R2_PUBLIC_BASE_URL`, `R2_PUBLIC_DELIVERY=worker`로 설정한다. 외부 네이버 사진은 그대로 둔다.

## 기존 사진과 신규분 복사

inventory는 Supabase 원본과 논리 registry의 합집합이며 복사 원본은 Supabase다. 현재 참조와 기존 이벤트 종료 규칙을 확인해 종료된 이벤트 사진·미참조 파일은 제외한다. 오늘 종료·미래 이벤트와 현재 데이터에서 재사용하는 사진, 신고·제보 증거는 유지한다. inventory의 migration_needed/migration_reason으로 대상·제외 사유와 대상 바이트를 집계하며, 복사 시작 전 서버와 예약 RPC에서도 다시 검사한다. 제외 원본은 삭제하지 않는다. 지원 저장소에 원본이 없는 파일은 누락시키지 않고 오류로 남긴다. 도구는 원본 삭제와 설정 활성화를 수행하지 않는다. manifest는 개인정보가 포함될 수 있어 Git 외부에 권한 0600으로 저장한다.

```bash
python3 infrastructure/r2-media/migrate.py --config /absolute/local/media-deployment.json
python3 infrastructure/r2-media/migrate.py --config /absolute/local/media-deployment.json --copy --manifest /absolute/local/r2-manifest.jsonl
```

목적지를 쓰기 전에 registry에 등록하고, 실제 목적지 바이트의 크기·MIME·SHA-256을 검증한다. 원본 timestamp가 바뀌거나 삭제 tombstone이 있으면 확정하지 않는다. R2 ETag는 체크섬과 별개다. 복사 중 들어온 신규분이 없어질 때까지 inventory와 복사를 반복한다. Cloud Run 검증·호환 복사에는 기존 클라우드의 전송 비용이 남는다. 자동 리사이즈·품질 변경은 하지 않는다.

## 검증과 활성화

실제 Cloudflare 자격 증명이 없으면 로컬 테스트를 실제 R2 검증으로 간주하지 않는다. 전용 staging 두 버킷으로 검사한다.

```bash
R2_SMOKE=staging node services/media-api/r2-smoke.mjs
```

별도로 staging API에서 Supabase 소유권/팔로우/차단/신고 정책, 실제 크기 제약의 거절, 공개 Worker GET·HEAD·304·비공개 경로 거절·삭제 후 404, 업로드 중 삭제·탈퇴를 검증한다. 한국 네트워크에서 같은 사진의 Supabase/Worker 최초·재조회 지연을 측정한다.

검증 후 service role에서 `configure_r2_media(true,false)`로 v2 비공개 읽기를 먼저 확인한다. 이후 `configure_r2_media(true,true)`로 v2 쓰기와 Supabase 호환 복사를 활성화한다. v1의 Supabase 직접 업로드는 inventory로 확인해 추가 복사한다. 구버전 지원 중 legacy mirror는 true를 유지한다. 공개 사진 누락 0건과 모든 공개 writer의 R2 복사를 확인한 후 앱 `media.r2_public_base_url`을 실제 Worker 주소로 설정해 배포한다. 공개 주소는 앱에 고정되므로 DB 읽기 플래그만으로 이미 배포된 주소를 되돌릴 수 없다.

## 삭제와 복구

사진 삭제는 논리 접근을 먼저 막고 모든 provider의 삭제를 재시도한다. pending PUT 유효 기간과 진행 중 finalize가 끝날 때까지 물리 삭제를 유예하며, 이 상태를 탈퇴 완료로 응답하지 않는다. 정리 작업은 30분마다 실행한다. 공개 Worker는 edge 캐시 없이 R2를 읽으므로 purge 토큰을 요구하지 않는다.

`configure_r2_media(false,false)`로 새로운 R2 선택을 중단하고 R2/Supabase 호환 앱·API를 유지한다. Worker URL이 배포됐다면 Worker와 객체 제공도 유지한다. 원본 보존 종료는 별도 결정이다. Supabase 원본의 일반 삭제는 전체 논리 사진 삭제로 전파되므로 단순 일괄 삭제하지 않는다.

기존 자체 도메인 방식도 `public_delivery=custom_domain`으로 지원하지만, 그 경우 소유 도메인·Cloudflare zone·Cache Purge 토큰이 필요하다. Worker 방식으로 바꾸며 이미 배포된 자체 도메인을 끄는 작업은 별도 검토한다.

사진용 Firebase/GCS SDK·버킷 생성·Rules 배포·읽기·쓰기·미러는 제거했다. 적용된 SQL 이력과 이전 위치 기록은 보존한다. 이전 저장소 주소가 고정된 앱은 새 공개 사진 주소를 지원하는 앱으로 업데이트해야 한다. Firebase 알림·분석·오류 수집은 별도 기능으로 유지한다.
