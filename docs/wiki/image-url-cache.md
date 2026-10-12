---
authority: reference
status: active
last_verified: 2026-10-12
source_ref: 44f6d4ac2952f9d77766b0a19bf8618f5af374c2 + working-tree
---

# 프로필·리뷰 이미지 링크 발급

사용자가 기본 배치 발급 전환을 승인한 뒤 공통 데이터 계층에 적용했다. 앱 배포 완료를 뜻하지 않는다.
결정 근거는 [DEC-0003](decisions.md#dec-0003-이미지-링크-기본-배치-발급-채택)에 있다.

## 현재 앱 경로

- [RemoteImageUrlDataSource](../../data/src/commonMain/kotlin/com/peto/ramap/data/datasource/image/RemoteImageUrlDataSource.kt):
  경로 중복 제거, 버킷별 64개 묶음, 최대 2배치 동시 실행으로 Storage 기본 batch sign API 호출.
  프로필·리뷰 두 버킷과 UUID 이미지 경로만 허용한다. Edge·Redis 호출은 없다.
- 인증은 SDK의 `authenticatedSupabaseApi`를 통해 현재 JWT/anon을 사용한다. 발급 API의 기존 Storage SELECT RLS가 적용된다.
  두 운영 버킷은 비공개임을 읽기로 확인했다. 정책·마이그레이션은 변경하지 않았다.
- [StorageSignedImageResponse](../../data/src/commonMain/kotlin/com/peto/ramap/data/model/StorageSignedImageResponse.kt):
  nullable URL과 항목별 오류를 처리한다. SDK 3.6.0의 non-null `SignedUrl` 모델 문제를 한 데이터 소스에서 보완한다.
- 누락·중복 응답·잘못된 링크는 해당 이미지 null, 버킷 요청 실패는 해당 배치 null. 취소를 전파하고 계정 변경 결과는 폐기한다.
- 링크 수명은 현재 5분. 요청 전 시각으로 만료를 계산한다.
  [AvatarCache](../../data/src/commonMain/kotlin/com/peto/ramap/data/repository/AvatarCache.kt)의 내 프로필 재사용은 기존처럼 만료 1분 전까지다.
- 검증: [공통 데이터 소스 테스트](../../data/src/commonTest/kotlin/com/peto/ramap/data/datasource/image/RemoteImageUrlDataSourceTest.kt),
  [Kotlin 실제 HTTP 비교](../../data/src/androidHostTest/kotlin/com/peto/ramap/data/datasource/image/ImageUrlLiveBenchmarkTest.kt).
  실제 네트워크 테스트는 명시적 환경 변수로만 실행하며 일반 CI에서는 건너뛴다.

## 성능 근거와 한계

[적용 후 보고서](../performance/image-url-cache/APPLIED-BATCH.md): 동일 22개 이미지의 Kotlin 데이터 소스를
각 30회 실행한 Android host JVM HTTP 측정에서 기존 Redis 적중 p50 669.756ms, 기본 배치 192.056ms였다.
p50 71.3%, 페이지당 Storage HTTP 22→2로 90.9% 감소했다. 실기기 UI·이미지 다운로드·Disk IO 감소는 미측정이다.

[이전 대안 비교](../performance/image-url-cache/CONTROLLED-COMPARISON.md)의 Node 수치와 섞어 전후 개선율을 계산하지 않는다.
[초기 파일럿](../performance/image-url-cache/RESULTS.md)의 47.6%는 여러 변경을 합친 효과였으며 Redis 단독 효과가 아니다.

2026-10-12 사용자가 미사용 리소스 정리를 승인했고, 이미지 캐시·연결 확인·임시 벤치마크 함수와 Upstash DB 및 전용 Secrets를 삭제했다.
의존성 확인 범위·한계와 사후 확인은 [정리 기록](../performance/image-url-cache/CLEANUP.md)에 있다.
과거 소스는 배포 폴더 밖의 `tools/image-url-benchmark/archived-functions/`에 보존했다. 삭제된 운영 리소스를 전제로 한 네트워크 비교는 그대로 재실행할 수 없다.
[기존 캐시 설계 기록](../performance/image-url-cache/DESIGN-RATIONALE.md)은 역사적 구현 설명이다.
[검토 후보](inbox/signed-image-cache-evidence.md)는 사용자 승인과 적용에 따라 처리했다. 다른 부하 조건에 대한 일반화는 여전히 제한된다.
