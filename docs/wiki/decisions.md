---
authority: canonical
status: active
last_verified: 2026-10-13
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

## DEC-0006 리뷰 목록의 가게 카드와 상세 표시 통합

- 기록일: 2026-10-12 (Asia/Seoul).
- 결정: 내 리뷰와 다른 사람 리뷰 목록에서 공통 가게 리뷰 카드를 사용합니다. 내 리뷰를 선택하면 현재 목록 위에 기존 가게 상세 바텀시트를 표시하고, 상세의 지도 보기에서 지도 화면으로 이동합니다.
- 이유: 두 리뷰 목록에서 같은 가게 정보를 보여주고 리뷰를 탐색하는 동안 현재 목록의 필터·스크롤 상태를 유지합니다.
- 근거: [이슈 #265](https://github.com/Ramap-Orgranization/Ramap-KMP/issues/265)의 범위와 사용자 계획 승인.
- 반영: [제품 정책](product-policy.md), [앱 실행 흐름](app-flows.md#리뷰-목록에서-가게-상세), [디자인시스템](design-system.md#가게-리뷰-목록-카드), 공통 카드·데이터 조회·리뷰 목록과 내비게이션 연결.
- 적용 상태: 구현은 [검증된 PR 코드 커밋](../../feature/review/my/src/commonMain/kotlin/com/peto/ramap/ui/review/my/MyReviewsRoute.kt) `c559e52c3248076dacab2f2928622d9d787bc1e5`에 반영됐습니다. 최종 PR 코드에서 Android APK와 iOS Simulator framework 빌드, 관련 모듈 lint가 통과했습니다. 스토어 앱 배포를 뜻하지 않습니다.
- 대체한 결정: 없음.

## DEC-0007 내 리뷰 상세 닫기에서 목록 재조회 제외

- 기록일: 2026-10-12 (Asia/Seoul).
- 결정: 내 리뷰에서 매장 상세를 변경 없이 닫을 때는 리뷰 목록을 다시 조회하지 않습니다. 상세에서 본인 리뷰를 삭제하면 해당 항목과 집계를 현재 목록 상태에 반영하고, 리뷰 작성·수정 화면에서 복귀할 때는 기존 목록 갱신 동작을 유지합니다.
- 이유: 단순 상세 열람은 리뷰 목록을 바꾸지 않으며, 재조회하면 새 서명 이미지 URL로 인해 기존 리뷰 사진까지 다시 로드됩니다.
- 근거: 사용자가 상세를 열고 닫을 때 전체 리뷰 이미지가 갱신되는 현상을 보고하고, 단순 닫기 시 재조회를 제외하는 수정에 동의했습니다.
- 반영: [제품 정책](product-policy.md), [앱 실행 흐름](app-flows.md#리뷰-목록에서-가게-상세), [MyReviewsRoute](../../feature/review/my/src/commonMain/kotlin/com/peto/ramap/ui/review/my/MyReviewsRoute.kt).
- 적용 상태: 구현은 검증된 PR 코드 커밋 `c559e52c3248076dacab2f2928622d9d787bc1e5`에 반영됐습니다. 닫기·복귀 경로의 실기기 확인은 이전 `1.1.9-debug` APK 기준이며, 최종 dialog 포함 코드를 기기에 재설치한 결과는 아닙니다. 스토어 앱 릴리스를 뜻하지 않습니다.
- 대체한 결정: 없음.

## DEC-0008 리뷰 변경 기록으로 화면 동기화

- 기록일: 2026-10-12 (Asia/Seoul).
- 결정: 성공한 리뷰 변경은 repository가 한 번 발행하고 화면·상세 캐시가 cursor로 적용합니다. 세션 전환은 기록을 초기화하고 접근 변경·기록 공백은 필요한 조회를 복구합니다. passive 복귀는 리뷰 조회·서명을 시작하지 않습니다.
- 이유: 중첩 작성·수정·삭제의 부모 목록 누락과 전체 이미지 재로딩을 함께 해결하고 페이지·필터·스크롤을 유지합니다.
- 근거: 사용자가 공통 ReviewChangeStore revision journal 방식을 승인하고, 정상 변경마다 전체 목록을 조회하지 않도록 요청했습니다.
- 반영: [설계 패턴](design-patterns.md#리뷰-변경-기록과-화면-구독), [실행 흐름](app-flows.md#리뷰-화면-복귀와-변경-반영), [제품 정책](product-policy.md), [백엔드](backend.md).
- 적용 상태: 앱 구현은 검증된 PR 코드 커밋 `c559e52c3248076dacab2f2928622d9d787bc1e5`에 반영됐으며 앱 스토어에는 배포하지 않았습니다. 리뷰 v2 RPC는 2026-10-13에 migration `20261012151044`로 운영 배포됐고 기존 UUID v1 API를 유지합니다. 메타데이터 보완 이후 domain 126 + data 145 + MyReviews feature 8, 총 279개 Android host test가 통과했습니다. 이 279건에는 OtherReviews 테스트가 포함되지 않습니다. 이전 463개 host test와 10개 모듈 lint·debug APK·iOS framework 결과는 메타데이터 보완 전 코드 기준입니다. Android 16 실기기에서는 리뷰 목록 복귀·상태·v2 임시 쓰기·사진 교체·계정 전환과 새 리뷰 매장 요약 첫 렌더를 이전 `1.1.9-debug` APK에서 확인했습니다. 최종 코드 그룹 1–9의 branch check는 모두 exit 0이며 `compileAndroidHostTest` 작업은 테스트 코드를 컴파일했지만 실행하지 않았습니다. PR9 코드에서는 Android APK, iOS Simulator framework 및 관련 lint도 통과했습니다. 상세 한계는 [백엔드](backend.md)와 [이미지 URL](image-url-cache.md)에 기록합니다.
- 대체한 결정: [DEC-0007](#dec-0007-내-리뷰-상세-닫기에서-목록-재조회-제외).

## DEC-0009 위키 갱신 시점

- 기록일: 2026-10-13 (Asia/Seoul).
- 결정: 구현·검증 작업마다 위키를 갱신하지 않고, 최종 PR 작성 단계에 누적된 로컬 변경 이력과 최종 diff를 대조해 관련 위키 내용을 모아 갱신합니다. 명시적으로 요청된 위키 수정은 요청 시 처리합니다.
- 이유: 진행 중인 작업의 반복적 문서 수정을 줄이면서 최종 코드·검증 결과와 위키 설명을 PR에서 함께 검토할 수 있게 합니다.
- 근거: 사용자가 최종 PR 준비 단계에서만 위키 사실·결정·후보를 갱신하도록 명시적으로 요청했습니다. 기록은 로컬 `docs/change-history/CHANGELOG.md`에 유지합니다.
- 반영: [유지보수 정책](maintenance.md), [PR 템플릿](../../.github/PULL_REQUEST_TEMPLATE.md).
- 적용 상태: 이 문서와 PR template, repository instructions/skills에 procedure가 반영됐습니다. 구현 규칙은 소유 Ramap 스킬에 유지합니다.
- 대체한 결정: 없음.

## DEC-0010 변경 목적별 작은 PR과 의존 관계

- 기록일: 2026-10-13 (Asia/Seoul).
- 결정: PR 분리와 의존 관계의 기준은 [PR 스킬](../../.agents/skills/pr/SKILL.md#small-prs)이 소유하고, 커밋 스킬·저장소 지침·PR 템플릿에서 연결합니다.
- 이유: 여러 책임의 변경을 목적별로 검토하고 순서대로 반영할 수 있도록 이번 리뷰 개선의 분리 방식을 이후 작업에도 적용합니다.
- 근거: 사용자가 리뷰 개선을 작은 PR 10개로 나눈 결과를 확인한 뒤 같은 분리 규칙을 추가하도록 명시적으로 요청했습니다. 구체적인 적용 사례는 [PR #266](https://github.com/Ramap-Orgranization/Ramap-KMP/pull/266)부터 [PR #275](https://github.com/Ramap-Orgranization/Ramap-KMP/pull/275)입니다.
- 반영: [PR 스킬](../../.agents/skills/pr/SKILL.md#small-prs), [커밋 스킬](../../.agents/skills/commit/SKILL.md), [AGENTS.md](../../AGENTS.md), [PR 템플릿](../../.github/PULL_REQUEST_TEMPLATE.md).
- 적용 상태: 소유 스킬과 연결 지침에 반영했습니다. 문서 갱신 시점은 DEC-0009를 유지하며 코드 변경의 검증 결과는 각 PR의 실제 기준 커밋으로 구분합니다.
- 대체한 결정: 없음.
