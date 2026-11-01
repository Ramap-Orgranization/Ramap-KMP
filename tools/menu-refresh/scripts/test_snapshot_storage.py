import copy
import gzip
import json
import unittest
from unittest.mock import Mock, patch
from urllib.parse import parse_qs, urlparse

from migrate_snapshot_storage import migrate_row
from snapshot_storage import AdminApi, BUCKET, SnapshotStorage, hydrate_snapshots, object_path, sha256, unpack_original


class FakeApi:
    def __init__(self):
        self.objects, self.registry = {}, {}
        self.corrupt_download = False
        self.upload_error = False
        self.attachments = []

    def request(self, method, path, body=None, headers=None, allowed=()):
        key = path.split(BUCKET + '/', 1)[1]
        if method == 'POST':
            if self.upload_error:
                return 400, b'{"error":"not a duplicate"}'
            if key in self.objects:
                return 409, b'{"statusCode":"409","error":"Duplicate"}'
            self.objects[key] = body
            return 200, b'{}'
        packed = self.objects[key]
        return 200, b'broken' if self.corrupt_download else packed

    def json(self, method, path, body=None, headers=None):
        if '/rpc/' in path:
            self.attachments.append(copy.deepcopy(body))
            return 'archived'
        if method == 'POST':
            for row in body:
                key = row['raw_sha256']
                self.registry.setdefault(key, {**row, 'verified_at': '2026-11-01T00:00:00Z'})
            return None
        keys = parse_qs(urlparse(path).query)['raw_sha256'][0].removeprefix('in.(').removesuffix(')').split(',')
        return [copy.deepcopy(self.registry[key]) for key in keys]


