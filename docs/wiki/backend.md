---
authority: reference
status: active
last_verified: 2026-10-13
source_ref: c559e52c3248076dacab2f2928622d9d787bc1e5
---

# 백엔드와 운영 탐색

이 문서는 저장소의 Supabase 연결과 운영 코드 위치를 설명합니다. 운영 DB 스키마 전체, 배포된 Edge Function 버전, 현재 접근 권한을 확인한 문서가 아닙니다. Supabase 작업에서는 해당 스킬을 읽고 작업 범위의 실제 계약을 확인합니다.

## 앱에서 서버까지

[SupabaseClient](../../core/network/src/commonMain/kotlin/com/peto/ramap/network/SupabaseClient.kt)는 Auth, PostgREST, Storage, Functions를 설치합니다. [NetworkModule](../../core/network/src/commonMain/kotlin/com/peto/ramap/network/di/NetworkModule.kt)이 client와 네트워크 서비스를 DI에 연결합니다. 설정 값의 입력 지점은 [core/network/build](../../core/network/build.gradle.kts)와 [RamapSecrets](../../core/network/src/commonMain/kotlin/com/peto/ramap/network/config/RamapSecrets.kt)입니다.

앱 계약은 [domain/repository](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/repository), 실제 조회·RPC·함수 호출은 [data](../../data/src/commonMain/kotlin/com/peto/ramap/data)에 있습니다. 메서드 이름만으로 서버 필터나 권한을 추정하지 않습니다. 예를 들어 [RemoteOperatingNoticeDataSource](../../data/src/commonMain/kotlin/com/peto/ramap/data/datasource/notice/RemoteOperatingNoticeDataSource.kt)의 Approved 이름과 별개로 클라이언트 조회에는 승인 조건이 없으므로 공개 범위는 서버 원천과 함께 확인해야 합니다.

## 서버와 운영 코드

| 목적 | 시작점 |
| --- | --- |
| 저장소에 기록된 스키마 변경 | [migrations](../../supabase/migrations), [운영 공지 SQL 테스트](../../supabase/tests/operating_notice_daily_schedules.sql) |
| 계정 삭제 | [delete-account](../../supabase/functions/delete-account/index.ts), [handler 테스트](../../supabase/functions/delete-account/handler.test.mjs) |
| 프로필·리뷰 임시 이미지 URL | [기본 배치 발급 구현](image-url-cache.md) |
| 이벤트 미리보기·등록 | [preview-event](../../supabase/functions/preview-event/index.ts), [register-event](../../supabase/functions/register-event/index.ts) |
| 관리자 수정·상태·영업 공지 | [admin-correct-registration](../../supabase/functions/admin-correct-registration/index.ts), [admin-event-status](../../supabase/functions/admin-event-status/index.ts), [admin-operating-notice](../../supabase/functions/admin-operating-notice/index.ts) |
| 이벤트 알림 | [notify-event-reminders](../../supabase/functions/notify-event-reminders/index.ts), [notify-shop-event](../../supabase/functions/notify-shop-event/index.ts) |
| 외부 저장 목록 가져오기 | [importation-match](../../supabase/functions/importation-match/index.ts), [kakao-importation](../../supabase/functions/kakao-importation/index.ts) |
| 관리자 인증의 공통 구현 | [admin-auth](../../supabase/functions/_shared/admin-auth.ts), [테스트](../../supabase/functions/_shared/admin-auth.test.mjs) |
| 네이버 영업 상태 수집 작업 | [worker 안내](../../supabase/workers/naver-operating-status/README.md), [package.json](../../supabase/workers/naver-operating-status/package.json) |
| Android 디버그 관리 화면 | [debug/admin](../../debug/admin) |

migration 디렉터리만으로 DB 전체를 재현할 수 있다고 가정하지 않습니다. 파일이 존재하는 것과 배포되었다는 것은 별개입니다. API 변경은 앱 소비자·함수·migration·검증 근거를 함께 기록합니다.

## 리뷰 변경 응답과 배포 호환성

2026-10-13(Asia/Seoul)에 운영 `submit_shop_review`, `update_shop_review`, `delete_shop_review`의 UUID 반환을 재확인했습니다. [v2 migration](../../supabase/migrations/20261012151044_review_mutation_receipts_v2.sql)은 이 함수들의 이름·인자·반환·권한을 유지하고, 별도 `*_v2` 함수에서 기존 쓰기를 호출합니다. 새 응답은 리뷰 ID, 작성자와 가게 ID, 변경 전후 단건 projection, 변경 후 본인 집계입니다. 변경된 리뷰와 집계를 로컬에서 반영하기 위한 계약이며 리뷰 목록 전체를 반환하지 않습니다.

