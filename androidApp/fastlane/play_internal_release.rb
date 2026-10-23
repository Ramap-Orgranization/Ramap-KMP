require "digest"
require "json"

class PlayInternalRelease
    PACKAGE_NAME = "com.peto.ramap"
    TRACK = "internal"

    def self.load_artifact(path, environment = ENV)
        artifact = JSON.parse(File.read(path))
        expected = {
            "tag" => environment.fetch("RELEASE_TAG"),
            "source_sha" => environment.fetch("SOURCE_SHA"),
            "version_code" => environment.fetch("VERSION_CODE"),
            "sha256" => environment.fetch("AAB_SHA256"),
            "package_name" => PACKAGE_NAME,
            "track" => TRACK
        }
        expected.each do |key, value|
            raise "Verified artifact #{key} mismatch." unless artifact.fetch(key) == value
        end
        raise "Invalid approved version code." unless artifact.fetch("version_code").match?(/\A[1-9]\d*\z/)
        raise "Invalid approved SHA-256." unless artifact.fetch("sha256").match?(/\A[0-9a-f]{64}\z/)
        raise "Approved AAB bytes changed." unless Digest::SHA256.file(artifact.fetch("aab")).hexdigest == artifact.fetch("sha256")
        artifact
    end

    def initialize(client, artifact, track)
        @client = client
        @artifact = artifact
        @track = track
        raise "Only the internal track is allowed." unless track.track == TRACK
        releases = Array(track.releases)
        valid = releases.length == 1 && releases.first.status == "completed" &&
            Array(releases.first.version_codes).map(&:to_s) == [artifact.fetch("version_code")]
        raise "Track must contain only the approved completed release." unless valid
    end

    def publish
        result = with_edit do
            bundles = current_bundles
            reject_newer_release!(bundles)
            bundle = matching_bundle(bundles)
            verify_bundle!(bundle) if bundle
            next "already_present" if bundle && internal_release_completed?

            unless bundle
                uploaded_code = @client.upload_bundle(@artifact.fetch("aab"))
                raise "Uploaded version code mismatch." unless uploaded_code.to_s == @artifact.fetch("version_code")
            end
            verify_bundle!(matching_bundle(current_bundles))
            @client.update_track(TRACK, @track)
            @client.validate_current_edit!
            @client.commit_current_edit!
            "published"
        end

        # Re-read a fresh edit after commit (or an idempotent retry) before reporting success.
        with_edit do
            verify_bundle!(matching_bundle(current_bundles))
            raise "Approved build is not completed on internal track." unless internal_release_completed?
        end
        result
    end

    private

    def with_edit
        @client.begin_edit(package_name: PACKAGE_NAME)
        begin
            yield
        ensure
            @client.abort_current_edit if @client.current_edit
        end
    end

    def current_bundles
        Array(@client.client.list_edit_bundles(PACKAGE_NAME, @client.current_edit.id).bundles)
    end

    def matching_bundle(bundles)
        bundles.find { |bundle| bundle.version_code.to_s == @artifact.fetch("version_code") }
    end

    def verify_bundle!(bundle)
        raise "Google Play bundle SHA-256 does not match the approved AAB." unless
            bundle && bundle.sha256.to_s.downcase == @artifact.fetch("sha256")
    end

    def reject_newer_release!(bundles)
        codes = bundles.map { |bundle| bundle.version_code.to_i }
        codes.concat(@client.apks_version_codes.map(&:to_i))
        raise "A newer Play build exists; refusing to replace it with an older release." if
            codes.any? { |code| code > @artifact.fetch("version_code").to_i }
    end

    def internal_release_completed?
        @client.track_releases(TRACK).any? do |release|
            release.status == "completed" && Array(release.version_codes).map(&:to_s).include?(@artifact.fetch("version_code"))
        end
    end
end
