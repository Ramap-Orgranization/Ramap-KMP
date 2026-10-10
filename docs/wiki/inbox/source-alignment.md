---
type: knowledge-candidate
authority: none
status: promoted
created_at: 2026-10-11
updated_at: 2026-10-11
target_path: docs/wiki/architecture.md
decision_id: DEC-0002
resolution_reason: 사용자의 현재 기준 정합화 요청에 따라 실제 모듈·mapper 소유권·analytics 의존성을 소유 스킬과 위키에 반영
---

# 개발 규칙과 구현 위치의 차이

## 내용과 근거

초기 위키 작성 기준 `33a0d49c`에서 로컬 프로젝트 스킬의 설명과 다음 구현 상태가 다릅니다.

| 관찰 | 근거 |
| --- | --- |
| module-structure 스킬의 profile/follow가 settings에 없음 | [settings.gradle.kts](../../../settings.gradle.kts), [profile/edit](../../../feature/profile/edit) |
| core/ui로 일원화된 mapper 설명과 달리 일부가 designsystem에 있음 | [UI mapper](../../../core/ui/src/commonMain/kotlin/com/peto/ramap/ui/resource), [디자인시스템 mapper](../../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/resource) |
| architecture의 domain 의존성 요약 외에 analytics 의존성이 있음 | [domain/build.gradle.kts](../../../domain/build.gradle.kts) |

2026-10-11 사용자가 “미확정은 모두 현재 기준으로 맞추고 최신화”하도록 요청했습니다. 현재 settings, build 파일, mapper 소비자, 로그인 분석 호출과 테스트를 대조해 세 항목을 현재 기준으로 확정했습니다. 과거 의도를 추정하지 않고 현행 구현에 맞춰 소유 스킬을 수정했습니다.

## 실제 사용 이력

| 작업 또는 이슈 | 날짜 | 판단에 사용한 맥락과 결과 |
| --- | --- | --- |
| Ramap LLM 위키 초기 구축 | 2026-10-11 | 구현 설명에 존재하지 않는 모듈을 넣지 않고 mapper의 실제 위치와 규칙을 구분 |
| 미확정 항목 현재 기준 정합화 | 2026-10-11 | 현재 소스와 사용자 확정을 근거로 소유 스킬 수정 및 DEC-0002 반영 |

## 처리

승격 완료. `ramap-module-structure`에서 존재하지 않는 profile/follow 목록을 제거하고, `ramap-architecture`·`ramap-module-structure`·`ramap-design-patterns`의 mapper 소유권과 analytics 의존성 설명을 현재 코드에 맞췄습니다. 현행 내용은 [아키텍처](../architecture.md), [모듈 구조](../module-structure.md), [디자인시스템](../design-system.md), 근거와 이유는 [DEC-0002](../decisions.md#dec-0002-현재-구현을-기준으로-개발-규칙-정합성-확정)에 있습니다.
