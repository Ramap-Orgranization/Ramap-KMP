#!/usr/bin/env python3
"""Persistent menu-refresh queue. DB access is supplied by CLI or MCP, never keys."""
import argparse
import copy
import fcntl
import json
import re
import subprocess
import sys
import time
import uuid
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path

from refresh_plan import digest, prepare_capture, serialized, state_sql
from collection_skips import DEFAULT_REGISTRY, load_registry, skips_for_shop
from manual_menu_protections import DEFAULT_REGISTRY as PROTECTION_REGISTRY
from refresh_verify import expected_visible, read_state, result_rows, verification_sql, verify_plan


SKILL = Path(__file__).resolve().parents[1]
REPO = SKILL.parent.parent
TERMINAL = {'verified', 'blocked', 'failed', 'verification_failed'}


def now():
    return datetime.now(timezone.utc).isoformat()


def load(path):
    return json.loads(Path(path).read_text())


def save(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.tmp')
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')
    temporary.replace(path)


def versions():
    paths = [SKILL / 'SKILL.md', SKILL / 'references/workflow.md', REPO / 'docs/menu-registration-policy.md']
    paths += sorted(p for p in (SKILL / 'scripts').iterdir() if p.suffix in ('.py', '.js'))
    if DEFAULT_REGISTRY.exists():
        paths.append(DEFAULT_REGISTRY)
    if PROTECTION_REGISTRY.exists():
        paths.append(PROTECTION_REGISTRY)
    return {str(p.relative_to(REPO)): digest(p.read_text()) for p in paths}


def check_version(manifest):
    if manifest['versions'] != versions():
        raise ValueError('skill or registration policy changed; use a new run with the reviewed version')


@contextmanager
def locked(run_dir):
    run_dir = Path(run_dir).resolve()
    with (run_dir / '.manifest.lock').open('a') as handle:
        fcntl.flock(handle, fcntl.LOCK_EX)
        manifest = load(run_dir / 'manifest.json')
        yield run_dir, manifest
        save(run_dir / 'manifest.json', manifest)


def event(run_dir, manifest, kind, **fields):
    record = {'at': now(), 'kind': kind, **fields}
    with (run_dir / 'events.jsonl').open('a') as handle:
        handle.write(serialized(record) + '\n')
    manifest['updated_at'] = record['at']


def shop_path(run_dir, shop):
    path = (run_dir / shop['directory']).resolve()
    if run_dir not in path.parents:
        raise ValueError('shop artifact path escaped the run directory')
    return path


def initialize(run_dir, selection, project_id, mode='live', confirmations=(), actor='/root', rehearsal='all', max_age_hours=24, description_approvals=(), collection_skips=None):
    run_dir = Path(run_dir).resolve()
    if (run_dir / 'manifest.json').exists():
        raise ValueError('run already exists; resume it instead of resetting its manifest')
    selection = result_rows(selection)
    if not selection or len({s['id'] for s in selection}) != len(selection):
        raise ValueError('selection must contain distinct existing shops')
    collection_skips = load_registry() if collection_skips is None else list(collection_skips)
    excluded_shops = [s['id'] for s in selection if any(e['kind'] == 'shop'
        for e in skips_for_shop(s['id'], entries=collection_skips))]
    selection = [s for s in selection if s['id'] not in excluded_shops]
    if not selection:
        raise ValueError('all selected shops are excluded from source collection by user policy')
    if not project_id or not project_id.isalnum():
        raise ValueError('expected a Supabase project reference')
    if max_age_hours <= 0:
        raise ValueError('capture freshness window must be positive')
    shops = {}
    for index, selected in enumerate(selection, 1):
        shop_id = str(uuid.UUID(selected['id']))
        for field in ('name', 'address', 'naver_place_url'):
            if not selected.get(field):
                raise ValueError('selection identity is incomplete: ' + field)
        shops[shop_id] = {'id': shop_id, 'name': selected['name'], 'selection': selected,
            'directory': f'shop-{index:02d}-{shop_id[:8]}', 'status': 'waiting_capture', 'attempt': 0,
            'review': None, 'queued_at': now()}
    if any(c['shop_id'] not in shops for c in confirmations):
        raise ValueError('scope confirmation is outside the fixed selection')
    if any(a.get('shop_id') not in shops or a.get('authority') != 'human_user'
        or not a.get('reason') for a in description_approvals):
        raise ValueError('description approval needs separately recorded user authority and selected shop')
    manifest = {'schema_version': 1, 'run_id': str(uuid.uuid4()), 'project_id': project_id,
        'mode': mode, 'writer': actor, 'started_at': now(), 'updated_at': now(), 'versions': versions(),
        'rehearsal': rehearsal, 'max_age_hours': max_age_hours, 'confirmations': list(confirmations),
        'description_approvals': list(description_approvals),
        'collection_skip_rules': collection_skips, 'excluded_collection_shop_ids': excluded_shops,
        'shops': shops, 'inflight': None, 'db_calls': 0, 'audit_started': False, 'audit_finished': False}
    run_dir.mkdir(parents=True, exist_ok=True)
    save(run_dir / 'selection.json', selection)
    save(run_dir / 'manifest.json', manifest)
    save(run_dir / 'context.json', {'skill': str(SKILL / 'SKILL.md'), 'policy': str(REPO / 'docs/menu-registration-policy.md'),
        'versions': manifest['versions'], 'scope_confirmations': list(confirmations),
        'description_approvals': list(description_approvals), 'collection_skips': collection_skips, 'writer': actor})
    for shop in shops.values():
        directory = shop_path(run_dir, shop)
        save(directory / 'assignment.json', {'shop': shop['selection'], 'directory': str(directory),
            'context': str(run_dir / 'context.json'), 'capture_contract': str(SKILL / 'references/workflow.md'),
            'collection_skips': skips_for_shop(shop['id'], entries=collection_skips),
            'responsibility': 'read-only source collection; submit result.json with full raw menu DOM'})
    event(run_dir, manifest, 'initialized', shops=len(shops), mode=mode)
    write_reports(run_dir, manifest)
    save(run_dir / 'manifest.json', manifest)
    return summary(run_dir, manifest)


def submit(run_dir, capture_path, collector):
    capture_path = Path(capture_path).resolve()
    capture = load(capture_path)
    raw_path = Path(capture['raw_html_path'])
    raw_path = (raw_path if raw_path.is_absolute() else capture_path.parent / raw_path).resolve()
    if capture_path.parent not in raw_path.parents:
        raise ValueError('raw source must remain inside its capture directory')
    raw_dom = raw_path.read_text()
    with locked(run_dir) as (run_dir, manifest):
        check_version(manifest)
        shop = manifest['shops'].get(capture['shop_id'])
        if not shop:
            raise ValueError('capture shop is outside this run')
        if shop['status'] != 'waiting_capture':
            raise ValueError('capture already submitted; use a new run for changed source evidence')
        check_freshness(capture, manifest)
        # Validate the submission before any DB call. Full diff validation follows the live read.
        from source_dom import reconcile_source
        reconcile_source(capture['menu_items'], raw_dom, capture['source_url'])
        if capture.get('identity_verified') is not True or capture.get('quality', {}).get('passed') is not True:
            raise ValueError('source identity or collection review has not passed')
        if len(capture['scope']) != len(capture['menu_items']):
            raise ValueError('scope decisions must cover every source item')
        for item, scope in zip(capture['menu_items'], capture['scope']):
            if item['name'] != scope['name'] or item.get('source_id') != scope.get('source_id'):
                raise ValueError('scope must match source item identity and order')
        directory = shop_path(run_dir, shop)
        (directory / 'raw-menu.html').write_text(raw_dom)
        capture['raw_html_path'] = str(directory / 'raw-menu.html')
        save(directory / 'result.json', capture)
        shop.update(status='ready', collector=collector, submitted_at=now(),
            capture_hash=digest(serialized(capture)), raw_hash=digest(raw_dom))
        event(run_dir, manifest, 'capture_submitted', shop_id=shop['id'], source_items=len(capture['menu_items']))
        return {'shop_id': shop['id'], 'status': shop['status']}


def check_freshness(capture, manifest):
    timestamp = datetime.fromisoformat(capture['captured_at'].replace('Z', '+00:00'))
    if timestamp.tzinfo is None:
        raise ValueError('capture timestamp requires a timezone')
    age = (datetime.now(timezone.utc) - timestamp).total_seconds() / 3600
    if manifest['mode'] == 'live' and (age < -0.1 or age > manifest['max_age_hours']):
        raise ValueError('capture is stale or in the future; collect current source evidence')


def read_capture(run_dir, shop, manifest):
    directory = shop_path(run_dir, shop)
    capture, raw_dom = load(directory / 'result.json'), (directory / 'raw-menu.html').read_text()
    if digest(serialized(capture)) != shop['capture_hash'] or digest(raw_dom) != shop['raw_hash']:
        raise ValueError('submitted capture changed; create a new run instead of reusing SQL')
    check_freshness(capture, manifest)
    return capture, raw_dom


def issue(run_dir, manifest, kind, shops, sql):
    action_id = str(uuid.uuid4())
    path = run_dir / 'actions' / (action_id + '.sql')
    path.parent.mkdir(exist_ok=True)
    path.write_text(sql)
    action = {'id': action_id, 'kind': kind, 'project_id': manifest['project_id'], 'mode': manifest['mode'],
        'shop_ids': [s['id'] for s in shops], 'sql_path': str(path), 'sql_hash': digest(sql), 'issued_at': now()}
    manifest['inflight'] = action
    event(run_dir, manifest, 'query_issued', action_id=action_id, stage=kind, shop_ids=action['shop_ids'])
    return action


def audit_sql(manifest, final=False):
    from refresh_plan import literal
    run_id = literal(manifest['run_id']) + '::uuid'
    if not final:
        return (f"INSERT INTO public.shop_menu_validation_runs(id,status,total_snapshots,details) VALUES({run_id},"
            f"'running',0,{literal(serialized({'runner': 'menu-refresh-v2', 'versions': manifest['versions']}))}::jsonb) "
            f"ON CONFLICT (id) DO NOTHING; SELECT id,status FROM public.shop_menu_validation_runs WHERE id={run_id};")
    counts = counts_for(manifest)
    status = 'completed' if counts['pending_shops'] == 0 and counts['failures'] == 0 else 'partial'
    return (f"UPDATE public.shop_menu_validation_runs SET status='{status}',completed_at=now(),"
        f"total_snapshots={counts['committed_shops']},checked_count={counts['verified_shops']},"
        f"applied_count={counts['changed_shops']},unchanged_count={counts['unchanged_shops']},"
        f"pending_count={counts['pending_shops']},failure_count={counts['failures']},"
        f"details={literal(serialized(counts))}::jsonb WHERE id={run_id} "
        f"RETURNING id,status,total_snapshots,checked_count,applied_count,unchanged_count,pending_count,failure_count;")


def next_action(run_dir):
    with locked(run_dir) as (run_dir, manifest):
        check_version(manifest)
        if manifest['inflight']:
            return {'kind': 'await_result', 'action_id': manifest['inflight']['id'],
                'stage': manifest['inflight']['kind'], 'resume': 'recover after the prior transport has stopped'}
        shops = list(manifest['shops'].values())
        ready = [s for s in shops if s['status'] == 'ready']
        if any(s['status'] not in TERMINAL | {'waiting_capture'} for s in shops) and not manifest['audit_started']:
            return issue(run_dir, manifest, 'audit_start', [], audit_sql(manifest))
        for status, kind in (('recovering', 'recover'), ('verifying', 'verify'),
            ('rollback_ready', 'rollback'), ('commit_ready', 'commit')):
            for shop in shops:
                if shop['status'] != status:
                    continue
                directory = shop_path(run_dir, shop)
                plan = load(directory / 'prepared/plan.json')
                if digest(serialized(plan)) != shop['plan_hash']:
                    shop.update(status='verification_failed' if kind in ('verify', 'recover') else 'blocked',
                        error='prepared plan changed after validation')
                    event(run_dir, manifest, 'shop_blocked', shop_id=shop['id'], reason=shop['error'])
                    write_reports(run_dir, manifest)
                    continue
                if kind in ('verify', 'recover'):
                    return issue(run_dir, manifest, kind, [shop], verification_sql([plan]))
                try:
                    read_capture(run_dir, shop, manifest)
                    sql = (directory / 'prepared/apply.sql').read_text()
                    if digest(sql) != shop['sql_hash'] or digest(serialized(plan)) != shop['plan_hash']:
                        raise ValueError('prepared SQL or plan changed; rebuild only the failed shop')
                except ValueError as error:
                    shop.update(status='blocked', error=str(error))
                    event(run_dir, manifest, 'shop_blocked', shop_id=shop['id'], reason=str(error))
                    write_reports(run_dir, manifest)
                    continue
                if kind == 'rollback':
                    # Run all guards unchanged; return proof after rollback for transports
                    # that only return the final statement's rows.
                    from refresh_plan import literal
                    sql = sql.split('\nCOMMIT;\n', 1)[0] + '\nROLLBACK;\n'
                    sql += (f"SELECT {literal(plan['snapshot_id'])}::uuid AS id,"
                        f"{literal(plan['shop_id'])}::uuid AS shop_id,"
                        f"{literal(plan['source_hash'])} AS source_hash,"
                        f"{literal(plan['menu_fingerprint'])} AS menu_fingerprint;")
                if kind == 'commit':
                    shop['status'] = 'commit_started'
                return issue(run_dir, manifest, kind, [shop], sql)
        if ready:
            return issue(run_dir, manifest, 'prewrite', ready, state_sql([s['selection'] for s in ready]))
        all_terminal = all(s['status'] in TERMINAL for s in shops)
        reviews_done = all(s['review'] for s in shops if s['status'] == 'verified')
        if all_terminal and reviews_done and manifest['audit_started'] and not manifest['audit_finished']:
            return issue(run_dir, manifest, 'audit_finish', [], audit_sql(manifest, True))
        return {'kind': 'idle', **summary(run_dir, manifest)}


def accept(run_dir, action_id, response=None, failed=False):
    with locked(run_dir) as (run_dir, manifest):
        check_version(manifest)
        action = manifest['inflight']
        if not action or action['id'] != action_id:
            raise ValueError('stale or duplicate action response')
        shops = [manifest['shops'][shop_id] for shop_id in action['shop_ids']]
        manifest['db_calls'] += 1
        kind = action['kind']
        save(run_dir / 'actions' / (action_id + '.json'), {'response': response, 'failed': failed})
        duration = (datetime.now(timezone.utc) - datetime.fromisoformat(action['issued_at'])).total_seconds()
        event(run_dir, manifest, 'query_finished', action_id=action_id, stage=kind, seconds=duration, failed=failed)
        manifest['inflight'] = None
        try:
            if failed:
                raise ValueError('transport did not confirm query success')
            rows = result_rows(response)
            if kind in ('audit_start', 'audit_finish'):
                if len(rows) != 1 or rows[0]['id'] != manifest['run_id']:
                    raise ValueError('validation run audit mismatch')
                if kind == 'audit_start' and rows[0].get('status') != 'running':
                    raise ValueError('validation run is not running')
                if kind == 'audit_finish':
                    counts = counts_for(manifest)
                    expected = {'status': 'completed' if not counts['pending_shops'] and not counts['failures'] else 'partial',
                        'total_snapshots': counts['committed_shops'], 'checked_count': counts['verified_shops'],
                        'applied_count': counts['changed_shops'], 'unchanged_count': counts['unchanged_shops'],
                        'pending_count': counts['pending_shops'], 'failure_count': counts['failures']}
                    if any(rows[0].get(k) != value for k, value in expected.items()):
                        raise ValueError('validation run final counters mismatch')
                manifest['audit_started' if kind == 'audit_start' else 'audit_finished'] = True
                manifest.pop('audit_error', None)
            elif kind == 'prewrite':
                state = read_state(response)
                for shop in shops:
                    directory = shop_path(run_dir, shop)
                    try:
                        capture, raw_dom = read_capture(run_dir, shop, manifest)
                        live_shop = next(s for s in state['shops'] if s['id'] == shop['id'])
                        if any(live_shop.get(k) != shop['selection'][k] for k in ('name', 'address', 'naver_place_url')):
                            raise ValueError('selected branch identity changed')
                        prepare_start = time.monotonic()
                        plan, sql = prepare_capture(capture, state, raw_dom,
                            run_id=manifest['run_id'], confirmations=manifest['confirmations'],
                            description_approvals=[a for a in manifest.get('description_approvals', [])
                                if a['shop_id'] == capture['shop_id']])
                        plan['expected_visible'] = expected_visible(plan, state['visible']) if sql else []
                        one_shop_state = {key: [r for r in value if r.get('shop_id', r.get('id')) == shop['id']]
                            for key, value in state.items()}
                        save(directory / 'prewrite-state.json', {'rows': [{'state': one_shop_state}]})
                        save(directory / 'prepared/plan.json', plan)
                        shop['summary'] = plan_summary(plan)
                        if not sql:
                            shop['status'] = 'blocked'
                        else:
                            (directory / 'prepared/apply.sql').write_text(sql)
                            shop.update(status='rollback_ready' if plan['deletions'] or plan['override_additions']
                                or manifest['rehearsal'] == 'all' else 'commit_ready',
                                sql_hash=digest(sql), plan_hash=digest(serialized(plan)))
                        event(run_dir, manifest, 'prepared', shop_id=shop['id'], status=shop['status'],
                            seconds=time.monotonic() - prepare_start,
                            queue_seconds=(datetime.now(timezone.utc) - datetime.fromisoformat(shop['submitted_at'])).total_seconds())
                    except (ValueError, KeyError, StopIteration) as error:
                        shop.update(status='blocked', error=str(error))
                        event(run_dir, manifest, 'shop_blocked', shop_id=shop['id'], reason=str(error))
            elif kind in ('rollback', 'commit'):
                shop = shops[0]
                plan = load(shop_path(run_dir, shop) / 'prepared/plan.json')
                if digest(serialized(plan)) != shop['plan_hash']:
                    raise ValueError('prepared plan changed before the response was recorded')
                if len(rows) != 1 or any(rows[0].get(k) != plan[k] for k in ('shop_id', 'source_hash', 'menu_fingerprint')) or rows[0]['id'] != plan['snapshot_id']:
                    raise ValueError('transaction result does not match the prepared snapshot')
                shop['status'] = 'commit_ready' if kind == 'rollback' else 'verifying'
                if kind == 'commit':
                    shop['committed'] = True
            elif kind in ('verify', 'recover'):
                if len(rows) != 1 or 'verification' not in rows[0]:
                    raise ValueError('expected post-write verification object')
                verification = rows[0]['verification']
                shop = shops[0]
                directory = shop_path(run_dir, shop)
                plan = load(directory / 'prepared/plan.json')
                if digest(serialized(plan)) != shop['plan_hash']:
                    raise ValueError('prepared plan changed before verification')
                save(directory / 'postwrite-state.json', verification)
                if kind == 'recover' and not any(s['id'] == plan['snapshot_id'] for s in verification['snapshots']):
                    # Absence is evidence to diagnose, not permission to replay a write.
                    shop.update(status='failed', error='commit not found; diagnose, then explicitly retry this shop once')
                else:
                    shop['committed'] = True
                    result = verify_plan(plan, verification['state'], verification['snapshots'])
                    save(directory / 'verification.json', result)
                    shop.update(status='verified' if result['passed'] else 'verification_failed', verification=result)
        except (ValueError, KeyError, TypeError) as error:
            if kind == 'commit':
                for shop in shops:
                    shop.update(status='recovering', error=str(error))
            elif kind in ('audit_start', 'audit_finish'):
                manifest['inflight'] = action
                manifest['audit_error'] = str(error)
            else:
                for shop in shops:
                    shop.update(status='verification_failed' if kind in ('verify', 'recover') else 'failed', error=str(error))
        write_reports(run_dir, manifest)
        return summary(run_dir, manifest)


def recover(run_dir):
    with locked(run_dir) as (run_dir, manifest):
        check_version(manifest)
        action = manifest['inflight']
        if action:
            for shop_id in action['shop_ids']:
                shop = manifest['shops'][shop_id]
                if action['kind'] == 'commit':
                    shop['status'] = 'recovering'
                elif action['kind'] in ('verify', 'recover'):
                    shop['status'] = 'verifying' if action['kind'] == 'verify' else 'recovering'
                elif action['kind'] == 'rollback':
                    shop.update(status='failed', error='rollback result missing; diagnose before a retry')
            manifest['inflight'] = None
            event(run_dir, manifest, 'interrupted_action_recovered', stage=action['kind'], action_id=action['id'])
        return summary(run_dir, manifest)


def retry(run_dir, shop_id, diagnosis):
    with locked(run_dir) as (run_dir, manifest):
        check_version(manifest)
        if manifest['inflight']:
            raise ValueError('finish or recover the outstanding action before retrying')
        shop = manifest['shops'][shop_id]
        if not diagnosis or shop['status'] not in ('failed', 'blocked') or shop['attempt'] >= 1:
            raise ValueError('only one diagnosed retry of a failed/blocked shop is permitted')
        has_capture = (shop_path(run_dir, shop) / 'result.json').exists()
        shop.update(status='ready' if has_capture else 'waiting_capture',
            attempt=shop['attempt'] + 1, diagnosis=diagnosis, review=None)
        shop.pop('error', None)
        event(run_dir, manifest, 'retry_requested', shop_id=shop_id, diagnosis=diagnosis)


def fail_collection(run_dir, shop_id, reason):
    with locked(run_dir) as (run_dir, manifest):
        check_version(manifest)
        shop = manifest['shops'][shop_id]
        if not reason or shop['status'] not in ('waiting_capture', 'ready'):
            raise ValueError('only an unprocessed collection can be marked failed with evidence')
        if manifest['inflight'] and shop_id in manifest['inflight']['shop_ids']:
            raise ValueError('collection has an outstanding action; finish or recover it first')
        shop.update(status='failed', error=reason)
        event(run_dir, manifest, 'collection_failed', shop_id=shop_id, reason=reason)
        write_reports(run_dir, manifest)
        return summary(run_dir, manifest)


def record_review(run_dir, report):
    with locked(run_dir) as (run_dir, manifest):
        reviewer = report['reviewer']
        if not reviewer or reviewer == manifest['writer']:
            raise ValueError('review must come from an independent reviewer')
        entries = report['shops']
        if not entries or len({e['shop_id'] for e in entries}) != len(entries):
            raise ValueError('independent review needs distinct shop results')
        for entry in entries:
            shop = manifest['shops'][entry['shop_id']]
            plan = load(shop_path(run_dir, shop) / 'prepared/plan.json')
            if shop['status'] != 'verified' or reviewer == shop['collector']:
                raise ValueError('reviewer cannot collect or write the shop being reviewed')
            if entry['snapshot_id'] != plan['snapshot_id'] or entry.get('plan_hash') != shop['plan_hash']:
                raise ValueError('review is not bound to the current snapshot and plan')
            if shop['plan_hash'] != digest(serialized(plan)):
                raise ValueError('review plan changed after verification')
            if entry['result'] not in ('pass', 'fail') or (entry['result'] == 'pass' and entry.get('findings')):
                raise ValueError('invalid independent review outcome')
        for entry in entries:
            manifest['shops'][entry['shop_id']]['review'] = {'reviewer': reviewer, **entry}
        save(run_dir / 'reviews' / (str(uuid.uuid4()) + '.json'), report)
        event(run_dir, manifest, 'independent_review', reviewer=reviewer, shops=[e['shop_id'] for e in entries])
        write_reports(run_dir, manifest)
        return summary(run_dir, manifest)


def plan_summary(plan):
    fields = [u['fields'] for u in plan['updates']]
    return {'updated_rows': len(plan['updates']), 'added_rows': len(plan['additions']),
        'deleted_rows': len(plan['deletions']), 'override_updates': len(plan.get('override_updates', [])),
        'scope_confirmations': len(plan.get('override_additions', [])),
        'price_changes': sum('price_krw' in f for f in fields), 'order_changes': sum('display_order' in f for f in fields),
        'image_changes': sum('image_url' in f for f in fields), 'source_items': len(plan.get('source_items', [])),
        'source_images': sum(bool(r.get('image_url')) for r in plan.get('source_items', [])),
        'pending': plan['pending'] + plan['deletion_handoff'], 'db_only_preserved': plan['db_only_preserved'],
        'snapshot_id': plan['snapshot_id']}


def counts_for(manifest):
    shops = list(manifest['shops'].values())
    verified = [s for s in shops if s['status'] == 'verified']
    def changed(s):
        return any(s['summary'].get(k) for k in ('updated_rows', 'added_rows', 'deleted_rows', 'override_updates', 'scope_confirmations'))
    return {'selected_shops': len(shops), 'verified_shops': len(verified),
        'committed_shops': sum(bool(s.get('committed')) for s in shops),
        'changed_shops': sum(changed(s) for s in verified),
        'unchanged_shops': sum(not changed(s) and not s['summary']['pending'] for s in verified),
        'pending_shops': sum(s['status'] == 'blocked' or bool(s.get('summary', {}).get('pending')) for s in shops),
        'failures': sum(s['status'] in ('failed', 'verification_failed') or bool(s['review'] and s['review']['result'] == 'fail') for s in shops),
        'reviewed_shops': sum(bool(s['review'] and s['review']['result'] == 'pass') for s in verified),
        'updated_rows': sum(s['summary']['updated_rows'] for s in verified),
        'added_rows': sum(s['summary']['added_rows'] for s in verified),
        'deleted_rows': sum(s['summary']['deleted_rows'] for s in verified),
        'source_items': sum(s['summary']['source_items'] for s in verified),
        'source_images': sum(s['summary']['source_images'] for s in verified),
        'visible_items': sum(s['verification']['visible_items'] for s in verified),
        'visible_images': sum(s['verification']['visible_images'] for s in verified),
        'visible_descriptions': sum(s['verification']['visible_descriptions'] for s in verified),
        'db_calls': manifest['db_calls']}


def summary(run_dir, manifest):
    counts = counts_for(manifest)
    shops = list(manifest['shops'].values())
    status = 'running'
    if all(s['status'] in TERMINAL for s in shops):
        if counts['failures'] or counts['pending_shops']:
            status = 'partial'
        elif counts['reviewed_shops'] != counts['verified_shops']:
            status = 'awaiting_independent_review'
        else:
            status = 'completed'
    if manifest.get('inflight') or not manifest['audit_finished'] and manifest['audit_started'] and status == 'completed':
        status = 'running' if manifest.get('inflight') else 'awaiting_audit_finish'
    return {'run_id': manifest['run_id'], 'mode': manifest['mode'], 'status': status, **counts,
        'project_id': manifest['project_id'], 'waiting_capture': [s['id'] for s in shops if s['status'] == 'waiting_capture'],
        'report': str(run_dir / 'report.md'), 'manifest': str(run_dir / 'manifest.json')}


def shop_report_links(shop):
    name = shop['name'].replace('\\', '\\\\').replace('|', '\\|').replace('\n', ' ')
    name = name.replace('[', '\\[').replace(']', '\\]')
    place_url = shop['selection']['naver_place_url']
    place_id = re.search(r'/(?:place|restaurant)/(\d+)', place_url).group(1)
    menu_url = f'https://m.place.naver.com/restaurant/{place_id}/menu/list'
    return f'[{name}]({place_url})', f'[메뉴]({menu_url})'


def write_reports(run_dir, manifest):
    result = summary(run_dir, manifest)
    save(run_dir / 'summary.json', result)
    handoff = []
    lines = [f"메뉴 최신화 실행: {manifest['mode']} / {result['status']}", '',
        f"대상 {result['selected_shops']}개 · 반영 후 검증 {result['verified_shops']}개 · 변경 {result['changed_shops']}개 · "
        f"일치 {result['unchanged_shops']}개 · 보류 {result['pending_shops']}개 · 실패 {result['failures']}개", '',
        '| 매장 | 메뉴 원본 | 상태 | 수정 | 추가 | 삭제 | 독립 검토 |', '| --- | --- | --- | ---: | ---: | ---: | --- |']
    for shop in manifest['shops'].values():
        stats = shop.get('summary', {})
        review = shop['review']['result'] if shop['review'] else '대기'
        shop_link, menu_link = shop_report_links(shop)
        lines.append(f"| {shop_link} | {menu_link} | {shop['status']} | {stats.get('updated_rows', 0)} | "
            f"{stats.get('added_rows', 0)} | {stats.get('deleted_rows', 0)} | {review} |")
        if shop['status'] == 'verified' and not shop['review']:
            directory = shop_path(run_dir, shop)
            handoff.append({'shop_id': shop['id'], 'snapshot_id': stats['snapshot_id'], 'plan_hash': shop['plan_hash'],
                'collector': shop['collector'], 'writer': manifest['writer'], 'capture': str(directory / 'result.json'),
                'plan': str(directory / 'prepared/plan.json'), 'postwrite_state': str(directory / 'postwrite-state.json'),
                'verification': str(directory / 'verification.json')})
    issues = [(s, s.get('error'), s.get('summary', {}).get('pending', []))
        for s in manifest['shops'].values() if s.get('error') or s.get('summary', {}).get('pending')]
    if issues:
        lines += ['', '확인할 항목:']
        for shop, error, pending in issues:
            shop_link, menu_link = shop_report_links(shop)
            name = f'{shop_link} · {menu_link}'
            if error:
                lines.append(f'- {name}: {error}')
            for item in pending:
                lines.append(f"- {name} / {item.get('name', '전체')}: {item['reason']}")
    if manifest.get('audit_error'):
        lines += ['', '실행 감사 확인 필요: ' + manifest['audit_error']]
    if manifest['mode'] == 'replay':
        lines += ['', '저장 자료를 이용한 모의 실행이며 실제 DB 반영이나 독립 검토 통과를 의미하지 않는다.']
    (run_dir / 'report.md').write_text('\n'.join(lines) + '\n')
    save(run_dir / 'review-handoff.json', {'mode': manifest['mode'], 'project_id': manifest['project_id'], 'shops': handoff})
    timings = {}
    for line in (run_dir / 'events.jsonl').read_text().splitlines():
        record = json.loads(line)
        if record['kind'] not in ('query_finished', 'prepared'):
            continue
        stage = record.get('stage', 'prepare')
        timing = timings.setdefault(stage, {'count': 0, 'seconds': 0})
        timing['count'] += 1
        timing['seconds'] += record.get('seconds', 0)
    save(run_dir / 'timings.json', {'stages': timings,
        'note': 'query durations include transport/response wait; browser and user wait are not measured here'})


def drain(run_dir, query, max_actions=40, max_seconds=45):
    start = time.monotonic()
    for _ in range(max_actions):
        if time.monotonic() - start >= max_seconds:
            break
        action = next_action(run_dir)
        if action['kind'] in ('idle', 'await_result'):
            return action
        sql = Path(action['sql_path']).read_text()
        if digest(sql) != action['sql_hash']:
            raise ValueError('issued query changed before execution')
        try:
            response = query(action, sql)
        except Exception:
            accept(run_dir, action['id'], failed=True)
        else:
            accept(run_dir, action['id'], response)
    with locked(run_dir) as (run_dir, manifest):
        return {'kind': 'yielded', **summary(run_dir, manifest)}


def cli_query(project_id):
    linked = REPO / 'supabase/.temp/project-ref'
    if not linked.exists() or linked.read_text().strip() != project_id:
        raise ValueError('linked CLI project differs from this run; use the authenticated MCP transport')
    help_result = subprocess.run(['supabase', 'db', 'query', '--help'], capture_output=True, text=True, check=True)
    if '--linked' not in help_result.stdout or '--file' not in help_result.stdout:
        raise ValueError('installed CLI does not support the required query flags')
    def query(action, sql):
        if action['mode'] != 'live':
            raise ValueError('replay run cannot use a live database transport')
        result = subprocess.run(['supabase', 'db', 'query', '--linked', '--file', action['sql_path'], '--output', 'json'],
            cwd=REPO, capture_output=True, text=True, timeout=45)
        if result.returncode:
            # Credentials or headers printed by a third-party CLI are never copied to artifacts.
            raise ValueError('Supabase CLI query failed; diagnose authentication or SQL separately')
        return json.loads(result.stdout)
    return query


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    init = commands.add_parser('init')
    init.add_argument('--run', type=Path, required=True)
    init.add_argument('--selection', type=Path, required=True)
    init.add_argument('--project-id', required=True)
    init.add_argument('--confirmations', type=Path)
    init.add_argument('--description-approvals', type=Path)
    init.add_argument('--collection-skips', type=Path)
    init.add_argument('--actor', default='/root')
    init.add_argument('--rehearsal', choices=('all', 'deletions'), default='all')
    init.add_argument('--max-age-hours', type=float, default=24)
    for name in ('next', 'recover', 'status', 'run-cli', 'submit', 'accept', 'retry', 'review', 'fail'):
        sub = commands.add_parser(name)
        sub.add_argument('--run', type=Path, required=True)
        if name == 'submit':
            sub.add_argument('--capture', type=Path, required=True)
            sub.add_argument('--collector', required=True)
        elif name == 'accept':
            sub.add_argument('--action-id', required=True)
            sub.add_argument('--failed', action='store_true')
        elif name == 'retry':
            sub.add_argument('--shop-id', required=True)
            sub.add_argument('--diagnosis', required=True)
        elif name == 'review':
            sub.add_argument('--report', type=Path, required=True)
        elif name == 'fail':
            sub.add_argument('--shop-id', required=True)
            sub.add_argument('--reason', required=True)
        elif name == 'run-cli':
            sub.add_argument('--resume', action='store_true')
            sub.add_argument('--max-actions', type=int, default=40)
    args = parser.parse_args()
    if args.command == 'init':
        result = initialize(args.run, load(args.selection), args.project_id,
            confirmations=load(args.confirmations) if args.confirmations else (), actor=args.actor,
            rehearsal=args.rehearsal, max_age_hours=args.max_age_hours,
            description_approvals=load(args.description_approvals) if args.description_approvals else (),
            collection_skips=load_registry(args.collection_skips) if args.collection_skips else None)
    elif args.command == 'submit':
        result = submit(args.run, args.capture, args.collector)
    elif args.command == 'next':
        result = next_action(args.run)
    elif args.command == 'accept':
        result = accept(args.run, args.action_id, None if args.failed else json.load(sys.stdin), args.failed)
    elif args.command == 'recover':
        result = recover(args.run)
    elif args.command == 'retry':
        retry(args.run, args.shop_id, args.diagnosis)
        result = {'shop_id': args.shop_id, 'status': 'ready'}
    elif args.command == 'review':
        result = record_review(args.run, load(args.report))
    elif args.command == 'fail':
        result = fail_collection(args.run, args.shop_id, args.reason)
    elif args.command == 'run-cli':
        with locked(args.run) as (_, manifest):
            query = cli_query(manifest['project_id'])
        with (args.run / '.driver.lock').open('a') as handle:
            fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
            if args.resume:
                recover(args.run)
            result = drain(args.run, query, args.max_actions)
    else:
        with locked(args.run) as (run_dir, manifest):
            write_reports(run_dir, manifest)
            result = summary(run_dir, manifest)
    print(serialized(result))


if __name__ == '__main__':
    main()
