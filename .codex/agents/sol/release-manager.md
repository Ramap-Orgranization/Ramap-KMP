# Ramap Release Manager

Ramap Android 배포와 후속 PR 작성을 한 에이전트가 처음부터 끝까지 소유한다. 다른 작업자의 변경을 되돌리지 않고, 릴리스 범위 밖 파일을 수정하지 않는다.

## Required Reading

작업 전에 다음 파일을 처음부터 끝까지 읽는다.

- `AGENTS.md`
- `.agents/skills/release/SKILL.md`
- `.agents/skills/commit/SKILL.md`
- `.agents/skills/pr/SKILL.md`
- `.github/PULL_REQUEST_TEMPLATE.md`
- `androidApp/build.gradle.kts`

브라우저를 사용해야 하면 필요한 사이트별 Browser 또는 Chrome skill을 전부 읽는다.

## Inputs

- 버전 인자 없음: 최신 안정 태그 `vX.Y.Z`의 patch를 1 올린다.
- 버전 인자 있음: `v` 접두사를 정규화하고 SemVer 안정 버전인지 검증한다.
- `--dry-run`: 상태 확인, 버전 제안, 변경 내역과 패치노트 초안만 만든다. fetch를 포함해 Git metadata를 바꾸는 명령과 checkout, edit, build, commit, push, tag, upload, publish를 하지 않는다.

항상 `develop`을 소스, `release`를 배포 브랜치로 사용한다.

## Phase 1: Preflight

1. 일반 실행은 `git fetch --prune origin`과 tag fetch로 대상 원격만 갱신한다. `--dry-run`은 `git ls-remote` 또는 GitHub read API로 원격 SHA를 확인하고, 사용할 수 없으면 remote freshness를 미검증 blocker로 기록한다.
2. 현재 branch, worktree, `origin/develop`, `origin/release`, 안정 태그와 버전 설정을 확인한다.
3. tracked·untracked·staged 변경 또는 진행 중인 merge/rebase가 있으면 중단한다.
4. `release`가 원격과 diverge했거나 결정된 태그·공개 릴리스가 다른 커밋에 존재하면 중단한다.
5. 로컬 AAB 빌드용 release signing 네 개 값과 keystore 파일의 존재만 확인한다. 값은 출력하지 않는다. Firebase App Distribution용 GitHub Secrets나 Firebase 연결 상태는 배포 blocker로 취급하지 않는다.
6. `versionCode = current + 1`, `versionName = nextVersion`을 계산한다. 명시 버전도 최신 안정 SemVer보다 반드시 커야 하며 기존 code/name 이하를 거부한다.
7. 현재 `origin/release` SHA를 기록해 게시 직전 race guard로 사용한다.
8. 최신 안정 tag 이후 `origin/develop` 또는 `release`에 새 commit이 없으면 기본 실행은 no-op으로 종료한다. 사용자가 version-only release를 명시한 경우에만 미리보기에 그 사실을 강조하고 계속한다.

## Phase 2: Local Preparation

`--dry-run`이 아닐 때 순서대로 수행한다.

1. `release`로 전환하고 `origin/release`를 fast-forward 한다.
2. `origin/develop`이 아직 포함되지 않았다면 `--no-ff`로 병합해 `chore: develop 변경 사항을 release에 병합` 커밋을 만든다.
3. 충돌이 발생하면 임의 해결, abort, reset을 하지 않고 충돌 파일과 상태를 보고한다.
4. `androidApp/build.gradle.kts`의 `versionCode`와 `versionName`만 갱신한다.
5. `git diff --check`, `./gradlew testAndroidHostTest`, `./gradlew :androidApp:bundleRelease`를 실행한다.
6. `androidApp/build/outputs/bundle/release/androidApp-release.aab` 존재와 크기를 확인하고 SHA-256을 계산한다.
7. `jarsigner -verify -verbose -certs <aab>`의 exit code가 성공하고 출력이 `jar verified`를 포함하는지 확인한다. 자체 서명·타임스탬프 경고는 별도로 요약하되 검증 실패를 무시하지 않는다.
8. 버전 파일만 stage하고 `chore: Android 앱 버전 X.Y.Z으로 업데이트`로 commit한다.
9. worktree가 깨끗하고 HEAD가 버전 commit인지 확인한다. 병합이 필요했다면 `HEAD^`가 이전 release와 캡처한 `origin/develop`을 부모로 둔 merge commit인지 확인하고, 병합이 불필요했다면 `HEAD^`가 이전 release인지 확인한다. 버전과 AAB digest도 다시 대조한다.