projection에는 작성자 정보·사진 경로·공개 여부·moderation·좋아요·작성자 공개 게시 리뷰 수·방문 번호가 포함됩니다. 본인 `public_count`는 모든 공개 리뷰이고 `published_count`는 공개이면서 `published`인 리뷰이므로 구분합니다. 삭제 응답은 삭제 전 projection을 보존합니다. `fetch_my_review_projection_v2`는 본인 단건 보완 조회이며 다른 작성자의 리뷰는 반환하지 않습니다. 이미지 URL은 기존 Storage 배치 발급 경로를 사용합니다.

[격리 SQL 회귀 검사](../../supabase/tests/review_mutation_receipts_v2.sql)는 기존 UUID 응답과 함수 정의·권한 보존, v2 생성·수정·삭제 projection과 집계, 타인 소유권 거절, 누락 이미지 거절을 확인합니다. 로컬 PostgreSQL 17.11에서 운영 v1 함수 정의와 최소 fixture로 통과했습니다. fixture의 인증 helper는 운영 계정 정지·삭제·회원 자격 정책 전체를 재현하지 않습니다.

2026-10-13(Asia/Seoul)에 운영 `ramap` DB에 `review_mutation_receipts_v2`를 적용했습니다. 운영 migration 버전은 `20261012151044`이며 로컬 파일명도 이 버전과 일치합니다. 배포 전후 기존 함수 3개의 OID·정의 해시·인자·UUID 반환·권한·search_path가 동일합니다. 새 공개 RPC 4개는 JSONB를 반환하고 anon 실행을 거절하며, 내부 helper 2개는 일반 호출자에게 실행 권한이 없습니다. 운영 기본 권한에 따라 공개 RPC는 authenticated와 service_role에 실행 권한이 있으며 함수 내부에서 현재 사용자 신원을 검사합니다.

