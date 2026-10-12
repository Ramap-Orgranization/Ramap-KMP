---
authority: canonical
status: active
last_verified: 2026-10-11
---

# Ramap LLM 위키

Ramap은 Android와 iOS에서 라멘 가게 탐색, 저장, 리뷰, 이벤트와 운영 소식을 제공하는 Kotlin Multiplatform 앱입니다. 이 위키는 작업에 필요한 코드·테스트·정책의 위치를 찾고, 세션에서 확인한 지식을 다음 작업에 전달하는 진입점입니다.

## 읽는 순서

1. 저장소의 [AGENTS.md](../../AGENTS.md)와 해당 프로젝트 스킬을 읽습니다. Codex 설정과 프로젝트 스킬은 Git으로 함께 공유합니다.
2. 아래에서 작업과 관련된 문서만 읽고, 연결된 코드·호출부·테스트를 확인합니다.
3. 변경 후 [유지보수 정책](maintenance.md)에 따라 문서 영향을 판단하고 `python3 scripts/check_wiki.py`를 실행합니다.

| 작업 | 시작 문서 |
| --- | --- |
| 기능의 진입점과 데이터 계약 찾기 | [기능 지도](feature-map.md) |
| 앱 시작, 화면 이동, 딥링크, 지도 흐름 | [앱 실행 흐름](app-flows.md) |
| 계층과 조립 경계 파악 | [아키텍처 탐색](architecture.md) |
| 모듈·소스 세트·Gradle 설정 찾기 | [모듈 구조](module-structure.md) |
| ViewModel, 로딩, 오류, 데이터 접근 읽기 | [설계 패턴 탐색](design-patterns.md) |
| 공통 UI와 리소스 재사용 | [디자인시스템](design-system.md) |
| 현재 구현된 제품 동작 확인 | [제품 정책과 구현 근거](product-policy.md) |
| Supabase, 운영 도구, 등록 절차 찾기 | [백엔드와 운영](backend.md) |
| 테스트와 빌드 선택 | [검증 안내](testing.md) |
| 문서 갱신, 확정, 폐기 처리 | [유지보수 정책](maintenance.md) |
| 변경 이유 확인 | [결정 기록](decisions.md) |
| 미확정 지식 검토 | [후보함](inbox/README.md) |

## 문서의 권위와 근거

`authority: canonical`은 이 위키의 유지보수 절차와 기록 형식을 정의합니다. `authority: reference`는 코드에서 확인한 구현 사실과 탐색 경로이며, 새 제품 요구사항을 승인하는 문서가 아닙니다. 후보와 양식은 `authority: none`입니다.

개발 규칙은 기존 `.agents/skills/ramap-{architecture,module-structure,design-patterns,coding-conventions}/SKILL.md`가 계속 소유합니다. 위키에서 규칙을 복제하거나 스킬과 다른 우선순위를 만들지 않습니다. 스킬이 없는 환경에서는 코드의 위치는 탐색할 수 있지만 팀 규칙을 확인했다고 보고하지 않습니다.

Codex 역할은 [.codex/agents](../../.codex/agents)의 독립 TOML 파일에서 관리합니다. 각 파일에 모델·추론 수준과 `developer_instructions`를 함께 두며 역할 이름과 경로에는 모델 접두사를 붙이지 않습니다([DEC-0005](decisions.md#dec-0005-codex-역할-정의-통합과-모델-독립-이름)). 역할별 실행 절차는 해당 스킬에서 읽습니다.

2026-10-11 기준 커밋 `33a0d49c2731e42144f2c79704748d7c7e0cf68b`의 소스를 확인해 갱신했습니다. 각 reference 페이지의 `source_ref`는 코드 확인 기준이며 운영 배포 버전은 아닙니다.

초기 미확정 항목 세 가지는 사용자 요청에 따라 현재 구현을 기준으로 확정하고 소유 스킬에 반영했습니다([DEC-0002](decisions.md#dec-0002-현재-구현을-기준으로-개발-규칙-정합성-확정)). 현재 미해결 후보는 없습니다.

## 참고한 구조

[YAPP LLM Wiki](https://github.com/YAPP-admin/yapp-plus-android/tree/develop/docs/wiki)의 분야별 문서, 결정 기록, 후보함, 검증 절차를 참고했습니다. Ramap의 코드·스킬을 근거로 내용을 작성했으며 YAPP의 제품 정책이나 Git 작업 규칙은 가져오지 않았습니다.
