---
name: release
description: Ramap Android 버전·GitHub Release·후속 PR과 Play 운영 심사 제출을 수행한다. iOS 업로드·App Store 최종 심사 제출도 포함한다. `/release`, `$release`, 앱 버전 배포·서명 AAB·패치노트·스토어 업로드 요청에 사용한다.
---

# Ramap Release

Android 릴리스는 전용 `release_manager` 서브에이전트로 버전 결정부터 AAB 첨부, 공개 검증, 후속 PR 작성까지 수행한다. 아래 Orchestration, Required Gates와 Release Contract는 Android 절차이며, iOS 요청은 별도의 App Store 배포 절차를 적용한다. 두 플랫폼의 기본 배포 완료 지점은 최종 심사 제출 상태 확인이다.

## Invocation

```text
/release
/release 1.1.0
/release --play-internal
/release 1.1.0 --play-internal
/release --dry-run
```

- 인자가 없으면 최신 안정 태그의 patch를 1 올린 버전을 제안한다.
- 명시적 버전은 `X.Y.Z` 또는 `vX.Y.Z`만 허용하고 최신 안정 SemVer보다 커야 한다.
- `--dry-run`은 Git metadata도 변경하지 않는 읽기 전용 사전 점검과 배포 미리보기까지만 수행한다. 원격 조회가 불가능하면 freshness를 미검증으로 표시한다.
- 소스 브랜치는 `develop`, 배포 브랜치는 `release`로 고정한다.
- 사용자가 iOS 배포를 명시하면 아래 `iOS 배포` 절차를 적용한다. Android와 iOS를 함께 요청하면 각 플랫폼을 최종 심사 제출까지 완료한다. TestFlight만 명시한 요청은 업로드·테스터 확인으로 제한한다.
- `--play-internal` 또는 Play 내부 테스트 배포 요청은 승인 미리보기에 `com.peto.ramap / internal`을 추가한다. 버전 인자와 이 옵션은 따로 파싱한다. 이 옵션은 운영 심사 대신 내부 테스트로 범위를 제한하며, `--dry-run`과 함께 쓰면 설정 조회·미리보기만 수행한다.
- 기본 Android 릴리스와 iOS 스토어 배포는 [최종 심사 제출](references/store-review-deployment.md)을 읽는다. Android는 GitHub Release의 승인된 AAB를 production 심사에 제출하고, iOS는 승인 IPA의 업로드·처리 대기·심사 제출까지 이어간다. 공개는 수동으로 유지한다.
- Play 내부 테스트가 포함되면 [Play 내부 테스트 배포](references/play-internal-deployment.md)를 읽는다. GitHub Release 공개 검증 후 `.github/workflows/play-internal.yml`을 명시적으로 실행하며, 승인한 AAB를 재빌드 없이 배포한다. 구현·설정 변경 요청은 실제 릴리스 실행으로 해석하지 않는다.

## Orchestration

1. `.codex/commands/release.md`, `AGENTS.md`, `.codex/agents/release-manager.toml`을 읽는다. 이 TOML에 실행 모델·추론 수준과 `developer_instructions`의 역할 지침이 함께 있다.
2. Named role `release_manager`에 전체 Android 릴리스 작업을 위임한다. Named role을 사용할 수 없으면 TOML의 모델과 추론 수준으로 별도 에이전트를 생성한다. 담당 에이전트는 같은 작업을 다시 위임하지 않고 준비부터 승인 대기·게시·후속 PR까지 소유한다.
3. 서브에이전트가 현재 Supabase Android 최소 빌드 번호와 새 `versionCode`를 포함한 배포 미리보기와 확인 요청을 반환하면 사용자에게 그대로 제시한다.
4. 사용자가 최신 미리보기와 DB 정책 갱신 여부를 명시적으로 승인하면 같은 서브에이전트에 승인을 전달해 계속한다.
5. 최종 결과에서 브랜치, 태그, 버전, 커밋, 릴리스 URL, 후속 PR URL·상태, AAB 경로·크기·SHA-256과 검증 결과를 확인해 보고한다. Play run URL·결론과 실제 심사 상태를 별도로 보고한다. 내부 테스트만 요청했으면 그 트랙 결과로 구분한다.

