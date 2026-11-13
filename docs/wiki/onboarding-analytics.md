---
authority: reference
status: active
last_verified: 2026-11-10
---

# 온보딩 퍼널 이벤트

온보딩이 필요한 앱 실행에 `attempt_id`를 발급합니다. 같은 화면으로 돌아오거나 앱을 잠깐 백그라운드에 보내도 ViewModel이 유지되는 동안 같은 ID를 씁니다. 앱을 종료한 뒤 다시 열어 온보딩을 이어가면 새 시도로 기록합니다. 이미 완료한 사용자가 앱을 여는 경우에는 온보딩 이벤트를 남기지 않습니다.

모든 이벤트에는 `attempt_id`, `schema_version`, `event_index`, `elapsed_ms`, `guide_pages_viewed`가 들어갑니다. 노출된 화면이 있으면 `stage`도 포함합니다. `event_index`로 같은 시도의 순서를 정렬합니다. `elapsed_ms`와 `previous_stage_ms`는 단조 시계 기준 경과 시간이며 백그라운드에 있던 시간도 포함합니다. `guide_pages_viewed`는 서로 다른 안내 페이지를 실제로 본 개수입니다.

단계 키는 `permission_guide`, `notification_menu`, `notification_types`, `review_map`, `shop_selection`, `login`입니다. 페이저의 화면 전환이 끝난 페이지를 기록합니다. 같은 단계의 재구성은 무시하고, 다른 단계를 본 뒤 돌아온 경우에는 재방문으로 기록합니다. 로그인 화면은 사용법 안내 조회 수에 포함하지 않습니다.

| 이벤트 | 기록 시점과 추가 필드 |
| --- | --- |
| `onboarding_started` | 미완료 상태를 확인해 온보딩에 진입할 때. `entry`는 `permission_guide`, `permissions_confirmed`, `shop_selection_completed` 중 하나 |
| `onboarding_stage_viewed` | 권한 안내, 각 사용법 안내, 매장 선택, 선택 로그인 화면이 표시될 때. 직전 단계가 있으면 `from_stage`, `previous_stage_ms` 포함 |
| `onboarding_permission_guide_next` | 권한 안내에서 유효한 다음 버튼 입력을 처리할 때 |
| `onboarding_permission_requested` | 위치·알림 권한 요청을 플랫폼에 전달하도록 예약할 때. `permission`은 `location` 또는 `notification` |
| `onboarding_permission_result` | 현재 요청에 해당하는 응답을 받을 때. `permission`, `result`, `granted` 포함. 요청 실패 시 `granted`는 생략 |
| `onboarding_guide_action` | 다음, 건너뛰기, 시스템 뒤로 가기 입력을 처리할 때. `action`은 `next`, `skip`, `system_back`이며 `action_stage`는 조작한 단계 |
| `onboarding_shop_load_result` | 추천 목록, 다음 페이지, 검색 조회 결과 또는 개인화 로딩 실패. `operation`은 `suggestions`, `next_page`, `search`, `personalization`이며 `result`는 `success`, `empty`, `failed`. 결과가 있으면 `result_count`, 실패 시 `error_type` 포함. 추천 목록은 안내를 보는 동안 미리 조회할 수 있음 |
| `onboarding_bookmark_result` | 매장 저장·해제 요청이 끝날 때. `action`은 `save` 또는 `remove`이며 `result`, `saved_count`, 실패 시 `error_type` 포함 |
| `onboarding_shop_goal_reached` | 매장 선택에서 시작 가능한 3곳 조건을 처음 충족했을 때. `saved_count` 포함. 이전 실행에서 저장한 매장도 조건에 포함 |
| `onboarding_finish_requested` | 로그인 성공 또는 비로그인 시작을 선택해 완료 기록 저장을 시작할 때. 이미 로그인한 사용자는 매장 선택 직후 기록. `saved_count` 포함 |
| `onboarding_finish_failed` | 완료 기록 저장에 실패할 때. `saved_count` 포함 |
| `onboarding_completed` | 완료 기록이 기기에 저장됐을 때. `saved_count` 포함. 기존 완료 기록을 읽는 동작과 구분 |
| `onboarding_backgrounded` | 화면 노출 후 생명주기가 `ON_STOP`으로 전환될 때. `pending_permission`으로 권한 응답 대기 여부 확인 |
| `onboarding_foregrounded` | 위 이벤트 뒤 같은 시도에서 `ON_START`로 복귀할 때 |

## 알림 권한 결과

| `result` | 의미 |
| --- | --- |
| `newly_granted` | 온보딩의 권한 요청에서 새로 허용된 응답을 받음 |
| `already_granted` | 요청 전에 이미 알림이 허용된 상태 |
| `enabled_without_runtime_request` | Android 12 이하에서 런타임 권한 요청 없이 앱 알림이 켜져 있음 |
| `not_granted` | 시스템 요청의 응답으로 허용되지 않음. Android 콜백에서는 거부, 팝업 닫기, 팝업을 띄울 수 없는 상태를 구분하지 못함 |
| `disabled_in_settings` | 시스템 설정에서 알림이 꺼져 있거나 iOS에서 이미 거부한 상태 |
| `failed` | 요청을 시작할 수 없거나 시스템 요청이 오류를 반환함. 권한의 허용 여부는 알 수 없음 |

신규 허용 전환은 알림 결과가 `newly_granted`인 시도로 집계합니다. 전체 알림 활성화는 `granted = 1`인 시도로 따로 봅니다. 권한 요청 이벤트는 운영체제 팝업이 표시됐다는 확인 없이 기록되므로 요청 건수와 실제 팝업 응답을 구분해 해석합니다.

## 퍼널 집계

- 단계별 이탈은 관찰 기간이 지난 미완료 시도의 마지막 `onboarding_stage_viewed`로 집계합니다. `onboarding_backgrounded`는 앱 전환과 권한 요청 중단도 포함하므로 이탈을 확정하는 기준으로 쓰지 않습니다.
- 사용법 안내를 모두 본 시도는 `guide_pages_viewed = 3`으로 구분합니다. 같은 시도의 `skip` 입력과 매장 선택 노출을 연결하면 안내를 건너뛰고 매장 선택에 도달한 경우를 볼 수 있습니다.
- 매장 선택 노출, 3곳 충족, 선택 로그인 화면 노출, 시작 요청, 완료 이벤트의 시도 수를 차례로 비교합니다. 화면 도달 뒤 조회·저장·완료 실패가 있었는지도 `attempt_id`로 연결할 수 있습니다. 온보딩에서 시작한 로그인은 공통 로그인 이벤트의 `source = onboarding`으로 구분합니다.
- 검색어, 매장 ID, 계정 ID, 위치 좌표, 예외 원문은 이벤트에 포함하지 않습니다.
