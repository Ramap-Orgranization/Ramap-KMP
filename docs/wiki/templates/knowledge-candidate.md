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

## 질문과 근거

- 확인할 정책·도메인 질문 또는 재사용할 판단:
- 확인한 사실과 승인 근거:
- 영향을 받는 규칙·개념:
- 미확인 범위와 다음 확인 방법:

## 처리

확정한 결론과 이유를 정책·도메인 문서에 반영합니다. 승격 시 `status: promoted`, 저장소 상대 `target_path`, 등록된 `decision_id`, `resolution_reason`을 채웁니다. 기각 시 `status: rejected`와 사유만 채우고 대상과 결정 ID는 `null`로 둡니다. 날짜도 갱신합니다. 작업 로그나 읽힌 횟수는 누적하지 않습니다.
