"""Private, content-addressed gzip originals; credentials never leave process memory."""
import gzip
import hashlib
import http.client
import io
import json
import re
import subprocess
import tempfile
import threading
import time
from pathlib import Path
from urllib.parse import urlencode


BUCKET = 'menu-snapshot-raw'
MAX_RAW_BYTES = 32 * 1024 * 1024
MAX_GZIP_BYTES = 16 * 1024 * 1024
REPO = Path(__file__).resolve().parents[3]


def sha256(value):
    return hashlib.sha256(value).hexdigest()


def object_path(raw_hash):
    if not isinstance(raw_hash, str) or not re.fullmatch('[0-9a-f]{64}', raw_hash):
        raise ValueError('invalid original SHA-256')
    return f'sha256/{raw_hash[:2]}/{raw_hash}.html.gz'


def unpack_original(packed, raw_hash, original_bytes, gzip_hash=None):
    object_path(raw_hash)
    if not isinstance(original_bytes, int) or not 0 < original_bytes <= MAX_RAW_BYTES:
        raise ValueError('invalid original byte length')
    if len(packed) > MAX_GZIP_BYTES or gzip_hash is not None and sha256(packed) != gzip_hash:
        raise ValueError('gzip checksum or length mismatch')
    try:
        with gzip.GzipFile(fileobj=io.BytesIO(packed)) as stream:
            raw = stream.read(original_bytes + 1)
    except (OSError, EOFError) as error:
        raise ValueError('invalid gzip original') from error
    if len(raw) != original_bytes or sha256(raw) != raw_hash:
        raise ValueError('restored original checksum or length mismatch')
    raw.decode('utf-8', errors='strict')
    return raw


class AdminApi:
    """Service credential from the authenticated CLI; one HTTPS connection per thread."""
    def __init__(self, project_id, service_key):
        if not re.fullmatch('[a-z0-9]{20}', project_id):
            raise ValueError('invalid Supabase project reference')
        self.project_id, self.service_key = project_id, service_key
        self.connections = threading.local()

    @classmethod
    def from_cli(cls, project_id):
        if not re.fullmatch('[a-z0-9]{20}', project_id):
            raise ValueError('invalid Supabase project reference')
        result = subprocess.run(['supabase', 'projects', 'api-keys', '--project-ref', project_id,
            '--output', 'json'], cwd=REPO, capture_output=True, text=True, timeout=45)
        if result.returncode:
            raise ValueError('could not load the project service credential from Supabase CLI')
        keys = json.loads(result.stdout)
        key = next((k.get('api_key') for k in keys if k.get('name') == 'service_role'), None)
        if not key or len(key) < 100:
            raise ValueError('Supabase CLI did not provide a service-role credential')
        return cls(project_id, key)

    def request(self, method, path, body=None, headers=None, allowed=(200, 201, 204)):
        request_headers = {'apikey': self.service_key, 'Authorization': 'Bearer ' + self.service_key,
            'Accept-Encoding': 'gzip' if path.startswith('/rest/v1/') else 'identity', **(headers or {})}
        if body is not None and not isinstance(body, bytes):
            body = json.dumps(body, ensure_ascii=False).encode('utf-8')
            request_headers['Content-Type'] = 'application/json'
        is_storage = path.startswith('/storage/v1/')
        connection_name = 'storage_http' if is_storage else 'http'
        hostname = self.project_id + ('.storage.supabase.co' if is_storage else '.supabase.co')
        for attempt in range(3):
            connection = getattr(self.connections, connection_name, None)
            if connection is None:
                connection = http.client.HTTPSConnection(hostname, timeout=60)
                setattr(self.connections, connection_name, connection)
            try:
                connection.request(method, path, body=body, headers=request_headers)
                response = connection.getresponse()
                data = response.read()
                if response.getheader('Content-Encoding') == 'gzip':
                    data = gzip.decompress(data)
                status = response.status
                if status in allowed:
                    return status, data
                if status not in (429, 500, 502, 503, 504, 520, 522, 524, 544):
                    # Never include provider bodies, headers, credentials or original HTML in errors.
                    raise ValueError(f'Supabase {method} failed with HTTP {status}')
            except (OSError, http.client.HTTPException):
                pass
            connection.close()
            setattr(self.connections, connection_name, None)
            if attempt < 2:
                time.sleep(attempt + 1)
        raise ValueError(f'Supabase {method} failed after bounded retries')

    def json(self, method, path, body=None, headers=None):
        _, data = self.request(method, path, body, headers)
        return json.loads(data) if data else None