릴리스 준비나 게시 작업을 메인 에이전트와 서브에이전트가 나누어 동시에 수행하지 않는다. 릴리스 매니저는 `commit`·`pr` 스킬의 절차와 규칙을 직접 적용하고, 이 하위 작업을 별도 Git 에이전트에 다시 나누지 않는다.

## Required Gates

- dirty worktree, 병합 충돌, 원격 브랜치 divergence, 이미 존재하는 다른 커밋의 태그, 테스트 실패, 서명 설정 누락이 있으면 자동으로 우회하지 않고 중단한다.
- 최신 안정 태그 이후 배포할 commit이 없으면 기본 `/release`는 빈 버전을 만들지 않고 no-op으로 끝낸다. version-only 배포는 사용자가 명시적으로 요청해야 한다.
- 강제업데이트 정책 갱신 여부는 릴리스마다 사용자에게 확인한다. 승인된 경우에만 새 Android `versionCode`를 `app_update_policies.minimum_build_number`에 반영한다.
- 로컬 준비 단계에서는 `release` 병합, 버전 변경, 테스트, AAB 빌드·검증과 `.codex/agents/release-manager.toml`에 정한 로컬 커밋을 준비한다. 병합 커밋과 버전 파일만 포함한 버전 커밋을 분리한다.
- `--dry-run`은 fetch·checkout·edit·build·commit을 포함한 변경 명령을 실행하지 않는다.
- 원격 `release` push, 태그 생성, AAB 업로드, GitHub Release 게시와 후속 PR 작성 전에는 하나의 배포 미리보기를 보여주고 명시적 승인을 받는다.
- Play 워크플로 실행도 원격 업로드 승인 범위에 포함한다. 미리보기에 태그, source SHA, versionCode, AAB SHA-256, `production` 심사(내부 테스트만 요청하면 `internal`) 대상과 수동 게시 설정 상태를 명시하고, 승인된 값만 전달한다. GitHub Release 게시만 승인받은 경우에는 Play 워크플로를 실행하지 않는다.
- 브라우저를 사용할 때는 업로드와 `Publish release` 직전에 브라우저 안전 확인 규칙도 지킨다.
- secrets, `local.properties`, keystore와 생성된 build 산출물을 commit하지 않는다.

## Release Contract