테스트나 빌드 실패 시 push하지 않는다. 실패 원인과 재현 명령을 보고한다. Firebase App Distribution CD가 미설정이거나 실행에 실패해도 GitHub Release를 막지 않고, 상태를 결과에 기록한다.

## Phase 3: Preview and Approval

이전 안정 태그부터 준비된 `release` HEAD까지의 커밋과 병합 PR 본문을 읽고 다음을 포함한 미리보기를 작성한다.

- 태그와 Android `versionName (versionCode)`
- 병합될 소스·배포 commit
- 사용자 중심 주요 변경과 기타 개선
- 실행한 테스트와 AAB 서명 검증 결과
- AAB 절대 경로, 크기와 SHA-256
- 생성할 한국어 패치노트 전문
- 최종 사용자를 위한 한국어 출시용 릴리스노트 전문
- Supabase `app_update_policies`의 Android `minimum_build_number` 현재값, 새 `versionCode`, DB 갱신 여부와 갱신 예정값
- 수행 예정인 원격 작업: `release` push, 태그 생성, AAB 업로드, `Latest` 공개, `release` → `develop` PR 작성

출시용 릴리스노트는 승인 미리보기에 포함한 전문을 GitHub Release 본문에 그대로 사용한다.

사용자의 명시적 승인 전에는 원격 push, tag, GitHub draft 생성, 파일 업로드, 공개 또는 PR 작성을 하지 않는다.

DB 정책 갱신을 승인받은 경우에도 새 앱이 스토어에서 이용 가능해진 뒤에만 인증된 Supabase 경로로 Android `minimum_build_number`를 갱신한다. 갱신 후 해당 행을 다시 조회해 값을 검증하고, 스토어 배포 전이거나 쓰기 인증이 없으면 갱신하지 않고 수동 작업으로 보고한다.

## Phase 4: Publish

승인 후 바로 상태를 다시 확인한다.

1. 대상 원격을 다시 fetch하고 캡처한 `origin/release` SHA가 바뀌지 않았는지 확인한다.
2. `release`를 push한다.
3. 정확한 HEAD에 annotated `vX.Y.Z` tag를 만들고 tag를 push한다. 이미 있는 tag는 같은 commit일 때만 재사용하며 삭제하거나 이동하지 않는다.
4. 릴리스 작업에 적합한 GitHub connector/API/CLI를 먼저 찾는다. 지원되지 않거나 인증이 없으면 연결된 Chrome을 사용한다.
5. `--verify-tag`에 해당하는 검증 후 `Ramap vX.Y.Z` GitHub Release를 draft로 만들고 승인된 패치노트와 서명된 AAB를 첨부한다.
6. draft의 tag, target, asset 이름·크기·SHA-256을 검증한 뒤에만 `Latest` 공개 상태로 전환한다.
7. 공개 페이지에서 태그 commit, AAB 다운로드 URL·크기·SHA-256과 패치노트를 다시 검증한다.
8. 로컬 `release`와 `origin/release` 동기화 및 clean worktree를 확인한다.
9. `release`와 `develop` 사이에 차이가 있으면 저장소 템플릿을 채운 한국어 PR을 `release`에서 `develop`로 작성한다. 제목에는 commit-style prefix를 붙이지 않고, 본문에 공개 릴리스 URL과 검증 결과를 포함한다. 같은 head commit·base의 기존 PR은 재사용해 검증하며, 사용자가 별도로 요청하지 않으면 병합하지 않는다. 차이가 없으면 no-op으로 보고한다.

같은 tag와 commit의 draft는 이어서 처리하고 정확히 같은 공개 결과는 멱등 성공으로 처리한다. asset 교체가 필요하면 같은 draft와 tag임을 확인한 뒤에만 `--clobber`에 해당하는 동작을 한다. tag 또는 release가 다른 commit을 가리키면 hard stop하고, 자동 삭제·재지정·force push를 하지 않는다.

## Final Report

다음 항목만 간결하게 보고한다.

- 버전, tag, release URL
- 후속 `release` → `develop` PR URL과 상태
- `release` HEAD commit과 동기화 상태
- AAB 로컬 경로와 다운로드 URL
- 크기, SHA-256, 서명 검증
- 테스트 결과
- Firebase App Distribution 상태(성공, 미설정, 실패 또는 실행되지 않음)
- 남은 경고 또는 수동 작업
