"""Evidence gates shared by planning, SQL generation and post-write checks."""
import hashlib
import json
import re
from pathlib import Path

from manual_menu_protections import normalized, protection_for_row
from source_dom import MenuParser, image_origin


POLICY = 'direct_registration_only_v2'
ORIGIN_CHECKS = {'registration_audit', 'human_input', 'manual_overrides', 'stored_media', 'aliases'}


def evidence_hash(value):
    text = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'))
    return hashlib.sha256(text.encode()).hexdigest()


def reference(value):
    return (isinstance(value, dict) and bool(value.get('ref'))
        and bool(re.fullmatch(r'[0-9a-f]{64}', value.get('sha256', ''))))


def validate_origin(row, reviews, overrides):
    matches = [r for r in reviews if r.get('menu_item_id') == row['id']]
    if len(matches) != 1:
        raise ValueError('deletion requires one exact registration-origin review: ' + row['id'])
    review = matches[0]
    if (review.get('shop_id') != row['shop_id'] or review.get('name') != row['name']
        or review.get('before_sha256') != evidence_hash(row)
        or review.get('overrides_sha256') != evidence_hash(sorted(overrides, key=lambda r: r['id']))):
        raise ValueError('registration-origin review is not bound to the current row and overrides')
    if (review.get('outcome') != 'not_direct_registration' or not review.get('reason')
        or review.get('unresolved_clues') != []
        or set(review.get('checked', [])) != ORIGIN_CHECKS
        or review.get('aliases_reviewed') is not True):
        raise ValueError('unresolved registration origin or alias evidence blocks deletion')
    refs = review.get('evidence_refs')
    if not isinstance(refs, list) or not refs or not all(reference(ref) for ref in refs):
        raise ValueError('registration-origin review needs hashed investigation evidence')
    # Uploaded media and past notes are investigation leads, not protection by themselves.
    # Require explicit resolutions instead of interpreting a missing binding as permission.
    required = {'registration_audit', 'human_input', 'aliases'}
    if overrides:
        required.add('manual_overrides')
    if '/storage/v1/object/' in (row.get('image_url') or ''):
        required.add('stored_media')
    resolutions = review.get('clue_resolutions', {})
    if any(not resolutions.get(kind) for kind in required):
        raise ValueError('registration clues have not been resolved: ' + row['id'])
    return review


def validate_absence(review, source_url, captured_at, raw_dom=None):
    if (review.get('schema_version') != 2 or review.get('complete') is not True
        or review.get('source_url') != source_url or review.get('captured_at') != captured_at
        or not re.fullmatch(r'[0-9a-f]{64}', review.get('raw_sha256', ''))
        or not review.get('reason')):
        raise ValueError('source absence requires v2 evidence bound to the full page')
    coverage = review.get('coverage', {})
    if (coverage.get('scope') != 'full_document' or coverage.get('expanded') is not True
        or coverage.get('loading_complete') is not True
        or coverage.get('other_menu_surfaces_reviewed') is not True
        or not reference(coverage.get('collection_evidence'))):
        raise ValueError('text panels alone do not establish complete source absence')
    boards = review.get('menu_board_reviews')
    if not isinstance(boards, list) or any(not reference(b.get('evidence'))
        or not b.get('image_url') or b.get('outcome') not in ('all_candidates_checked', 'not_current')
        or not b.get('reason') or b.get('unresolved_candidates') != [] for b in boards):
        raise ValueError('menu-board images require an evidenced review without unresolved candidates')
    urls = [image_origin(b['image_url']) for b in boards]
    if len(urls) != len(set(urls)):
        raise ValueError('duplicate menu-board reviews')
    if raw_dom is None:
        return
    if hashlib.sha256(raw_dom.encode()).hexdigest() != review['raw_sha256']:
        raise ValueError('source absence raw hash mismatch')
    if not re.search(r'<html(?:\s|>)', raw_dom, re.I) or not re.search(r'</html>\s*$', raw_dom, re.I):
        raise ValueError('source absence needs the complete document, not selected menu panels')
    parser = MenuParser()
    parser.feed(raw_dom)
    actual = set()
    for node in parser.root.walk():
        if node.tag != 'img':
            continue
        parent = node.parent
        board = bool(re.search(r'메뉴판', node.attrs.get('alt', '')))
        while parent:
            board = board or parent.attrs.get('data-nlog-area') == 'plc_bmv.menupanimg'
            parent = parent.parent
        if not board:
            continue
        url = next((node.attrs.get(k) for k in ('src', 'data-src', 'data-lazy-src')
            if (node.attrs.get(k) or '').startswith('https://')), None)
        if not url:
            raise ValueError('unloaded menu-board image blocks source absence')
        actual.add(image_origin(url))
    if actual != set(urls):
        raise ValueError('menu-board inventory and reviewed images differ')


def validate_deletions(plan, raw_dom=None):
    deletions = plan.get('deletions', [])
    before = {r['id']: r for r in plan['before']}
    expected = {r['id'] for r in plan['expected']}
    deleted = [d['id'] for d in deletions]
    if len(set(deleted)) != len(deleted) or set(before) - expected != set(deleted):
        raise ValueError('deletion evidence does not cover the actual removed rows')
    if not deletions:
        return
    if plan.get('source_absence_policy') != POLICY:
        raise ValueError('obsolete deletion policy; investigate provenance in a new run')
    for deletion in deletions:
        row = before.get(deletion['id'])
        if row is None or row['name'] != deletion['name'] or row['section_id'] != deletion['section_id']:
            raise ValueError('deletion target changed')
        if protection_for_row(row, plan['manual_menu_protections']):
            raise ValueError('directly registered menu requires a separate explicit deletion instruction')
        validate_origin(row, plan.get('registration_origin_reviews', []), plan['overrides'])
    if any(d['reason'] == 'source_absent' for d in deletions):
        validate_absence(plan.get('source_absence_review') or {}, plan['source_url'], plan['captured_at'], raw_dom)
        if any(d.get('evidence') != plan['source_absence_review'] for d in deletions if d['reason'] == 'source_absent'):
            raise ValueError('deletion record and reviewed source evidence differ')
    names = {normalized(r['name']) for r in plan['source_items']}
    if any(d['reason'] == 'source_absent' and normalized(d['name']) in names for d in deletions):
        raise ValueError('source-present menu cannot be deleted as absent')


def validate_evidence_files(plan):
    """Resolve local evidence before SQL creation; do not trust self-declared hashes."""
    if not plan.get('deletions'):
        return
    refs = [ref for review in plan.get('registration_origin_reviews', []) for ref in review.get('evidence_refs', [])]
    if any(d['reason'] == 'source_absent' for d in plan['deletions']):
        review = plan['source_absence_review']
        refs += [review['coverage']['collection_evidence']]
        refs += [board['evidence'] for board in review['menu_board_reviews']]
    for ref in refs:
        path = Path(ref['ref'])
        if not path.is_absolute() or not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != ref['sha256']:
            raise ValueError('deletion investigation evidence is missing or changed: ' + ref['ref'])
