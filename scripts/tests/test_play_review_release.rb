require_relative "test_play_internal_release"
require_relative "../../androidApp/fastlane/play_review_release"

class PlayReviewReleaseTest < Minitest::Test
    class ReviewClient < PlayInternalReleaseTest::FakeClient
        def authorization
            nil
        end

        def track_releases(track)
            raise "Wrong track" unless track == "production"
            @releases
        end
    end

    class ReviewApi
        attr_accessor :summaries, :after_commit, :fail_commit
        attr_reader :commits

        def initialize
            @summaries, @commits = [], []
        end

        def releases
            @summaries
        end

        def commit(id)
            raise "Another review started" if @fail_commit
            @commits << id
            @summaries = @after_commit if @after_commit
        end
    end

    def setup
        @client = ReviewClient.new
        @api = ReviewApi.new
        @artifact = { "version_code" => "30", "sha256" => "b" * 64, "aab" => "approved.aab" }
        @track = PlayInternalReleaseTest::Track.new("production", [PlayInternalReleaseTest::Release.new(["30"], "completed")])
        @api.after_commit = [summary("IN_REVIEW")]
    end

    def summary(state, code = 30)
        { "track" => "production", "activeArtifacts" => [{ "versionCode" => code }],
          "releaseLifecycleState" => "RELEASE_LIFECYCLE_STATE_#{state}" }
    end

    def publisher(managed = "true")
        PlayReviewRelease.new(@client, @artifact, @track, api: @api, managed_publishing: managed, pause: -> {})
    end

    def test_운영트랙_업로드와_실제_심사상태를_함께_검증한다
        result = publisher
        assert_equal "submitted_for_review", result.publish
        assert_equal "IN_REVIEW", result.review_state
        assert_equal ["production"], @client.updates
        assert_equal ["edit"], @api.commits
    end

    def test_완료된_트랙이라도_미제출이면_최종심사를_요청한다
        @client.bundles = [PlayInternalReleaseTest::Bundle.new(30, "b" * 64)]
        @client.releases = @track.releases
        @api.summaries = [summary("NOT_SENT_FOR_REVIEW")]
        assert_equal "submitted_for_review", publisher.publish
        assert_equal 0, @client.uploads
        assert_equal ["edit"], @api.commits
    end

    def test_이미_제출한_동일빌드는_중복_커밋하지_않는다
        @client.bundles = [PlayInternalReleaseTest::Bundle.new(30, "b" * 64)]
        @api.summaries = [summary("IN_REVIEW")]
        assert_equal "already_submitted", publisher.publish
        assert_empty @api.commits
        assert_equal 0, @client.uploads
    end

    def test_다른_심사나_공개상태를_자동으로_바꾸지_않는다
        [summary("IN_REVIEW", 29), summary("PUBLISHED"), summary("NOT_SENT_FOR_REVIEW", 29)].each do |state|
            @api.summaries = [state]
            assert_raises(RuntimeError) { publisher.publish }
        end
        assert_empty @api.commits
        assert_equal 0, @client.uploads
    end

    def test_수동게시_확인과_심사상태_검증_실패를_성공으로_처리하지_않는다
        assert_raises(RuntimeError) { publisher("false") }
        @api.after_commit = [summary("NOT_SENT_FOR_REVIEW")]
        assert_raises(RuntimeError) { publisher.publish }
        assert_equal ["edit"], @api.commits
    end

    def test_동시에_시작된_심사를_취소하거나_초안으로_우회하지_않는다
        @api.fail_commit = true
        assert_raises(RuntimeError) { publisher.publish }
        assert_nil @client.current_edit
        assert_empty @api.commits
    end

    def test_실제_review_lane이_승인파일과_출시노트를_운영심사로_연결한다
        require "fastlane"
        require "supply"
        require "minitest/mock"
        Fastlane.load_actions
        previous_environment = ENV.to_h
        Dir.mktmpdir do |directory|
            aab = File.join(directory, "approved.aab")
            File.write(aab, "approved")
            digest = Digest::SHA256.file(aab).hexdigest
            artifact = @artifact.merge("aab" => aab, "sha256" => digest, "tag" => "v1.2.3", "source_sha" => "a" * 40,
                                       "package_name" => "com.peto.ramap", "track" => "production", "release_notes" => "승인된 출시 노트")
            ENV["VERIFIED_RELEASE"] = File.join(directory, "verified.json")
            ENV["PLAY_RESULT"] = File.join(directory, "result.json")
            ENV["RELEASE_TAG"], ENV["SOURCE_SHA"] = artifact.values_at("tag", "source_sha")
            ENV["VERSION_CODE"], ENV["AAB_SHA256"], ENV["PLAY_MANAGED_PUBLISHING"] = "30", digest, "true"
            File.write(ENV.fetch("VERIFIED_RELEASE"), JSON.generate(artifact))
            @client.uploaded_digest = digest
            fastfile = Fastlane::FastFile.new(File.expand_path("../../androidApp/fastlane/Fastfile", __dir__))
            Supply::Client.stub(:make_from_config, @client) do
                PlayReviewApi.stub(:new, @api) do
                    fastfile.runner.execute(:review, :android)
                    result = JSON.parse(File.read(ENV.fetch("PLAY_RESULT")))
                    assert_equal "submitted_for_review", result.fetch("status")
                    assert_equal "IN_REVIEW", result.fetch("review_state")
                    assert_equal "ko-KR", @client.releases.first.release_notes.first.language
                    assert_equal "승인된 출시 노트", @client.releases.first.release_notes.first.text
                    ENV["SOURCE_SHA"] = "c" * 40
                    assert_raises(RuntimeError) { fastfile.runner.execute(:review, :android) }
                    refute File.exist?(ENV.fetch("PLAY_RESULT"))
                end
            end
        end
    ensure
        ENV.replace(previous_environment) if previous_environment
    end
end
