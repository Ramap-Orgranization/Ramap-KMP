---
authority: canonical
status: active
last_verified: 2026-10-11
---

# 결정 기록

정책의 도입·변경·폐기 이유를 남깁니다. 현재 내용은 분야별 문서에서 읽습니다. 새 항목은 사용하지 않은 다음 `DEC-NNNN` ID로 추가하고 이전 항목은 보존합니다. 경로 수정이나 구현 사실의 단순 갱신에는 결정이 필요하지 않습니다.

## DEC-0001 Ramap LLM 위키와 작업 내 유지보수 도입

- 기록일: 2026-10-11 (Asia/Seoul)
- 결정: 공유 위키를 `docs/wiki/`에 두고 코드 탐색 문서·유지보수 정책·결정 기록·미확정 후보를 구분합니다. 기존 개발 규칙의 소유권은 Ramap 스킬에 유지합니다.
- 이유: 작업에 필요한 근거를 빠르게 찾고 세션의 확정 지식을 보존하면서 로컬 운영 자료와 규칙의 중복을 줄입니다.
- 근거: 이번 세션의 사용자 요청인 “우리 앱의 LLM 위키와 유지보수 정책을 구축”과 기존 로컬 `AGENTS.md`의 스킬 소유권 지침. 구조 참고: [YAPP LLM Wiki](https://github.com/YAPP-admin/yapp-plus-android/tree/develop/docs/wiki).
- 반영: [목차](README.md), [유지보수 정책](maintenance.md), [후보함](inbox/README.md), [PR 템플릿](../../.github/PULL_REQUEST_TEMPLATE.md), [검사 workflow](../../.github/workflows/wiki.yml), 로컬 `AGENTS.md`.
- 적용 상태: 문서와 로컬 검사·CI 설정을 추가했습니다. 원격 CI 실행과 branch protection 설정, 배포를 완료했다는 의미는 아닙니다.
- 대체한 결정: 없음.

## DEC-0002 현재 구현을 기준으로 개발 규칙 정합성 확정

- 기록일: 2026-10-11 (Asia/Seoul)
- 결정: 미확정이었던 모듈 목록, resource mapper 소유권, domain의 analytics 의존성을 현재 구현 기준으로 확정합니다. settings에 없는 profile/follow 표기를 제거하고, core/ui와 core/designsystem의 mapper 분담 및 domain → core/analytics의 공통 API 사용을 소유 스킬에 반영합니다.
- 이유: 이후 작업에서 실제 모듈과 다른 경로를 찾거나 공통 mapper를 잘못 이동하지 않도록 개발 규칙과 코드를 일치시킵니다.
- 근거: 사용자의 “미확정은 모두 현재 기준으로 맞추고 최신화” 요청, [settings](../../settings.gradle.kts), [domain build](../../domain/build.gradle.kts), [core/ui build](../../core/ui/build.gradle.kts), [designsystem build](../../core/designsystem/build.gradle.kts), [SignInUseCase](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/usecase/SignInUseCase.kt), [RankingFilters](../../feature/main/ranking/src/commonMain/kotlin/com/peto/ramap/ui/main/ranking/component/RankingFilters.kt).
- 반영: 로컬 `ramap-architecture`, `ramap-module-structure`, `ramap-design-patterns` 스킬, [아키텍처](architecture.md), [모듈 구조](module-structure.md), [디자인시스템](design-system.md), [승격한 후보](inbox/source-alignment.md).
- 적용 상태: 소유 스킬과 위키에 반영했습니다. 앱 코드의 동작과 모듈 배치는 변경하지 않았습니다.
- 대체한 결정: 없음. DEC-0001의 스킬 소유권 원칙을 유지하며 초기 후보를 해소합니다.


## DEC-0003 이미지 링크 기본 배치 발급 채택

- 기록일: 2026-10-12 (Asia/Seoul).
- 결정: 프로필·리뷰 이미지 URL은 경로 중복 제거 후 버킷별 Storage 기본 배치 API로 발급한다. 앱의 이미지 발급에서 Edge·Redis 의존성을 제외하고 호출자의 JWT와 기존 RLS를 유지한다.
- 이유: 기본 배치와 Redis 없는 Edge 배치가 현재 공유 캐시보다 빨랐으며, 캐시 적중에도 이미지별 권한 조회가 남아 있었다. 추가 운영 복잡성을 정당화할 이득이 확인되지 않았다.
- 근거: 사용자의 “Redis 빼고 1번 기본 배치 발급 방식으로 앱 수정…기존 권한 검증 유지…적용 후 성능 비교” 요청, [대안 비교](../performance/image-url-cache/CONTROLLED-COMPARISON.md), [Kotlin 적용 후 비교](../performance/image-url-cache/APPLIED-BATCH.md).
- 반영: [이미지 발급 탐색](image-url-cache.md), [백엔드](backend.md), [처리 후보](inbox/signed-image-cache-evidence.md), 공통 데이터 소스와 테스트.
- 적용 상태: 로컬 앱 코드 적용 및 검증 완료. Kotlin HTTP 측정 p50 669.756→192.056ms. 앱 릴리스·실기기 UI·Disk IO 개선은 미검증. 후속 사용자 요청과 삭제 확인에 따라 미사용 운영 캐시·연결 확인·임시 벤치마크 함수, Upstash DB, 전용 Secrets를 제거했다. [정리 근거와 범위](../performance/image-url-cache/CLEANUP.md), 과거 소스와 측정 원본은 보존한다.
- 대체한 결정: 없음. 기존 Redis 구현은 확정된 팀 정책으로 등록되지 않았다.
