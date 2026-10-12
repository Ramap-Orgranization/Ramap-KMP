#!/usr/bin/env python3
"""Build a frozen selection review; resolve only choices sent by the human user."""
import argparse
import copy
import hashlib
import json
import re
from datetime import datetime, timezone
from pathlib import Path


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def fingerprint(value):
    return hashlib.sha256(canonical(value).encode()).hexdigest()


def normalized(value):
    return re.sub(r'\s+', '', value.strip().lower())


def load(path):
    return json.loads(Path(path).read_text())


def write_json(path, value):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def price_label(row):
    price = row.get('price', row.get('price_krw'))
    return f'{price:,}원' if type(price) is int else row.get('price_text') or '가격 표기 없음'


def build_registry(pending, evidence, pending_path, evidence_path):
    state = evidence['state']
    snapshots = {s['id']: s for s in evidence['snapshots']}
    shops = {s['id']: s for s in state['shops']}
    visible_ids = {r['id'] for r in state['visible']}
    groups = []
    duplicates = {}
    pending_ids = set()
    for record in pending:
        record_id = fingerprint(record)[:24]
        if record_id in pending_ids:
            raise ValueError('pending record ID collision')
        pending_ids.add(record_id)
        group_key = (record['shop_id'], normalized(record.get('name', '')), record.get('snapshot_id'))
        if record.get('review_kind') == 'duplicate_values' and group_key in duplicates:
            duplicates[group_key]['records'].append({'id': record_id, 'value': copy.deepcopy(record)})
            continue
        group = {'records': [{'id': record_id, 'value': copy.deepcopy(record)}]}
        groups.append(group)
        if record.get('review_kind') == 'duplicate_values':
            duplicates[group_key] = group
    entries = []
    for group in groups:
        record = group['records'][0]['value']
        sid = record['shop_id']
        kind = record.get('review_kind', 'source_unavailable')
        is_menu = record['kind'] == 'menu_review'
        source = snapshots.get(record.get('snapshot_id'))
        sources = []
        if is_menu:
            if not source or source['shop_id'] != sid:
                raise ValueError('pending source snapshot missing or mismatched')
            for index, item in enumerate(source['menu_items']):
                if normalized(item['name']) == normalized(record['name']):
                    sources.append({'key': fingerprint({'snapshot': source['id'], 'index': index, 'item': item})[:24],
                        'source_index': index, 'value': copy.deepcopy(item)})
            if not sources:
                raise ValueError('pending source item missing: ' + record['name'])
        exact = [r for r in state['items'] if r['shop_id'] == sid
            and normalized(r['name']) == normalized(record.get('name', ''))]
        candidates = [r for r in state['items'] if r['shop_id'] == sid
            and r['name'] in record.get('candidates', [])]
        if kind == 'identity_mapping' and not candidates:
            candidates = [r for r in state['items'] if r['shop_id'] == sid
                and r.get('is_visible') and r.get('event_id') is None]
        existing = exact or candidates
        choices = []
        default = None

        def add(key, label, action, src=None, current=None):
            value = {'key': key, 'label': label, 'action': action,
                'source_key': src['key'] if src else None, 'existing_item_id': current['id'] if current else None}
            choices.append(value)
            return key

        first = sources[0] if len(sources) == 1 else None
        current = exact[0] if len(exact) == 1 else None
        if kind == 'registration_scope' and first and not exact:
            default = add('register', '라멘 본품으로 등록', 'register_source_menu', first)
        elif kind == 'description_change' and first and current:
            default = add('description', '원본 설명으로 변경', 'replace_description', first, current)
        elif kind == 'identity_mapping' and first:
            for item in existing:
                add('match-' + item['id'], '같은 메뉴로 갱신: ' + item['name'], 'match_existing_menu', first, item)
            add('register-new', '별도의 신규 메뉴로 등록', 'register_source_menu', first)
            if len(existing) == 1:
                default = choices[0]['key']
        elif kind == 'duplicate_values':
            for index, src in enumerate(sources, 1):
                item = src['value']
                detail = (item.get('description') or '설명 없음').replace('\n', ' ')
                add('source-' + src['key'], f'원본 {index} 적용 · {price_label(item)} · {detail[:65]}',
                    'choose_source_variant', src, current)
        elif kind == 'visibility_event' and first and current:
            default = add('keep-state', '현재 노출 상태 유지', 'confirm_current_visibility', first, current)
            add('show-regular', '상시 메뉴로 노출', 'show_regular_menu', first, current)
            add('hide', '메뉴 숨김', 'hide_menu', first, current)
        elif kind == 'sale_status' and first:
            if current:
                add('selling', '판매 메뉴로 유지', 'confirm_current_sale', first, current)
                add('sale-ended', '판매 종료로 숨김', 'hide_menu', first, current)
            else:
                add('register', '원본 가격 표기 그대로 등록', 'register_source_menu', first)
                add('exclude', '등록 대상에서 제외', 'exclude_source_menu', first)
        elif kind == 'price_unknown' and first:
            if current:
                default = add('source-price', '원본 기준 가격 승인: ' + price_label(first['value']),
                    'confirm_source_price', first, current)
            else:
                default = add('source-price-text', '원본 가격 표기 그대로 등록: ' + price_label(first['value']),
                    'register_source_menu', first)
        entry_id = fingerprint({'pending_ids': [r['id'] for r in group['records']]})[:24]
        entries.append({'id': entry_id, 'records': group['records'], 'shop_id': sid,
            'shop_name': record['shop_name'], 'address': shops.get(sid, {}).get('address'),
            'name': record.get('name', '원본 메뉴 확인'), 'kind': kind, 'scope_status': record.get('scope_status'),
            'reason': record['reason'], 'historical_reason': record.get('historical_reason'),
            'naver_place_url': record['naver_place_url'], 'menu_url': record['menu_url'],
            'snapshot_id': source['id'] if source else None, 'source_hash': source.get('source_hash') if source else None,
            'captured_at': source.get('captured_at') if source else None,
            'sources': sources, 'existing': [copy.deepcopy(r) for r in existing],
            'currently_visible_ids': [r['id'] for r in existing if r['id'] in visible_ids],
            'choices': choices, 'default_choice': default, 'approvable': bool(choices)})
    entries.sort(key=lambda e: (e['kind'] == 'source_unavailable', e['shop_name'], e['name'], e['id']))
    if len({e['id'] for e in entries}) != len(entries):
        raise ValueError('entry ID collision')
    body = {'schema_version': 1, 'created_at': datetime.now(timezone.utc).isoformat(),
        'pending_path': str(Path(pending_path).resolve()), 'pending_fingerprint': fingerprint(pending),
        'evidence_path': str(Path(evidence_path).resolve()), 'evidence_fingerprint': fingerprint(evidence),
        'counts': {'pending_records': len(pending), 'menu_records': sum(r['kind'] == 'menu_review' for r in pending),
            'source_records': sum(r['kind'] != 'menu_review' for r in pending),
            'shops': len({r['shop_id'] for r in pending}), 'entries': len(entries),
            'approvable_entries': sum(e['approvable'] for e in entries)}, 'entries': entries}
    return {**body, 'bundle_id': fingerprint(body)}


