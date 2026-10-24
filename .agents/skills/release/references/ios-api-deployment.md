# iOS API 키 배포

브라우저 세션 대신 App Store Connect 팀 API 키와 Fastlane을 사용한다. `iosApp/Gemfile.lock`의 버전을 `scripts/ios-release.sh`가 설치·실행하며, 일반 Apple ID 인증으로 자동 전환하지 않는다.

## 인증 설정

1. `./scripts/ios-release.sh setup`으로 이 Mac의 의존성과 외부 설정 파일을 준비한다. Ruby 3.2 이상이 필요하며 Homebrew Ruby가 있으면 우선 사용한다.
2. App Store Connect의 사용자 및 액세스 → 통합 → App Store Connect API에서 팀 키를 확인한다. 아직 API 액세스가 없으면 계정 소유자가 Apple 이용 동의와 액세스 신청을 완료해야 한다. 기존 키와 로컬 `.p8`가 짝을 이루면 재사용한다. APNs·Sign in with Apple 키는 App Store Connect 배포 키로 사용하지 않는다.
3. 업로드·심사 제출용 팀 키는 필요한 앱 관리 역할로 발급한다. 키를 새로 만들거나 접근 권한을 확대할 때는 현재 도구의 승인 규칙을 따른다. 인증서·프로파일 접근이 없는 키는 앱 서명을 완료할 수 없으므로 API 인증 성공과 서명 성공을 구분한다.
4. `.p8`는 저장소 밖 `~/.config/ramap/appstore-connect/`에 보관한다. 디렉터리는 `700`, 키와 설정 파일은 `600`으로 제한하고 키 본문·JWT를 출력하지 않는다. GitHub Secrets 등록은 별도 요청이 있을 때만 한다.
5. `~/.config/ramap/appstore-connect/config.json`의 `key_id`, `issuer_id`, `key_filepath`, `team_id`를 채운다. 구조는 `iosApp/fastlane/api-key.example.json`에 있다. `RAMAP_ASC_CONFIG`로 설정 파일 경로를 바꿀 수 있다. `key_filepath`는 외부 `.p8` 파일 경로이며 키 본문을 JSON에 넣지 않는다.
6. `./scripts/ios-release.sh status`로 앱 `com.peto.ramap`의 API 인증과 빌드·버전 조회를 검증한다. 업로드 없이 인증만 검증할 수 있다. API 액세스 미승인이나 키 누락은 설정 완료로 보고하지 않는다.

App Manager API 키의 클라우드 서명이 거부되더라도 관리자 키로 자동 승격하지 않는다. 이 Mac에 유효한 Apple Distribution 인증서와 개인 키가 있어야 로컬 배포 서명을 할 수 있다. 기존 인증서를 먼저 재사용하고, 배포 설정 범위 안에서 새 인증서가 필요한 경우 현재 API 권한으로 발급 가능한지 확인한다. 기존 인증서를 폐기하지 않는다. 전용 키체인을 사용하면 외부 설정의 `signing_keychain_path`, `signing_keychain_password_filepath`에 각각 키체인과 `600` 권한 암호 파일 경로를 지정한다. Archive/export 명령은 해당 키체인만 잠금 해제하며 암호나 개인 키를 출력하지 않는다. 인증서·CSR·개인 키·키체인은 모두 저장소 밖에서 관리한다. 기존 Archive로 `export`를 실행해 IPA 서명까지 검증할 수 있으며 이 검증은 빌드를 업로드하지 않는다.

로컬 배포 인증서에 연결된 `IOS_APP_STORE` 프로파일을 준비하고, 필요한 경우 외부 설정의 `provisioning_profile_uuid`에 UUID를 지정한다. 기존 클라우드 서명 프로파일은 로컬 인증서를 포함하지 않을 수 있다. 이 값이 있으면 IPA 내보내기 단계만 해당 프로파일과 Apple Distribution 인증서를 명시한다. Xcode 프로젝트의 Automatic Signing과 `CODE_SIGN_IDENTITY`는 변경하지 않는다. 프로파일의 인증서·만료일·앱 ID를 확인하고 기존 프로파일을 자동 폐기·교체하지 않는다.

