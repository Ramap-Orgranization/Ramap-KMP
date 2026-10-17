#!/usr/bin/env python3
"""Prepare SQL and evidence for reviewed menu captures; never execute writes."""
import argparse
import copy
import hashlib
import json
import re
import uuid
from collections import Counter, defaultdict
from datetime import date, datetime, timezone
from pathlib import Path
from urllib.parse import parse_qs, urlparse
from zoneinfo import ZoneInfo

from source_dom import reconcile_source
from collection_skips import collection_skips_sql, matching_skip, skips_for_shop
from manual_menu_protections import protection_for_row, protections_for_shop, protections_sql
from deletion_guards import POLICY, validate_absence, validate_deletions, validate_evidence_files, validate_origin


def normalized(value):
    return re.sub(r'\s+', '', (value or '').strip().lower())


def description_content(value):
    return re.sub(r'\s+', '', value or '')


def approved_ramen_type(name):
    value = normalized(name)
    if re.search(r'(?:면|사리|토핑|소스)추가|육수팩|소스$', value):
        return None
    return next((kind for kind in ('탄탄면', '마제소바', '츠케멘', '중화소바') if kind in value), None)


def postgres_timestamp(value):
    if value.tzinfo is None:
        raise ValueError('confirmation timestamp requires a timezone')
    value = value.astimezone(timezone.utc)
    fraction = f'.{value.microsecond:06d}'.rstrip('0') if value.microsecond else ''
    return value.strftime('%Y-%m-%dT%H:%M:%S') + fraction + '+00:00'


