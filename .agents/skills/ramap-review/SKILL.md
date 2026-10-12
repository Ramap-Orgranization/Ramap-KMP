---
name: ramap-review
description: Ramap Kotlin Multiplatform 변경사항을 전용 code-reviewer 서브에이전트로 검토한다. `/ramap-review`, `$ramap-review`, 코드 리뷰 요청, 브랜치·revision·파일 범위 리뷰, UX 상태 전이·디자인 시스템 공통화·객체지향 설계 중심 리뷰를 요청할 때 사용한다.
---

# Ramap Review

Ramap 전용 리뷰 기준으로 변경사항을 읽기 전용 검토한다.

## Workflow

1. `AGENTS.md`, `.codex/agents/code-reviewer.toml`과 변경 범위에 관련된 Ramap 프로젝트 skill을 읽는다. 경로는 저장소 루트 기준이다. 이 TOML에 실행 모델·추론 수준과 `developer_instructions`의 리뷰 기준·절차가 함께 있다.
2. 사용자의 인자를 기준 브랜치, revision 범위, 파일 경로 또는 리뷰 초점으로 해석한다. 인자가 없으면 작업 트리의 tracked 및 untracked 변경을 리뷰하고, 변경이 없으면 현재 브랜치와 `develop`의 merge base를 비교한다.
3. 기준 revision, diff 범위, 파일 또는 책임 범위와 리뷰 초점을 명시해 named role `code_reviewer`에 위임한다. Named role을 사용할 수 없으면 TOML의 모델과 추론 수준으로 별도 에이전트를 생성한다. `task_name`만으로 역할이나 모델을 선택하지 않는다.
4. 리뷰 에이전트는 diff뿐 아니라 관련 호출부, 상태 소유자, domain/data 경계, 디자인 시스템 컴포넌트와 테스트를 확인한다. 같은 리뷰를 다시 위임하지 않고 읽기 전용으로 수행한다.
5. 필요한 읽기 전용 빌드·테스트의 실제 결과와 실행하지 못한 검증을 구분해 보고한다. 자동 포맷이나 소스 수정은 리뷰에 포함하지 않는다.
6. 완료될 때까지 기다린 뒤 같은 원인의 중복 지적을 합쳐 사용자에게 전달한다.

## Review Contract

- `.codex/agents/code-reviewer.toml`의 정확성, UX 상태 전이, Compose 및 디자인 시스템, 객체지향 설계, 아키텍처와 테스트 기준을 모두 적용한다. 상세 기준은 이 스킬에 복제하지 않는다.
- 발견 사항을 `P0`부터 `P3`까지 심각도 순으로 먼저 제시한다.
- 각 발견 사항에 좁은 파일·줄 범위, 발생 조건, 사용자 또는 시스템 영향, 코드 근거와 가장 작은 개선 방향을 포함한다.
- 스타일 취향이나 구체적 실패 시나리오가 없는 추측을 결함으로 보고하지 않는다.
- 이번 변경으로 새로 발생하거나 악화되지 않은 기존 문제는 참고 사항으로 분리한다. 확인할 수 없는 내용은 질문 또는 가정으로 표시한다.
- 발견 사항이 없으면 명시하고 남은 테스트 또는 검증 공백을 보고한다.
- 사용자가 별도로 수정을 요청하지 않는 한 파일 수정, commit, push 또는 PR 생성을 하지 않는다.

## Examples

```text
$ramap-review
$ramap-review develop 기준으로 현재 브랜치 리뷰
$ramap-review feature/main/map만 리뷰
$ramap-review 로딩 실패 후 재시도 UX를 중점 검토
```
