import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch

DIRECTORY = Path(__file__).resolve().parent

def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, DIRECTORY / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

provision = load('r2_provision_test', 'provision.py')
import sys
sys.path.insert(0, str(DIRECTORY))
deploy_worker = load('r2_worker_deploy_test', 'deploy_worker.py')
migrate = load('r2_migrate_test', 'migrate.py')

class R2ToolsTest(unittest.TestCase):
    def setUp(self):
        self.config = {'r2_account_id': 'a' * 32, 'r2_public_bucket': 'ramap-test-public', 'r2_private_bucket': 'ramap-test-private'}

    def test_domain_can_be_added_later_but_private_and_public_must_differ(self):
        self.assertEqual(provision.validate(self.config), self.config)
        with self.assertRaises(ValueError):
            provision.validate({**self.config, 'r2_private_bucket': self.config['r2_public_bucket']})

    def test_owned_domain_is_required_for_cdn(self):
        for domain in ['http://images.example.com', 'https://public.r2.dev', 'https://images.example.com/path', 'https://user:secret@images.example.com']:
            with self.assertRaises(ValueError):
                provision.validate({**self.config, 'r2_public_base_url': domain, 'cloudflare_zone_id': 'b' * 32})

    def test_worker_address_needs_no_owned_domain_or_zone(self):
        config = {**self.config, 'public_worker_name': 'ramap-images', 'r2_public_base_url': 'https://ramap-images.account.workers.dev'}
        self.assertEqual(provision.validate(config), config)
        for base in ['https://other.account.workers.dev', 'https://account.workers.dev', 'https://ramap-images.account.workers.dev/path']:
            with self.assertRaises(ValueError):
                provision.validate({**config, 'r2_public_base_url': base})

    def test_worker_has_only_public_bucket_binding_and_no_secrets(self):
        settings = deploy_worker.worker_configuration(self.config)
        self.assertEqual(settings['r2_buckets'], [{'binding': 'PUBLIC_IMAGES', 'bucket_name': self.config['r2_public_bucket']}])
        self.assertTrue(settings['workers_dev'])
        self.assertNotIn('routes', settings)
        self.assertNotIn('vars', settings)
        self.assertNotIn(self.config['r2_private_bucket'], json.dumps(settings))

    def test_private_bucket_with_public_domain_is_rejected(self):
        def api(config, method, path, body=None):
            if path == '':
                return {'buckets': [{'name': self.config['r2_public_bucket']}, {'name': self.config['r2_private_bucket']}]}
            if path.endswith('custom'):
                return {'domains': [{'domain': 'private.example.com', 'enabled': True}]} if 'private' in path else {'domains': []}
            return {}
        with patch.object(provision, 'cloudflare', api), self.assertRaises(ValueError):
            provision.provision(self.config)

    def test_provision_requests_standard_apac_and_disables_development_domain(self):
        calls = []
        def api(config, method, path, body=None):
            calls.append((method, path, body))
            if method == 'GET' and not path:
                return {'buckets': []}
            return {'domains': []}
        with patch.object(provision, 'cloudflare', api):
            provision.provision(self.config)
        created = [c[2] for c in calls if c[0] == 'POST']
        self.assertEqual(len(created), 2)
        self.assertTrue(all(c['locationHint'] == 'apac' and c['storageClass'] == 'Standard' for c in created))
        self.assertEqual(len([c for c in calls if c[0] == 'PUT' and c[2] == {'enabled': False}]), 2)

    def test_copy_uses_supabase_source_and_never_removes_original(self):
        tool = object.__new__(migrate.Migration)
        tool.config = {'media_api_url': 'https://media.example.com'}
        tool.key = 'test'
        requests = []
        tool.request = lambda method, url, data, headers: requests.append((method, url, json.loads(data))) or b'{"copied":true}'
        source = {'provider': 'supabase', 'bucket': 'profile-avatars', 'path': 'a.jpg', 'updated_at': '2026-11-03T00:00:00Z'}
        row = {'migration_needed': True, 'migration_reason': 'current_reference', 'bucket': 'profile-avatars', 'path': 'a.jpg', 'locations': [], 'legacy_updated_at': '2026-11-03T00:00:00Z'}
        self.assertTrue(tool.copy(row)['copied'])
        self.assertEqual(requests[0][0], 'POST')
        self.assertTrue(requests[0][1].endswith('/internal/v2/backfill'))
        self.assertEqual(requests[0][2]['source'], source)
        self.assertEqual(len(requests), 1)

    def test_unused_images_are_skipped_before_storage_requests(self):
        tool = object.__new__(migrate.Migration)
        tool.request = lambda *args: self.fail('An excluded image was transferred')
        row = {'migration_needed': False, 'migration_reason': 'ended_event'}
        self.assertTrue(tool.copy(row)['skipped'])
        with self.assertRaises(ValueError):
            migrate.needed({'locations': []})

    def test_only_verified_live_r2_copies_are_complete(self):
        for verified, deleted, expected in [(True, False, True), (False, False, False), (True, True, False)]:
            self.assertEqual(migrate.copied({'locations': [{'provider': 'r2', 'verified': verified, 'deleted': deleted}]}), expected)

if __name__ == '__main__':
    unittest.main()