## 실행

```bash
# API 인증 및 현재 빌드/심사 상태 조회
./scripts/ios-release.sh status

# 새 빌드 번호는 App Store Connect의 모든 접수 빌드보다 커야 한다.
# 프로젝트 버전 파일을 바꾸지 않고 지정한 버전/빌드로 Archive와 IPA를 만든다.
./scripts/ios-release.sh archive version:1.1.11 build:21

# 기존 Archive를 API 키로 배포 서명해 로컬 IPA만 생성한다.
./scripts/ios-release.sh export archive:/absolute/path/Ramap.xcarchive

# 서명·Bundle ID·Release Firebase·APNs production을 검사한 IPA 업로드
./scripts/ios-release.sh upload ipa:/absolute/path/Ramap.ipa

# 기본 스토어 배포: 승인 IPA 업로드부터 최종 심사 상태 확인까지
./scripts/ios-release.sh deploy ipa:/absolute/path/Ramap.ipa sha256:APPROVED_SHA256 notes:/absolute/path/notes.txt

# 이미 업로드된 빌드의 처리 대기·심사 제출을 재개
./scripts/ios-release.sh review version:1.1.11 build:21
```

`archive`와 `export`는 `build/ios-release/`에 로컬 산출물을 만들며 업로드하지 않는다. `upload`는 App Store 심사 제출과 외부 테스터 배포를 하지 않고, Apple 처리 완료까지 기다리지 않는다. 업로드 후 `status` 및 필요한 TestFlight 그룹 조회로 처리·설치 가능 상태를 확인한다. 내부 그룹의 Xcode 자동 배포 설정도 실제 연결을 재조회해 판단한다.

`deploy`와 `review`의 최종 심사 제출·재시도·GitHub Actions 설정은 [최종 심사 제출](store-review-deployment.md)을 따른다. `review`는 지정한 버전·빌드만 대상으로 하며 기존 스크린샷·설명과 수출규정 응답을 사용한다. 다음 업데이트의 한국어 App Store 이름은 `iosApp/fastlane/metadata/ko/name.txt`의 `라멥`으로 맞추고, 편집 가능한 앱 정보에서 저장 결과를 재조회한 뒤 심사를 제출한다. 이미 제출된 빌드의 재실행은 이름을 변경하지 않는다. 새 버전은 승인한 한국어 노트와 함께 MANUAL로 생성한다. 기존 정보가 없으면 먼저 보완하고, 다른 준비·심사 버전은 자동 교체하지 않는다. 수동 출시 설정을 유지하므로 심사 승인이 자동 공개를 뜻하지 않는다. 제출 후 `status`로 `WAITING_FOR_REVIEW` 또는 실제 심사 상태를 확인한다. 별도 요청 없이 심사 중인 빌드를 취소·교체하거나 앱을 공개하지 않는다.

API JWT는 실행 중 갱신되지만 `.p8` 자체는 계속 비밀로 보관한다. 키 폐기·계약 미동의·역할 부족·서명 인증서 문제는 브라우저 세션 만료와 별도로 처리한다. API 키 배포가 실패하면 원인과 재현 명령을 보고하고 기존 Organizer 경로로 가능한지 판단하되, 업로드 결과를 먼저 확인해 중복 빌드를 만들지 않는다.

공식 근거: [Apple API 키](https://developer.apple.com/help/app-store-connect/get-started/app-store-connect-api), [Fastlane API 인증](https://docs.fastlane.tools/app-store-connect-api/), [빌드/서명](https://docs.fastlane.tools/actions/build_app/), [TestFlight 업로드](https://docs.fastlane.tools/actions/upload_to_testflight/), [App Store 심사 제출](https://docs.fastlane.tools/actions/upload_to_app_store/).
