"""Approved AAB provenance and byte-identity regression tests (no network)."""

import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch


SPEC = importlib.util.spec_from_file_location("play_internal", Path(__file__).resolve().parents[1] / "play_internal.py")
RELEASE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RELEASE)


class PlayInternalTest(unittest.TestCase):
    def setUp(self):
        self.approved = {
            "RELEASE_TAG": "v1.2.3", "SOURCE_SHA": "a" * 40, "VERSION_CODE": "30",
            "AAB_SHA256": "b" * 64, "GITHUB_REPOSITORY": "example/ramap",
        }
        self.asset = {"name": RELEASE.ASSET_NAME, "state": "uploaded", "size": 10,
                      "digest": "sha256:" + "b" * 64}
        self.release = {"tag_name": "v1.2.3", "draft": False, "prerelease": False, "assets": [self.asset]}
        self.ref = {"object": {"type": "tag", "sha": "c" * 40}}
        self.tag = {"tag": "v1.2.3", "object": {"type": "commit", "sha": "a" * 40}}

    def test_승인된_정확한_입력만_허용한다(self):
        self.assertEqual(self.approved, RELEASE.approved_inputs(self.approved))
        for name, value in [("RELEASE_TAG", "v1.2.3;echo x"), ("SOURCE_SHA", "main"),
                            ("VERSION_CODE", "0"), ("VERSION_CODE", "2100000001"),
                            ("AAB_SHA256", "b" * 63), ("GITHUB_REPOSITORY", "repo/../../other")]:
            with self.subTest(name=name, value=value), self.assertRaises(ValueError):
                RELEASE.approved_inputs(dict(self.approved, **{name: value}))

    def test_공개_릴리스와_annotated_tag의_커밋을_대조한다(self):
        self.assertEqual(self.asset, RELEASE.release_asset(self.release, self.ref, self.tag, self.approved))
        self.tag["object"]["sha"] = "d" * 40
        with self.assertRaisesRegex(ValueError, "source commit"):
            RELEASE.release_asset(self.release, self.ref, self.tag, self.approved)

    def test_초안과_사전릴리스와_다른_태그는_거부한다(self):
        for key, value in [("draft", True), ("prerelease", True), ("tag_name", "v1.2.4")]:
            with self.subTest(key=key), self.assertRaises(ValueError):
                RELEASE.release_asset(dict(self.release, **{key: value}), self.ref, self.tag, self.approved)
        self.ref["object"]["type"] = "commit"
        with self.assertRaises(ValueError):
            RELEASE.release_asset(self.release, self.ref, self.tag, self.approved)

    def test_누락되거나_중복되거나_다른_해시의_자산은_거부한다(self):
        for assets in [[], [self.asset, self.asset], [dict(self.asset, digest="sha256:" + "d" * 64)],
                       [dict(self.asset, state="starter")], [dict(self.asset, size=0)]]:
            with self.subTest(assets=assets), self.assertRaises(ValueError):
                RELEASE.release_asset(dict(self.release, assets=assets), self.ref, self.tag, self.approved)

    def test_기존_GitHub_자산에_digest가_없어도_다운로드_검증을_진행한다(self):
        release = copy.deepcopy(self.release)
        del release["assets"][0]["digest"]
        self.assertEqual(RELEASE.ASSET_NAME, RELEASE.release_asset(release, self.ref, self.tag, self.approved)["name"])

    def test_다운로드한_파일이_한_바이트라도_다르면_거부한다(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "app.aab"
            path.write_bytes(b"approved")
            digest = RELEASE.hashlib.sha256(b"approved").hexdigest()
            RELEASE.verify_hash(path, digest)
            path.write_bytes(b"approved!")
            with self.assertRaisesRegex(ValueError, "SHA-256 mismatch"):
                RELEASE.verify_hash(path, digest)

    def test_실제_매니페스트의_앱과_버전과_debuggable을_검사한다(self):
        xml = '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.peto.ramap" android:versionCode="30" android:versionName="1.2.3"><application /></manifest>'
        RELEASE.verify_manifest(xml, self.approved)
        for invalid in [xml.replace("com.peto.ramap", "com.peto.ramap.debug"),
                        xml.replace('versionCode="30"', 'versionCode="31"'),
                        xml.replace('versionName="1.2.3"', 'versionName="1.2.4"'),
                        xml.replace("<application", '<application android:debuggable="true"'),
                        xml.replace("<application />", "")]:
            with self.subTest(xml=invalid), self.assertRaises(ValueError):
                RELEASE.verify_manifest(invalid, self.approved)

    def test_서명되지_않은_AAB는_매니페스트_검사_전에도_거부한다(self):
        with patch.object(RELEASE, "verify_hash"), patch.object(RELEASE, "run", return_value="jar is unsigned."):
            with self.assertRaisesRegex(ValueError, "signature"):
                RELEASE.verify_aab(Path("test.aab"), self.approved, Path("bundletool.jar"))

    def test_검증_실패_시_이전_실행의_검증_파일을_남기지_않는다(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary)
            (output / "verified.json").write_text("old result")
            with patch.object(RELEASE, "github_json", return_value={}), self.assertRaises(ValueError):
                RELEASE.prepare(self.approved, output)
            self.assertFalse((output / "verified.json").exists())

    def test_승인_파일을_다운로드해_검증한_후에만_결과를_작성한다(self):
        release = dict(self.release, html_url="https://github.com/example/ramap/releases/tag/v1.2.3")
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary)
            def download(*args):
                self.assertEqual(("gh", "release", "download", "v1.2.3"), args[:4])
                (output / RELEASE.ASSET_NAME).write_bytes(b"1234567890")
            with patch.object(RELEASE, "github_json", side_effect=[self.ref, self.tag, release]), \
                    patch.object(RELEASE, "run", side_effect=download), \
                    patch.object(RELEASE, "bundletool", return_value=output / "tool.jar"), \
                    patch.object(RELEASE, "verify_aab") as verify:
                result = RELEASE.prepare(self.approved, output)
            verify.assert_called_once()
            self.assertEqual("internal", result["track"])
            self.assertEqual(self.approved["AAB_SHA256"], result["sha256"])
            self.assertEqual(result, json.loads((output / "verified.json").read_text()))

    def test_운영심사는_허용된_트랙과_출시노트를_요구한다(self):
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary)
            with self.assertRaises(ValueError):
                RELEASE.prepare(self.approved, output, "beta")
            for notes in ["", "가" * 501]:
                with patch.object(RELEASE, "github_json", side_effect=[self.ref, self.tag, dict(self.release, body=notes)]), \
                        patch.object(RELEASE, "run") as download, self.assertRaises(ValueError):
                    RELEASE.prepare(self.approved, output, "production")
                download.assert_not_called()


if __name__ == "__main__":
    unittest.main()
