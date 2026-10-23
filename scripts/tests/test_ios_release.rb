require "minitest/autorun"
require "fileutils"
require "tmpdir"
require_relative "../../iosApp/fastlane/release_support"

class IosReleaseTest < Minitest::Test
    def setup
        @temporary = Dir.mktmpdir("ramap-ios-release-test-")
        @previous_config = ENV["RAMAP_ASC_CONFIG"]
        ENV["RAMAP_ASC_CONFIG"] = File.join(@temporary, "config.json")
    end

    def teardown
        ENV["RAMAP_ASC_CONFIG"] = @previous_config
        FileUtils.remove_entry(@temporary)
    end

    def test_명시한_정상_버전과_빌드만_허용한다
        assert_equal ["1.2.3", "21"], RamapRelease.version_and_build(version: "1.2.3", build: "21")
        [{}, { version: "1.2.3", build: "0" }, { version: "1.2.3;touch /tmp/example", build: "21" }].each do |options|
            assert_raises(RuntimeError) { RamapRelease.version_and_build(options) }
        end
    end

    def test_기존에_접수된_모든_빌드보다_큰_번호가_필요하다
        assert_raises(RuntimeError) { RamapRelease.ensure_new_build!("20", ["18", "20", "19"]) }
        assert_raises(RuntimeError) { RamapRelease.ensure_new_build!("19", ["20"]) }
        RamapRelease.ensure_new_build!("21", ["18", "20", "19"])
    end

    def test_인증_설정이_없거나_비어있으면_실패한다
        assert_raises(RuntimeError) { RamapRelease.load_credentials }
        File.write(ENV.fetch("RAMAP_ASC_CONFIG"), JSON.generate(key_id: ""))
        assert_raises(RuntimeError) { RamapRelease.load_credentials }
    end

    def test_저장소_밖의_보호된_키를_읽는다
        key = File.join(@temporary, "AuthKey_TEST.p8")
        File.write(key, "test fixture only", perm: 0o600)
        configure(key)
        assert_equal File.realpath(key), RamapRelease.load_credentials.fetch("key_filepath")
    end

    def test_다른_사용자가_읽을_수_있는_키는_거부한다
        key = File.join(@temporary, "AuthKey_TEST.p8")
        File.write(key, "test fixture only", perm: 0o644)
        configure(key)
        assert_raises(RuntimeError) { RamapRelease.load_credentials }
    end

    def test_전용_키체인이_없으면_기존_서명_환경을_사용한다
        assert_nil RamapRelease.unlock_signing_keychain!({})
    end

    def test_공개된_키체인_암호는_사용하기_전에_거부한다
        password = File.join(@temporary, "keychain-password")
        File.write(password, "test fixture only", perm: 0o644)
        credentials = {
            "signing_keychain_path" => File.join(@temporary, "missing.keychain-db"),
            "signing_keychain_password_filepath" => password
        }
        assert_raises(RuntimeError) { RamapRelease.unlock_signing_keychain!(credentials) }
    end

    def test_저장소_안의_키를_가리키는_심볼릭_링크도_거부한다
        Dir.mktmpdir("ios-key-test-", RamapRelease::ROOT) do |directory|
            key = File.join(directory, "AuthKey_TEST.p8")
            File.write(key, "test fixture only", perm: 0o600)
            link = File.join(@temporary, "AuthKey_LINK.p8")
            File.symlink(key, link)
            configure(link)
            assert_raises(RuntimeError) { RamapRelease.load_credentials }
        end
    end

    def test_다른_앱이나_디버그_Firebase가_포함된_아카이브는_거부한다
        archive = File.join(@temporary, "Ramap.xcarchive")
        app = File.join(archive, "Products", "Applications", "Ramap.app")
        FileUtils.mkdir_p(app)
        File.write(File.join(app, "Info.plist"), JSON.generate(CFBundleIdentifier: "com.example.other"))
        assert_raises(RuntimeError) { RamapRelease.archive_path(archive) }
        File.write(File.join(app, "Info.plist"), JSON.generate(CFBundleIdentifier: RamapRelease::BUNDLE_ID))
        File.write(File.join(app, "GoogleService-Info-Release.plist"), JSON.generate(BUNDLE_ID: "com.peto.ramap.debug"))
        assert_raises(RuntimeError) { RamapRelease.archive_path(archive) }
        File.write(File.join(app, "GoogleService-Info-Release.plist"), JSON.generate(BUNDLE_ID: RamapRelease::BUNDLE_ID))
        assert_equal archive, RamapRelease.archive_path(archive)
        File.write(File.join(app, "GoogleService-Info.plist"), JSON.generate(BUNDLE_ID: "com.peto.ramap.debug"))
        assert_raises(RuntimeError) { RamapRelease.archive_path(archive) }
    end

    def configure(key)
        File.write(
            ENV.fetch("RAMAP_ASC_CONFIG"),
            JSON.generate(key_id: "TEST", issuer_id: "TEST", key_filepath: key, team_id: "TEST")
        )
    end
end
