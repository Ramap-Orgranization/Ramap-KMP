require "minitest/autorun"
require_relative "../../iosApp/fastlane/review_submission"

class IosReviewSubmissionTest < Minitest::Test
    Build = Struct.new(:id, :version, :processing_state, :uses_non_exempt_encryption) do
        def update(attributes:)
            self.uses_non_exempt_encryption = attributes.fetch(:usesNonExemptEncryption)
        end
    end
    Version = Struct.new(:version_string, :app_version_state, :release_type, :selected) do
        def get_build
            selected
        end

        def select_build(build_id:)
            raise "Wrong build" unless selected.id == build_id
        end
    end
    App = Struct.new(:id, :versions) do
        def get_app_store_versions(filter:)
            versions
        end
    end
    class BuildList
        class << self
            attr_accessor :builds
            def all(**options)
                builds
            end
        end
    end
    Localization = Struct.new(:locale, :name, :updates, :save_changes) do
        def update(attributes:)
            updates << attributes
            self.name = attributes.fetch(:name) if save_changes
        end
    end
    AppInfo = Struct.new(:state, :localizations) do
        def get_app_info_localizations
            localizations
        end
    end
    Response = Struct.new(:to_models)
    module Api
        Build = BuildList
        class << self
            attr_accessor :app_infos, :app_info_reads

            def get_app_infos(app_id:)
                self.app_info_reads += 1
                Response.new(app_infos)
            end
        end
    end

    def setup
        @build = Build.new("approved-build", "21", "VALID", false)
        BuildList.builds = [@build]
        @version = Version.new("1.1.12", "PREPARE_FOR_SUBMISSION", "MANUAL", @build)
        @app = App.new("app", [@version])
        @korean = Localization.new("ko", "라맵", [], true)
        @english = Localization.new("en-US", "Ramap", [], true)
        @info = AppInfo.new("PREPARE_FOR_SUBMISSION", [@korean, @english])
        Api.app_infos = [@info]
        Api.app_info_reads = 0
    end

    def submission(pause: -> {})
        IosReviewSubmission.new(@app, "1.1.12", "21", api: Api, pause: pause)
    end

    def test_제출후_정확한_빌드와_수동게시_심사대기를_확인한다
        result = submission.submit { @version.app_version_state = "WAITING_FOR_REVIEW" }
        assert_equal "WAITING_FOR_REVIEW", result.fetch("state")
        assert_equal "approved-build", result.fetch("build_id")
        assert_equal "MANUAL", result.fetch("release_type")
    end

    def test_이미_제출한_빌드는_재제출하지_않는다
        @version.app_version_state = "IN_REVIEW"
        result = submission.submit { flunk "중복 제출" }
        assert_equal "IN_REVIEW", result.fetch("state")
        assert_equal 0, Api.app_info_reads
        assert_empty @korean.updates
    end

    def test_다음_심사제출_전에_한국어_스토어_이름만_라멥으로_저장한다
        published = Localization.new("ko", "라맵", [], true)
        Api.app_infos.unshift(AppInfo.new("READY_FOR_DISTRIBUTION", [published]))
        submission.submit do
            assert_equal "라멥", @korean.name
            @version.app_version_state = "WAITING_FOR_REVIEW"
        end
        assert_equal [{ name: "라멥" }], @korean.updates
        assert_empty published.updates
        assert_empty @english.updates
        assert_equal "Ramap", @english.name
    end

    def test_이미_라멥인_초안은_이름을_다시_저장하지_않는다
        @korean.name = "라멥"
        submission.submit { @version.app_version_state = "WAITING_FOR_REVIEW" }
        assert_empty @korean.updates
    end

    def test_편집할_앱정보가_없거나_여러개면_심사를_제출하지_않는다
        [[], [@info, @info]].each do |infos|
            Api.app_infos = infos
            assert_raises(RuntimeError) { submission.submit { flunk "잘못된 앱정보로 제출" } }
        end
    end

    def test_한국어_앱정보가_없거나_이름_저장이_확인되지_않으면_제출하지_않는다
        @info.localizations = [@english]
        assert_raises(RuntimeError) { submission.submit { flunk "한국어 앱정보 없이 제출" } }
        @info.localizations = [@korean]
        @korean.save_changes = false
        assert_raises(RuntimeError) { submission.submit { flunk "이름 저장 확인 없이 제출" } }
    end

    def test_다른_빌드가_심사중이면_완료로_보고하지_않는다
        @version.app_version_state = "WAITING_FOR_REVIEW"
        @version.selected = Build.new("other", "20", "VALID", false)
        assert_raises(RuntimeError) { submission.submit { flunk "다른 빌드 교체" } }
    end

    def test_다른_버전의_심사를_취소하지_않는다
        @app.versions = [Version.new("1.1.10", "IN_REVIEW", "MANUAL", @build)]
        assert_raises(RuntimeError) { submission.submit { flunk "기존 심사 취소" } }
    end

    def test_처리중인_빌드를_기다린뒤_제출한다
        @build.processing_state = "PROCESSING"
        waited = 0
        runner = submission(pause: -> { waited += 1; @build.processing_state = "VALID" })
        runner.submit { @version.app_version_state = "WAITING_FOR_REVIEW" }
        assert_equal 1, waited
    end

    def test_처리실패와_시간초과는_제출하지_않는다
        %w[INVALID PROCESSING].each do |state|
            @build.processing_state = state
            assert_raises(RuntimeError) { submission.submit { flunk "잘못된 빌드 제출" } }
        end
    end

    def test_수출규정_응답을_임의로_추측하지_않는다
        @build.uses_non_exempt_encryption = nil
        assert_raises(RuntimeError) { submission.submit { flunk "수출규정 미응답" } }
        assert_nil @build.uses_non_exempt_encryption
        submission.submit(exempt_encryption: true) { @version.app_version_state = "WAITING_FOR_REVIEW" }
        assert_equal false, @build.uses_non_exempt_encryption
    end

    def test_자동공개_또는_제출후_상태미변경은_실패한다
        @version.release_type = "AFTER_APPROVAL"
        assert_raises(RuntimeError) { submission.submit { flunk "자동공개" } }
        @version.release_type = "MANUAL"
        assert_raises(RuntimeError) { submission.submit {} }
    end

    def test_실제_review_lane은_정확한_빌드와_수동게시를_제출액션에_전달한다
        require "fastlane"
        require "minitest/mock"
        Fastlane.load_actions
        fastfile = Fastlane::FastFile.new(File.expand_path("../../iosApp/fastlane/Fastfile", __dir__))
        result = { "state" => "WAITING_FOR_REVIEW" }
        workflow = Object.new
        workflow.define_singleton_method(:submit) do |**options, &block|
            block.call
            result
        end
        submitted = nil
        fastfile.define_singleton_method(:upload_to_app_store) { |options| submitted = options }
        fastfile.stub(:ramap_app, @app) do
            fastfile.stub(:ramap_api_key, {}) do
                IosReviewSubmission.stub(:new, workflow) do
                    fastfile.runner.execute(:review, :ios, { version: "1.1.12", build: "21" })
                end
            end
        end
        assert_equal "1.1.12", submitted.fetch(:app_version)
        assert_equal "21", submitted.fetch(:build_number)
        assert_equal true, submitted.fetch(:submit_for_review)
        assert_equal false, submitted.fetch(:automatic_release)
        assert_equal true, submitted.fetch(:skip_binary_upload)
        assert_equal true, submitted.fetch(:skip_screenshots)
    end
end
