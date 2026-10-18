"""User-authorized skip-only decisions, separate from menu deletion/suppression."""
import json
import re
import uuid
from pathlib import Path

DEFAULT_REGISTRY = Path(__file__).resolve().parents[1] / 'data/collection-skips.json'


def load_registry(path=DEFAULT_REGISTRY):
    path = Path(path)
    if not path.exists():
        return []
    value = json.loads(path.read_text())
    if value.get('schema_version') != 1 or not isinstance(value.get('entries'), list):
        raise ValueError('invalid collection skip registry')
    for entry in value['entries']:
        uuid.UUID(entry['id'])
        uuid.UUID(entry['shop_id'])
        if entry.get('authority') != 'human_user' or entry.get('kind') not in ('menu', 'shop'):
            raise ValueError('collection skips require actual user authority and scope')
        if entry.get('status') not in ('active', 'inactive'):
            raise ValueError('invalid collection skip status')
        if entry['kind'] == 'menu' and not (entry.get('source_ids') or entry.get('normalized_names')):
            raise ValueError('menu collection skip needs exact source IDs or normalized names')
    return value['entries']


def skips_for_shop(shop_id, state=None, entries=None):
    if state is not None and 'collection_skips' in state:
        entries = state['collection_skips']
    elif entries is None:
        entries = load_registry()
    return sorted((e for e in entries if e.get('shop_id') == shop_id
        and e.get('authority') == 'human_user' and e.get('status') == 'active'), key=lambda e: e['id'])


def matching_skip(entries, item):
    name = re.sub(r'\s+', '', (item.get('name') or '').strip().lower())
    source_id = str(item['source_id']) if item.get('source_id') is not None else None
    return next((e for e in entries if e['kind'] == 'shop'
        or (source_id is not None and source_id in e.get('source_ids', []))
        or name in e.get('normalized_names', [])), None)


def collection_skips_sql(shop_ids=None):
    ids = ','.join("'" + str(uuid.UUID(s)) + "'" for s in shop_ids) if shop_ids is not None else None
    if ids == '':
        raise ValueError('collection skip read needs shop IDs')
    condition = f"AND binding->>'shop_id' IN ({ids}) " if ids is not None else ''
    return ("SELECT coalesce(jsonb_agg(binding ORDER BY binding->>'id'),'[]'::jsonb) AS collection_skips FROM ("
        "SELECT DISTINCT ON (binding->>'id') binding FROM public.shop_menu_validation_runs r "
        "CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(r.details->'collection_skip_bindings')='array' "
        "THEN r.details->'collection_skip_bindings' ELSE '[]'::jsonb END) AS binding "
        "WHERE r.details->>'collection_skip_authority'='human_user' "
        + condition + "ORDER BY binding->>'id',r.completed_at DESC NULLS LAST,r.id DESC"
        ") latest WHERE binding->>'authority'='human_user' AND binding->>'status'='active'")
