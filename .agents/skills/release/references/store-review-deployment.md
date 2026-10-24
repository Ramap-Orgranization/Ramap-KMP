# Android · iOS 최종 심사 제출

기본 스토어 배포의 완료 지점은 승인된 빌드의 **최종 심사 제출 상태 확인**이다. Android는 `production`, iOS는 App Store 심사이며 공개는 각각 Managed publishing과 `MANUAL`로 유지한다. 내부 테스트나 TestFlight만 명시한 요청은 그 범위로 제한한다. 구현·설정 변경 요청만으로 새 버전을 만들거나 기존 심사를 취소하지 않는다.

## Android

`.github/workflows/play-review.yml`은 기본 브랜치의 배포 코드를 실행하고 공개 GitHub Release의 승인 AAB를 재사용한다. 기존 `play-internal.yml`은 내부 테스트 전용으로 남으며 두 워크플로는 앱 단위 concurrency를 공유한다.

최초 연결:

1. 워크플로와 배포 코드를 기본 브랜치 `develop`에 반영한다.
2. `play-review` Environment의 배포 브랜치를 `develop`으로 제한하고 환경 Variables `PLAY_WIF_PROVIDER`, `PLAY_SERVICE_ACCOUNT`를 설정한다. 서비스 계정에는 이 앱의 조회·운영 출시 권한이 필요하다. 내부 테스트 전용 권한으로 충분하다고 보고하지 않는다.
3. OIDC provider 조건과 서비스 계정 impersonation은 repository ID, `refs/heads/develop`, subject `repo:Ramap-Orgranization/Ramap-KMP:environment:play-review`로 제한한다. 기존 `play-internal` subject를 그대로 복사하지 않는다.
4. 실행마다 Play Console 게시 개요에서 **Managed publishing ON**과 이번 배포 외의 미제출 변경이 없는지 확인한다. 공개 API는 수동 게시 설정이나 모든 정책·메타데이터 초안을 반환하지 않으므로 `managed_publishing=true`는 실제 확인을 기록하는 입력이다. 확인 없이 추측하거나 `completed` 상태를 수동 게시 설정의 근거로 삼지 않는다.

승인 미리보기에는 production 심사 제출, 수동 게시 설정, tag/source SHA/versionCode/AAB SHA-256 및 공개 GitHub Release의 한국어 노트 전문을 포함한다. 노트는 Play 제한인 500자 이내여야 한다. 사용자가 이 범위를 이미 승인했으면 같은 입력의 재시도에 승인을 다시 요청하지 않는다.

```bash
gh workflow run play-review.yml --repo "$GITHUB_REPOSITORY" --ref develop \
    -f tag="$RELEASE_TAG" -f source_sha="$SOURCE_SHA" \
    -f version_code="$VERSION_CODE" -f aab_sha256="$AAB_SHA256" \
    -f managed_publishing=true
```

`Play review TAG (VERSION_CODE) AAB_SHA256` run name, dispatch 시각, ref와 이벤트를 대조해 이번 run을 확인한다. `result.json`의 `review_state`가 `IN_REVIEW` 또는 `APPROVED_NOT_PUBLISHED`여야 완료다. 단순 업로드·track의 `completed`·edit commit 성공만으로 완료 처리하지 않는다.

- 동일 해시의 동일 빌드가 이미 심사 중이면 재업로드·재제출하지 않는다.
- `changesNotSentForReview=false`로 요청하며 실패 시 draft로 자동 우회하지 않는다.
- `changesInReviewBehavior=ERROR_IF_IN_REVIEW`로 다른 심사를 자동 취소하지 않는다.
- Google이 Console 제출을 요구하거나 조회 대기 시간이 끝나면 이번 버전만 게시 개요에서 확인하고, 승인된 범위 안에서 최종 제출까지 이어간다. 다른 미제출 변경이 섞였으면 자동으로 일괄 제출하지 않는다. 로그인·권한 때문에 진행 불가능하면 정확한 남은 단계를 보고한다.
- 심사 요청은 아직 이용 가능한 스토어 버전이 아니므로 강제업데이트 최소 빌드는 바꾸지 않는다.