def serialized(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'))


def serialized_guard_rows(rows):
    """Match SQL jsonb_agg ORDER BY row->>'id' without changing display order."""
    return serialized(sorted(rows, key=lambda row: row['id']))


def digest(value):
    return hashlib.sha256(value.encode('utf-8')).hexdigest()


def literal(value):
    if value is None:
        return 'NULL'
    if isinstance(value, bool):
        return 'true' if value else 'false'
    if isinstance(value, int):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def image_identity(value):
    if not value:
        return None
    url = urlparse(value)
    return parse_qs(url.query).get('src', [value])[0] if url.hostname == 'search.pstatic.net' else value


def valid_image(value):
    return value is None or (isinstance(value, str) and value.startswith('https://')
        and not re.search(r'placeholder|blank\.(?:gif|png)', value, re.I))


def select_sql(before, limit, field='menu'):
    cutoff = datetime.combine(date.fromisoformat(before), datetime.min.time(), ZoneInfo('Asia/Seoul')).isoformat()
    if not 1 <= limit <= 1000:
        raise ValueError('limit must be between 1 and 1000')
    column = {'menu': 'u.updated_at', 'shop': 'r.updated_at', 'capture': 'm.captured_at'}[field]
    return f"""select r.id,r.name,r.address,r.naver_place_url,r.updated_at as shop_updated_at,
u.updated_at as menu_updated_at,m.captured_at as last_menu_capture
from public.ramen_shops r
left join public.shop_menu_update_timestamps u on u.shop_id=r.id
left join lateral (select max(captured_at) as captured_at from public.shop_menu_snapshots where shop_id=r.id) m on true
where {column} < timestamptz {literal(cutoff)} and r.is_visible and r.menu_snapshot_enabled
and r.naver_place_url ~ '^https://(map\\.naver\\.com|m\\.place\\.naver\\.com)/'
and not exists(select 1 from jsonb_array_elements((select collection_skips from ({collection_skips_sql()}) skips)) skipped where skipped->>'kind'='shop' and skipped->>'shop_id'=r.id::text)
order by {column} asc,r.id asc limit {limit};"""


def state_rows(state, key, shop_id):
    return sorted((r for r in state[key] if r['shop_id'] == shop_id), key=lambda r: r['id'])


def state_sql(selection):
    if isinstance(selection, dict):
        selection = selection['rows']
    ids = ','.join(literal(str(uuid.UUID(s['id']))) + '::uuid' for s in selection)
    if not ids:
        raise ValueError('empty selection')
    parts = [f"'shops',(SELECT jsonb_agg(to_jsonb(r) ORDER BY r.id) FROM public.ramen_shops r WHERE r.id IN ({ids}))"]
    sources = {
        'sections': f'SELECT to_jsonb(s) AS row FROM public.shop_menu_sections s WHERE s.shop_id IN ({ids})',
        'items': f"SELECT to_jsonb(i)||jsonb_build_object('shop_id',s.shop_id) AS row FROM public.shop_menu_items i JOIN public.shop_menu_sections s ON s.id=i.section_id WHERE s.shop_id IN ({ids})",
        'visible': f'SELECT to_jsonb(v) AS row FROM public.visible_shop_menu_items v WHERE v.shop_id IN ({ids})',
        'overrides': f"SELECT to_jsonb(o) AS row FROM public.shop_menu_manual_overrides o WHERE o.shop_id IN ({ids}) AND o.status='active'",
        'rules': f'SELECT to_jsonb(r) AS row FROM public.shop_menu_exclusion_rules r WHERE r.shop_id IN ({ids}) AND r.is_active',
        'menu_timestamps': f'SELECT to_jsonb(t) AS row FROM public.shop_menu_update_timestamps t WHERE t.shop_id IN ({ids})'}
    for key, query in sources.items():
        parts.append(f"{literal(key)},(SELECT coalesce(jsonb_agg(row ORDER BY row->>'id',row->>'shop_id'),'[]'::jsonb) FROM ({query}) q)")
    parts.append("'collection_skips',(" + collection_skips_sql([s['id'] for s in selection]) + ')')
    parts.append("'manual_menu_protections',(" + protections_sql([s['id'] for s in selection]) + ')')
    return 'SELECT jsonb_build_object(' + ','.join(parts) + ') AS state;'


def make_plan(capture, state, *, description_approvals=(), registration_overrides=None):
    shop_id = str(uuid.UUID(capture['shop_id']))
    shop = next(r for r in state['shops'] if r['id'] == shop_id)
    plan = {'shop_id': shop_id, 'shop_name': shop['name'], 'source_url': capture.get('source_url'),
        'updates': [], 'additions': [], 'deletions': [], 'deletion_handoff': [],
        'pending': [], 'scope_confirmed_by_override': [], 'scope_confirmed_by_existing': [],
        'scope_confirmed_by_type': [], 'description_equivalences': [], 'description_approvals': [],
        'description_updates_by_policy': [], 'excluded_new_by_price': [],
        'override_updates': [], 'override_revocations': [],
        'duplicate_resolutions': [], 'variable_price_preserved': [], 'db_only_preserved': [], 'expected': [],
        'collection_skipped': [], 'collection_skip_rules': skips_for_shop(shop_id, state),
        'status': 'blocked', 'snapshot_id': str(uuid.uuid4())}
    quality = capture.get('quality', {})
    if capture.get('status') != 'collected' or quality.get('passed') is not True:
        plan['pending'] = [{'reason': '원본 수집 품질 미확인', 'details': quality.get('issues', [])}]
        return plan
    source = capture['menu_items']
    if not source or quality.get('collected_count') != len(source):
        raise ValueError('empty or incomplete capture')
    if quality.get('source_image_count') != quality.get('collected_image_count'):
        raise ValueError('source image count mismatch')
    if sum(bool(i.get('image_url')) for i in source) != quality['collected_image_count']:
        raise ValueError('collected image count mismatch')
    if not quality.get('sequence_evidence') or quality.get('source_count', 0) < len(source):
        raise ValueError('source count or ordering evidence missing')
    place_id = re.search(r'/(?:place|restaurant)/(\d+)', shop['naver_place_url']).group(1)
    if urlparse(capture['source_url']).hostname not in ('m.place.naver.com', 'pcmap.place.naver.com', 'map.naver.com'):
        raise ValueError('unexpected source host')
    if not re.search(r'/(?:place|restaurant)/' + place_id + r'(?:/|$|\?)', capture['source_url']):
        raise ValueError('source place ID mismatch')
    identity = capture.get('source_shop') or {}
    if str(identity.get('id')) != place_id or not identity.get('name') or not identity.get('address'):
        raise ValueError('source identity evidence missing')
    if capture.get('identity_verified') is not True:
        raise ValueError('agent must verify branch name and address before preparing SQL')
    captured_at = datetime.fromisoformat(capture['captured_at'].replace('Z', '+00:00'))
    if captured_at.tzinfo is None:
        raise ValueError('capture timestamp requires a timezone')
    sections = state_rows(state, 'sections', shop_id)
    regular = [s for s in sections if normalized(s['title']) == '상시메뉴' and s['is_visible']]
    if len(regular) != 1:
        plan['pending'].append({'reason': '상시메뉴 섹션 별도 정리 필요'})
        return plan
    section_id = regular[0]['id']
    before = state_rows(state, 'items', shop_id)
    protections = protections_for_shop(shop_id, state)
    origin_overrides = state_rows(state, 'overrides', shop_id) if registration_overrides is None else registration_overrides
    plan['manual_menu_protections'] = protections
    plan['source_absence_policy'] = POLICY
    plan['source_absence_review'] = copy.deepcopy(capture.get('source_absence_review'))
    plan['registration_origin_reviews'] = copy.deepcopy(capture.get('registration_origin_reviews', []))
    expected = {r['id']: copy.deepcopy(r) for r in before}
    by_name = defaultdict(list)
    for row in before:
        by_name[normalized(row['name'])].append(row)
    overrides = {r['normalized_name']: r for r in state_rows(state, 'overrides', shop_id)}
    rules = state_rows(state, 'rules', shop_id)
    source_counts = Counter(normalized(i['name']) for i in source)
    if len(capture['scope']) != len(source):
        raise ValueError('scope decision missing')
    resolved_duplicates = {}
    for resolution in capture.get('duplicate_resolutions', []):
        name = normalized(resolution['name'])
        candidates = [(index, item, capture['scope'][index]) for index, item in enumerate(source)
            if normalized(item['name']) == name]
        ids = [item.get('source_id') for _, item, _ in candidates]
        if (name in resolved_duplicates or len(candidates) < 2 or not all(ids)
            or len(set(ids)) != len(ids) or set(ids) != set(resolution.get('source_ids', []))
            or not resolution.get('evidence') or resolution.get('scope_verified') is not True):
            raise ValueError('duplicate resolution lacks complete source evidence')
        first = candidates[0][1]
        if (type(first['price']) is not int or first['price'] < 0 or not first.get('description')
            or any(item['price'] != first['price'] or type(item['price']) is not int
                or description_content(item.get('description')) != description_content(first['description'])
                or item.get('category') != first.get('category')
                or (scope['decision'] == 'exclude' and (item['source_id'] == resolution.get('selected_source_id')
                    or scope.get('duplicate_of') != resolution.get('selected_source_id')))
                for _, item, scope in candidates)):
            raise ValueError('duplicate price, composition, category or scope conflict')
        preferred = max(candidates, key=lambda candidate: (bool(candidate[1].get('is_featured')),
            bool(candidate[1].get('image_url')) + bool(candidate[1].get('description')), -candidate[0]))[1]
        if preferred['source_id'] != resolution.get('selected_source_id'):
            raise ValueError('duplicate resolution must follow representative metadata and source order')
        resolved_duplicates[name] = preferred['source_id']
        plan['duplicate_resolutions'].append(copy.deepcopy(resolution))
    matched_order = []
    source_names = {normalized(i['name']) for i in source}
    absent = [row for row in before if normalized(row['name']) not in source_names]
    plan['db_only_preserved'].extend(row['name'] for row in absent if protection_for_row(row, protections))
    absent = [row for row in absent if not protection_for_row(row, protections)]
    if absent:
        review = capture.get('source_absence_review') or {}
        try:
            validate_absence(review, capture['source_url'], capture['captured_at'])
            for old in absent:
                validate_origin(old, plan['registration_origin_reviews'], origin_overrides)
        except ValueError as error:
            plan['pending'].append({'reason': '원본 부재·직접 등록 경위 검증 미완료', 'details': str(error)})
            return plan
        for old in absent:
            if matching_skip(plan['collection_skip_rules'], old):
                plan['pending'].append({'name': old['name'],
                    'reason': '원본 부재 항목이 사용자 재수집 제외 범위와 충돌; 보호로 간주하지 않고 별도 처리 필요'})
                return plan
            plan['deletions'].append({'id': old['id'], 'section_id': old['section_id'],
                'name': old['name'], 'price_krw': old['price_krw'], 'reason': 'source_absent',
                'evidence': copy.deepcopy(review)})
            expected.pop(old['id'])
            override = overrides.get(normalized(old['name']))
            if override and override['action'] == 'upsert':
                plan['override_revocations'].append({'id': override['id'],
                    'reason': 'source_absent_without_direct_registration'})
    for item, decision in zip(source, capture['scope']):
        name = normalized(item['name'])
        if normalized(decision['name']) != name or not decision.get('reason'):
            raise ValueError('scope decision is not bound to the source item')
        if item.get('source_id') != decision.get('source_id'):
            raise ValueError('scope item ID mismatch')
        skipped = matching_skip(plan['collection_skip_rules'], item)
        if skipped:
            plan['collection_skipped'].append({'name': item['name'], 'source_id': item.get('source_id'),
                'skip_id': skipped['id'], 'reason': skipped['reason']})
            continue
        if not valid_image(item.get('image_url')):
            raise ValueError('invalid source image')
        if type(item['price']) is int and item.get('price_text') is not None:
            price_text = re.sub(r'[,\s원]', '', item['price_text'])
            if not price_text.isdigit() or int(price_text) != item['price']:
                raise ValueError('fixed price and source display price conflict')
        if name in resolved_duplicates and item['source_id'] != resolved_duplicates[name]:
            continue
        override = overrides.get(name)
        excluded = override and override['action'] == 'suppress'
        excluded = excluded or any(r['match_type'] == 'name_contains' and normalized(r['match_value']) in name for r in rules)
        if (not excluded and not by_name[name] and not (override and override['action'] == 'upsert')
            and type(item['price']) is int and 0 <= item['price'] <= 8000):
            plan['excluded_new_by_price'].append({'name': item['name'], 'source_id': item.get('source_id'),
                'price_krw': item['price'], 'reason': '사용자 판정: 신규 메뉴는 8,000원 이하이면 사이드'})
            continue
        existing_scope_confirmed = bool(by_name[name]) and not excluded and decision['decision'] in ('manual', 'exclude')
        approved_type = approved_ramen_type(item['name']) if not excluded else None
        type_scope_confirmed = approved_type is not None and decision['decision'] in ('manual', 'exclude')
        if excluded or (decision['decision'] == 'exclude' and not existing_scope_confirmed and not type_scope_confirmed):
            existing = by_name[name]
            if existing and override and override['action'] == 'upsert':
                plan['pending'].append({'name': item['name'], 'reason': '활성 수동 등록 범위와 제외 판정 충돌'})
                continue
            if existing:
                exact = (len(existing) == 1 and source_counts[name] == 1
                    and existing[0]['section_id'] == section_id and existing[0]['is_visible']
                    and existing[0]['event_id'] is None and type(item['price']) is int
                    and existing[0]['price_krw'] == item['price'])
                affirmed = decision.get('delete_existing') is True and decision.get('deletion_evidence')
                if exact and affirmed and decision['decision'] == 'exclude':
                    row = existing[0]
                    plan['deletions'].append({'id': row['id'], 'section_id': row['section_id'],
                        'name': row['name'], 'price_krw': row['price_krw'],
                        'reason': decision['reason'], 'evidence': decision['deletion_evidence']})
                    expected.pop(row['id'])
                else:
                    plan['deletion_handoff'].append({'name': item['name'],
                        'ids': [r['id'] for r in existing], 'reason': decision['reason'],
                        'details': '제외 대상 기존 행: 삭제 근거·정체 확인 또는 별도 menu-add 정리 필요'})
            continue
        if source_counts[name] != 1 and name not in resolved_duplicates:
            plan['pending'].append({'name': item['name'], 'reason': '원본 동일 이름 다중 항목: 가격·사진 확인 필요'})
            continue
        scope_confirmed = decision['decision'] == 'manual' and override and override['action'] == 'upsert'
        if scope_confirmed:
            plan['scope_confirmed_by_override'].append({'name': item['name'], 'override_id': override['id']})
        if existing_scope_confirmed:
            plan['scope_confirmed_by_existing'].append({'name': item['name'],
                'ids': [r['id'] for r in by_name[name]]})
        if type_scope_confirmed:
            plan['scope_confirmed_by_type'].append({'name': item['name'], 'type': approved_type})
        if (decision['decision'] != 'include' and not scope_confirmed
            and not existing_scope_confirmed and not type_scope_confirmed and name not in resolved_duplicates):
            plan['pending'].append({'name': item['name'], 'reason': decision['reason']})
            continue
        fixed_price = type(item['price']) is int and item['price'] >= 0
        if not fixed_price and item['price'] is not None:
            raise ValueError('invalid source price')
        if override and override['action'] == 'upsert':
            conflicts = [k for k in ('is_featured',)
                if override.get(k) is not None and item.get(k) is not None and override[k] != item[k]]
            if conflicts:
                plan['pending'].append({'name': item['name'], 'reason': '활성 수동 확인 충돌', 'fields': conflicts})
                continue
        matches = by_name[name]
        if len(matches) > 1 or (matches and (matches[0]['section_id'] != section_id or matches[0]['event_id'] or not matches[0]['is_visible'])):
            plan['pending'].append({'name': item['name'], 'reason': '이벤트·숨김·다중 섹션 메뉴 별도 확인'})
            continue
        description_held = False
        if not matches:
            if not fixed_price:
                plan['pending'].append({'name': item['name'], 'reason': '신규 메뉴의 고정 판매가 근거 없음', 'price_text': item.get('price_text')})
                continue
            description = normalized(item.get('description'))
            joined_names = {name + description,
                normalized(re.sub(r'^\[강추\]\s*', '', item['name'])) + description} if description else set()
            alternatives = [r['name'] for r in before if normalized(r['name']) not in source_names
                and (normalized(r['name']) in joined_names or
                    (r['price_krw'] == item['price'] and
                    ((item.get('image_url') and image_identity(r['image_url']) == image_identity(item['image_url']))
                     or (item.get('description') and normalized(r['description']) == normalized(item['description'])))))]
            if alternatives:
                plan['pending'].append({'name': item['name'], 'reason': '이름 변경/의미상 중복 확인 필요', 'candidates': alternatives})
                unresolved = [row for row in absent if row['name'] in alternatives]
                if unresolved:
                    ids = {row['id'] for row in unresolved}
                    plan['deletions'] = [row for row in plan['deletions'] if row['id'] not in ids]
                    for row in unresolved:
                        expected[row['id']] = copy.deepcopy(row)
                    plan['absence_identity_unresolved'] = True
                continue
            row = {'id': str(uuid.uuid4()), 'shop_id': shop_id, 'section_id': section_id,
                'name': item['name'], 'price_krw': item['price'], 'price_text': item.get('price_text'),
                'description': item.get('description'), 'image_url': item.get('image_url'),
                'is_featured': bool(item.get('is_featured')), 'is_visible': True, 'event_id': None,
                'display_order': 0, 'source_url': None}
            plan['additions'].append(row)
            expected[row['id']] = row
        else:
            old = matches[0]
            description = item.get('description')
            description_approved = False
            if not (description or '').strip():
                description = None
            if (description is not None and old['description'] not in (None, '')
                and description_content(old['description']) != description_content(description)):
                equivalences = [e for e in capture.get('description_equivalences', [])
                    if e.get('menu_item_id') == old['id'] and normalized(e.get('name')) == name]
                approvals = [e for e in description_approvals
                    if e.get('menu_item_id') == old['id'] and normalized(e.get('name')) == name]
                reviewed = equivalences + approvals
                if reviewed:
                    if (len(reviewed) != 1 or not reviewed[0].get('reason')
                        or reviewed[0].get('source_id') != item.get('source_id')
                        or reviewed[0].get('before_description') != old['description']
                        or reviewed[0].get('source_description') != description
                        or (approvals and approvals[0].get('authority') != 'human_user')):
                        raise ValueError('description review is not bound to the current menu and source')
                if approvals:
                    plan['description_approvals'].append(copy.deepcopy(approvals[0]))
                    description_approved = True
                elif equivalences:
                    plan['description_equivalences'].append(copy.deepcopy(equivalences[0]))
                else:
                    plan['description_updates_by_policy'].append({'menu_item_id': old['id'],
                        'name': item['name'], 'source_id': item.get('source_id'),
                        'before_description': old['description'], 'source_description': description,
                        'authority': 'standing_user_policy',
                        'reason': '2026-10-08 사용자 지시: 같은 메뉴의 설명 내용 변경만으로 보류하지 않음'})
                    description_approved = True
                if equivalences and not approvals:
                    description = None
            row = expected[old['id']]
            changes = {}
            if fixed_price and old['price_krw'] != item['price']:
                changes['price_krw'] = item['price']
                if old['price_text'] is not None:
                    changes['price_text'] = item.get('price_text') or f"{item['price']:,}"
            if not fixed_price:
                plan['variable_price_preserved'].append({'id': old['id'], 'name': item['name'],
                    'price_krw': old['price_krw'], 'source_price_text': item.get('price_text')})
                if old['price_krw'] is None and item.get('price_text') and old['price_text'] != item['price_text']:
                    changes['price_text'] = item['price_text']
                elif old['price_krw'] is not None:
                    plan['pending'].append({'name': item['name'], 'reason': '현재 고정 가격의 새 숫자 근거 없음; 확인된 메타데이터는 반영', 'price_text': item.get('price_text')})
            if description is not None and old['description'] != description:
                if description_approved or old['description'] in (None, '') or description_content(old['description']) == description_content(description):
                    changes['description'] = description
            if item.get('image_url') and image_identity(old['image_url']) != image_identity(item['image_url']):
                changes['image_url'] = item['image_url']
            if item.get('is_featured') and not old['is_featured']:
                changes['is_featured'] = True
            if changes:
                row.update(changes)
        if override and override['action'] == 'upsert':
            override_fields = {}
            if fixed_price and override.get('price_krw') != item['price']:
                override_fields['price_krw'] = item['price']
            if (override.get('image_url') and item.get('image_url')
                and image_identity(override['image_url']) != image_identity(item['image_url'])):
                override_fields['image_url'] = item['image_url']
            if (override.get('description') is not None and (item.get('description') or '').strip()
                and row.get('description') == item['description']
                and override['description'] != item['description']):
                override_fields['description'] = item['description']
            if override_fields:
                override_fields.update(source_type='admin_verified', confirmed_at=postgres_timestamp(captured_at),
                    note=((override.get('note') or '') + '\n사용자 상시 지시에 따라 검증된 최신 원본 가격·사진·설명 동기화: ' + capture['source_url']).strip())
                plan['override_updates'].append({'id': override['id'], 'fields': override_fields})
        if not description_held:
            matched_order.append(row['id'])
    # Only evidenced direct registrations may remain without a source row.
    # A held source row keeps the entire section's old order.
    remaining = [r for r in before if r['section_id'] == section_id
        and r['id'] in expected and r['id'] not in matched_order]
    if not plan['collection_skipped'] and not any(normalized(r['name']) in source_names for r in remaining):
        order = matched_order + [r['id'] for r in sorted(remaining, key=lambda r: (r['display_order'], r['id']))]
        for index, row_id in enumerate(order):
            expected[row_id]['display_order'] = index
    for old in before:
        if old['id'] not in expected:
            continue
        changes = {k: expected[old['id']][k] for k in old if expected[old['id']][k] != old[k]}
        if changes:
            plan['updates'].append({'id': old['id'], 'section_id': old['section_id'], 'fields': changes})
    for row in plan['additions']:
        row['display_order'] = expected[row['id']]['display_order']
    expected_overrides = {row['id']: copy.deepcopy(row) for row in state_rows(state, 'overrides', shop_id)}
    for update in plan['override_updates']:
        expected_overrides[update['id']].update(update['fields'])
    for revocation in plan['override_revocations']:
        expected_overrides.pop(revocation['id'])
    plan.update(status='blocked' if plan.get('absence_identity_unresolved') else
        'cleanup_required' if plan['deletion_handoff'] else 'prepared',
        captured_at=capture['captured_at'], source_items=source,
        parser_version=capture.get('parser_version', 'naver-menu-rendered-v1'), expected=sorted(expected.values(), key=lambda r: r['id']),
        before=before, sections=sections, overrides=origin_overrides,
        expected_overrides=sorted(expected_overrides.values(), key=lambda row: row['id']), rules=rules,
        identity={k: shop[k] for k in ('id', 'name', 'address', 'naver_place_url')})
    try:
        validate_deletions(plan)
    except ValueError as error:
        plan['status'] = 'blocked'
        plan['pending'].append({'reason': '삭제 근거 검증 미완료', 'details': str(error)})
    return plan


def transaction_sql(plan, raw_dom, reconciliation=None):
    if plan['status'] != 'prepared':
        raise ValueError('blocked plan cannot produce mutation SQL')
    validate_deletions(plan, raw_dom)
    validate_evidence_files(plan)
    plan['source_reconciliation'] = reconciliation or reconcile_source(plan['source_items'], raw_dom, plan['source_url'])
    source_names = {normalized(row['name']) for row in plan['source_items']}
    protections = plan.get('manual_menu_protections')
    if plan.get('source_absence_policy') != POLICY or not isinstance(protections, list):
        raise ValueError('source absence policy and exact direct registration bindings are required')
    for row in plan['expected']:
        if normalized(row['name']) not in source_names and not protection_for_row(row, protections):
            raise ValueError('unprotected DB-only row cannot remain in the expected state')
    expected_ids = {row['id'] for row in plan['expected']}
    for row in plan['before']:
        if normalized(row['name']) not in source_names and protection_for_row(row, protections) and row['id'] not in expected_ids:
            raise ValueError('directly registered menu cannot be deleted for source absence')
    shop_id = literal(plan['shop_id']) + '::uuid'
    snapshot_id = literal(plan['snapshot_id']) + '::uuid'
    body = ['DECLARE n integer; BEGIN']
    identity = plan['identity']
    body.append(f"PERFORM 1 FROM public.ramen_shops WHERE id={shop_id} AND name={literal(identity['name'])} AND address={literal(identity['address'])} AND naver_place_url={literal(identity['naver_place_url'])} AND is_visible AND menu_snapshot_enabled FOR UPDATE; IF NOT FOUND THEN RAISE EXCEPTION 'Shop identity or enabled state changed'; END IF;")
    for table, condition in (('shop_menu_sections', f'shop_id={shop_id}'),
        ('shop_menu_items', f'section_id IN (SELECT id FROM public.shop_menu_sections WHERE shop_id={shop_id})'),
        ('shop_menu_manual_overrides', f"shop_id={shop_id} AND status='active'"),
        ('shop_menu_exclusion_rules', f'shop_id={shop_id} AND is_active')):
        body.append(f'PERFORM 1 FROM public.{table} WHERE {condition} FOR UPDATE;')
    checks = [
        (f"SELECT to_jsonb(i)||jsonb_build_object('shop_id',s.shop_id) AS row FROM public.shop_menu_items i JOIN public.shop_menu_sections s ON s.id=i.section_id WHERE s.shop_id={shop_id}", plan['before']),
        (f'SELECT to_jsonb(s) AS row FROM public.shop_menu_sections s WHERE s.shop_id={shop_id}', plan['sections']),
        (f"SELECT to_jsonb(o) AS row FROM public.shop_menu_manual_overrides o WHERE o.shop_id={shop_id} AND status='active'", plan['overrides']),
        (f'SELECT to_jsonb(r) AS row FROM public.shop_menu_exclusion_rules r WHERE r.shop_id={shop_id} AND is_active', plan['rules'])]
    skips_query = collection_skips_sql([plan['shop_id']])
    checks.append((f'SELECT entry AS row FROM jsonb_array_elements((SELECT collection_skips FROM ({skips_query}) s)) entry',
        plan['collection_skip_rules']))
    protection_query = protections_sql([plan['shop_id']])
    checks.append((f'SELECT entry AS row FROM jsonb_array_elements((SELECT manual_menu_protections FROM ({protection_query}) s)) entry',
        protections))
    for query, rows in checks:
        body.append(f"IF (SELECT coalesce(jsonb_agg(row ORDER BY row->>'id'),'[]'::jsonb) FROM ({query}) q) IS DISTINCT FROM {literal(serialized_guard_rows(rows))}::jsonb THEN RAISE EXCEPTION 'Concurrent menu or protection change'; END IF;")
    body.append(f"IF EXISTS(SELECT 1 FROM public.shop_menu_snapshots WHERE id={snapshot_id}) THEN RAISE EXCEPTION 'Snapshot already applied; read result instead of replaying'; END IF;")
    for row in plan.get('override_additions', []):
        body.append(f"INSERT INTO public.shop_menu_manual_overrides({','.join(row)}) VALUES({','.join(literal(v) for v in row.values())}) ON CONFLICT (shop_id,normalized_name) WHERE status='active' DO NOTHING; GET DIAGNOSTICS n=ROW_COUNT; IF n<>1 THEN RAISE EXCEPTION 'Scope confirmation insertion mismatch'; END IF;")
    allowed = {'price_krw', 'price_text', 'description', 'image_url', 'is_featured', 'display_order'}
    for deletion in plan['deletions']:
        body.append(f"DELETE FROM public.shop_menu_items WHERE id={literal(deletion['id'])}::uuid AND section_id={literal(deletion['section_id'])}::uuid AND name={literal(deletion['name'])} AND price_krw IS NOT DISTINCT FROM {literal(deletion['price_krw'])}; GET DIAGNOSTICS n=ROW_COUNT; IF n<>1 THEN RAISE EXCEPTION 'Deletion count mismatch'; END IF;")
    for revocation in plan.get('override_revocations', []):
        body.append(f"UPDATE public.shop_menu_manual_overrides SET status='revoked' WHERE id={literal(revocation['id'])}::uuid AND shop_id={shop_id} AND action='upsert' AND status='active'; GET DIAGNOSTICS n=ROW_COUNT; IF n<>1 THEN RAISE EXCEPTION 'Override revocation count mismatch'; END IF;")
    for update in plan['updates']:
        if not set(update['fields']) <= allowed:
            raise ValueError('unexpected update field')
        assignments = ','.join(k + '=' + literal(v) for k, v in update['fields'].items())
        body.append(f"UPDATE public.shop_menu_items SET {assignments} WHERE id={literal(update['id'])}::uuid AND section_id={literal(update['section_id'])}::uuid; GET DIAGNOSTICS n=ROW_COUNT; IF n<>1 THEN RAISE EXCEPTION 'Update count mismatch'; END IF;")
    for row in plan['additions']:
        values = {k: v for k, v in row.items() if k != 'shop_id'}
        body.append(f"INSERT INTO public.shop_menu_items({','.join(values)}) VALUES({','.join(literal(v) for v in values.values())}) ON CONFLICT (section_id,(regexp_replace(lower(btrim(name)), '[[:space:]]+', '', 'g'))) DO NOTHING; GET DIAGNOSTICS n=ROW_COUNT; IF n<>1 THEN RAISE EXCEPTION 'Addition conflict; rebuild plan'; END IF;")
    for update in plan.get('override_updates', []):
        if not set(update['fields']) <= {'price_krw', 'image_url', 'source_type', 'confirmed_at', 'note'}:
            raise ValueError('unexpected override update field')
        assignments = ','.join(k + '=' + literal(v) for k, v in update['fields'].items())
        body.append(f"UPDATE public.shop_menu_manual_overrides SET {assignments} WHERE id={literal(update['id'])}::uuid AND shop_id={shop_id} AND action='upsert' AND status='active'; GET DIAGNOSTICS n=ROW_COUNT; IF n<>1 THEN RAISE EXCEPTION 'Override update count mismatch'; END IF;")
    query = checks[0][0]
    body.append(f"IF (SELECT coalesce(jsonb_agg(row ORDER BY row->>'id'),'[]'::jsonb) FROM ({query}) q) IS DISTINCT FROM {literal(serialized_guard_rows(plan['expected']))}::jsonb THEN RAISE EXCEPTION 'Post-write menu mismatch'; END IF;")
    for query, rows in (checks[1], checks[3]):
        body.append(f"IF (SELECT coalesce(jsonb_agg(row ORDER BY row->>'id'),'[]'::jsonb) FROM ({query}) q) IS DISTINCT FROM {literal(serialized_guard_rows(rows))}::jsonb THEN RAISE EXCEPTION 'Unexpected section or rule change'; END IF;")
    expected_overrides = [{k: v for k, v in row.items() if k != 'updated_at'}
        for row in plan.get('expected_overrides', plan['overrides'])]
    query = checks[2][0].replace('to_jsonb(o)', "(to_jsonb(o)-'updated_at')")
    body.append(f"IF (SELECT coalesce(jsonb_agg(row ORDER BY row->>'id'),'[]'::jsonb) FROM ({query}) q) IS DISTINCT FROM {literal(serialized_guard_rows(expected_overrides))}::jsonb THEN RAISE EXCEPTION 'Post-write override mismatch'; END IF;")
    plan['source_hash'] = digest(raw_dom)
    plan['menu_fingerprint'] = digest(serialized(plan['source_items']))
    body.append(f"INSERT INTO public.shop_menu_snapshots(id,shop_id,captured_at,source_url,parser_version,source_hash,raw_dom,menu_items,menu_fingerprint) VALUES({snapshot_id},{shop_id},{literal(plan['captured_at'])}::timestamptz,{literal(plan['source_url'])},{literal(plan['parser_version'])},{literal(plan['source_hash'])},{literal(raw_dom)},{literal(serialized(plan['source_items']))}::jsonb,{literal(plan['menu_fingerprint'])}); GET DIAGNOSTICS n=ROW_COUNT; IF n<>1 THEN RAISE EXCEPTION 'Snapshot count mismatch'; END IF;")
    if plan.get('validation_run_id') and (plan['updates'] or plan['additions'] or plan['deletions'] or plan.get('override_updates') or plan.get('override_additions') or plan.get('override_revocations')):
        summary = {k: plan.get(k, []) for k in ('updates', 'additions', 'deletions', 'override_updates', 'override_revocations', 'override_additions', 'duplicate_resolutions', 'variable_price_preserved', 'pending', 'db_only_preserved', 'manual_menu_protections', 'registration_origin_reviews')}
        summary.update(source_absence_policy=plan['source_absence_policy'], source_absence_review=plan.get('source_absence_review'))
        previous = f'FROM public.shop_menu_snapshots WHERE shop_id={shop_id} AND id<>{snapshot_id} ORDER BY captured_at DESC,id DESC LIMIT 1'
        body.append(f"INSERT INTO public.shop_menu_change_events(shop_id,previous_menu_fingerprint,new_menu_fingerprint,previous_source_hash,new_source_hash,change_summary,validation_run_id) VALUES({shop_id},coalesce((SELECT menu_fingerprint {previous}),{literal(digest(serialized(plan['before'])))}),{literal(plan['menu_fingerprint'])},coalesce((SELECT source_hash {previous}),'no_previous_snapshot'),{literal(plan['source_hash'])},{literal(serialized(summary))}::jsonb,{literal(plan['validation_run_id'])}::uuid); GET DIAGNOSTICS n=ROW_COUNT; IF n<>1 THEN RAISE EXCEPTION 'Change audit count mismatch'; END IF;")
    body.append('END;')
    body = '\n'.join(body)
    tag = '$refresh_' + uuid.uuid4().hex + '$'
    while tag in body:
        tag = '$refresh_' + uuid.uuid4().hex + '$'
    return f"BEGIN;\nSET LOCAL standard_conforming_strings=on;\nDO {tag}\n{body}\n{tag};\nCOMMIT;\nSELECT id,shop_id,captured_at,source_hash,menu_fingerprint FROM public.shop_menu_snapshots WHERE id={snapshot_id};\n"


def confirmed_scope_state(capture, state, confirmations):
    """Turn separately recorded user scope decisions into atomic upsert inserts."""
    virtual = copy.deepcopy(state)
    additions = []
    names = set()
    for confirmation in confirmations:
        if confirmation['shop_id'] != capture['shop_id']:
            continue
        name = normalized(confirmation['name'])
        if name in names or not confirmation.get('evidence'):
            raise ValueError('duplicate or undocumented user scope confirmation')
        names.add(name)
        matches = [(item, scope) for item, scope in zip(capture['menu_items'], capture['scope'])
            if normalized(item['name']) == name and item.get('source_id') == confirmation.get('source_id')]
        if len(matches) != 1:
            raise ValueError('scope confirmation must identify one current source item')
        item, scope = matches[0]
        if scope['decision'] not in ('include', 'manual') or type(item['price']) is not int or item['price'] < 0:
            raise ValueError('scope confirmation requires an eligible fixed-price source item')
        existing = [r for r in virtual['overrides'] if r['shop_id'] == capture['shop_id']
            and r['normalized_name'] == name]
        if existing:
            if existing[0]['action'] != 'upsert':
                raise ValueError('existing suppression requires a separate lifecycle decision')
            continue
        confirmed = postgres_timestamp(datetime.fromisoformat(confirmation['confirmed_at'].replace('Z', '+00:00')))
        row = {'id': str(uuid.uuid4()), 'shop_id': capture['shop_id'], 'normalized_name': name,
            'action': 'upsert', 'name': item['name'], 'price_krw': item['price'],
            'description': None, 'image_url': None, 'is_featured': None, 'section_title': None,
            'source_type': 'admin_verified',
            'note': confirmation['evidence'] + '\n출처: ' + capture['source_url'],
            'confirmed_at': confirmed, 'review_after': None, 'status': 'active',
            'created_at': confirmed, 'updated_at': confirmed}
        virtual['overrides'].append(row)
        additions.append(row)
    return virtual, additions


def prepare_capture(capture, state, raw_dom, run_id=None, snapshot_id=None, confirmations=(), description_approvals=()):
    """Shared preparation for the CLI and queue runner; parse source DOM once."""
    virtual, override_additions = confirmed_scope_state(capture, state, confirmations)
    plan = make_plan(capture, virtual, description_approvals=description_approvals,
        registration_overrides=state_rows(state, 'overrides', capture['shop_id']))
    plan['override_additions'] = override_additions
    if 'overrides' in plan:
        plan['overrides'] = state_rows(state, 'overrides', capture['shop_id'])
    if snapshot_id:
        plan['snapshot_id'] = str(uuid.UUID(snapshot_id))
    if run_id:
        plan['validation_run_id'] = str(uuid.UUID(run_id))
    sql = None
    if plan['status'] in ('prepared', 'cleanup_required'):
        evidence = reconcile_source(plan['source_items'], raw_dom, plan['source_url'])
        plan['source_reconciliation'] = evidence
        if plan['status'] == 'prepared':
            sql = transaction_sql(plan, raw_dom, evidence)
    return plan, sql


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    select = sub.add_parser('select-sql')
    select.add_argument('--before', required=True)
    select.add_argument('--limit', type=int, default=10)
    select.add_argument('--date-field', choices=('menu', 'shop', 'capture'), default='menu')
    read = sub.add_parser('state-sql')
    read.add_argument('--selection', type=Path, required=True)
    prepare = sub.add_parser('prepare')
    prepare.add_argument('--capture', type=Path, required=True)
    prepare.add_argument('--state', type=Path, required=True)
    prepare.add_argument('--output', type=Path, required=True)
    prepare.add_argument('--run-id')
    prepare.add_argument('--description-approvals', type=Path)
    args = parser.parse_args()
    if args.command == 'select-sql':
        print(select_sql(args.before, args.limit, args.date_field))
        return
    if args.command == 'state-sql':
        print(state_sql(json.loads(args.selection.read_text())))
        return
    capture = json.loads(args.capture.read_text())
    state = json.loads(args.state.read_text())
    if 'rows' in state:
        state = state['rows'][0]['state']
    description_approvals = json.loads(args.description_approvals.read_text()) if args.description_approvals else ()
    plan = make_plan(capture, state, description_approvals=description_approvals)
    previous_plan_path = args.output / 'plan.json'
    if previous_plan_path.exists() and plan['status'] == 'prepared':
        previous_plan = json.loads(previous_plan_path.read_text())
        if (previous_plan.get('shop_id') == plan['shop_id']
            and previous_plan.get('captured_at') == plan['captured_at']
            and previous_plan.get('source_url') == plan['source_url']
            and previous_plan.get('source_items') == plan['source_items']):
            plan['snapshot_id'] = str(uuid.UUID(previous_plan['snapshot_id']))
    args.output.mkdir(parents=True, exist_ok=True)
    if plan['status'] in ('prepared', 'cleanup_required'):
        raw_path = Path(capture['raw_html_path']).resolve()
        if args.capture.resolve().parent not in raw_path.parents:
            raise ValueError('raw source must be inside the capture artifact directory')
        raw_dom = raw_path.read_text()
        if not raw_dom.strip() or 'place_section_content' not in raw_dom and '<li' not in raw_dom:
            raise ValueError('raw source is not a menu DOM capture')
        plan, sql = prepare_capture(capture, state, raw_dom, args.run_id, plan['snapshot_id'],
            description_approvals=description_approvals)
        if sql:
            (args.output / 'apply.sql').write_text(sql)
        elif (args.output / 'apply.sql').exists():
            (args.output / 'apply.sql').unlink()
    elif (args.output / 'apply.sql').exists():
        (args.output / 'apply.sql').unlink()
    (args.output / 'plan.json').write_text(json.dumps(plan, ensure_ascii=False, indent=2) + '\n')
    print(serialized({k: plan[k] for k in ('shop_name', 'status', 'pending', 'db_only_preserved')}))
    print(serialized({'updated_rows': len(plan['updates']), 'added_rows': len(plan['additions']),
        'deleted_rows': len(plan['deletions']), 'deletion_handoff': plan['deletion_handoff']}))


if __name__ == '__main__':
    main()
