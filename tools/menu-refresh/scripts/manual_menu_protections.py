"""Exact, human-evidenced direct registrations; ordinary upserts are not protection."""
import json
import re
import uuid
from pathlib import Path


DEFAULT_REGISTRY = Path(__file__).resolve().parents[1] / 'data/manual-menu-protections.json'


def normalized(value):
    return re.sub(r'\s+', '', (value or '').strip().lower())


def validate_binding(entry):
    for key in ('id', 'shop_id', 'menu_item_id'):
        uuid.UUID(entry[key])
    if (entry.get('authority') != 'human_user'
        or entry.get('kind') != 'direct_registration'
        or entry.get('status') not in ('active', 'inactive')
        or not entry.get('name') or not entry.get('evidence_ref')
        or not entry.get('reason')):
        raise ValueError('direct registration protection needs exact IDs and human evidence')


def load_registry(path=DEFAULT_REGISTRY):
    value = json.loads(Path(path).read_text())
    if value.get('schema_version') != 1 or not isinstance(value.get('entries'), list):
        raise ValueError('invalid direct registration registry')
    for entry in value['entries']:
        validate_binding(entry)
    return value['entries']


def protections_for_shop(shop_id, state):
    entries = state.get('manual_menu_protections')
    if not isinstance(entries, list):
        raise ValueError('fresh direct registration protection read is required')
    for entry in entries:
        validate_binding(entry)
    return sorted((entry for entry in entries if entry['shop_id'] == shop_id
        and entry['status'] == 'active'), key=lambda entry: entry['id'])


def protection_for_row(row, entries):
    matches = [entry for entry in entries if entry['shop_id'] == row['shop_id']
        and entry['menu_item_id'] == row['id']]
    if any(normalized(entry['name']) != normalized(row['name']) for entry in matches):
        raise ValueError('direct registration protection name changed; review exact binding')
    return matches[0] if matches else None


def protections_sql(shop_ids):
    ids = ','.join("'" + str(uuid.UUID(value)) + "'" for value in shop_ids)
    if not ids:
        raise ValueError('protection read needs shop IDs')
    return ("SELECT coalesce(jsonb_agg(binding ORDER BY binding->>'id'),'[]'::jsonb) AS manual_menu_protections FROM ("
        "SELECT DISTINCT ON (binding->>'id') binding FROM public.shop_menu_validation_runs r "
        "CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(r.details->'manual_menu_protection_bindings')='array' "
        "THEN r.details->'manual_menu_protection_bindings' ELSE '[]'::jsonb END) AS binding "
        "WHERE r.details->>'manual_menu_protection_authority'='human_user' "
        "AND binding->>'shop_id' IN (" + ids + ") "
        "ORDER BY binding->>'id',r.completed_at DESC NULLS LAST,r.id DESC"
        ") latest WHERE binding->>'authority'='human_user' AND binding->>'kind'='direct_registration' "
        "AND binding->>'status'='active'")
