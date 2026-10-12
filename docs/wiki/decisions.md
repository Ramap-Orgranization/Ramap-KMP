---
authority: canonical
status: active
last_verified: 2026-10-12
---

# 결정 기록

정책의 도입·변경·폐기 이유를 남깁니다. 현재 내용은 분야별 문서에서 읽습니다. 새 항목은 사용하지 않은 다음 `DEC-NNNN` ID로 추가하고 이전 항목은 보존합니다. 경로 수정이나 구현 사실의 단순 갱신에는 결정이 필요하지 않습니다.

## DEC-0001 Ramap LLM 위키와 작업 내 유지보수 도입

- 기록일: 2026-10-11 (Asia/Seoul)
- 결정: 공유 위키를 `docs/wiki/`에 두고 코드 탐색 문서·유지보수 정책·결정 기록·미확정 후보를 구분합니다. 기존 개발 규칙의 소유권은 Ramap 스킬에 유지합니다.
- 이유: 작업에 필요한 근거를 빠르게 찾고 세션의 확정 지식을 보존하면서 로컬 운영 자료와 규칙의 중복을 줄입니다.
- 근거: 이번 세션의 사용자 요청인 “우리 앱의 LLM 위키와 유지보수 정책을 구축”과 기존 로컬 `AGENTS.md`의 스킬 소유권 지침. 구조 참고: [YAPP LLM Wiki](https://github.com/YAPP-admin/yapp-plus-android/tree/develop/docs/wiki).
- 반영: [목차](README.md), [유지보수 정책](maintenance.md), [후보함](inbox/README.md), [PR 템플릿](../../.github/PULL_REQUEST_TEMPLATE.md), [검사 workflow](../../.github/workflows/wiki.yml), 로컬 `AGENTS.md`.
- 적용 상태: 문서와 로컬 검사·CI 설정을 추가했습니다. 원격 CI 실행과 branch protection 설정, 배포를 완료했다는 의미는 아닙니다.
- 대체한 결정: 없음.

## DEC-0002 현재 구현을 기준으로 개발 규칙 정합성 확정

- 기록일: 2026-10-11 (Asia/Seoul)
- 결정: 미확정이었던 모듈 목록, resource mapper 소유권, domain의 analytics 의존성을 현재 구현 기준으로 확정합니다. settings에 없는 profile/follow 표기를 제거하고, core/ui와 core/designsystem의 mapper 분담 및 domain → core/analytics의 공통 API 사용을 소유 스킬에 반영합니다.
- 이유: 이후 작업에서 실제 모듈과 다른 경로를 찾거나 공통 mapper를 잘못 이동하지 않도록 개발 규칙과 코드를 일치시킵니다.
- 근거: 사용자의 “미확정은 모두 현재 기준으로 맞추고 최신화” 요청, [settings](../../settings.gradle.kts), [domain build](../../domain/build.gradle.kts), [core/ui build](../../core/ui/build.gradle.kts), [designsystem build](../../core/designsystem/build.gradle.kts), [SignInUseCase](../../domain/src/commonMain/kotlin/com/peto/ramap/domain/usecase/SignInUseCase.kt), [RankingFilters](../../feature/main/ranking/src/commonMain/kotlin/com/peto/ramap/ui/main/ranking/component/RankingFilters.kt).
- 반영: 로컬 `ramap-architecture`, `ramap-module-structure`, `ramap-design-patterns` 스킬, [아키텍처](architecture.md), [모듈 구조](module-structure.md), [디자인시스템](design-system.md), [승격한 후보](inbox/source-alignment.md).
- 적용 상태: 소유 스킬과 위키에 반영했습니다. 앱 코드의 동작과 모듈 배치는 변경하지 않았습니다.
- 대체한 결정: 없음. DEC-0001의 스킬 소유권 원칙을 유지하며 초기 후보를 해소합니다.

## DEC-0003 프로필·리뷰 이미지 URL 기본 배치 발급 채택

- 기록일: 2026-10-12 (Asia/Seoul).
- 결정: 프로필·리뷰 이미지 URL은 Supabase Storage 기본 배치 API로 발급합니다. 앱 발급 경로는 Edge Function과 Redis에 의존하지 않으며 호출자의 JWT와 기존 Storage RLS 검사를 사용합니다.
- 이유: 검토 후 사용자가 기본 배치 방식을 선택했습니다. 이 경로는 별도 캐시 서비스 없이 여러 경로를 한 요청으로 묶으면서 기존 권한 검사를 유지합니다.
- 근거: 사용자의 기본 배치 방식 선택 및 적용 요청.
- 반영: [이미지 URL 구현](image-url-cache.md), [백엔드 탐색](backend.md), 공통 데이터 소스와 회귀 테스트.
- 적용 상태: 앱 코드에 적용했습니다. 이 기록만으로 앱 릴리스나 운영 성능 개선을 주장하지 않습니다.
- 대체한 결정: 없음.

## DEC-0004 Codex 설정과 프로젝트 스킬 Git 공유

- 기록일: 2026-10-12 (Asia/Seoul).
- 결정: 기존에 무시했던 `AGENTS.md`, `.codex/`, `.agents/`를 Git 추적 대상으로 전환합니다. 메뉴 스킬의 실제 진입점과 재사용 파일, 호환 연결, 메뉴 등록 정책도 함께 공유합니다. 개인 설정·비밀값·생성 파일·DB 캐시·과거 작업 산출물은 계속 제외합니다.
- 이유: 다른 checkout에서도 동일한 Codex 역할·명령·프로젝트 규칙·스킬을 사용할 수 있도록 저장소에서 관리합니다.
- 근거: 사용자의 “.gitIgnore로 안올렸던 코덱스 설정 파일들 다 develop 에 커밋/푸시해줘” 요청.
- 반영: [AGENTS.md](../../AGENTS.md), [.gitignore](../../.gitignore), [목차](README.md), [유지보수 정책](maintenance.md), Codex 역할·명령, 프로젝트 스킬, 메뉴 최신화 도구와 메뉴 등록 정책.
- 적용 상태: 설정과 스킬의 공유 범위를 반영했습니다. 앱 코드와 스킬의 실행 동작은 변경하지 않았습니다.
- 대체한 결정: DEC-0001의 로컬 전용 공유 범위를 변경합니다. 위키 유지보수 절차와 스킬 소유권 원칙은 유지합니다.

## DEC-0005 Codex 역할 정의 통합과 모델 독립 이름

- 기록일: 2026-10-12 (Asia/Seoul).
- 결정: Codex 역할의 설정과 상세 지침을 `.codex/agents/`의 역할별 독립 TOML로 통합합니다. 모델군별 폴더와 접두사를 제거하고 `planner`, `implementer`, `git_commit`처럼 역할 기반 이름을 사용합니다. 모델과 추론 수준은 각 TOML의 설정값에서 관리합니다.
- 이유: 역할별 설정·지침의 중복을 없애고 모델 변경 때문에 역할 이름과 호출 경로를 다시 바꾸지 않도록 합니다.
- 근거: 사용자의 중복 제거·모델 접두사 금지·레거시 역할 명칭 최신화 요청, [OpenAI 공식 Subagents 문서](https://learn.chatgpt.com/docs/agent-configuration/subagents)의 독립 TOML 정의와 필수 `name`·`description`·`developer_instructions` 형식.
- 반영: [AGENTS.md](../../AGENTS.md), [역할 정의](../../.codex/agents), 관련 스킬·명령·UI metadata, [목차](README.md), [유지보수 정책](maintenance.md), 메뉴 최신화 스킬의 독립 검토자 참조.
- 적용 상태: 10개 역할의 설정과 지침을 통합하고 기존 상세 역할 계약 및 런타임 모델·추론 수준을 유지했습니다. 구현·릴리스·게시 승인 절차와 앱 동작은 변경하지 않았습니다.
- 대체한 결정: 없음. DEC-0004의 공유 범위를 유지하면서 역할 정의 구조와 이름 규칙을 구체화합니다.
