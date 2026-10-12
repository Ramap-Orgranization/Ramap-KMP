---
authority: reference
status: active
last_verified: 2026-10-11
source_ref: 009f67d2924a285353213d1aa91ade5406e87324 + working-tree
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
| 이벤트 미리보기·등록 | [preview-event](../../supabase/functions/preview-event/index.ts), [register-event](../../supabase/functions/register-event/index.ts) |
| 관리자 수정·상태·영업 공지 | [admin-correct-registration](../../supabase/functions/admin-correct-registration/index.ts), [admin-event-status](../../supabase/functions/admin-event-status/index.ts), [admin-operating-notice](../../supabase/functions/admin-operating-notice/index.ts) |
| 이벤트 알림 | [notify-event-reminders](../../supabase/functions/notify-event-reminders/index.ts), [notify-shop-event](../../supabase/functions/notify-shop-event/index.ts) |
| 외부 저장 목록 가져오기 | [importation-match](../../supabase/functions/importation-match/index.ts), [kakao-importation](../../supabase/functions/kakao-importation/index.ts) |
| 관리자 인증의 공통 구현 | [admin-auth](../../supabase/functions/_shared/admin-auth.ts), [테스트](../../supabase/functions/_shared/admin-auth.test.mjs) |
| 네이버 영업 상태 수집 작업 | [worker 안내](../../supabase/workers/naver-operating-status/README.md), [package.json](../../supabase/workers/naver-operating-status/package.json) |
| Android 디버그 관리 화면 | [debug/admin](../../debug/admin) |

migration 디렉터리만으로 DB 전체를 재현할 수 있다고 가정하지 않습니다. 파일이 존재하는 것과 배포되었다는 것은 별개입니다. API 변경은 앱 소비자·함수·migration·검증 근거를 함께 기록합니다.

## 관리자 연속 날짜 등록

이번 working-tree에서 확인한 범위는 관리자 날짜 선택·미리보기·등록 요청과 `register-event`의 날짜 검증입니다. 일반 이벤트는 선택한 시작일과 종료일을 유지합니다. 신메뉴의 7일 기간과 리뉴얼의 종료일 없음은 기존 규칙을 유지합니다. 일반 영업 변동의 `schedule_override`는 시작일부터 종료일까지 적용하며 월을 넘길 수 있습니다. 날짜 순서가 뒤집혔거나 실제 달력에 없는 날짜는 등록 전에 거부하고, 기존 변경 영업시간과의 중복은 선택한 기간 전체로 확인합니다.

앱 상태·전송 범위는 [관리자 ViewModel 테스트](../../debug/admin/src/androidHostTest/kotlin/com/peto/ramap/debug/admin/ui/registration/AdminRegistrationViewModelTest.kt), 실제 달력 선택은 [기기 테스트](../../debug/admin/src/androidDeviceTest/kotlin/com/peto/ramap/debug/admin/AdminRegistrationFlowTest.kt), 서버 저장 값은 [등록 handler 테스트](../../supabase/functions/register-event/handler.test.mjs), 영업 상태 적용은 [공지 도메인 테스트](../../domain/src/commonTest/kotlin/com/peto/ramap/domain/model/notice/MonthlyOperatingNoticeTest.kt)가 검증합니다.

2026-10-11에 `register-event` 운영 버전 19와 `add_operating_notice_daily_schedules` 스키마 반영을 확인했습니다. 이 함수가 사용하는 `daily_schedules` 컬럼은 [기존 migration](../../supabase/migrations/20261003032631_add_operating_notice_daily_schedules.sql)이 정의하며, 빈 배열은 기존 연속 공지 동작을 유지합니다. 운영 API는 존재하지 않는 검증용 매장명으로 날짜 검증 통과 후 저장 전에 중단되는 응답을 확인했습니다. 실제 저장은 격리한 handler·DB 테스트로 확인했으며 운영 공지를 추가하지 않았습니다.

## 로컬 등록 절차 찾기

매장·메뉴·영업시간·이벤트 등록은 로컬 `.agents/skills/`의 `db-add`, `menu-add`, `work-time`, `event-add`, `operating-notice-add`가 담당합니다. 메뉴 갱신의 실제 진입점은 `tools/menu-refresh/SKILL.md`, 정책은 `docs/menu-registration-policy.md`입니다. `.agents/skills/menu-refresh/SKILL.md`의 미완성 placeholder를 완성된 절차로 사용하지 않습니다.

이 경로들은 일반 checkout에 없을 수 있습니다. 위키는 등록·배포 승인 절차를 대체하지 않으며, 로컬 자료를 임의로 공개하지 않습니다. 운영 동작을 바꾼 뒤에는 코드 확인과 실제 배포 확인을 구분해 기록합니다.