운영 REST에서 기존·신규 RPC 7개의 anon 요청이 모두 HTTP 401 / SQLSTATE 42501로 거절됐고, DB의 authenticated 역할에 사용자 신원이 없는 호출 7개도 `Authentication required`로 거절됐습니다. 보안 점검에는 의도된 [authenticated SECURITY DEFINER 알림](https://supabase.com/docs/guides/database/database-linter?lint=0029_authenticated_security_definer_function_executable) 4개가 추가됐으며, anon 실행 허용·가변 search_path 항목은 늘지 않았습니다. 이 문단의 재확인 범위는 리뷰 v2 배포이며 기준 소스는 `4736aa00 + working-tree`입니다.

같은 날짜에 Android 16 실기기(SM_S931N)의 `1.1.9-debug` 앱과 기존 로그인 세션으로 사진 없는 임시 비공개 리뷰의 v2 생성·본문 수정·삭제를 확인했습니다. 목록 전체 조회 없이 해당 행과 본인 집계가 반영됐고, 삭제 후 집계가 원래 값으로 돌아왔으며 운영 DB의 임시 리뷰 잔존 수는 0입니다. 내 리뷰·타인 리뷰의 매장 상세 닫기, 내 리뷰 편집 취소 복귀, 지도 매장 리뷰에서 작성자 프로필 복귀도 확인했습니다. 이 선행 검증의 상세 숫자와 호출 관찰은 로컬 전용 `docs/change-history/review-device-e2e-20261013/`에 기록했습니다. 단말 검증의 기준 소스도 `4736aa00 + working-tree`입니다.

이어진 같은 기기 검증에서는 임시 리뷰에 테스트 사진을 올리고 다른 사진으로 교체한 뒤 비공개→공개→비공개로 저장했습니다. 각 저장 구간의 전체 리뷰 목록 조회는 0이었고 공개·비공개 집계와 필터의 행 포함 여부가 반영됐습니다. 교체 전 사진의 Storage 객체는 제거됐고 새 사진 객체만 리뷰에 연결됐습니다. 사용자가 직접 다른 계정으로 로그인하자 그 계정의 본인 집계가 표시됐으며, 타인 프로필과 동일 매장 상세에서도 임시 비공개 리뷰가 숨겨졌습니다. 작성 계정으로 돌아오면 임시 리뷰와 사진이 다시 표시됐고, 마지막 삭제 뒤 운영 리뷰 행과 두 테스트 사진 객체의 잔존 수는 모두 0입니다. 상세 증거는 로컬 전용 `docs/change-history/review-device-photo-session-20261013/`에 기록했습니다. 사진은 한 장씩 올렸으며 다중 사진·최대 용량·업로드 실패·계정 전환 중 진행 중 요청과 기록 공백 복구는 단말에서 확인하지 않았습니다.

같은 날짜의 metadata 후속 수정에서는 내 리뷰에 새 행을 추가한 직후 매장 thumbnail·주소·카테고리가 첫 화면에 표시되는 것을 Android 16 실기기에서 확인했습니다. 목록에 없던 매장은 reducer/repository 테스트로 확인했습니다. 이 보완은 앱 구현이며 신규 서버 API나 DB 배포를 포함하지 않습니다. 새 앱 버전은 스토어에 배포하지 않았습니다.

앱 구현은 검증된 PR 코드 커밋 `c559e52c3248076dacab2f2928622d9d787bc1e5`에 반영됐으며 스토어에 새 앱 버전을 배포하지 않았습니다. 새 앱이 v2 쓰기에 실패했을 때 v1 쓰기를 자동으로 재시도하면 이중 생성 위험이 있어 사용하지 않습니다. 앱 배포 후에도 구버전 지원이 끝날 때까지 v1을 유지합니다.

## 관리자 연속 날짜 등록

관리자 날짜 선택·미리보기·등록 요청과 `register-event`의 날짜 검증에서 확인한 범위입니다. 일반 이벤트는 선택한 시작일과 종료일을 유지합니다. 신메뉴의 7일 기간과 리뉴얼의 종료일 없음은 기존 규칙을 유지합니다. 일반 영업 변동의 `schedule_override`는 시작일부터 종료일까지 적용하며 월을 넘길 수 있습니다. 날짜 순서가 뒤집혔거나 실제 달력에 없는 날짜는 등록 전에 거부하고, 기존 변경 영업시간과의 중복은 선택한 기간 전체로 확인합니다.

앱 상태·전송 범위는 [관리자 ViewModel 테스트](../../debug/admin/src/androidHostTest/kotlin/com/peto/ramap/debug/admin/ui/registration/AdminRegistrationViewModelTest.kt), 실제 달력 선택은 [기기 테스트](../../debug/admin/src/androidDeviceTest/kotlin/com/peto/ramap/debug/admin/AdminRegistrationFlowTest.kt), 서버 저장 값은 [등록 handler 테스트](../../supabase/functions/register-event/handler.test.mjs), 영업 상태 적용은 [공지 도메인 테스트](../../domain/src/commonTest/kotlin/com/peto/ramap/domain/model/notice/MonthlyOperatingNoticeTest.kt)가 검증합니다.

2026-10-11에 `register-event` 운영 버전 19와 `add_operating_notice_daily_schedules` 스키마 반영을 확인했습니다. 이 함수가 사용하는 `daily_schedules` 컬럼은 [기존 migration](../../supabase/migrations/20261003032631_add_operating_notice_daily_schedules.sql)이 정의하며, 빈 배열은 기존 연속 공지 동작을 유지합니다. 운영 API는 존재하지 않는 검증용 매장명으로 날짜 검증 통과 후 저장 전에 중단되는 응답을 확인했습니다. 실제 저장은 격리한 handler·DB 테스트로 확인했으며 운영 공지를 추가하지 않았습니다.

## 로컬 등록 절차 찾기

매장·메뉴·영업시간·이벤트 등록은 `.agents/skills/`의 `db-add`, `menu-add`, `work-time`, `event-add`, `operating-notice-add`가 담당합니다. 메뉴 갱신의 실제 진입점은 `tools/menu-refresh/SKILL.md`, 정책은 `docs/menu-registration-policy.md`입니다. `.agents/skills/menu-refresh/SKILL.md`의 미완성 placeholder를 완성된 절차로 사용하지 않습니다.

이 경로들은 일반 checkout에 없을 수 있습니다. 위키는 등록·배포 승인 절차를 대체하지 않으며, 로컬 자료를 임의로 공개하지 않습니다. 운영 동작을 바꾼 뒤에는 코드 확인과 실제 배포 확인을 구분해 기록합니다.
