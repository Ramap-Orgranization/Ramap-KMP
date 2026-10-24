require_relative "release_support"

class IosReviewSubmission
    ACCEPTED = %w[WAITING_FOR_REVIEW IN_REVIEW PENDING_DEVELOPER_RELEASE].freeze
    EDITABLE = %w[PREPARE_FOR_SUBMISSION DEVELOPER_REJECTED REJECTED METADATA_REJECTED READY_FOR_REVIEW].freeze

    def initialize(app, version, build, api: Spaceship::ConnectAPI, pause: -> { sleep 30 })
        @app, @version, @build, @api, @pause = app, version, build, api, pause
    end

    def submit(notes: nil, exempt_encryption: false)
        app_version = find_version
        if app_version && ACCEPTED.include?(app_version.app_version_state)
            return verify_submission!(app_version)
        end
        ensure_available_version!(app_version)
        selected_build = wait_for_build
        if selected_build.uses_non_exempt_encryption.nil? && exempt_encryption
            selected_build.update(attributes: { usesNonExemptEncryption: false })
            selected_build = find_build
        end
        raise "Complete export compliance or provide the approved exempt_encryption:true declaration." if selected_build.uses_non_exempt_encryption.nil?

        unless app_version
            raise "Provide approved notes:/absolute/path before creating an App Store version." if notes.to_s.strip.empty?
            app_version = @api.post_app_store_version(
                app_id: @app.id,
                attributes: { versionString: @version, platform: "IOS", releaseType: "MANUAL" }
            ).first
        end
        raise "App Store release type must remain MANUAL." unless app_version.release_type == "MANUAL"
        synchronize_store_name!
        if notes
            localizations = app_version.get_app_store_version_localizations
            korean = localizations.find { |localization| localization.locale == "ko" }
            raise "Prepare the Korean store listing before review." unless korean
            korean.update(attributes: { whatsNew: notes })
        end
        app_version.select_build(build_id: selected_build.id)
        yield
        20.times do |attempt|
            latest = find_version
            return verify_submission!(latest) if latest && ACCEPTED.include?(latest.app_version_state)
            raise "Apple rejected the submission; inspect App Store Connect." if latest && %w[REJECTED METADATA_REJECTED].include?(latest.app_version_state)
            @pause.call if attempt < 19
        end
        raise "Final App Store review submission is NOT verified. Retry review for the same version and build; do not upload again."
    end

    private

    def synchronize_store_name!
        name = File.read(File.join(__dir__, "metadata", "ko", "name.txt"), encoding: "UTF-8").strip
        raise "The Korean App Store name must contain 2–30 characters." unless (2..30).cover?(name.length)
        infos = @api.get_app_infos(app_id: @app.id).to_models.select { |info| EDITABLE.include?(info.state) }
        raise "Prepare exactly one editable App Store app info before review." unless infos.one?
        info = infos.first
        korean = info.get_app_info_localizations.find { |localization| localization.locale == "ko" }
        raise "Prepare the Korean app info localization before review." unless korean
        korean.update(attributes: { name: name }) unless korean.name == name
        saved = info.get_app_info_localizations.find { |localization| localization.locale == "ko" }
        raise "The Korean App Store name was not saved; review was not submitted." unless saved && saved.name == name
    end

    def versions
        @app.get_app_store_versions(filter: { platform: "IOS" })
    end

    def find_version
        matches = versions.select { |version| version.version_string == @version }
        raise "Ambiguous App Store version." if matches.length > 1
        matches.first
    end

    def ensure_available_version!(current)
        others = versions.reject { |version| version.version_string == @version }
        raise "Another App Store version is pending; it will not be cancelled or replaced automatically." if
            others.any? { |version| (ACCEPTED + EDITABLE + %w[WAITING_FOR_EXPORT_COMPLIANCE PENDING_APPLE_RELEASE]).include?(version.app_version_state) }
        if current
            raise "The requested App Store version is not editable: #{current.app_version_state}." unless EDITABLE.include?(current.app_version_state)
            raise "App Store release type must remain MANUAL." unless current.release_type == "MANUAL"
        end
    end

    def find_build
        builds = @api::Build.all(app_id: @app.id, version: @version, build_number: @build, platform: "IOS")
        raise "Ambiguous App Store build." if builds.length > 1
        builds.first
    end

    def wait_for_build
        120.times do |attempt|
            build = find_build
            raise "Apple build processing failed: #{build.processing_state}." if build && %w[FAILED INVALID].include?(build.processing_state)
            return build if build && build.processing_state == "VALID"
            @pause.call if attempt < 119
        end
        raise "Apple build processing timed out. Retry review with the same version and build."
    end

    def verify_submission!(version)
        raise "App Store release type must remain MANUAL." unless version.release_type == "MANUAL"
        selected = version.get_build
        expected = find_build
        raise "The submitted build differs from the approved build." unless selected && expected && selected.id == expected.id && selected.version == @build
        { "version" => @version, "build" => @build, "build_id" => selected.id,
          "state" => version.app_version_state, "release_type" => version.release_type }
    end
end