class SnapshotStorage:
    def __init__(self, api, backup_dir=None):
        self.api = api
        self.backup_dir = Path(backup_dir) if backup_dir else None

    @classmethod
    def from_cli(cls, project_id, backup_dir=None):
        return cls(AdminApi.from_cli(project_id), backup_dir)

    def fetch_object(self, record):
        path = object_path(record['raw_sha256'])
        if record['storage_bucket'] != BUCKET or record['storage_path'] != path:
            raise ValueError('snapshot original points outside its private content-addressed bucket')
        _, packed = self.api.request('GET', f'/storage/v1/object/authenticated/{BUCKET}/{path}')
        if len(packed) != record['gzip_bytes']:
            raise ValueError('downloaded gzip byte length mismatch')
        raw = unpack_original(packed, record['raw_sha256'], record['original_bytes'], record['gzip_sha256'])
        return raw

    def archive(self, raw, register=True):
        raw = raw.encode('utf-8') if isinstance(raw, str) else raw
        if not 0 < len(raw) <= MAX_RAW_BYTES:
            raise ValueError('original exceeds the supported byte length')
        raw_hash = sha256(raw)
        path = object_path(raw_hash)
        packed = gzip.compress(raw, compresslevel=9, mtime=0)
        if len(packed) > MAX_GZIP_BYTES:
            raise ValueError('gzip original exceeds the private bucket byte limit')
        status, body = self.api.request('POST', f'/storage/v1/object/{BUCKET}/{path}', packed,
            {'Content-Type': 'application/gzip', 'x-upsert': 'false'}, allowed=(200, 201, 400, 409))
        if status not in (200, 201):
            try:
                duplicate = json.loads(body)
            except (ValueError, UnicodeError) as error:
                raise ValueError('unexpected gzip upload failure') from error
            if str(duplicate.get('statusCode')) != '409' and duplicate.get('error') not in ('Duplicate', 'Asset Already Exists'):
                raise ValueError('gzip upload failed without a confirmed immutable-object conflict')
        _, downloaded = self.api.request('GET', f'/storage/v1/object/authenticated/{BUCKET}/{path}')
        restored = unpack_original(downloaded, raw_hash, len(raw))
        if restored != raw:
            raise ValueError('Storage round-trip differs from the database original')
        record = {'raw_sha256': raw_hash, 'storage_bucket': BUCKET, 'storage_path': path,
            'compression': 'gzip', 'original_bytes': len(raw), 'gzip_bytes': len(downloaded),
            'gzip_sha256': sha256(downloaded)}
        registered = self.register_many([record])[raw_hash] if register else record
        if self.backup_dir:
            target = self.backup_dir / (raw_hash + '.html.gz')
            target.parent.mkdir(parents=True, exist_ok=True)
            with tempfile.NamedTemporaryFile(dir=target.parent, delete=False) as stream:
                temporary = Path(stream.name)
                stream.write(downloaded)
            temporary.replace(target)
        return registered

    def register_many(self, records):
        unique = {}
        for record in records:
            key = record['raw_sha256']
            if key in unique and record != unique[key]:
                raise ValueError('different archive metadata for the same original')
            unique[key] = record
        if not unique or len(unique) > 100:
            raise ValueError('archive registry batch must contain 1..100 originals')
        self.api.json('POST', '/rest/v1/menu_snapshot_raw_objects?on_conflict=raw_sha256', list(unique.values()),
            {'Prefer': 'resolution=ignore-duplicates,return=minimal'})
        stored = self.api.json('GET', '/rest/v1/menu_snapshot_raw_objects?' + urlencode(
            {'raw_sha256': 'in.(' + ','.join(unique) + ')', 'select': '*'}))
        if len(stored) != len(unique) or any(any(r.get(k) != v for k, v in unique[r['raw_sha256']].items()) for r in stored):
            raise ValueError('stored archive registry does not match the verified objects')
        return {r['raw_sha256']: r for r in stored}

    def attach(self, snapshot_id, raw_hash):
        return self.api.json('POST', '/rest/v1/rpc/archive_shop_menu_snapshot',
            {'p_snapshot_id': snapshot_id, 'p_raw_sha256': raw_hash})

    def attach_many(self, bindings):
        return self.api.json('POST', '/rest/v1/rpc/archive_shop_menu_snapshot_batch', {'p_bindings': bindings})


def hydrate_snapshots(snapshots, storage):
    """Every archived verification downloads and hashes the actual original, including deletions."""
    for snapshot in snapshots:
        record = snapshot.get('raw_storage')
        if record is None:
            continue
        raw = storage.fetch_object(record)
        snapshot['raw_hash'] = sha256(raw)
        if snapshot.pop('needs_deletion_raw', False):
            snapshot['deletion_raw_dom'] = raw.decode('utf-8')


def main():
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=('put', 'read'))
    parser.add_argument('--project-id', required=True)
    parser.add_argument('--raw-file', type=Path)
    parser.add_argument('--snapshot-id')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    storage = SnapshotStorage.from_cli(args.project_id)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.command == 'put':
        if not args.raw_file:
            parser.error('put requires --raw-file')
        record = storage.archive(args.raw_file.read_bytes())
        args.output.write_text(json.dumps(record, indent=2) + '\n')
        print(json.dumps({'verified': True, 'raw_sha256': record['raw_sha256'], 'gzip_bytes': record['gzip_bytes']}))
        return
    import uuid
    if not args.snapshot_id:
        parser.error('read requires --snapshot-id')
    snapshot_id = str(uuid.UUID(args.snapshot_id))
    rows = storage.api.json('GET', '/rest/v1/shop_menu_snapshots?' + urlencode(
        {'id': 'eq.' + snapshot_id, 'select': 'id,raw_dom,raw_object_sha256,raw_storage:menu_snapshot_raw_objects(*)'}))
    if len(rows) != 1:
        raise ValueError('snapshot not found')
    snapshot = rows[0]
    raw = storage.fetch_object(snapshot['raw_storage']) if snapshot['raw_storage'] else snapshot['raw_dom'].encode('utf-8')
    args.output.write_bytes(raw)
    print(json.dumps({'verified': bool(snapshot['raw_storage']), 'raw_sha256': sha256(raw), 'original_bytes': len(raw)}))


if __name__ == '__main__':
    main()