- 다음 `versionCode`는 현재 Android 값보다 정확히 1 크게 한다.
- 다음 `versionName`은 결정된 안정 버전과 일치시킨다.
- 미리보기에 현재 DB 최소 빌드 번호, 새 `versionCode`, DB 갱신 여부를 포함한다. 갱신은 새 앱이 스토어에서 이용 가능해진 뒤 인증된 Supabase 경로로 수행하고, 완료 후 해당 Android 행을 다시 조회해 검증한다.
- 새 앱이 스토어에서 이용 가능하지 않거나 인증된 Supabase 쓰기 경로가 없으면 DB를 갱신하지 않고 수동 작업으로 보고한다.
- `origin/develop`을 `release`에 병합한 커밋과 Android 버전 상승 커밋을 분리한다.
- 커밋 메시지는 저장소의 한국어 `<type>: <subject>` 규칙을 따른다.
- 기본 검증은 `./gradlew testAndroidHostTest`와 `./gradlew :androidApp:bundleRelease`이다.
- AAB에 `jarsigner -verify -verbose -certs`와 SHA-256 계산을 수행한다.
- 패치노트는 이전 태그 이후의 merge commit과 PR 본문을 근거로 사용자 변화 중심으로 작성한다.
- 출시용 릴리스노트는 최종 사용자가 이해할 수 있는 한국어로 작성하고, 승인 미리보기에 전문을 포함한다.
- GitHub Release는 `release` HEAD를 가리키는 annotated `vX.Y.Z` 태그, `Latest` 레이블, 서명된 `androidApp-release.aab` 자산을 사용한다.
- GitHub Release 공개 본문에는 승인된 출시용 릴리스노트를 그대로 반영한다.
- AAB가 붙은 draft를 먼저 검증한 뒤 공개 상태로 전환해 자산 없는 공개 릴리스를 방지한다.
- 게시 후 태그 커밋, 릴리스 URL, AAB 다운로드 URL·크기·digest가 로컬 결과와 일치하는지 확인한다.
- 승인된 Play 배포는 공개 GitHub Release의 동일 AAB를 내려받아 수행한다. 워크플로는 기본 브랜치의 배포 코드로 실행하고 입력 태그의 코드를 실행하지 않는다. SHA-256·서명·패키지·버전 검증에 실패하면 Play 인증과 업로드 전에 중단한다.
- Play 재시도에는 같은 승인 입력과 AAB를 사용한다. Fastlane은 Play의 번들 해시와 대상 트랙·실제 심사 상태를 조회해 같은 제출 빌드이면 재업로드하지 않는다. 다른 해시·더 최신 빌드·상태 조회 실패는 성공으로 처리하지 않는다. Play 실패 후 버전 증가나 GitHub Release 게시를 다시 시작하지 않는다.
- GitHub Release 게시와 Play 배포는 별도 결과이다. Play 실패 시 이미 게시된 릴리스·태그를 삭제하거나 이동하지 않고, 후속 PR은 기존 절차대로 준비하며 Play 실패 단계와 run URL을 보고한다. `internal` 트랙 등록은 운영 출시나 모든 사용자의 업데이트 가능 상태가 아니므로 강제업데이트 DB 갱신의 근거로 사용하지 않는다.
- 공개 검증 후 `release`에서 `develop`로 PR을 작성해 릴리스 버전 변경을 개발 브랜치에 반영할 준비를 한다. 저장소 PR 템플릿과 한국어 제목 규칙을 따르고 릴리스 URL과 검증 결과를 포함하며, 사용자가 별도로 요청하지 않으면 병합하지 않는다.
- 같은 head commit·base의 기존 PR은 재사용해 검증하고, 다른 변경을 포함한 충돌 PR은 자동 수정하거나 닫지 않고 중단한다. 두 브랜치 사이에 차이가 없으면 PR을 만들지 않고 no-op으로 보고한다.
- 생성한 후속 PR은 Codex의 `mcp__codex_app__attach_artifact`로 현재 채팅에 첨부한다.

## iOS 배포

iOS 스토어 배포는 Android 버전·AAB·GitHub Release 절차와 분리해서 처리한다. 기본은 `deploy`로 최종 App Store 심사 제출까지 수행한다. TestFlight만 요청한 경우 `upload`와 테스터 확인 절차를 사용한다.

### 사전 확인

- 현재 브랜치와 작업 트리를 확인하고, 사용자가 지정한 `develop` 기준 변경을 보존한다. 기존 미커밋 작업을 reset하거나 덮어쓰지 않는다.
- App Store Connect의 TestFlight 빌드 목록에서 앱의 최신 빌드 번호를 확인한다. `CURRENT_PROJECT_VERSION`은 App Store Connect에 이미 접수된 가장 큰 빌드 번호보다 커야 한다. 버전 문자열은 `MARKETING_VERSION`과 App Store Connect 버전 정보가 일치하는지 확인한다.
- Release 대상의 Bundle ID가 `com.peto.ramap`인지 확인한다. Debug Firebase plist가 Release 앱에 포함되지 않도록 Archive 안의 Firebase 설정을 확인하고, APNs entitlement가 Release용 `production`인지 확인한다.
- 코드 서명 자동 관리가 설정되어 있는지 확인한다. `CODE_SIGN_IDENTITY`를 `Apple Distribution`으로 고정해 Automatic Signing과 충돌시키지 않는다.

### Archive와 업로드

