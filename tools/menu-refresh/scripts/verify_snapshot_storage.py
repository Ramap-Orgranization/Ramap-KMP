#!/usr/bin/env python3
"""Reconcile every snapshot with acknowledged round trips, including uncertain commits."""
import argparse
import concurrent.futures
import json
from pathlib import Path
from urllib.parse import urlencode

from migrate_snapshot_storage import statistics, save
from snapshot_storage import SnapshotStorage


def verify(storage, artifacts, baseline_path):
    artifacts = Path(artifacts)
    journal = {}
    for path in artifacts.glob('*/verified-snapshots.jsonl'):
        for line in path.read_text().splitlines():
            row = json.loads(line)
            previous = journal.get(row['id'])
            if previous and previous['raw_sha256'] != row['raw_sha256']:
                raise ValueError('snapshot has conflicting acknowledged originals')
            journal[row['id']] = row
    snapshots, cursor = [], None
    while True:
        params = {'select': 'id,source_hash,raw_object_sha256,raw_storage:menu_snapshot_raw_objects(*)',
            'order': 'id', 'limit': 500}
        if cursor:
            params['id'] = 'gt.' + cursor
        rows = storage.api.json('GET', '/rest/v1/shop_menu_snapshots?' + urlencode(params))
        if not rows:
            break
        snapshots.extend(rows)
        cursor = rows[-1]['id']
    baseline = json.loads(Path(baseline_path).read_text())
    if len(snapshots) != baseline['snapshots']:
        raise ValueError('snapshot population changed; compare the current scope before finalizing')
    missing = []
    for row in snapshots:
        if not row['raw_storage'] or row['raw_storage']['raw_sha256'] != row['raw_object_sha256']:
            raise ValueError('snapshot original is not registered in private Storage')
        acknowledged = journal.get(row['id'])
        if not acknowledged:
            missing.append(row)
        elif acknowledged['raw_sha256'] != row['raw_object_sha256'] or acknowledged['source_hash'] != row['source_hash']:
            raise ValueError('current snapshot differs from the acknowledged original and audit fields')
    def recover(row):
        # A transaction can commit even when its response is lost; verify the actual file again.
        raw = storage.fetch_object(row['raw_storage'])
        return {'id': row['id'], 'raw_sha256': row['raw_object_sha256'], 'original_bytes': len(raw),
            'source_hash': row['source_hash'], 'status': 'uncertain_commit_reverified'}
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        recovered = list(pool.map(recover, missing))
    after = statistics(storage.api.project_id)
    preserved = {key: baseline[key] == after[key] for key in
        ('snapshots', 'metadata_digest', 'menu_items_digest', 'menu_sections_digest')}
    if after['inline_count'] or not all(preserved.values()):
        raise ValueError('inline originals remain or baseline audit/menu fields changed')
    result = {'passed': True, 'snapshots': len(snapshots), 'acknowledged_round_trips': len(snapshots) - len(missing),
        'uncertain_commits_reverified': recovered, 'preserved': preserved,
        'historical_source_hash_differences_preserved': sum(s['source_hash'] != s['raw_object_sha256'] for s in snapshots),
        'before': baseline, 'after': after}
    save(artifacts / 'final-verification.json', result)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project-id', required=True)
    parser.add_argument('--artifacts', type=Path, required=True)
    parser.add_argument('--baseline', type=Path, required=True)
    args = parser.parse_args()
    result = verify(SnapshotStorage.from_cli(args.project_id), args.artifacts, args.baseline)
    print(json.dumps({key: value for key, value in result.items() if key not in ('before', 'after', 'uncertain_commits_reverified')}))


if __name__ == '__main__':
    main()
