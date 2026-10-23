require "json"
require "open3"
require "pathname"
require "tmpdir"

module RamapRelease
    ROOT = File.expand_path("../..", __dir__)
    BUNDLE_ID = "com.peto.ramap"
    APP_ID = "6797046935"

    def self.load_credentials
        path = File.expand_path(ENV.fetch("RAMAP_ASC_CONFIG", "~/.config/ramap/appstore-connect/config.json"))
        raise "Run ./scripts/ios-release.sh setup and configure the external API key first." unless File.file?(path)
        data = JSON.parse(File.read(path))
        %w[key_id issuer_id key_filepath team_id].each do |key|
            raise "Configure #{key} in #{path}." if data.fetch(key, "").strip.empty?
        end
        key_path = File.realpath(File.expand_path(data.fetch("key_filepath")))
        raise "Keep the API private key outside the repository." if Pathname.new(key_path).ascend.any? { |parent| parent.to_s == ROOT }
        raise "API private key must not be accessible to other users; chmod 600 the key." unless (File.stat(key_path).mode & 0o077).zero?
        data.merge("key_filepath" => key_path)
    end

    def self.version_and_build(options)
        version = options[:version].to_s
        build = options[:build].to_s
        raise "Specify version:X.Y.Z and build:N explicitly." unless version.match?(/\A\d+\.\d+\.\d+\z/) && build.match?(/\A[1-9]\d*\z/)
        [version, build]
    end

    def self.unlock_signing_keychain!(credentials)
        keychain = credentials["signing_keychain_path"]
        return if keychain.to_s.empty?
        password_path = credentials.fetch("signing_keychain_password_filepath")
        raise "Signing keychain password must have permissions 600." unless (File.stat(password_path).mode & 0o077).zero?
        password = File.read(password_path).strip
        _, status = Open3.capture2e("security", "unlock-keychain", "-p", password, keychain)
        raise "Could not unlock the dedicated distribution keychain." unless status.success?
    end

    def self.ensure_new_build!(build, existing_builds)
        maximum = existing_builds.filter_map { |number| Integer(number, exception: false) }.max || 0
        raise "Build must exceed the latest received build (#{maximum})." unless build.to_i > maximum
    end

    def self.read_plist(path)
        text, status = Open3.capture2("plutil", "-convert", "json", "-o", "-", path)
        raise "Could not read plist: #{path}" unless status.success?
        JSON.parse(text)
    end

    def self.verify_app!(path, distribution:)
        info = read_plist(File.join(path, "Info.plist"))
        raise "Only the Release Ramap bundle is allowed." unless info["CFBundleIdentifier"] == BUNDLE_ID
        firebase = read_plist(File.join(path, "GoogleService-Info-Release.plist"))
        raise "Release Firebase bundle mismatch." unless firebase["BUNDLE_ID"] == BUNDLE_ID
        raise "Debug Firebase configuration is included." if File.exist?(File.join(path, "GoogleService-Info.plist"))
        raise "Debug Firebase configuration is included." unless Dir.glob(File.join(path, "**", "*Debug*.plist")).empty?
        return info unless distribution

        output, status = Open3.capture2e("codesign", "--verify", "--deep", "--strict", path)
        raise "IPA code signature verification failed." unless status.success?
        output, error, status = Open3.capture3("codesign", "-d", "--entitlements", ":-", path)
        raise "Could not read distribution entitlements." unless status.success?
        entitlement_xml = output.empty? ? error : output
        Dir.mktmpdir("ramap-entitlements-") do |temporary|
            plist_path = File.join(temporary, "entitlements.plist")
            File.write(plist_path, entitlement_xml)
            entitlements = read_plist(plist_path)
            raise "APNs must use production." unless entitlements["aps-environment"] == "production"
            raise "Debugging must be disabled in the distribution app." unless entitlements["get-task-allow"] == false
        end
        info
    end

    def self.archive_path(value)
        raise "Specify archive:/absolute/path/to/Ramap.xcarchive." if value.to_s.empty?
        path = File.expand_path(value)
        raise "Archive does not exist." unless File.directory?(path) && File.extname(path) == ".xcarchive"
        apps = Dir.glob(File.join(path, "Products", "Applications", "*.app"))
        raise "Archive must contain exactly one app." unless apps.one?
        verify_app!(apps.first, distribution: false)
        path
    end

    def self.verify_ipa!(value)
        raise "Specify ipa:/absolute/path/to/Ramap.ipa." if value.to_s.empty?
        path = File.expand_path(value)
        raise "IPA does not exist." unless File.file?(path) && File.extname(path) == ".ipa"
        metadata = Dir.mktmpdir("ramap-ipa-") do |temporary|
            _, status = Open3.capture2e("ditto", "-x", "-k", path, temporary)
            raise "Could not extract IPA." unless status.success?
            apps = Dir.glob(File.join(temporary, "Payload", "*.app"))
            raise "IPA must contain exactly one app." unless apps.one?
            verify_app!(apps.first, distribution: true)
        end
        {
            path: path,
            version: metadata.fetch("CFBundleShortVersionString"),
            build: metadata.fetch("CFBundleVersion")
        }
    end
end
