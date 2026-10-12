# 프로필·리뷰 이미지 임시 링크 캐시

> 2026-10-12: 앱은 기본 배치 발급으로 전환했다. 이 함수는 기존 빌드와 성능 비교의 기준용으로 보존되며 새 앱 이미지 경로에서 호출하지 않는다. [적용 기록](../../../docs/performance/image-url-cache/APPLIED-BATCH.md)을 참고한다.

`image-signed-urls`는 KMP 앱의 이미지 경로를 받아 Supabase Storage의 서명 URL을 반환한다.
사진 파일은 Storage에 그대로 두고, **링크와 실제 만료 시각만 Redis에 저장**한다.

## 요청과 응답

```http
POST /functions/v1/image-signed-urls
Authorization: Bearer <현재 사용자 access token 또는 프로젝트 anon JWT>
apikey: <프로젝트 클라이언트 API key>
Content-Type: application/json
```

```json
{
  "images": [
    {
      "bucket": "profile-avatars",
      "path": "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002.jpg"
    }
  ]
}
```

응답 `images`의 각 항목에는 `bucket`, `path`, `url`, `expiresAt`, `error`가 들어간다.
`expiresAt`은 **Unix epoch 밀리초**다. 권한이 없거나 파일이 없으면 `url`과 `expiresAt`은
`null`, `error`는 `image_unavailable`이다. 일시적인 오류는 `image_temporarily_unavailable`이다.
같은 요청 안의 중복 이미지는 하나의 응답 항목으로 합친다.

## 접근 권한

- 플랫폼의 `verify_jwt = true`를 유지한다. 비로그인은 기존 프로젝트의 anon JWT를 사용한다.
- 캐시 조회 **전에 매 요청자의 JWT로** Storage의 인증된 object info API를 호출한다.
  기존 `storage.objects` SELECT RLS를 통과하고 파일의 현재 버전을 확인한 경우에만 반환한다.
- 새 링크 발급도 동일한 요청자의 JWT로 수행한다. `service_role`을 사용하지 않는다.
- 허용 버킷은 `profile-avatars`, `shop-review-images` 두 개다. 현재 업로드 규칙인
  `<user UUID>/<image UUID>.jpg|png`만 받는다. 요청은 최대 64개, 본문은 최대 20,000바이트다.
- 캐시 응답에 대한 접근 권한을 별도로 저장하거나 공유하지 않는다. 다른 사용자에게
  같은 링크를 반환할 때에도 해당 사용자의 현재 RLS를 다시 평가한다.
- 이미 받은 링크는 남은 유효 기간 동안 접근 가능한 기존 signed URL 특성을 가진다.
  차단·비공개 전환은 이후 API 요청의 권한 검사에 반영되며, 발급된 링크를 즉시 회수하지 않는다.

## 캐시와 동시 요청

- 링크 유효 기간: **5분**. 기존 리뷰·커뮤니티 사진의 발급 기간과 같다.
- 만료 **1분 전**부터 재사용하지 않고 새 링크를 발급한다. Redis 항목도 그 시각에 만료한다.
- 키에는 프로젝트 origin, 버킷, 경로, object ID, 버전, 최종 수정 시각, 유효 기간을 포함한다.
  키 본문은 SHA-256으로 해시한다. 덮어쓰기·삭제 후 같은 경로에 재업로드하면 이전 항목을 사용하지 않는다.
- 서버 인스턴스 간에는 Redis `SET NX EX`로 경로/버전별 발급 권한을 하나만 얻는다.
  잠금 수명은 10초, Storage 요청 제한은 3초다. 다른 요청은 최대 5.5초 동안 결과를 기다린다.
- 잠금 소유자 확인과 캐시 저장/잠금 해제는 Redis Lua 명령으로 수행한다.
  오래된 작업이 새로운 소유자의 캐시나 잠금을 덮어쓰거나 지우지 않는다.
- 잠금 만료·퇴출·복제/장애 전환·Redis 장애 시 중복 발급이 생길 수 있다.
  이 잠금은 중복 억제 수단이며 정확히 한 번 발급이나 Redlock을 보장하지 않는다.
- 정상 Redis 잠금 대기가 끝나면 일시적인 오류를 반환하며 중복 발급하지 않는다.
- Redis 설정 누락·접속 장애·한도 초과 시에는 요청자 권한으로 Storage에서 직접 발급한다.
  이 경우 사진 표시를 유지하지만 캐시 재사용과 중복 발급 방지는 보장하지 않는다.
  해당 함수 요청 안에서는 Redis 연결 실패 후 남은 사진마다 접속을 재시도하지 않는다.
- 사진 처리는 함수 요청당 최대 8개를 동시에 수행한다.

## 앱 연결