- API 키가 설정되어 있으면 [API 키 배포 절차](references/ios-api-deployment.md)를 읽고 `scripts/ios-release.sh`를 사용한다. 로컬 Archive/IPA를 검증한 뒤 `deploy`로 업로드부터 최종 심사 제출까지 실행한다. 이미 업로드된 빌드는 `review`로 재개한다. 인증 설정만 요청받았을 때 새 빌드를 업로드하거나 심사를 제출하지 않는다.
- iOS workspace(`iosApp/iosApp.xcworkspace`)와 앱 scheme으로 Release Archive를 만든다. 아카이브의 앱 버전, 빌드 번호, Bundle ID, APNs 환경과 Release Firebase 설정을 확인한다.
- API 키를 사용할 수 없으면 Xcode Organizer에서 해당 Archive를 선택해 `Distribute App` → `App Store Connect`로 진행한다. Organizer의 업로드 단계가 자동 서명과 App Store Connect 업로드를 맡는다.
- Archive에 `Apple Development` 서명이 보이더라도 Organizer가 App Store Connect 배포 서명을 적용할 수 있다. 따라서 Archive의 서명만으로 업로드 가능 여부를 판정하지 않는다.
- `xcodebuild -exportArchive`가 `No Accounts` 또는 `No signing certificate "iOS Distribution" found`로 실패해도 Xcode Organizer 업로드가 불가능하다고 단정하지 않는다. 이 오류는 CLI export 환경의 계정/서명 상태를 나타낼 수 있다. Organizer에서 실제 업로드를 시도하고, 그 단계의 구체적 결과를 기준으로 판단한다.
- Xcode Organizer에서 `Upload completed` 또는 `Uploaded to Apple` 상태가 확인될 때 업로드 성공으로 기록한다. dSYM 경고가 있어도 앱 업로드가 완료됐다면 TestFlight 처리를 계속 확인하고, 누락된 프레임워크 심볼과 경고를 결과에 보고한다.

### App Store Connect 검증

- 기본 스토어 배포는 [최종 심사 제출](references/store-review-deployment.md)의 실제 심사 상태·선택된 build ID·수동 게시 검증까지 수행한다. 업로드 성공이나 TestFlight 준비 상태만으로 완료 처리하지 않는다. 아래 테스터 그룹·기기 검증은 TestFlight 요청 범위에 적용한다.

- App Store Connect → TestFlight → iOS의 빌드 업로드 상태에서 업로드한 버전과 빌드 번호를 확인한다. `Processing` 동안은 테스터 설치 가능 상태라고 보고하지 않는다.
- Apple 처리가 끝나면 빌드 상세에 표시된 상태를 확인한다. `Missing Export Compliance` 또는 수출 규정 문서 누락이 있으면 앱의 실제 암호화 사용을 근거로 필요한 항목을 완료하고, 임의로 답을 선택하지 않는다.
- 빌드가 테스트 가능한 상태인지, 의도한 내부/외부 테스터 그룹이 연결됐는지 확인한다. 그룹이 연결되지 않았다면 사용자가 의도한 기존 그룹만 연결하고 저장 결과를 다시 확인한다.
- TestFlight에 빌드가 보이는 것과 기기에 설치된 것을 구분한다. 알림 수신 테스트는 새 빌드 설치 후 앱이 최신 FCM 토큰을 등록한 다음 진행한다. 이전 빌드 토큰으로 전송한 결과를 새 빌드 검증으로 간주하지 않는다.
- 최종 보고에는 브랜치/소스 SHA, 앱 버전·빌드 번호, Archive 경로, Organizer 업로드 결과, App Store Connect 처리 상태, 최종 심사 상태·수동 게시 여부, 요청한 테스터 그룹, dSYM 또는 수출 규정 경고, 알림 테스트 여부를 적는다.

## Recovery

- GitHub connector 또는 인증된 `gh`가 릴리스와 자산 업로드를 지원하면 우선 사용한다.
- 그렇지 않으면 연결된 Chrome의 로그인 세션을 사용한다.
- Chrome 파일 업로드 권한이 없으면 릴리스 양식을 보존하고 권한 설정 방법을 안내한 뒤 중단한다.
- 같은 커밋의 draft는 이어서 처리하고 같은 공개 릴리스는 멱등 성공으로 검증한다. 다른 커밋의 tag나 release는 삭제·이동하지 않고 중단한다.
