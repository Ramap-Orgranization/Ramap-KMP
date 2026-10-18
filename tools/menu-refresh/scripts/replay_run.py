#!/usr/bin/env python3
"""Exercise the actual queue from saved captures, using an explicitly fake DB."""
import argparse
import copy
import tempfile
import time
from pathlib import Path

from refresh_run import counts_for, drain, initialize, load, save, submit
from refresh_verify import read_state


class ReplayDatabase:
    def __init__(self, run_dir, state):
        self.run_dir, self.state = Path(run_dir), copy.deepcopy(state)
        self.snapshots = {}
        self.actions = []

    def plan(self, shop_id):
        shop = load(self.run_dir / 'manifest.json')['shops'][shop_id]
        return load(self.run_dir / shop['directory'] / 'prepared/plan.json')

    def __call__(self, action, sql):
        if action['mode'] != 'replay':
            raise ValueError('the fake database only accepts replay runs')
        self.actions.append({'kind': action['kind'], 'shop_ids': action['shop_ids']})
        kind = action['kind']
        manifest = load(self.run_dir / 'manifest.json')
        if kind in ('audit_start', 'audit_finish'):
            counts = counts_for(manifest)
            row = {'id': manifest['run_id'], 'status': 'running'}
            if kind == 'audit_finish':
                row.update(status='completed' if not counts['pending_shops'] and not counts['failures'] else 'partial',
                    total_snapshots=counts['committed_shops'], checked_count=counts['verified_shops'],
                    applied_count=counts['changed_shops'], unchanged_count=counts['unchanged_shops'],
                    pending_count=counts['pending_shops'], failure_count=counts['failures'])
            return {'rows': [row]}
        if kind == 'prewrite':
            return {'rows': [{'state': copy.deepcopy(self.state)}]}
        plan = self.plan(action['shop_ids'][0])
        snapshot = {key: plan[key] for key in ('shop_id', 'captured_at', 'source_url', 'parser_version', 'source_hash', 'menu_fingerprint')}
        snapshot.update(id=plan['snapshot_id'], raw_hash=plan['source_hash'], menu_items=plan['source_items'])
        if kind in ('rollback', 'commit'):
            if kind == 'commit':
                for key, expected in (('items', plan['expected']), ('visible', plan['expected_visible']),
                    ('sections', plan['sections']), ('overrides', plan['expected_overrides']), ('rules', plan['rules'])):
                    self.state[key] = [r for r in self.state[key] if r['shop_id'] != plan['shop_id']] + copy.deepcopy(expected)
                self.snapshots[snapshot['id']] = snapshot
            return {'rows': [snapshot]}
        if kind in ('verify', 'recover'):
            return {'rows': [{'verification': {'state': copy.deepcopy(self.state),
                'snapshots': [self.snapshots[plan['snapshot_id']]] if plan['snapshot_id'] in self.snapshots else []}}]}
        raise ValueError('unsupported replay action')


def replay(run_dir, selection, state, captures, rehearsal='deletions', description_approvals=()):
    initialized = initialize(run_dir, selection, 'offline', mode='replay', rehearsal=rehearsal,
        description_approvals=description_approvals)
    database = ReplayDatabase(run_dir, state)
    start = time.monotonic()
    for source in captures:
        capture_path = Path(source['capture']).resolve()
        capture = load(capture_path)
        raw_path = Path(source['raw']).resolve()
        # Explicit archive import: keep originals untouched and fix their relocated
        # raw path only in this temporary replay input directory.
        with tempfile.TemporaryDirectory(prefix='menu-refresh-import-') as directory:
            directory = Path(directory)
            (directory / 'raw-menu.html').write_text(raw_path.read_text())
            capture['raw_html_path'] = str(directory / 'raw-menu.html')
            save(directory / 'result.json', capture)
            submit(run_dir, directory / 'result.json', source.get('collector', '/historical-collector'))
    result = drain(run_dir, database, max_actions=len(captures) * 5 + 5, max_seconds=55)
    result.update(replay_seconds=round(time.monotonic() - start, 4), actual_db_calls=0,
        note='queue and verification exercised with a fake DB; SQL execution/browser latency not measured')
    save(Path(run_dir) / 'replay-result.json', result)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', type=Path, required=True)
    parser.add_argument('--selection', type=Path, required=True)
    parser.add_argument('--state', type=Path, required=True)
    parser.add_argument('--captures', type=Path, required=True,
        help='array of {capture, raw, collector?}; paths must explicitly identify saved evidence')
    args = parser.parse_args()
    import json
    print(json.dumps(replay(args.run, load(args.selection), read_state(load(args.state)), load(args.captures)), ensure_ascii=False))


if __name__ == '__main__':
    main()