`data.datasource.image.RemoteImageUrlDataSource`가 현재 Supabase 세션으로 함수를 호출한다.
리뷰 목록의 이미지와 작성자 사진을 모아 중복을 제거하고 64개씩 묶어 요청한다.
앱의 동시 API 요청은 최대 2개이며, 함수는 Storage와 Redis가 있는 시드니 리전에서 실행한다.
내 프로필, 다른 사용자 프로필, 차단 사용자 목록, 리뷰 목록/상세/편집의 이미지가 모두 연결된다.
기존 내 프로필의 메모리 캐시는 반환받은 `expiresAt - 1분`까지 재사용한다.
기존의 수신 시점 기준 29분 재사용을 제거해, 서버에서 재사용한 링크를 새 링크로 오인하지 않는다.
내 프로필 전용 발급 기간도 기존 30분에서 공통 5분으로 통일된다.
앱 세션 사용자가 응답을 기다리는 도중 바뀌면 결과를 반환하지 않는다.

함수는 운영 서버에 배포할 수 있지만, 기존 배포 앱은 이전 코드를 사용한다.
앱 연결은 이 변경이 포함된 Android/iOS 빌드부터 적용된다.

## 서버 설정

기존 서버 비밀 설정을 사용한다.

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `UPSTASH_REDIS_REST_URL`
- `UPSTASH_REDIS_REST_TOKEN`

Redis 키나 관리자 키를 앱 설정에 추가하지 않는다. DB 마이그레이션/RLS 변경은 필요하지 않다.
서버 코드는 외부 패키지 없이 Web API와 Redis REST 명령을 사용한다.

## 요청 횟수를 확인하는 방법

함수 로그 `event=image_signed_urls`와 응답 `X-Image-Metrics`에는 다음 개수와 처리 시간을 기록한다.
`schemaVersion=1`, 임의의 `requestId`로 원본 표본과 로그를 대조할 수 있다.

- `requested`: 받은 이미지 수
- `unique`: 중복을 제외한 이미지 수
- `cacheHits`: 기존 링크 재사용 수
- `issued`: 실제 Storage 서명 발급에 성공한 수
- `cacheBypasses`: Redis를 사용할 수 없거나 저장하지 못한 수
- `denied`: RLS가 거부하거나 파일이 없는 수
- `failed`: 일시적인 실패 수
- `lockWaits`: 다른 요청의 발급 완료를 기다린 수
- `operations.authorize`: Storage 권한/정보 조회 시도·실패 수·시간 합계
- `operations.sign`: Storage 서명 발급 시도·실패 수·시간 합계
- `operations.redis`: Redis REST 호출·실패 수·시간 합계
- `totalMs`: 핸들러 내부 전체 처리 시간 (`Server-Timing`에도 포함)

경로, 사용자 ID, 링크, 비밀 키는 계측에 기록하지 않는다. `issued`는 성공 수이고,
`operations.sign.requests`는 실패를 포함한 발급 시도 수다. 병렬 작업의 `durationSumMs`는
시간 합계여서 `totalMs`보다 클 수 있다. Redis 호출 수는 청구 단위를 뜻하지 않는다.
운영 설정을 변경하는 벤치마크 모드나 캐시 강제 우회 API는 제공하지 않는다.

[비교 도구](../../../tools/image-url-benchmark/README.md)와
[실측 결과](../../../docs/performance/image-url-cache/RESULTS.md)에 재현 조건과 전체 표본을 남긴다.
추가 검증 시 아래 시나리오를 실행해 응답 계측과 함수/Storage 로그를 대조한다.

1. 접근 가능한 같은 사진을 차가운 캐시에서 연속 요청: 최초 발급 이후 `cacheHits`가 증가해야 한다.
2. 같은 사진을 여러 기기/사용자가 동시에 요청: 정상 잠금 기간에는 같은 링크를 반환하고 발급은 하나여야 한다.
3. 같은 작성자의 리뷰 20개 표시: 작성자 사진이 한 번만 포함되고 `unique`가 줄어야 한다.
4. 만료 1분 전 경계: 이전 링크 대신 새 링크를 반환하고 반환된 `expiresAt`도 갱신되어야 한다.
5. 공개 사진을 비로그인으로 요청: 현재 RLS가 허용하면 캐시/신규 링크 모두 표시되어야 한다.
6. 캐시를 채운 뒤 접근 권한을 잃은 사용자로 요청: 캐시 존재 여부와 무관하게 URL이 없어야 한다.
7. 사진 삭제/덮어쓰기: 삭제한 사진은 반환하지 않고 다른 버전은 다른 키를 사용해야 한다.
8. Redis 장애: 권한이 있는 사진은 직접 발급하고 `cacheBypasses`가 증가해야 한다.
9. 리뷰 이미지 일부가 실패: 내 리뷰/편집은 기존 순서에 `null`을 보존하고 공개 리뷰는 표시 가능한 사진을 유지해야 한다.

권한 변경·장애 시나리오는 개발/검증 환경에서 수행한다. 운영 데이터나 Redis 설정을
검증 목적으로 변경하지 않는다. 이 캐시는 **서명 발급 횟수를 줄이는 기능**이며,
매 요청의 권한 조회와 다른 DB 작업까지 없애거나 Disk IO 경고 해결을 보장하지 않는다.

참고: [Storage info](https://supabase.com/docs/reference/javascript/storage-from-info),
[Storage RLS](https://supabase.com/docs/guides/storage/security/access-control),
[Redis REST API](https://upstash.com/docs/redis/features/restapi).