def resolve_selection(registry, selection):
    body = {k: v for k, v in registry.items() if k != 'bundle_id'}
    if fingerprint(body) != registry['bundle_id'] or selection.get('bundle_id') != registry['bundle_id']:
        raise ValueError('review bundle changed or selection refers to another bundle')
    entries = {e['id']: e for e in registry['entries']}
    seen = set()
    target_ids = set()
    approved = []
    for selected in selection.get('selected', []):
        eid = selected.get('entry_id')
        if eid in seen or eid not in entries:
            raise ValueError('unknown or duplicate selection entry')
        seen.add(eid)
        entry = entries[eid]
        choice = next((c for c in entry['choices'] if c['key'] == selected.get('choice_key')), None)
        if choice is None:
            raise ValueError('selection must name a displayed decision')
        target = choice.get('existing_item_id')
        if target and target in target_ids:
            raise ValueError('multiple selected decisions target the same existing menu')
        if target:
            target_ids.add(target)
        approved.append({'entry_id': eid, 'pending_ids': [r['id'] for r in entry['records']],
            'records': copy.deepcopy(entry['records']), 'shop_id': entry['shop_id'], 'shop_name': entry['shop_name'],
            'snapshot_id': entry['snapshot_id'], 'source_hash': entry['source_hash'], 'decision': copy.deepcopy(choice),
            'source': copy.deepcopy(next((s['value'] for s in entry['sources'] if s['key'] == choice['source_key']), None)),
            'existing_guard': copy.deepcopy(next((r for r in entry['existing'] if r['id'] == target), None))})
    if not approved:
        raise ValueError('no selected decisions')
    return {'bundle_id': registry['bundle_id'], 'pending_fingerprint': registry['pending_fingerprint'],
        'approved_entries': len(approved), 'approved_pending_records': sum(len(e['pending_ids']) for e in approved),
        'unselected_pending_records': registry['counts']['pending_records'] - sum(len(e['pending_ids']) for e in approved),
        'decisions': approved, 'note': 'Selection only; use the actual human message as authority and guard live DB values before writes.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    build = commands.add_parser('build')
    build.add_argument('--pending', type=Path, required=True)
    build.add_argument('--evidence', type=Path, required=True)
    build.add_argument('--registry', type=Path, required=True)
    build.add_argument('--output', type=Path, required=True)
    resolve = commands.add_parser('resolve')
    resolve.add_argument('--registry', type=Path, required=True)
    resolve.add_argument('--selection', type=Path, required=True)
    resolve.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.command == 'resolve':
        write_json(args.output, resolve_selection(load(args.registry), load(args.selection)))
        return
    if args.registry.exists() or args.output.exists():
        raise ValueError('frozen review already exists; choose a new review path')
    registry = build_registry(load(args.pending), load(args.evidence), args.pending, args.evidence)
    template = Path(__file__).resolve().parents[1] / 'assets/review-checklist.html'
    config = {'bundleId': registry['bundle_id'], 'registryPath': str(args.registry.resolve()),
        'counts': registry['counts'], 'pageSize': 10,
        'entries': [{k: v for k, v in e.items() if k not in ('records', 'source_hash', 'historical_reason')}
            | {'pending_count': len(e['records'])} for e in registry['entries']]}
    encoded = json.dumps(config, ensure_ascii=False, separators=(',', ':')).replace('<', '\\u003c')
    fragment = template.read_text().replace('__MENU_REVIEW_DATA__', encoded)
    if len(fragment.encode()) >= 1_000_000 or '__MENU_REVIEW_DATA__' in fragment:
        raise ValueError('invalid visualization fragment')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    write_json(args.registry, registry)
    args.output.write_text(fragment)
    print(json.dumps({'bundle_id': registry['bundle_id'], 'counts': registry['counts'],
        'fragment_bytes': len(fragment.encode()), 'output': str(args.output), 'actual_db_calls': 0}, ensure_ascii=False))


if __name__ == '__main__':
    main()
