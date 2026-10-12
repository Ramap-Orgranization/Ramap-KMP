---
authority: reference
status: active
last_verified: 2026-10-13
source_ref: c559e52c3248076dacab2f2928622d9d787bc1e5
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

## 리뷰 변경에서 URL 유지

이번 변경 범위인 [RemoteReviewDataSource](../../data/src/commonMain/kotlin/com/peto/ramap/data/datasource/review/RemoteReviewDataSource.kt)는 리뷰 조회로 얻은 URL을 세션별로 최대 512개 보관하고, 만료 여유가 1분 이하인 경로와 새 경로만 공통 서명 발급기에 요청합니다. 계정 epoch 변경, 기록 공백과 해당 작성자의 접근 변경은 보관 URL을 폐기합니다. 일부 새 사진의 발급이 실패하면 성공한 경로를 유지한 채 같은 리뷰 receipt의 미해결 경로를 재시도합니다. 전체 이미지 발급기의 기본 배치 정책은 유지합니다.

[MyReviewPhoto](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/review/MyReviewPhoto.kt)는 경로·nullable URL·만료 시각을 함께 유지하고 [ReviewAuthor](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/community/ReviewAuthor.kt)는 아바타 경로·만료 시각을 갖습니다. [ReviewChangeReducer](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/review/change/ReviewChangeReducer.kt)는 같은 경로의 아직 유효한 URL을 보존하고, 만료 URL이나 바뀐 아바타 경로에는 최신 결과를 적용합니다. 상세 닫기·취소·단순 화면 복귀는 리뷰 URL 서명 발급을 시작하지 않습니다. 앱 코드는 출시 전 구현입니다. 리뷰 v2 서버 API의 운영 배포 상태는 [백엔드 문서](backend.md#리뷰-변경-응답과-배포-호환성)에 기록합니다.

2026-10-13(Asia/Seoul)에 `4736aa00 + working-tree`의 `1.1.9-debug`를 Android 16 실기기(SM_S931N)에서 확인했습니다. 내 리뷰의 매장 상세 닫기(전체 필터와 스크롤한 공개 필터), 편집 취소 복귀, 타인 리뷰의 매장 상세 닫기, 지도 매장 리뷰에서 작성자 프로필 복귀 구간에서 리뷰 조회·서명 발급 메서드의 추가 호출이 0이었습니다. 상세·프로필 복귀 전후 목록·사진 영역의 픽셀이 동일했고 필터와 스크롤이 유지됐습니다. 임시 비공개 리뷰의 본문을 수정한 뒤에도 다른 리뷰 행과 기존 사진 영역이 동일했습니다. 생성·수정에서는 해당 변경 응답의 아바타 URL 발급이 관찰됐으므로 모든 쓰기에서 서명 발급이 0이라는 검증은 아닙니다. 메서드 관찰은 코루틴 재진입을 포함하므로 양수 호출 수를 실제 HTTP 요청 수로 해석하지 않습니다.

같은 날짜의 후속 검증에서 테스트 사진 A의 업로드·표시, 사진 B로의 교체·표시 및 사진 A의 Storage 객체 삭제를 확인했습니다. 이때 다른 리뷰 행과 기존 사진 영역의 픽셀이 동일했습니다. 비공개→공개 저장 구간은 목록 조회와 공통 서명 발급의 추가 호출이 0이었고 기존 사진 영역이 동일했습니다. 공개→비공개 저장 구간에는 변경 응답의 서명 갱신이 관찰됐으며 전체 목록 조회는 없었습니다. 따라서 공개 설정 변경에서도 만료 여유에 따른 단건 서명 갱신을 허용하는 현재 구현과 구분해 해석합니다.

다른 계정으로 전환하면 이전 계정의 임시 비공개 사진이 본인 목록·타인 프로필·매장 상세에 남지 않았고, 작성 계정으로 복귀하면 같은 사진 픽셀이 다시 표시됐습니다. 마지막 리뷰 삭제 후 테스트 사진 두 객체의 잔존 수는 0입니다. 이는 완료된 요청 뒤의 계정 전환과 한 장의 업로드·교체 검증이며, 진행 중 요청과 세션 전환의 경합·여러 사진·최대 용량·업로드 오류와 캐시 성능은 단말에서 확인하지 않았습니다.

새 리뷰 행의 매장 thumbnail·주소·카테고리는 매장 요약을 포함한 변경 snapshot에 적용합니다. 2026-10-13(Asia/Seoul) Android 16 실기기에서 첫 복귀 화면을 확인했고, 목록에 없는 매장의 경로는 단위 테스트로 확인했습니다. 이 매장 요약 보완은 서명 URL 캐시 정책이나 서버 계약을 바꾸지 않습니다.
