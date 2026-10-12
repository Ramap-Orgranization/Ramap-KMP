# Redis 서버 연결 확인

Supabase Edge Function에서 Upstash Redis에 접속하고 값을 저장한 뒤 읽을 수 있는지 확인한다.

## 설정

운영 프로젝트의 Edge Function Secrets에 다음 값을 등록한다. 실제 값은 소스에 저장하지 않는다.

- `UPSTASH_REDIS_REST_URL`
- `UPSTASH_REDIS_REST_TOKEN`

## 호출

`POST /functions/v1/redis-connection-check`는 Supabase 플랫폼의 JWT 검증을 유지한다.
추가로 `x-redis-connection-token` 헤더에 저장소의 접속키를 전달해야 하므로 이 키를 가진 운영자만 호출할 수 있다.
Redis 접속키는 앱에 넣지 않으며, 일반 앱 사용자는 이 함수에 접근할 수 없다.

정상 응답은 `{"configured":true,"connected":true,"readWrite":true}`다.
매 호출은 Redis에서 `PING`, `SET`, `GET`을 한 번씩 실행한다.
확인에 사용한 값은 별도 임의 경로에 저장하고 15초 뒤 자동 만료된다.
접속키와 저장된 이미지 링크는 응답이나 로그에 포함하지 않는다.

이 함수는 서버 연결을 확인한다. 이미지 서명 URL의 발급·캐싱과 앱 연결은
[image-signed-urls](../image-signed-urls/README.md)에서 처리한다.
