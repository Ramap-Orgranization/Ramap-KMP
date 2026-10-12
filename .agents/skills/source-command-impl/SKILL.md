---
name: source-command-impl
description: Ramap 변경을 ASTRA 계획, 사용자 승인, LUNA 이슈·브랜치 생성, SOL 구현과 검증 순서로 진행한다. `/impl` 또는 계획 승인 후 구현하는 단계적 절차를 명시적으로 요청할 때 사용한다.
---

# Source Command Impl

## 적용 범위와 에이전트 선택

`/impl` 또는 계획 승인 후 구현을 진행하는 절차를 명시적으로 요청한 경우에 적용합니다. 일반 구현 요청은 `AGENTS.md`에 따라 primary agent가 조사부터 구현과 검증까지 담당합니다.

아래 named role을 선택하고, 각 TOML의 모델·추론 수준과 짝을 이루는 Markdown 역할 지침을 따릅니다. `task_name`은 작업 표시 이름이며 역할이나 모델을 선택하지 않습니다. Named role을 사용할 수 없으면 해당 TOML의 모델과 추론 수준을 명시해 별도 에이전트를 생성합니다.

모델군 표기는 TOML의 `model`을 기준으로 합니다. 현재 계획은 ASTRA(`gpt-6-astra`), 구현·검증은 SOL(`gpt-6.1-sol`), Git 작업은 LUNA(`gpt-6-luna`)입니다. `AGENTS.md`나 역할 본문에 남은 이전 별칭으로 실행 모델을 추론하지 않습니다. SOL 패키지의 실제 호출 ID와 파일명에는 `terra_*`·`terra-*.toml`이 남아 있으므로 현재 TOML의 `name`과 경로를 그대로 사용합니다.

| 단계 | 실제 호출 ID | 모델 / 추론 수준 | 런타임 설정 | 역할 지침 |
| --- | --- | --- | --- | --- |
| ASTRA 계획 | `astra_planner` | `gpt-6-astra` / `high` | `.codex/agents/astra/astra-planner.toml` | `.codex/agents/astra/planner.md` |
| LUNA 이슈·브랜치 | `luna_git_issue_branch` | `gpt-6-luna` / `low` | `.codex/agents/luna/luna-git-issue-branch.toml` | `.codex/agents/luna/git-issue-branch.md` |
| SOL 구현 | `terra_implementer` | `gpt-6.1-sol` / `medium` | `.codex/agents/sol/terra-implementer.toml` | `.codex/agents/sol/implementer.md` |
| SOL 검증 | `terra_test_validator` | `gpt-6.1-sol` / `low` | `.codex/agents/sol/terra-test-validator.toml` | `.codex/agents/sol/test-validator.md` |

각 역할은 별도의 에이전트 호출로 순차 실행합니다. 코딩 컨벤션, 아키텍처, 모듈 구조와 디자인 패턴의 상세 규칙은 해당 Ramap 프로젝트 skill을 기준으로 적용합니다.

## 1. ASTRA 계획

`astra_planner`는 역할 지침, `AGENTS.md`, 관련 Ramap 프로젝트 skill과 대상 코드·호출자·의존 코드·테스트를 읽습니다. 계획 단계는 읽기 전용이며 파일을 변경하거나 테스트를 실행하지 않습니다.

계획에는 다음 내용을 포함합니다.

- 변경할 파일과 각 변경 내용
- `commonMain`, `androidMain`, `iosMain`과 Android·iOS 앱 진입점의 영향
- 구현 순서, 영향 범위와 위험 요소
- 모호한 요구 사항의 선택지, 권장안과 근거, 남은 질문
- 승인 후 수행할 GitHub 이슈·로컬 브랜치 생성

`implementation_handoff`에는 담당 파일 또는 책임 범위, 구현할 동작, 제약과 수용 기준을 명시합니다. 요청 범위 밖의 리팩터링은 포함하지 않습니다.

`validation_handoff`에는 변경 플랫폼과 위험도에 맞는 최소 검증 명령, 선택 근거, 기대 결과와 알려진 환경 제약을 명시합니다. 명령은 `.codex/agents/sol/implementer.md`와 실제 Gradle 구성을 확인해 선택합니다.

