---
authority: reference
status: active
last_verified: 2026-10-12
source_ref: cf916e7b + working-tree
---

# 프로필·리뷰 이미지 서명 URL

프로필과 리뷰 이미지는 공통 데이터 계층의 [RemoteImageUrlDataSource](../../data/src/commonMain/kotlin/com/peto/ramap/data/datasource/image/RemoteImageUrlDataSource.kt)에서 서명 URL을 발급합니다. 이 페이지는 코드 구현을 설명하며 운영 배포 상태를 뜻하지 않습니다.

- 버킷과 객체 경로를 요청 키로 사용하고 중복 경로를 제거합니다.
- 버킷별로 최대 64개씩 묶어 Supabase Storage 기본 배치 발급 API를 호출합니다. 입력이 커질 때 `async`가 배치별 작업을 동시에 만들기 때문에 `Semaphore(2)`로 실제 Storage 요청을 최대 두 개로 제한합니다. 회귀 테스트에서 130개 입력은 64·64·2개 세 요청으로 나뉘고, 관찰된 최대 동시 요청이 2개인지 확인합니다. 두 개라는 값은 측정된 최적값이 아니라 요청 폭주를 막는 보수적 상한이며, 현재 검증은 동시성 제한만 보장하고 최적 상한이나 운영 성능 향상을 입증하지 않습니다.
- 현재 Supabase 세션의 JWT를 사용하므로 Storage의 기존 접근 정책이 적용됩니다. 프로필 소유자 경로 검증은 기존 데이터 소스에 남아 있습니다.
- 응답의 누락·부분 오류는 해당 이미지 실패로 처리하고, 코루틴 취소는 다시 전달합니다. 발급 도중 계정이 바뀌면 결과를 반환하지 않습니다.
- URL 만료 시각은 발급 시각과 설정한 수명을 기준으로 계산합니다. 내 프로필 아바타의 메모리 재사용은 기존 `AvatarCache`에서 처리합니다.
- 앱 경로에는 Redis나 Edge Function 호출이 없습니다.

회귀 테스트는 [RemoteImageUrlDataSourceTest](../../data/src/commonTest/kotlin/com/peto/ramap/data/datasource/image/RemoteImageUrlDataSourceTest.kt)에서 요청 그룹화, 버킷 분리, 실패 및 세션 변경을 확인합니다. 구현 선택은 [DEC-0003](decisions.md#dec-0003-프로필리뷰-이미지-url-기본-배치-발급-채택)에 기록했습니다.
