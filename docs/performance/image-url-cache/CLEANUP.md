# 이미지 캐시 운영 리소스 정리

확인일: 2026-10-12 (Asia/Seoul). 기본 Storage 배치 발급 채택 후 사용자의 미사용 리소스 삭제 요청과 최종 확인에 따라 정리했다.
앱 릴리스 완료를 의미하지 않는다.

## 삭제 전 의존성 확인

- 현재 앱 공통 소스, 플랫폼 앱, feature/shared/core/domain, CI, Supabase 함수·worker·설정에서 Redis/Upstash와 대상 함수 참조를 검색했다.
  런타임 참조는 종료 대상 두 함수에만 있었고, 앱의 Redis 호출 코드는 명시적 실행이 필요한 과거 비교 테스트에만 남았다.
- 운영 Edge Function 14개의 배포 소스를 모두 조회했다. 실제 Redis 설정을 읽는 함수는 `image-signed-urls`, `redis-connection-check` 두 개였다.
  `image-url-benchmark-f7003c2e`는 이미 HTTP 410만 반환하는 종료 코드였다. 나머지 11개에서 관련 참조를 찾지 못했다.
- 운영 `cron.job` 5개와 시스템 스키마 외 DB 함수 본문에서 Redis/Upstash/대상 함수명·엔드포인트 참조를 검사했다. 일치 없음.
- Upstash `Ramap`의 Data Browser는 빈 목록이었다. Monitor에서 과거 이미지 실험 namespace가 관찰됐다.
- 최근 24시간 Edge 요청 집계에서 이미지 함수 32회, 임시 벤치마크 함수 102회, 연결 확인 함수 0회를 확인했다.
  요청이 전혀 없었다고 판단한 것이 아니다. 직전 성능 실험이 포함된 수치이며 모든 요청의 호출자를 인증한 결과는 아니다.

이 확인은 현재 저장소와 조회한 운영 리소스에 한정된다. 외부 별도 저장소·배포 바이너리·잠복한 클라이언트까지 전수 확인한 것은 아니다.
구 Redis 실험 빌드를 실행하면 삭제된 함수 호출이 실패한다. 현재 수정된 앱은 Storage 기본 배치 API를 직접 호출한다.

## 정리 대상

| 종류 | 대상 | 상태 |
| --- | --- | --- |
| Edge Function | `image-signed-urls` | 삭제 확인 |
| Edge Function | `redis-connection-check` | 삭제 확인 |
| Edge Function | `image-url-benchmark-f7003c2e` | 삭제 확인 |
| Edge Secrets | `UPSTASH_REDIS_REST_URL`, `UPSTASH_REDIS_REST_TOKEN` | 삭제 확인 |
| Upstash Redis | Personal / `Ramap`, Sydney, ID `f1f2a897-daed-4cc6-bb00-0e3ca0e69cfe` | 삭제 후 Redis 목록 비어 있음 |
| 로컬 배포 설정 | 위 두 고정 함수의 `supabase/config.toml` 항목 | 제거 |
| 로컬 실험 소스 | 두 함수 디렉터리 | 배포 폴더 밖으로 이동·보존 |

Supabase 프로젝트, DB 데이터, Storage 이미지 파일·버킷·RLS는 이 정리에서 변경하지 않았다.
Upstash 계정과 다른 제품도 삭제하지 않았다. 해당 Redis는 Free Tier였으므로 유료 요금 절감액을 주장하지 않는다.

## 보존한 재현 근거

- [소스 보관](../../../tools/image-url-benchmark/archived-functions/README.md): 기존 9개 파일은 이동 전후 SHA-256이 같다.
- [Kotlin 적용 후 측정](APPLIED-BATCH.md), [대안 비교](CONTROLLED-COMPARISON.md), 기존 원본 JSON은 보존했다.
- 분석 도구는 기존 JSON을 계속 읽을 수 있다. 소스 경로를 참조하는 측정 도구 2개는 보관 경로로 변경했다.
  원본 결과의 측정 스크립트 해시는 **측정 당시 버전**을 뜻하며 경로 수정 후 파일 해시와 동일하다고 주장하지 않는다.
- 이전 네트워크 비교는 제거한 운영 함수·Redis를 필요로 하므로 그대로 재실행할 수 없다.
  재실험은 별도로 승인된 검증 환경에서 한다. 리소스를 자동 재생성하지 않는다.

## 사후 확인

운영 함수 목록에서 대상 3개 부재와 나머지 11개 존속을 확인했다.
Secrets 삭제에 따라 다른 함수들의 배포 버전 번호가 증가했지만 코드 해시·함수 ID·JWT 검증 설정은 유지됐다.
Secrets 화면에서 전용 2개 제거와 나머지 설정 보존, Upstash 목록에서 DB 부재를 확인했다.
로컬 위키 링크 검사와 `git diff --check`를 수행했다. 이번 정리에서는 실서비스 벤치마크나 앱 테스트를 재실행하지 않았다.
