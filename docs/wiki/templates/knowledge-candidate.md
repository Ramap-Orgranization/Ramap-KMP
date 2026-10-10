---
type: knowledge-candidate
authority: none
status: template
created_at: YYYY-MM-DD
updated_at: YYYY-MM-DD
target_path: null
decision_id: null
resolution_reason: null
---

# 후보 제목

`docs/wiki/inbox/<주제>.md`로 복사하고 `status: candidate`와 날짜를 채웁니다. 해결 전 대상 경로·결정 ID·사유는 `null`입니다.

## 내용과 근거

- 재사용할 판단 또는 확인할 불일치:
- 코드·테스트·승인된 요구사항:
- 적용 범위와 미확인 사항:
- 다음 확인 방법:

## 실제 사용 이력

| 작업 또는 이슈 | 날짜 | 판단에 사용한 맥락과 결과 |
| --- | --- | --- |

같은 작업은 한 번만 기록합니다. 사용 이력이 없는 후보도 기록할 수 있습니다.

## 처리

승격 시 `status: promoted`, 저장소 상대 `target_path`, 등록된 `decision_id`, `resolution_reason`을 채웁니다. 기각 시 `status: rejected`와 사유만 채우고 대상과 결정 ID는 `null`로 둡니다. 날짜도 갱신합니다.
