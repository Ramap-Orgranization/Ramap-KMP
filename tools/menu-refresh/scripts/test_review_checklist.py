"""Approval boundaries: never include unselected or substituted decisions."""
import copy
import unittest

from review_checklist import build_registry, fingerprint, resolve_selection


class SelectionBoundaryTests(unittest.TestCase):
    def setUp(self):
        self.pending = [{'shop_id': 'shop-a', 'shop_name': '테스트 매장', 'kind': 'menu_review',
            'review_kind': 'registration_scope', 'scope_status': 'unconfirmed_new', 'name': name,
            'reason': '등록 범위 확인', 'snapshot_id': 'snapshot-a', 'item_number': number,
            'naver_place_url': 'https://map.naver.com/p/entry/place/123',
            'menu_url': 'https://m.place.naver.com/restaurant/123/menu/list'}
            for number, name in enumerate(('메뉴 A', '메뉴 B'), 1)]
        self.evidence = {'state': {'shops': [{'id': 'shop-a', 'address': '테스트 주소'}], 'items': [], 'visible': []},
            'snapshots': [{'id': 'snapshot-a', 'shop_id': 'shop-a', 'source_hash': 'unchanged-source',
                'menu_items': [{'name': name, 'price': 10000, 'price_text': '10,000원', 'source_id': name}
                    for name in ('메뉴 A', '메뉴 B')]}]}
        self.registry = build_registry(self.pending, self.evidence, '/pending.json', '/evidence.json')

    def selected(self, *entries):
        return {'bundle_id': self.registry['bundle_id'], 'selected': [{'entry_id': e['id'],
            'choice_key': e['choices'][0]['key']} for e in entries]}

    def test_only_selected_entry_is_resolved_and_inputs_stay_unchanged(self):
        before = copy.deepcopy(self.registry)
        entry = self.registry['entries'][1]
        result = resolve_selection(self.registry, self.selected(entry))
        self.assertEqual([entry['id']], [r['entry_id'] for r in result['decisions']])
        self.assertEqual(1, result['unselected_pending_records'])
        self.assertEqual(before, self.registry)
        self.assertNotIn('authority', result)

    def test_selection_from_other_review_is_rejected(self):
        selected = self.selected(self.registry['entries'][0])
        selected['bundle_id'] = 'another-review'
        with self.assertRaisesRegex(ValueError, 'another bundle'):
            resolve_selection(self.registry, selected)

    def test_substituted_decision_is_rejected(self):
        selected = self.selected(self.registry['entries'][0])
        selected['selected'][0]['choice_key'] = 'delete-everything'
        with self.assertRaisesRegex(ValueError, 'displayed decision'):
            resolve_selection(self.registry, selected)

    def test_unknown_duplicate_and_empty_selections_are_rejected(self):
        entry = self.registry['entries'][0]
        for selected in (self.selected(), self.selected(entry, entry),
            {'bundle_id': self.registry['bundle_id'], 'selected': [{'entry_id': 'unknown', 'choice_key': 'register'}]}):
            with self.subTest(selected=selected), self.assertRaises(ValueError):
                resolve_selection(self.registry, selected)

    def test_registry_values_cannot_change_after_display(self):
        selected = self.selected(self.registry['entries'][0])
        self.registry['entries'][0]['sources'][0]['value']['price'] = 999999
        with self.assertRaisesRegex(ValueError, 'bundle changed'):
            resolve_selection(self.registry, selected)

    def test_conflicting_existing_menu_targets_are_rejected(self):
        for entry in self.registry['entries']:
            entry['choices'][0]['existing_item_id'] = 'same-existing-menu'
        self.registry['bundle_id'] = fingerprint({k: v for k, v in self.registry.items() if k != 'bundle_id'})
        with self.assertRaisesRegex(ValueError, 'same existing menu'):
            resolve_selection(self.registry, self.selected(*self.registry['entries']))

    def test_duplicate_menu_hold_group_keeps_both_original_record_ids(self):
        first = copy.deepcopy(self.pending[0])
        first['review_kind'] = 'duplicate_values'
        second = copy.deepcopy(first)
        second['item_number'] = 2
        registry = build_registry([first, second], self.evidence, '/pending.json', '/evidence.json')
        self.assertEqual(1, len(registry['entries']))
        entry = registry['entries'][0]
        selection = {'bundle_id': registry['bundle_id'], 'selected': [{'entry_id': entry['id'],
            'choice_key': entry['choices'][0]['key']}]}
        result = resolve_selection(registry, selection)
        self.assertEqual(2, result['approved_pending_records'])
        self.assertEqual({fingerprint(first)[:24], fingerprint(second)[:24]}, set(result['decisions'][0]['pending_ids']))


if __name__ == '__main__':
    unittest.main()