class SnapshotStorageTest(unittest.TestCase):
    def setUp(self):
        self.api = FakeApi()
        self.storage = SnapshotStorage(self.api)
        self.raw = '<html><body>한글 메뉴\r\n<img alt="메뉴판" src="https://example.com/board.jpg"></body></html>'.encode()

    def test_원문을_바이트_단위로_복원하고_검증한_파일만_등록한다(self):
        record = self.storage.archive(self.raw)
        self.assertEqual(self.raw, self.storage.fetch_object(record))
        self.assertEqual(sha256(self.raw), record['raw_sha256'])
        self.assertEqual('gzip', record['compression'])
        self.assertEqual(1, len(self.api.registry))

    def test_같은_원문은_덮어쓰지_않고_동일한_파일을_재사용한다(self):
        first = self.storage.archive(self.raw)
        second = self.storage.archive(self.raw)
        self.assertEqual(first, second)
        self.assertEqual(1, len(self.api.objects))

    def test_기존_gzip_헤더가_달라도_실제_다운로드한_파일의_해시를_등록한다(self):
        packed = gzip.compress(self.raw, compresslevel=1, mtime=10)
        self.api.objects[object_path(sha256(self.raw))] = packed
        record = self.storage.archive(self.raw)
        self.assertEqual(sha256(packed), record['gzip_sha256'])
        self.assertEqual(self.raw, self.storage.fetch_object(record))

    def test_다운로드가_손상되면_등록과_DB_이전을_시작하지_않는다(self):
        self.api.corrupt_download = True
        with self.assertRaises(ValueError):
            migrate_row(self.storage, {'id': 'snapshot', 'source_hash': sha256(self.raw), 'raw_dom': self.raw.decode()})
        self.assertEqual({}, self.api.registry)
        self.assertEqual([], self.api.attachments)

    def test_중복이_아닌_업로드_오류를_재사용_성공으로_처리하지_않는다(self):
        self.api.upload_error = True
        with self.assertRaises(ValueError):
            self.storage.archive(self.raw)
        self.assertEqual({}, self.api.registry)

    def test_복원_길이와_원문_해시와_압축_해시를_각각_검사한다(self):
        packed = gzip.compress(self.raw, mtime=0)
        for raw_hash, size, gzip_hash in (
            ('0' * 64, len(self.raw), sha256(packed)),
            (sha256(self.raw), len(self.raw) - 1, sha256(packed)),
            (sha256(self.raw), len(self.raw), '0' * 64)):
            with self.subTest(size=size, raw_hash=raw_hash, gzip_hash=gzip_hash), self.assertRaises(ValueError):
                unpack_original(packed, raw_hash, size, gzip_hash)

    def test_과거_출처_해시가_달라도_보존하고_실제_원문_해시로_이전한다(self):
        row = {'id': 'snapshot', 'source_hash': '1' * 64, 'raw_dom': self.raw.decode()}
        result = migrate_row(self.storage, row)
        self.assertTrue(result['historical_hash_differs'])
        self.assertEqual('1' * 64, row['source_hash'])
        self.assertEqual(sha256(self.raw), self.api.attachments[0]['p_raw_sha256'])

    def test_삭제_검증은_Storage에서_복원한_전체_원문을_사용한다(self):
        record = self.storage.archive(self.raw)
        snapshot = {'raw_storage': record, 'raw_hash': None, 'needs_deletion_raw': True}
        hydrate_snapshots([snapshot], self.storage)
        self.assertEqual(sha256(self.raw), snapshot['raw_hash'])
        self.assertEqual(self.raw, snapshot['deletion_raw_dom'].encode())

    def test_비공개_버킷_밖의_경로나_해시와_다른_파일은_거절한다(self):
        record = self.storage.archive(self.raw)
        for changed in ({'storage_bucket': 'public'}, {'storage_path': '../elsewhere'}):
            with self.subTest(changed=changed), self.assertRaises(ValueError):
                self.storage.fetch_object({**record, **changed})

    def test_스냅샷_검증시_손상된_파일의_검증_해시를_채우지_않는다(self):
        record = self.storage.archive(self.raw)
        snapshot = {'raw_storage': record, 'raw_hash': None}
        self.api.corrupt_download = True
        with self.assertRaises(ValueError):
            hydrate_snapshots([snapshot], self.storage)
        self.assertIsNone(snapshot['raw_hash'])

    def test_DB_조회_전송_압축은_JSON을_복원하고_원문_gzip과_분리한다(self):
        api = AdminApi('a' * 20, 'test-only-key')
        connection = Mock()
        response = Mock(status=200)
        response.read.return_value = gzip.compress(b'[{"id":"snapshot"}]')
        response.getheader.return_value = 'gzip'
        connection.getresponse.return_value = response
        api.connections.http = connection
        api.connections.storage_http = connection
        self.assertEqual([{'id': 'snapshot'}], api.json('GET', '/rest/v1/shop_menu_snapshots'))
        self.assertEqual('gzip', connection.request.call_args.kwargs['headers']['Accept-Encoding'])
        api.request('GET', '/storage/v1/object/authenticated/test/file.gz')
        self.assertEqual('identity', connection.request.call_args.kwargs['headers']['Accept-Encoding'])

    def test_원문_파일은_프로젝트의_전용_Storage_호스트로_전송한다(self):
        api = AdminApi('a' * 20, 'test-only-key')
        connection = Mock()
        response = Mock(status=200)
        response.read.return_value = b'file'
        response.getheader.return_value = None
        connection.getresponse.return_value = response
        with patch('snapshot_storage.http.client.HTTPSConnection', return_value=connection) as connect:
            api.request('GET', '/storage/v1/object/authenticated/test/file.gz')
        connect.assert_called_once_with('a' * 20 + '.storage.supabase.co', timeout=60)

    def test_일시적인_게이트웨이_오류는_연결을_바꿔_제한적으로_재시도한다(self):
        for status in (520, 544):
            with self.subTest(status=status):
                api = AdminApi('a' * 20, 'test-only-key')
                failed, succeeded = Mock(), Mock()
                for connection, code in ((failed, status), (succeeded, 200)):
                    response = Mock(status=code)
                    response.read.return_value = b'{}'
                    response.getheader.return_value = None
                    connection.getresponse.return_value = response
                with patch('snapshot_storage.http.client.HTTPSConnection', side_effect=[failed, succeeded]), patch('snapshot_storage.time.sleep'):
                    self.assertEqual({}, api.json('POST', '/rest/v1/test', {'id': 'idempotent-test'}))
                failed.close.assert_called_once()
                succeeded.request.assert_called_once()


if __name__ == '__main__':
    unittest.main()
