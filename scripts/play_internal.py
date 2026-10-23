#!/usr/bin/env python3
"""Download and verify the exact approved GitHub Release AAB; never build or upload."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.request
import xml.etree.ElementTree as ET


PACKAGE_NAME = "com.peto.ramap"
ASSET_NAME = "androidApp-release.aab"
BUNDLETOOL_VERSION = "1.18.3"
BUNDLETOOL_SHA256 = "a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def approved_inputs(environ):
    names = ("RELEASE_TAG", "SOURCE_SHA", "VERSION_CODE", "AAB_SHA256", "GITHUB_REPOSITORY")
    values = {name: environ.get(name, "") for name in names}
    patterns = {
        "RELEASE_TAG": r"v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)",
        "SOURCE_SHA": r"[0-9a-f]{40}",
        "VERSION_CODE": r"[1-9]\d*",
        "AAB_SHA256": r"[0-9a-f]{64}",
        "GITHUB_REPOSITORY": r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+",
    }
    for name, pattern in patterns.items():
        require(re.fullmatch(pattern, values[name]) is not None, f"Invalid {name}.")
    require(int(values["VERSION_CODE"]) <= 2100000000, "Version code exceeds the Play limit.")
    return values


def run(*args):
    return subprocess.check_output(args, text=True, stderr=subprocess.PIPE)


def github_json(repository, endpoint):
    return json.loads(run("gh", "api", f"repos/{repository}/{endpoint}"))


def release_asset(release, tag_ref, annotated_tag, approved):
    require(tag_ref.get("object", {}).get("type") == "tag", "An annotated release tag is required.")
    require(annotated_tag.get("tag") == approved["RELEASE_TAG"], "Annotated tag name mismatch.")
    target = annotated_tag.get("object", {})
    require(target.get("type") == "commit" and target.get("sha") == approved["SOURCE_SHA"],
            "Tag does not point to the approved source commit.")
    require(release.get("tag_name") == approved["RELEASE_TAG"], "Release tag mismatch.")
    require(release.get("draft") is False and release.get("prerelease") is False,
            "A published stable GitHub Release is required.")
    assets = [asset for asset in release.get("assets", []) if asset.get("name") == ASSET_NAME]
    require(len(assets) == 1, "Release must contain exactly one androidApp-release.aab.")
    asset = assets[0]
    require(asset.get("state") == "uploaded" and asset.get("size", 0) > 0, "AAB upload is incomplete.")
    digest = asset.get("digest")
    require(not digest or digest == "sha256:" + approved["AAB_SHA256"], "GitHub asset digest mismatch.")
    return asset


def verify_hash(path, expected):
    with Path(path).open("rb") as source:
        digest = hashlib.file_digest(source, "sha256").hexdigest()
    require(digest == expected, f"SHA-256 mismatch: {Path(path).name}")


def verify_manifest(xml, approved):
    manifest = ET.fromstring(xml)
    require(manifest.tag == "manifest", "Invalid Android manifest.")
    require(manifest.get("package") == PACKAGE_NAME, "AAB package must be com.peto.ramap.")
    require(manifest.get(ANDROID_NS + "versionCode") == approved["VERSION_CODE"], "AAB versionCode mismatch.")
    require(manifest.get(ANDROID_NS + "versionName") == approved["RELEASE_TAG"][1:], "AAB versionName mismatch.")
    application = manifest.find("application")
    require(application is not None, "AAB application manifest is missing.")
    require(application.get(ANDROID_NS + "debuggable", "false") == "false", "Debuggable AAB is forbidden.")


def bundletool(directory):
    path = directory / f"bundletool-all-{BUNDLETOOL_VERSION}.jar"
    if not path.exists():
        url = f"https://github.com/google/bundletool/releases/download/{BUNDLETOOL_VERSION}/{path.name}"
        with urllib.request.urlopen(url, timeout=120) as response, path.open("wb") as output:
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
    verify_hash(path, BUNDLETOOL_SHA256)
    return path


def verify_aab(path, approved, tool):
    verify_hash(path, approved["AAB_SHA256"])
    signature = run("jarsigner", "-J-Duser.language=en", "-J-Duser.country=US", "-verify", "-verbose", "-certs", str(path))
    require("jar verified." in signature and "jar is unsigned" not in signature,
            "AAB signature verification failed.")
    # Self-signed upload certificates are valid; jarsigner's strict CA check is not applicable.
    xml = run("java", "-jar", str(tool), "dump", "manifest", f"--bundle={path}", "--module=base")
    verify_manifest(xml, approved)


def prepare(approved, directory):
    directory.mkdir(parents=True, exist_ok=True)
    # A failed retry must not leave a previously verified manifest available to the upload lane.
    (directory / "verified.json").unlink(missing_ok=True)
    repository = approved["GITHUB_REPOSITORY"]
    tag = approved["RELEASE_TAG"]
    tag_ref = github_json(repository, f"git/ref/tags/{tag}")
    require(tag_ref.get("object", {}).get("type") == "tag", "An annotated release tag is required.")
    tag_object = tag_ref["object"]["sha"]
    require(re.fullmatch(r"[0-9a-f]{40}", tag_object) is not None, "Invalid tag object.")
    annotated_tag = github_json(repository, f"git/tags/{tag_object}")
    release = github_json(repository, f"releases/tags/{tag}")
    asset = release_asset(release, tag_ref, annotated_tag, approved)
    path = directory / ASSET_NAME
    require(not path.exists(), "Use a fresh output directory; an AAB already exists.")
    run("gh", "release", "download", tag, "--repo", repository, "--pattern", ASSET_NAME, "--dir", str(directory))
    require(path.stat().st_size == asset["size"], "Downloaded AAB size mismatch.")
    verify_aab(path, approved, bundletool(directory))
    result = {
        "package_name": PACKAGE_NAME,
        "track": "internal",
        "tag": tag,
        "source_sha": approved["SOURCE_SHA"],
        "version_code": approved["VERSION_CODE"],
        "sha256": approved["AAB_SHA256"],
        "aab": str(path.resolve()),
        "release_url": release["html_url"],
    }
    (directory / "verified.json").write_text(json.dumps(result, indent=4) + "\n", encoding="utf-8")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    arguments = parser.parse_args()
    try:
        result = prepare(approved_inputs(os.environ), arguments.output.resolve())
    except (ValueError, OSError, subprocess.CalledProcessError, ET.ParseError) as error:
        # Subprocess stderr may contain provider details; do not forward credentials or signed URLs.
        parser.exit(1, f"Release verification failed: {error}\n")
    print(json.dumps(result, indent=4))


if __name__ == "__main__":
    main()