## iOS

API 키·Archive·서명 절차는 [iOS API 배포](ios-api-deployment.md)를 따른다. 로컬에서 승인한 IPA는 다음 한 명령으로 업로드 → Apple 처리 대기 → 빌드 연결 → 심사 제출 → 상태 재조회까지 진행한다.

```bash
./scripts/ios-release.sh deploy ipa:"$IPA_PATH" sha256:"$IPA_SHA256" notes:"$RELEASE_NOTES_PATH"
```

수출규정이 미응답이면 앱의 암호화 사용을 확인한 승인 근거가 있을 때만 `exempt_encryption:true`를 추가한다. 기본값으로 규정 응답을 추측하지 않는다. 기존 스크린샷과 스토어 설명을 유지하고 지정한 한국어 출시 노트를 반영한다. 한국어 App Store 이름은 `iosApp/fastlane/metadata/ko/name.txt`의 `라멥`으로 편집 가능한 앱 정보에 저장하고 재조회한 뒤 제출한다. 이미 심사에 제출된 버전은 이름을 자동 변경하지 않는다. 편집 가능한 버전이 없으면 노트를 받아 지정 버전을 `MANUAL`로 생성한다. 다른 버전이 준비·심사 중이면 자동 교체하지 않는다.

이미 업로드했거나 업로드 응답이 불명확할 때는 재업로드하지 않고 다음 명령으로 재개한다. Apple 처리 대기는 최대 약 60분, 제출 상태 확인은 약 10분이며 시간 초과는 성공이 아니다.

```bash
./scripts/ios-release.sh review version:"$IOS_VERSION" build:"$IOS_BUILD" notes:"$RELEASE_NOTES_PATH"
```

업로드된 빌드의 이후 절차는 `.github/workflows/ios-review.yml`에서도 실행할 수 있다. `app-store-review` Environment를 `develop`으로 제한하고 Secrets `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_PRIVATE_KEY`, Variable `ASC_TEAM_ID`를 구성한다. 키는 실행 중 외부 임시 디렉터리에 `600`으로 저장하고 종료 시 삭제한다. 서명 인증서나 IPA를 GitHub에 올릴 필요는 없다.

```bash
gh workflow run ios-review.yml --repo "$GITHUB_REPOSITORY" --ref develop \
    -f version="$IOS_VERSION" -f build="$IOS_BUILD" \
    -f release_notes="$RELEASE_NOTES" -f exempt_encryption=false
```

이번 run의 버전·빌드와 `IOS_REVIEW_RESULT`를 확인한다. 선택된 build ID까지 일치하고 `WAITING_FOR_REVIEW`, `IN_REVIEW`, `PENDING_DEVELOPER_RELEASE` 중 하나여야 완료다. 같은 빌드의 재실행은 조회로 완료하며 다른 빌드가 제출돼 있으면 중단한다. 심사 취소나 실제 공개는 별도 명시 요청이 있어야 한다.

## 결과와 검증

플랫폼별 run URL 또는 로컬 실행 결과, 버전·빌드, 실제 심사 상태, 수동 게시 여부와 남은 작업을 구분한다. 새 workflow가 기본 브랜치에 없거나 인증 환경이 없으면 코드 구현 완료와 CD 활성화를 구분해 보고한다.

검증은 Python AAB provenance tests, Play 내부/운영 심사 Ruby tests, iOS 서명/심사 Ruby tests와 Fastlane lanes를 사용하며 `.github/workflows/release-tools.yml`이 이를 연결한다. 워크플로 변경은 로컬 actionlint로 추가 검사한다. 테스트에는 실제 스토어 쓰기를 사용하지 않는다.

공식 근거: [Play edit commit](https://developers.google.com/android-publisher/api-ref/rest/v3/edits/commit), [Play 실제 심사 상태](https://developers.google.com/android-publisher/api-ref/rest/v3/applications.tracks.releases), [Managed publishing](https://support.google.com/googleplay/android-developer/answer/9859654), [Fastlane App Store 제출](https://docs.fastlane.tools/actions/upload_to_app_store/).