## 2. 승인

이슈·브랜치 생성과 두 handoff를 포함한 계획을 사용자에게 제시하고 명시적인 승인을 기다립니다. 수정 요청을 받으면 계획과 handoff를 갱신해 다시 제시합니다. 승인 전에는 이슈·브랜치를 생성하거나 구현·테스트 파일을 변경하지 않습니다.

## 3. LUNA 이슈·브랜치 생성

승인 후 `issue-branch` skill을 적용해 `luna_git_issue_branch`를 호출합니다. `.codex/commands/issue-branch.md`의 위임 규칙을 따릅니다.

- 승인된 작업 설명과 작업 유형을 전달합니다.
- 에이전트는 역할 지침, `AGENTS.md`, `.agents/skills/issue-branch/SKILL.md`와 해당 이슈 템플릿을 읽습니다.
- 현재 브랜치와 worktree를 확인하고 기존 변경을 보존합니다.
- 한국어 GitHub 이슈와 현재 브랜치를 기반으로 한 로컬 브랜치를 생성합니다. 유형 선택과 브랜치 이름은 `issue-branch` skill을 따릅니다.
- 이슈 URL, 생성한 브랜치와 이전 기준 브랜치를 확인한 뒤 구현을 시작합니다.

생성이 실패하면 완료된 이슈·브랜치 상태와 실패 원인을 보고합니다. 이슈 URL과 작업 브랜치를 확인할 때까지 구현을 시작하지 않습니다.

## 4. SOL 구현

`terra_implementer`에 승인된 계획과 `implementation_handoff`, 이슈 URL과 작업 브랜치를 전달합니다. 다른 에이전트와 사용자의 변경을 보존하고 동시 변경에 맞춰 작업하도록 명시합니다.

구현 에이전트는 자신의 역할 지침, `AGENTS.md`, 네 가지 Ramap 프로젝트 skill과 승인된 handoff를 읽고 현재 브랜치·worktree를 확인합니다. 지정된 파일 또는 책임 범위와 수용 기준에 맞는 최소 코드를 구현합니다.

테스트 작성·수정은 별도 필수 단계가 아닙니다. 요청이나 수용 기준에 필요한 테스트 변경은 계획에 포함하고 승인된 범위에서 수행합니다.

구현 후 `validation_handoff`를 갱신해 변경 모듈·소스셋, 사용자에게 보이는 위험, 실행할 명령과 선택 근거, 기대 결과·환경 제약을 전달합니다. 구현 에이전트가 직접 검증하는 경우는 즉시 오류 확인이 필요하거나 validator를 호출할 수 없을 때로 한정하고 이유와 결과를 보고합니다.

## 5. SOL 검증과 완료 보고

별도의 `terra_test_validator`가 역할 지침과 구현 handoff를 읽고 지정된 Gradle 테스트·빌드·정적 검사를 실행합니다. Kotlin UI·ViewModel 변경은 `AGENTS.md`와 관련 Ramap skill을 읽고 패턴 준수 여부도 읽기 전용으로 확인합니다.

Validator는 구현·테스트 파일을 수정하지 않습니다. 자동 포맷처럼 소스를 변경하는 명령은 implementer가 수행하고, validator에는 읽기 전용 검사 명령을 전달합니다.

검증 결과에는 실행 명령, 성공·실패, 실패 시 종료 코드·핵심 오류·재현 조건, 실행하지 못한 검증과 이유를 포함합니다. 실패 원인은 구현 오류, 기존 변경, 환경 제약으로 구분합니다. 코드 수정이 필요하면 implementer에 전달하고, 범위나 설계 변경이 필요하면 planner에 전달해 계획과 승인을 갱신합니다. 수정 후에는 영향을 받은 검증을 다시 수행합니다.

최종 보고에는 이슈 URL과 작업 브랜치, 구현 내용, 검증 결과와 남은 제약을 포함합니다. 실행하지 못한 검증을 통과한 것으로 보고하지 않습니다.

커밋, stage, push, PR 생성은 이 절차에 포함하지 않습니다.
