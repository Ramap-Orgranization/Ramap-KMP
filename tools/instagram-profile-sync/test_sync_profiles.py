import importlib.util
import sys
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("sync_profiles.py")
SPEC = importlib.util.spec_from_file_location("sync_profiles", MODULE_PATH)
sync_profiles = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = sync_profiles
SPEC.loader.exec_module(sync_profiles)


class UsernameFromUrlTest(unittest.TestCase):
    def test_extracts_username_from_profile_url(self):
        self.assertEqual("ramen_shop", sync_profiles.username_from_url("https://instagram.com/ramen_shop/"))

    def test_removes_query_string_and_at_prefix(self):
        self.assertEqual("ramen_shop", sync_profiles.username_from_url("https://instagram.com/@ramen_shop?x=1"))


class SelectSourcesTest(unittest.TestCase):
    def setUp(self):
        self.rows = [
            {"shop_id": "1", "instagram_url": "https://instagram.com/first/"},
            {"shop_id": "2", "instagram_url": "https://instagram.com/second/"},
            {"shop_id": "3", "instagram_url": "https://instagram.com/third/"},
        ]

    def test_limits_sources_when_usernames_are_not_given(self):
        sources = sync_profiles.select_sources(self.rows, [], 2)

        self.assertEqual(["first", "second"], [source.username for source in sources])

    def test_selects_requested_usernames_in_request_order(self):
        sources = sync_profiles.select_sources(self.rows, ["THIRD", "first"], 1)

        self.assertEqual(["third", "first"], [source.username for source in sources])

    def test_selects_all_sources_when_limit_is_none(self):
        sources = sync_profiles.select_sources(self.rows, [], None)

        self.assertEqual(["first", "second", "third"], [source.username for source in sources])


class ChargeLimitTest(unittest.TestCase):
    def test_uses_minimum_limit_for_small_dry_run(self):
        self.assertEqual(0.01, sync_profiles.charge_limit_usd(3))

    def test_caps_full_run_charge_limit(self):
        self.assertEqual(1.25, sync_profiles.charge_limit_usd(10_000))


if __name__ == "__main__":
    unittest.main()
