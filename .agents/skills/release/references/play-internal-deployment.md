# 승인된 AAB의 Play 내부 테스트 배포

`/release --play-internal` 또는 자연어로 Play 내부 테스트까지 요청한 Android 릴리스에 적용한다. 기본 `/release`와 iOS 배포 범위는 유지한다. GitHub Release에 게시한 `androidApp-release.aab`를 재사용하며 새 AAB 빌드·서명, 스토어 문구·스크린샷 변경, 운영 트랙 승격, 테스터 목록 변경은 수행하지 않는다.

## 최초 연결

1. `.github/workflows/play-internal.yml`, `scripts/play_internal.py`, `androidApp/Gemfile`·lock과 `androidApp/fastlane/`을 저장소 기본 브랜치(`develop`)에 반영한다. `workflow_dispatch`는 기본 브랜치에 워크플로가 있어야 한다. 배포 실행도 `develop`을 ref로 지정한다. 승인 태그는 내려받을 AAB의 출처이며 실행할 코드의 ref가 아니다.
2. Google Play에서 기존 `com.peto.ramap` 앱의 첫 AAB 업로드와 내부 테스트 테스터 설정을 완료한다. 서비스 계정에는 이 앱의 정보 조회와 테스트 트랙 출시 권한만 부여한다. 운영 출시·관리자 권한을 요구하지 않는다. 연결된 Google Cloud 프로젝트의 Google Play Android Developer API를 활성화한다.
3. GitHub OIDC용 Workload Identity Federation provider와 서비스 계정 impersonation을 구성한다. 외부 identity에 해당 서비스 계정의 `roles/iam.workloadIdentityUser`를 부여한다. Provider의 attribute mapping/condition은 실제 GitHub repository ID, `refs/heads/develop`, subject `repo:Ramap-Orgranization/Ramap-KMP:environment:play-internal`로 제한한다. 저장소 이름만 확인하는 광범위한 조건을 사용하지 않는다. 저장소·기본 브랜치를 옮기면 이 조건과 환경 제한을 함께 변경한다.
4. GitHub Environment `play-internal`을 만들고 배포 가능 브랜치를 `develop`으로 제한한다. 다음 **환경 Variables**를 등록한다. 장기 서비스 계정 JSON 키나 Android keystore는 이 워크플로에 필요하지 않다.

| Variable | 값의 형식 |
| --- | --- |
| `PLAY_WIF_PROVIDER` | `projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/POOL/providers/PROVIDER` |
| `PLAY_SERVICE_ACCOUNT` | `SERVICE_ACCOUNT@PROJECT_ID.iam.gserviceaccount.com` |

워크플로의 `id-token: write`와 auth action이 실행 시 임시 ADC 파일을 제공한다. Fastlane은 이 ADC로 Google Play에 접근한다. 환경 required reviewer를 추가했다면 해당 승인 대기도 결과에 표시한다. 기존 Codex 승인만으로 GitHub 환경 보호 규칙을 우회하지 않는다.

설정 조회는 GitHub connector가 지원하면 우선 사용하고, 지원하지 않으면 인증된 `gh api`로 workflow·environment·variable 존재를 확인한다. 설정 누락 시 실제 빠진 항목을 보고한다. 인증정보 생성, 권한 확대, 설정값 추측은 하지 않는다.

## 미리보기와 승인

기존 릴리스 미리보기에 다음을 추가한다.

- 배포 대상: Google Play `com.peto.ramap`, **internal 테스트 트랙**
- 승인한 annotated tag, 그 태그가 가리킬 전체 commit SHA, `versionCode`, 로컬 AAB SHA-256
- GitHub Actions/OIDC 설정 상태와 실행할 워크플로
- GitHub Release 공개 검증 후 위 파일을 내부 테스트 트랙에 등록한다는 원격 작업

사용자가 이 대상까지 승인한 뒤에만 dispatch한다. 파일·버전·소스가 바뀌면 미리보기를 갱신한다. Play 설정이 빠졌는데 GitHub Release만 진행하기로 승인했다면 Play를 실행하지 않고 `미설정`으로 보고한다. `--dry-run`은 조회·미리보기까지만 수행한다.

## 게시 후 실행

1. 기존 절차대로 GitHub Release의 태그 commit, AAB 이름·크기·SHA-256을 검증한다. 이 검증 전에 워크플로를 실행하지 않는다.
2. 승인한 값으로 아래 명령의 환경변수를 채운다. `GITHUB_REPOSITORY`는 현재 저장소의 `owner/name`이다. 사용자 입력을 셸 문자열로 이어 붙이지 않는다.

