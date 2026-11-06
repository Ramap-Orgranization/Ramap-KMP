#!/usr/bin/env python3
"""Resume-safe gzip archival. Only a verified round trip can detach an inline original."""
import argparse
import concurrent.futures
import json
import subprocess
import tempfile
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlencode

from snapshot_storage import SnapshotStorage, backend_workdir, linked_backend_project


def save(path, value):
    path = Path(path)
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    temporary.replace(path)


def statistics(project_id):
    workdir = backend_workdir()
    if linked_backend_project(workdir) != project_id:
        raise ValueError('linked CLI project differs from the archive project')
    sql = """SELECT jsonb_build_object(
        'measured_at',now(),
        'snapshots',(SELECT count(*) FROM public.shop_menu_snapshots),
        'inline_count',(SELECT count(*) FROM public.shop_menu_snapshots WHERE raw_dom IS NOT NULL),
        'archived_count',(SELECT count(*) FROM public.shop_menu_snapshots WHERE raw_object_sha256 IS NOT NULL),
        'metadata_digest',(SELECT md5(string_agg(md5(jsonb_build_object(
            'id',id,'shop_id',shop_id,'captured_at',captured_at,'source_url',source_url,
            'parser_version',parser_version,'source_hash',source_hash,'menu_items',menu_items,
            'menu_fingerprint',menu_fingerprint)::text),'' ORDER BY id)) FROM public.shop_menu_snapshots),
        'menu_items_digest',(SELECT md5(string_agg(md5(to_jsonb(i)::text),'' ORDER BY id)) FROM public.shop_menu_items i),
        'menu_sections_digest',(SELECT md5(string_agg(md5(to_jsonb(s)::text),'' ORDER BY id)) FROM public.shop_menu_sections s),
        'object_count',(SELECT count(*) FROM public.menu_snapshot_raw_objects),
        'object_original_bytes',(SELECT coalesce(sum(original_bytes),0) FROM public.menu_snapshot_raw_objects),
        'object_gzip_bytes',(SELECT coalesce(sum(gzip_bytes),0) FROM public.menu_snapshot_raw_objects),
        'relation_bytes',pg_total_relation_size('public.shop_menu_snapshots'::regclass),
        'database_bytes',pg_database_size(current_database())) AS statistics;"""
    with tempfile.NamedTemporaryFile(mode='w', suffix='.sql') as stream:
        stream.write(sql)
        stream.flush()
        result = subprocess.run(['supabase', 'db', 'query', '--linked', '--file', stream.name, '--output', 'json'],
            cwd=workdir, capture_output=True, text=True, timeout=90)
    if result.returncode:
        raise ValueError('could not read archive statistics through the linked CLI')
    return json.loads(result.stdout)['rows'][0]['statistics']


def archive_row(storage, row):
    record = storage.archive(row['raw_dom'], register=False)
    return {'id': row['id'], 'source_hash': row['source_hash'], 'raw_sha256': record['raw_sha256'],
        'historical_hash_differs': row['source_hash'] != record['raw_sha256'],
        'original_bytes': record['original_bytes'], 'gzip_bytes': record['gzip_bytes'], 'object': record,
        'verified_at': datetime.now(timezone.utc).isoformat()}


def migrate_row(storage, row):
    record = archive_row(storage, row)
    storage.register_many([record.pop('object')])
    record['status'] = storage.attach(row['id'], record['raw_sha256'])
    if record['status'] not in ('archived', 'already_archived'):
        raise ValueError('snapshot attachment did not return a verified archival result')
    return record


def migrate(storage, output, workers=8, batch_size=20, limit=None, baseline=None, after=None):
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    before = json.loads(Path(baseline).read_text()) if baseline else statistics(storage.api.project_id)
    save(output / 'before.json', before)
    processed, historical, failures, cursor = 0, 0, [], after
    started = time.monotonic()
    with (output / 'verified-snapshots.jsonl').open('a') as journal:
        with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
            while limit is None or processed < limit:
                remaining = min(batch_size, limit - processed) if limit else batch_size
                params = {'select': 'id,raw_dom,source_hash', 'raw_dom': 'not.is.null', 'order': 'id', 'limit': remaining}
                if cursor:
                    params['id'] = 'gt.' + cursor
                rows = storage.api.json('GET', '/rest/v1/shop_menu_snapshots?' + urlencode(params))
                if not rows:
                    break
                cursor = rows[-1]['id']
                jobs = {pool.submit(archive_row, storage, row): row['id'] for row in rows}
                verified = []
                for job in concurrent.futures.as_completed(jobs):
                    snapshot_id = jobs[job]
                    try:
                        record = job.result()
                        verified.append(record)
                    except Exception as error:
                        failures.append({'id': snapshot_id, 'error_type': type(error).__name__, 'error': str(error)})
                if verified:
                    storage.register_many([r['object'] for r in verified])
                    attached = storage.attach_many([{'snapshot_id': r['id'], 'raw_sha256': r['raw_sha256']} for r in verified])
                    statuses = {r['snapshot_id']: r['status'] for r in attached}
                    if len(statuses) != len(verified) or any(statuses.get(r['id']) not in ('archived', 'already_archived') for r in verified):
                        raise ValueError('batch attachment did not confirm every verified snapshot')
                    for record in verified:
                        record.pop('object')
                        record['status'] = statuses[record['id']]
                        processed += 1
                        historical += int(record['historical_hash_differs'])
                        journal.write(json.dumps(record) + '\n')
                    journal.flush()
                print(json.dumps({'verified': processed, 'historical_hash_differences': historical,
                    'failures': len(failures), 'elapsed_seconds': round(time.monotonic() - started, 1)}), flush=True)
                save(output / 'progress.json', {'verified': processed, 'historical_hash_differences': historical,
                    'failures': failures, 'cursor': cursor})
    after = statistics(storage.api.project_id)
    preserved = {k: before[k] == after[k] for k in
        ('snapshots', 'metadata_digest', 'menu_items_digest', 'menu_sections_digest')}
    report = {'before': before, 'after': after, 'verified_this_run': processed,
        'historical_hash_differences': historical, 'failures': failures, 'preserved': preserved,
        'elapsed_seconds': round(time.monotonic() - started, 1)}
    save(output / 'report.json', report)
    if failures or not all(preserved.values()):
        raise ValueError('archival requires review; inline originals and prior audit fields were not discarded on failure')
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project-id', required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--workers', type=int, default=8)
    parser.add_argument('--batch-size', type=int, default=20)
    parser.add_argument('--limit', type=int)
    parser.add_argument('--baseline', type=Path, help='preserve the original before.json when resuming an interrupted archival')
    parser.add_argument('--after', type=uuid.UUID, help='start after this snapshot UUID for bounded range workers')
    args = parser.parse_args()
    if not 1 <= args.workers <= 32 or not 1 <= args.batch_size <= 100 or args.limit is not None and args.limit < 1:
        parser.error('workers 1..32, batch size 1..100 and a positive limit are required')
    storage = SnapshotStorage.from_cli(args.project_id, args.output / 'originals')
    report = migrate(storage, args.output, args.workers, args.batch_size, args.limit, args.baseline,
        str(args.after) if args.after else None)
    print(json.dumps({'complete': report['after']['inline_count'] == 0, 'verified': report['verified_this_run'],
        'remaining': report['after']['inline_count'], 'preserved': report['preserved']}))


if __name__ == '__main__':
    main()
