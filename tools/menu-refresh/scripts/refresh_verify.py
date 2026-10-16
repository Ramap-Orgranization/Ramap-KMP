"""Reusable post-write reads and checks for both CLI and MCP transports."""
import re
import uuid
from datetime import datetime

from refresh_plan import digest, literal, serialized, state_rows, state_sql
from collection_skips import skips_for_shop
from manual_menu_protections import protection_for_row, protections_for_shop, normalized


VISIBLE_FIELDS = ('id', 'shop_id', 'section_id', 'name', 'price_krw', 'price_text',
    'description', 'image_url', 'is_featured', 'display_order', 'event_id', 'source_url')


def result_rows(value):
    """Read only structured rows; never evaluate text received from a database."""
    import json
    if isinstance(value, str):
        value = json.loads(value)
    if isinstance(value, list):
        return value
    if not isinstance(value, dict) or value.get('isError'):
        raise ValueError('database response is not a successful row result')
    if isinstance(value.get('rows'), list):
        return value['rows']
    if 'content' in value:
        blocks = [c['text'] for c in value['content'] if c.get('type') == 'text']
        if len(blocks) != 1:
            raise ValueError('ambiguous database response content')
        return result_rows(blocks[0])
    result = value.get('result')
    if isinstance(result, (list, dict)):
        return result_rows(result)
    if isinstance(result, str):
        match = re.search(r'<untrusted-data-([^>]+)>\s*(\[[\s\S]*?\])\s*</untrusted-data-\1>', result)
        return result_rows(match[2] if match else result)
    raise ValueError('database response has no rows')


def read_state(value):
    rows = result_rows(value)
    if len(rows) != 1 or not isinstance(rows[0].get('state'), dict):
        raise ValueError('expected one database state object')
    state = rows[0]['state']
    for key in ('shops', 'sections', 'items', 'visible', 'overrides', 'rules', 'menu_timestamps', 'manual_menu_protections'):
        if not isinstance(state.get(key), list):
            raise ValueError('missing state array: ' + key)
    if 'collection_skips' in state and not isinstance(state['collection_skips'], list):
        raise ValueError('missing collection skip array')
    return state


def verification_sql(plans):
    if not plans:
        raise ValueError('empty verification batch')
    selection = [{'id': p['shop_id']} for p in plans]
    ids = ','.join(literal(str(uuid.UUID(p['snapshot_id']))) + '::uuid' for p in plans)
    query = state_sql(selection).rstrip(';')
    return (f"WITH current_state AS ({query}) SELECT jsonb_build_object("
        "'state',(SELECT state FROM current_state),"
        "'snapshots',(SELECT coalesce(jsonb_agg(to_jsonb(s) ORDER BY s.id),'[]'::jsonb) FROM "
        "(SELECT id,shop_id,captured_at,source_url,parser_version,source_hash,menu_fingerprint,menu_items,"
        "encode(sha256(convert_to(raw_dom,'UTF8')),'hex') AS raw_hash "
        f"FROM public.shop_menu_snapshots WHERE id IN ({ids})) s)) AS verification;")


def expected_visible(plan, baseline_visible):
    old_visible = {r['id'] for r in baseline_visible if r['shop_id'] == plan['shop_id']}
    new_ids = {r['id'] for r in plan['additions']}
    return [{k: row.get(k) for k in VISIBLE_FIELDS} for row in plan['expected']
        if row['id'] in old_visible | new_ids]


def projected(rows, fields=None, ignored=()):
    return sorted(({k: v for k, v in row.items() if k not in ignored and (fields is None or k in fields)}
        for row in rows), key=lambda row: row['id'])


def same_time(left, right):
    return datetime.fromisoformat(left.replace('Z', '+00:00')) == datetime.fromisoformat(right.replace('Z', '+00:00'))


def verify_plan(plan, state, snapshots):
    issues = []
    shop_id = plan['shop_id']
    try:
        protections = protections_for_shop(shop_id, state)
        if plan.get('source_absence_policy') != 'direct_registration_only_v1':
            issues.append('source absence policy missing or obsolete')
        if protections != plan.get('manual_menu_protections'):
            issues.append('direct registration protection mismatch')
        source_names = {normalized(row['name']) for row in plan['source_items']}
        actual_rows = state_rows(state, 'items', shop_id)
        actual_ids = {row['id'] for row in actual_rows}
        for row in actual_rows:
            if normalized(row['name']) not in source_names and not protection_for_row(row, protections):
                issues.append('unprotected menu absent from Naver: ' + row['id'])
        for row in plan['before']:
            if normalized(row['name']) not in source_names and protection_for_row(row, protections) and row['id'] not in actual_ids:
                issues.append('directly registered menu deleted: ' + row['id'])
    except ValueError as error:
        issues.append(str(error))
    actual_shop = next((r for r in state['shops'] if r['id'] == shop_id), None)
    if actual_shop is None or any(actual_shop.get(k) != v for k, v in plan['identity'].items()):
        issues.append('shop identity mismatch')
    if actual_shop and (actual_shop.get('is_visible') is False or actual_shop.get('menu_snapshot_enabled') is False):
        issues.append('shop no longer enabled')
    comparisons = (
        ('items', plan['expected'], ()), ('sections', plan['sections'], ()),
        ('rules', plan['rules'], ()),
        ('overrides', plan.get('expected_overrides', plan['overrides']), ('updated_at',)))
    if skips_for_shop(shop_id, state) != plan.get('collection_skip_rules', []):
        issues.append('collection skip policy mismatch')
    for key, expected, ignored in comparisons:
        if projected(state_rows(state, key, shop_id), ignored=ignored) != projected(expected, ignored=ignored):
            issues.append(key + ' mismatch')
    actual_visible = state_rows(state, 'visible', shop_id)
    if projected(actual_visible, VISIBLE_FIELDS) != projected(plan['expected_visible'], VISIBLE_FIELDS):
        issues.append('visible menu mismatch')
    matching = [s for s in snapshots if s['id'] == plan['snapshot_id']]
    if len(matching) != 1:
        issues.append('snapshot missing or duplicated')
    else:
        snapshot = matching[0]
        for key in ('shop_id', 'source_url', 'parser_version', 'source_hash', 'menu_fingerprint'):
            if snapshot.get(key) != plan[key]:
                issues.append('snapshot ' + key + ' mismatch')
        if not same_time(snapshot['captured_at'], plan['captured_at']):
            issues.append('snapshot capture time mismatch')
        if snapshot.get('menu_items') != plan['source_items']:
            issues.append('snapshot source items mismatch')
        if snapshot.get('raw_hash') != plan['source_hash']:
            issues.append('snapshot raw source hash mismatch')
        if digest(serialized(snapshot.get('menu_items'))) != plan['menu_fingerprint']:
            issues.append('snapshot menu fingerprint mismatch')
    return {'passed': not issues, 'shop_id': shop_id, 'snapshot_id': plan['snapshot_id'], 'issues': issues,
        'source_items': len(plan['source_items']), 'visible_items': len(actual_visible),
        'visible_images': sum(bool(r.get('image_url')) for r in actual_visible),
        'visible_descriptions': sum(bool(r.get('description')) for r in actual_visible)}
