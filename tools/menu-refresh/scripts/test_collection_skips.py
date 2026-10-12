import copy
import tempfile
import unittest
import uuid
from pathlib import Path

from collection_skips import skips_for_shop
from refresh_plan import make_plan
from refresh_run import initialize, load
from test_refresh_plan import fixture


def skip_for(capture, kind='menu'):
    item = capture['menu_items'][0]
    return {'id': str(uuid.uuid4()), 'shop_id': capture['shop_id'], 'authority': 'human_user',
        'kind': kind, 'status': 'active', 'source_ids': [str(item['source_id'])] if item.get('source_id') else [],
        'normalized_names': [''.join(item['name'].strip().lower().split())], 'reason': '사용자 재수집 제외'}


class CollectionSkipsTest(unittest.TestCase):
    def test_existing_menu_keeps_price_description_image_and_order(self):
        capture, state = fixture()
        state['collection_skips'] = [skip_for(capture)]
        capture['menu_items'][0].update(description='변경된 원본 설명', image_url='https://example.com/new.jpg')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['updates'])
        self.assertEqual([], plan['deletions'])
        self.assertEqual([], plan['pending'])
        self.assertEqual(state['items'], plan['expected'])
        self.assertEqual(1, len(plan['collection_skipped']))

    def test_new_skipped_menu_is_not_added_or_held(self):
        capture, state = fixture()
        capture['menu_items'][0]['name'] = '미선택 신규메뉴'
        capture['scope'][0].update(name='미선택 신규메뉴', decision='manual', reason='구성 확인')
        state['collection_skips'] = [skip_for(capture)]
        plan = make_plan(capture, state)
        self.assertEqual([], plan['additions'])
        self.assertEqual([], plan['pending'])
        self.assertEqual(state['items'], plan['expected'])

    def test_competing_prices_of_skipped_name_do_not_create_review(self):
        capture, state = fixture()
        state['collection_skips'] = [skip_for(capture)]
        second = copy.deepcopy(capture['menu_items'][0])
        second.update(price=13000, price_text='13,000원', source_id='another-source')
        capture['menu_items'].append(second)
        capture['scope'].append({**capture['scope'][0], 'source_id': second['source_id']})
        capture['quality'].update(collected_count=2, source_count=2, source_image_count=2, collected_image_count=2)
        plan = make_plan(capture, state)
        self.assertEqual(2, len(plan['collection_skipped']))
        self.assertEqual([], plan['pending'])
        self.assertEqual([], plan['updates'])

    def test_same_source_id_still_skips_after_name_changes(self):
        capture, state = fixture()
        state['collection_skips'] = [skip_for(capture)]
        capture['menu_items'][0]['name'] = '새 표기 이름'
        capture['scope'][0]['name'] = '새 표기 이름'
        plan = make_plan(capture, state)
        self.assertEqual([], plan['additions'])
        self.assertEqual([], plan['updates'])
        self.assertEqual(1, len(plan['collection_skipped']))

    def test_other_shop_or_inactive_decision_cannot_skip_menu(self):
        capture, state = fixture()
        price = state['items'][0]['price_krw'] + 1000
        capture['menu_items'][0].update(price=price, price_text=f'{price:,}원')
        skipped = skip_for(capture)
        skipped['shop_id'] = str(uuid.uuid4())
        state['collection_skips'] = [skipped]
        self.assertEqual(1, len(make_plan(capture, state)['updates']))
        skipped['shop_id'] = capture['shop_id']
        skipped['status'] = 'inactive'
        self.assertEqual(1, len(make_plan(capture, state)['updates']))

    def test_explicit_empty_live_registry_overrides_stale_cache(self):
        capture, _ = fixture()
        self.assertEqual([], skips_for_shop(capture['shop_id'], {'collection_skips': []}, [skip_for(capture)]))

    def test_assignment_contains_this_shops_skip_rules(self):
        capture, state = fixture()
        skipped = skip_for(capture)
        with tempfile.TemporaryDirectory() as folder:
            run = Path(folder) / 'run'
            initialize(run, state['shops'], 'offline', mode='replay', collection_skips=[skipped])
            shop = load(run / 'manifest.json')['shops'][capture['shop_id']]
            self.assertEqual([skipped], load(run / shop['directory'] / 'assignment.json')['collection_skips'])

    def test_excluded_shop_is_not_assigned_to_collector(self):
        capture, state = fixture()
        second = {**state['shops'][0], 'id': str(uuid.uuid4()), 'name': '다른 매장'}
        with tempfile.TemporaryDirectory() as folder:
            run = Path(folder) / 'run'
            initialize(run, state['shops'] + [second], 'offline', mode='replay', collection_skips=[skip_for(capture, 'shop')])
            manifest = load(run / 'manifest.json')
            self.assertEqual({second['id']}, set(manifest['shops']))
            self.assertEqual([capture['shop_id']], manifest['excluded_collection_shop_ids'])


if __name__ == '__main__':
    unittest.main()