```bash
gh workflow run play-internal.yml --repo "$GITHUB_REPOSITORY" --ref develop \
    -f tag="$RELEASE_TAG" \
    -f source_sha="$SOURCE_SHA" \
    -f version_code="$VERSION_CODE" \
    -f aab_sha256="$AAB_SHA256"
```

3. dispatch 시각을 기록하고 생성된 run을 조회한다. run name은 `Play internal TAG (VERSION_CODE) AAB_SHA256`이다. workflow, `workflow_dispatch` 이벤트, `develop` 브랜치, 시각·run name을 함께 대조해 이번 실행의 ID를 고른다. 예전 성공 실행을 이번 결과로 사용하지 않는다.

```bash
gh run list --repo "$GITHUB_REPOSITORY" --workflow play-internal.yml \
    --event workflow_dispatch --branch develop --limit 20 \
    --json databaseId,displayTitle,createdAt,status,conclusion,url
gh run view "$PLAY_RUN_ID" --repo "$GITHUB_REPOSITORY" \
    --json status,conclusion,url,jobs
```

4. 실행이 끝나면 run 결론과 `Summarize deployment`의 결과를 확인한다. 결과에는 tag, source SHA, versionCode, AAB SHA-256, GitHub Release URL과 `published` 또는 `already_present`가 포함된다. 실패·취소·skipped 또는 결과 없음은 성공이 아니다.
5. GitHub Release, Play, Firebase와 후속 PR 결과를 따로 보고한다. `internal` 트랙에 `completed`로 등록됐다는 결과가 테스터의 실제 설치나 운영 스토어 공개를 뜻하지 않는다. 테스터 그룹·Google 검토·기기 설치 상태는 별도 확인이며, 이 결과로 강제업데이트 최소 빌드를 갱신하지 않는다.

## 검증과 재시도

- Actions는 기본 브랜치의 배포 코드만 실행하며 입력 태그의 코드를 checkout하지 않는다. 모든 dispatch 입력을 검증하고 annotated tag의 commit을 확인한다.
- 공개된 안정 GitHub Release의 이름이 정확히 일치하는 AAB 하나만 내려받는다. 승인 해시·크기, JAR 서명, 실제 AAB의 package/versionName/versionCode/debuggable을 확인한다. bundletool은 버전과 배포 파일 해시를 고정한다.
- Fastlane은 업로드 직전 로컬 파일 해시를 다시 확인한다. Play edit의 번들 SHA-256도 커밋 전에 대조하고 **internal 트랙 하나만** 갱신한다. 스토어 메타데이터는 변경하지 않는다.
- 같은 버전·해시가 이미 internal에서 `completed`이면 `already_present`로 검증한다. 동일 번들이 접수돼 있지만 트랙 연결만 빠졌으면 재업로드 없이 같은 번들을 연결한다. 다른 해시나 더 최신 Play 빌드가 있으면 중단한다.
- 커밋 후 새 edit으로 번들 해시와 internal 상태를 다시 조회한다. API 오류를 임의로 성공으로 처리하거나 `changes_not_sent_for_review`를 자동 전환하지 않는다.
- 실패 시 기존 run의 실패 job을 재실행하거나 동일 입력으로 다시 dispatch한다. 같은 승인 범위의 재시도에 새 버전·태그·AAB를 만들지 않는다. 업로드 결과가 불명확하면 먼저 같은 입력의 재조회로 상태를 확인한다. 기존 공개 릴리스나 태그는 삭제·이동하지 않는다.
- 앱 단위 concurrency로 Play 작업을 직렬화하며 실행 중인 업로드를 새 요청으로 취소하지 않는다. 대기 중 요청이 교체되거나 취소되었는지도 run 결과로 확인한다. 같은 앱을 Play Console 등에서 동시에 편집하지 않는다.

로컬 검증은 Python 3.11 이상과 `androidApp/Gemfile`의 Ruby/Bundler 환경에서 수행한다.

```bash
python3 -m unittest discover -s scripts/tests -p test_play_internal.py -v
cd androidApp
bundle exec ruby ../scripts/tests/test_play_internal_release.rb
bundle exec fastlane lanes
```

공식 근거: [GitHub workflow_dispatch](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_dispatch), [Google GitHub OIDC 인증](https://github.com/google-github-actions/auth), [Fastlane Google Play](https://docs.fastlane.tools/actions/upload_to_play_store/), [Play bundle 해시](https://developers.google.com/android-publisher/api-ref/rest/v3/edits.bundles).
