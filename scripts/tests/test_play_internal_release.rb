require "minitest/autorun"
require "tmpdir"
require_relative "../../androidApp/fastlane/play_internal_release"

class PlayInternalReleaseTest < Minitest::Test
    Bundle = Struct.new(:version_code, :sha256)
    Release = Struct.new(:version_codes, :status)
    Track = Struct.new(:track, :releases)

    class FakeClient
        attr_accessor :bundles, :releases, :current_edit, :uploaded_digest, :lose_commit, :fail_commit, :apk_codes
        attr_reader :uploads, :commits, :updates, :aborts

        def initialize
            @bundles, @releases, @updates, @apk_codes = [], [], [], []
            @uploads, @commits, @aborts = 0, 0, 0
            @uploaded_digest = "b" * 64
        end

        def client
            self
        end

        def begin_edit(package_name:)
            raise "Wrong package" unless package_name == "com.peto.ramap"
            @current_edit = Struct.new(:id).new("edit")
        end

        def list_edit_bundles(*args)
            Struct.new(:bundles).new(@bundles)
        end

        def apks_version_codes
            @apk_codes
        end

        def track_releases(track)
            raise "Wrong track" unless track == "internal"
            @releases
        end

        def upload_bundle(path)
            @uploads += 1
            @bundles << Bundle.new(30, @uploaded_digest)
            30
        end

        def update_track(name, track)
            @updates << name
            @releases = track.releases
        end

        def validate_current_edit!; end

        def commit_current_edit!
            raise "commit failed" if @fail_commit
            @commits += 1
            @current_edit = nil
            @releases = [] if @lose_commit
        end

        def abort_current_edit
            @aborts += 1
            @current_edit = nil
        end
    end

    def setup
        @client = FakeClient.new
        @artifact = { "version_code" => "30", "sha256" => "b" * 64, "aab" => "approved.aab" }
        @track = Track.new("internal", [Release.new(["30"], "completed")])
    end

    def publisher
        PlayInternalRelease.new(@client, @artifact, @track)
    end

    def test_새_AAB만_업로드하고_내부_테스트에만_커밋한다
        assert_equal "published", publisher.publish
        assert_equal 1, @client.uploads
        assert_equal 1, @client.commits
        assert_equal ["internal"], @client.updates
        assert_nil @client.current_edit
    end

    def test_같은_해시의_완료된_버전은_재업로드하지_않는다
        @client.bundles = [Bundle.new(30, "b" * 64)]
        @client.releases = @track.releases
        assert_equal "already_present", publisher.publish
        assert_equal 0, @client.uploads
        assert_equal 0, @client.commits
        assert_empty @client.updates
    end

    def test_같은_해시의_기존_번들은_재업로드없이_내부트랙에_연결한다
        @client.bundles = [Bundle.new(30, "b" * 64)]
        assert_equal "published", publisher.publish
        assert_equal 0, @client.uploads
        assert_equal 1, @client.commits
    end

    def test_동일한_버전이_다른_파일이면_완료상태여도_거부한다
        @client.bundles = [Bundle.new(30, "c" * 64)]
        @client.releases = @track.releases
        assert_raises(RuntimeError) { publisher.publish }
        assert_equal 0, @client.uploads
        assert_equal 0, @client.commits
        assert_nil @client.current_edit
    end

    def test_더_새로운_AAB나_APK가_있으면_이전_버전으로_덮어쓰지_않는다
        @client.bundles = [Bundle.new(31, "c" * 64)]
        assert_raises(RuntimeError) { publisher.publish }
        @client.bundles = []
        @client.apk_codes = [31]
        assert_raises(RuntimeError) { publisher.publish }
        assert_equal 0, @client.commits
        assert_empty @client.updates
    end

    def test_Play가_반환한_업로드_해시가_다르면_커밋하지_않는다
        @client.uploaded_digest = "c" * 64
        assert_raises(RuntimeError) { publisher.publish }
        assert_equal 1, @client.uploads
        assert_equal 0, @client.commits
        assert_empty @client.updates
    end

    def test_커밋이_실패하면_성공으로_처리하지_않는다
        @client.fail_commit = true
        assert_raises(RuntimeError) { publisher.publish }
        assert_equal 0, @client.commits
        assert_nil @client.current_edit
    end

    def test_커밋후_트랙에서_빌드를_확인하지_못하면_성공으로_처리하지_않는다
        @client.lose_commit = true
        assert_raises(RuntimeError) { publisher.publish }
        assert_equal 1, @client.commits
        assert_nil @client.current_edit
    end

    def test_운영트랙이나_승인하지_않은_버전은_거부한다
        @track.track = "production"
        assert_raises(RuntimeError) { publisher }
        @track.track = "internal"
        @track.releases.first.version_codes = ["31"]
        assert_raises(RuntimeError) { publisher }
    end

    def test_검증후_파일이_바뀌거나_승인입력이_다르면_업로드전에_거부한다
        Dir.mktmpdir do |directory|
            aab = File.join(directory, "app.aab")
            File.write(aab, "approved")
            artifact = {
                "tag" => "v1.2.3", "source_sha" => "a" * 40, "version_code" => "30",
                "sha256" => Digest::SHA256.file(aab).hexdigest, "aab" => aab,
                "package_name" => "com.peto.ramap", "track" => "internal"
            }
            path = File.join(directory, "verified.json")
            File.write(path, JSON.generate(artifact))
            environment = { "RELEASE_TAG" => artifact["tag"], "SOURCE_SHA" => artifact["source_sha"],
                            "VERSION_CODE" => "30", "AAB_SHA256" => artifact["sha256"] }
            assert_equal artifact, PlayInternalRelease.load_artifact(path, environment)
            assert_raises(RuntimeError) { PlayInternalRelease.load_artifact(path, environment.merge("VERSION_CODE" => "31")) }
            File.write(aab, "tampered")
            assert_raises(RuntimeError) { PlayInternalRelease.load_artifact(path, environment) }
        end
    end

    def test_실제_Fastfile이_승인정보와_내부트랙을_연결하고_실패시_이전결과를_제거한다
        require "fastlane"
        require "supply"
        require "minitest/mock"
        Fastlane.load_actions
        previous_environment = ENV.to_h
        Dir.mktmpdir do |directory|
            aab = File.join(directory, "approved.aab")
            File.write(aab, "approved")
            digest = Digest::SHA256.file(aab).hexdigest
            artifact = @artifact.merge(
                "aab" => aab, "sha256" => digest, "tag" => "v1.2.3", "source_sha" => "a" * 40,
                "package_name" => "com.peto.ramap", "track" => "internal"
            )
            ENV["VERIFIED_RELEASE"] = File.join(directory, "verified.json")
            ENV["PLAY_RESULT"] = File.join(directory, "result.json")
            ENV["RELEASE_TAG"], ENV["SOURCE_SHA"] = artifact.values_at("tag", "source_sha")
            ENV["VERSION_CODE"], ENV["AAB_SHA256"] = "30", digest
            File.write(ENV.fetch("VERIFIED_RELEASE"), JSON.generate(artifact))
            @client.uploaded_digest = digest
            fastfile = Fastlane::FastFile.new(File.expand_path("../../androidApp/fastlane/Fastfile", __dir__))
            Supply::Client.stub(:make_from_config, @client) do
                fastfile.runner.execute(:internal, :android)
                result = JSON.parse(File.read(ENV.fetch("PLAY_RESULT")))
                assert_equal "published", result.fetch("status")
                assert_equal digest, result.fetch("sha256")
                assert_equal ["internal"], @client.updates
                assert_equal false, Supply.config[:rescue_changes_not_sent_for_review]
                ENV["SOURCE_SHA"] = "c" * 40
                assert_raises(RuntimeError) { fastfile.runner.execute(:internal, :android) }
                refute File.exist?(ENV.fetch("PLAY_RESULT"))
                assert_equal 1, @client.uploads
            end
        end
    ensure
        ENV.replace(previous_environment) if previous_environment
    end
end
