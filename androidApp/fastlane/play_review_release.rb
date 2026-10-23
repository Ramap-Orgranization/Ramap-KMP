require "net/http"
require "uri"
require_relative "play_internal_release"

# The pinned Google Ruby client predates release lifecycle queries and the
# ERROR_IF_IN_REVIEW commit option. Use the documented REST API with its ADC.
class PlayReviewApi
    ROOT = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.peto.ramap"

    def initialize(authorization)
        @authorization = authorization
    end

    def releases
        request(Net::HTTP::Get, "/tracks/production/releases").fetch("releases", [])
    end

    def commit(edit_id)
        id = URI.encode_www_form_component(edit_id)
        request(Net::HTTP::Post, "/edits/#{id}:commit?changesNotSentForReview=false&changesInReviewBehavior=ERROR_IF_IN_REVIEW")
    end

    private

    def request(method, path)
        uri = URI(ROOT + path)
        headers = {}
        @authorization.apply!(headers)
        request = method.new(uri)
        headers.each { |key, value| request[key.to_s] = value }
        response = Net::HTTP.start(uri.host, uri.port, use_ssl: true, open_timeout: 30, read_timeout: 120) do |http|
            http.request(request)
        end
        unless response.is_a?(Net::HTTPSuccess)
            # Never print provider request headers or credentials.
            raise "Play review API failed (HTTP #{response.code}). Check permissions, pending reviews and Publishing overview; no draft fallback was used."
        end
        JSON.parse(response.body)
    end
end

class PlayReviewRelease < PlayInternalRelease
    TRACK = "production"
    PREFIX = "RELEASE_LIFECYCLE_STATE_"
    ACCEPTED = %w[IN_REVIEW APPROVED_NOT_PUBLISHED].freeze

    attr_reader :review_state

    def initialize(client, artifact, track, api:, managed_publishing:, pause: -> { sleep 30 })
        raise "Confirm Managed publishing is ON in Play Console before submitting." unless managed_publishing == "true"
        super(client, artifact, track)
        @api, @pause = api, pause
    end

    def publish
        summaries = @api.releases
        @review_state = state_for(summaries)
        if ACCEPTED.include?(@review_state)
            with_edit { verify_bundle!(matching_bundle(current_bundles)) }
            return "already_submitted"
        end
        raise "The approved build is already public; review-only deployment cannot undo publication." if @review_state == "PUBLISHED"
        raise "Another release is in review; it will not be cancelled automatically." if
            summaries.any? { |release| release["releaseLifecycleState"] == PREFIX + "IN_REVIEW" }
        raise "Another production draft is pending; resolve it before submitting this release." if summaries.any? do |release|
            %w[DRAFT NOT_SENT_FOR_REVIEW].any? { |state| release["releaseLifecycleState"] == PREFIX + state } &&
                release.fetch("activeArtifacts", []).map { |artifact| artifact.fetch("versionCode").to_s } != [@artifact.fetch("version_code")]
        end

        super
        20.times do |attempt|
            @review_state = state_for(@api.releases)
            return "submitted_for_review" if ACCEPTED.include?(@review_state)
            raise "Play rejected this release; inspect Publishing overview." if @review_state == "NOT_APPROVED"
            raise "Play published the release; verify Managed publishing immediately." if @review_state == "PUBLISHED"
            @pause.call if attempt < 19
        end
        raise "Production track saved, but final review submission is NOT verified. Check Publishing overview and retry the same artifact."
    end

    private

    def state_for(summaries)
        matching = summaries.select do |release|
            release.fetch("track") == TRACK &&
                release.fetch("activeArtifacts", []).map { |artifact| artifact.fetch("versionCode").to_s } == [@artifact.fetch("version_code")]
        end
        raise "Ambiguous Play review state for the approved build." if matching.length > 1
        matching.first&.fetch("releaseLifecycleState")&.delete_prefix(PREFIX)
    end

    def already_present?
        # completed is a rollout configuration, not proof of submission for review.
        false
    end

    def commit_edit!
        @api.commit(@client.current_edit.id)
        @client.current_edit = nil
    end
end
