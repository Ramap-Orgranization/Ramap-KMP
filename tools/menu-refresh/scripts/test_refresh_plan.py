import copy
import unittest
from datetime import datetime

from refresh_plan import approved_ramen_type, image_identity, literal, make_plan, postgres_timestamp, select_sql, transaction_sql


SHOP = '00000000-0000-0000-0000-000000000001'
SECTION = '00000000-0000-0000-0000-000000000002'
ROW = '00000000-0000-0000-0000-000000000003'


def fixture():
    row = {'id': ROW, 'shop_id': SHOP, 'section_id': SECTION, 'name': '쇼유라멘',
        'price_krw': 10000, 'price_text': '10,000', 'description': '닭 육수',
        'image_url': 'https://example.com/menu.jpg', 'event_id': None, 'is_visible': True,
        'is_featured': True, 'display_order': 0, 'source_url': None}
    source = {'source_id': 'naver-item-1', 'name': '쇼유라멘', 'price': 10000,
        'price_text': '10,000원', 'description': '닭 육수', 'image_url': row['image_url'],
        'source_image_url': row['image_url'], 'is_featured': False}
    state = {'shops': [{'id': SHOP, 'name': '테스트라멘', 'address': '서울 서초구 테스트로 1',
        'naver_place_url': 'https://map.naver.com/p/entry/place/123'}],
        'sections': [{'id': SECTION, 'shop_id': SHOP, 'title': '상시메뉴', 'is_visible': True,
            'display_order': 0, 'description': None}], 'items': [row], 'overrides': [], 'rules': []}
    capture = {'shop_id': SHOP, 'status': 'collected', 'identity_verified': True,
        'source_url': 'https://m.place.naver.com/restaurant/123/menu/list',
        'source_shop': {'id': '123', 'name': '테스트라멘', 'address': '서울 서초구 테스트로 1'},
        'captured_at': '2026-10-07T02:00:00Z', 'menu_items': [source],
        'quality': {'passed': True, 'source_count': 1, 'collected_count': 1,
            'source_image_count': 1, 'collected_image_count': 1, 'sequence_evidence': 'regular 1'},
        'scope': [{'source_id': 'naver-item-1', 'name': '쇼유라멘', 'decision': 'include', 'reason': '라멘 본품'}]}
    return capture, state


class RefreshPlanTest(unittest.TestCase):
    def test_existing_registered_menu_is_ramen_scope_without_reconfirmation(self):
        capture, state = fixture()
        state['items'][0]['name'] = '돈코츠'
        capture['menu_items'][0].update(name='돈코츠', price=11000, price_text='11,000원')
        capture['scope'][0].update(name='돈코츠', decision='manual', reason='이름에 라멘 없음')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual([], plan['additions'])
        self.assertEqual(ROW, plan['updates'][0]['id'])
        self.assertEqual(11000, plan['updates'][0]['fields']['price_krw'])
        self.assertEqual([{'name': '돈코츠', 'ids': [ROW]}], plan['scope_confirmed_by_existing'])

    def test_new_ambiguous_menu_does_not_inherit_existing_scope_rule(self):
        capture, state = fixture()
        capture['menu_items'][0]['name'] = '새로운소바'
        capture['scope'][0].update(name='새로운소바', decision='manual', reason='구성 불명')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['additions'])
        self.assertEqual([], plan['updates'])
        self.assertEqual('구성 불명', plan['pending'][0]['reason'])

    def test_four_approved_noodle_types_register_without_scope_reapproval(self):
        for name in ('라구탄탄면(파스타)', '사천탄탄면(땅콩소스)', '쿄우노 마제소바',
                     '마제소바(사리 포함)', '[NEW] 츠케멘', '중화소바'):
            for decision in ('manual', 'exclude'):
                with self.subTest(name=name, decision=decision):
                    capture, state = fixture()
                    state['items'] = []
                    capture['menu_items'][0]['name'] = name
                    capture['scope'][0].update(name=name, decision=decision, reason='기존 자동 분류')
                    plan = make_plan(capture, state)
                    self.assertEqual([], plan['pending'])
                    self.assertEqual([name], [row['name'] for row in plan['additions']])

    def test_approved_type_does_not_register_an_add_on_or_ignore_suppression(self):
        self.assertIsNone(approved_ramen_type('츠케멘 면 추가'))
        capture, state = fixture()
        state['items'] = []
        capture['menu_items'][0]['name'] = '중화소바'
        capture['scope'][0].update(name='중화소바', decision='manual', reason='이름만 확인')
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP,
            'normalized_name': '중화소바', 'action': 'suppress'}]
        self.assertEqual([], make_plan(capture, state)['additions'])

    def test_collector_exclusion_cannot_delete_existing_registered_menu(self):
        capture, state = fixture()
        state['items'][0]['name'] = '교카이 돈코츠(어패류 계열)'
        capture['menu_items'][0]['name'] = state['items'][0]['name']
        capture['scope'][0].update(name=state['items'][0]['name'], decision='exclude',
            reason='라멘 본품이 아닌 메뉴', delete_existing=True,
            deletion_evidence='고정 가격이 기존 행과 같음')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['deletions'])
        self.assertEqual([], plan['deletion_handoff'])
        self.assertEqual([], plan['pending'])
        self.assertEqual([ROW], [row['id'] for row in plan['expected']])
        self.assertEqual(1, len(plan['scope_confirmed_by_existing']))

    def test_reviewed_description_equivalence_preserves_existing_text(self):
        capture, state = fixture()
        old = state['items'][0]
        old['description'] = '뽀얀 국물의 라멘'
        capture['menu_items'][0]['description'] = '뽀얀 국물의 돈코츠라멘'
        capture['description_equivalences'] = [{'menu_item_id': ROW, 'name': old['name'],
            'source_id': 'naver-item-1', 'before_description': old['description'],
            'source_description': capture['menu_items'][0]['description'],
            'reason': '정확한 두 설명의 동등성 확인; 기존 설명 보존'}]
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual([], plan['updates'])
        self.assertEqual(old['description'], plan['expected'][0]['description'])
        self.assertEqual(capture['description_equivalences'], plan['description_equivalences'])

    def test_explicit_description_approval_updates_only_bound_source_text(self):
        capture, state = fixture()
        capture['menu_items'][0]['description'] = '닭 육수와 차슈 2pcs'
        approval = {'menu_item_id': ROW, 'name': '쇼유라멘', 'source_id': 'naver-item-1',
            'before_description': '닭 육수', 'source_description': '닭 육수와 차슈 2pcs',
            'authority': 'human_user', 'reason': '해당 보류 미리보기 사용자 승인'}
        plan = make_plan(capture, state, description_approvals=[approval])
        self.assertEqual([], plan['pending'])
        self.assertEqual('닭 육수와 차슈 2pcs', plan['updates'][0]['fields']['description'])
        approval['authority'] = 'collector'
        with self.assertRaises(ValueError):
            make_plan(capture, state, description_approvals=[approval])

    def test_collector_cannot_claim_a_user_description_approval(self):
        capture, state = fixture()
        capture['menu_items'][0]['description'] = '다른 육수와 차슈 2pcs'
        capture['description_approvals'] = [{'menu_item_id': ROW, 'name': '쇼유라멘',
            'source_id': 'naver-item-1', 'before_description': '닭 육수',
            'source_description': '다른 육수와 차슈 2pcs', 'authority':'human_user',
            'reason':'수집자가 주장한 승인'}]
        plan = make_plan(capture, state)
        self.assertEqual([], plan['description_approvals'])
        self.assertEqual('standing_user_policy', plan['description_updates_by_policy'][0]['authority'])
        self.assertEqual('다른 육수와 차슈 2pcs', plan['updates'][0]['fields']['description'])
        self.assertEqual([], plan['pending'])

    def test_stale_description_equivalence_cannot_clear_new_content_change(self):
        for field, wrong in [('before_description', '옛 설명'),
                             ('source_description', '다른 구성'), ('source_id', 'other-source')]:
            with self.subTest(field=field):
                capture, state = fixture()
                capture['menu_items'][0]['description'] = '차슈가 추가된 라멘'
                resolution = {'menu_item_id': ROW, 'name': '쇼유라멘', 'source_id': 'naver-item-1',
                    'before_description': '닭 육수', 'source_description': '차슈가 추가된 라멘',
                    'reason': '이전 동등성 확인'}
                resolution[field] = wrong
                capture['description_equivalences'] = [resolution]
                with self.assertRaises(ValueError):
                    make_plan(capture, state)

    def test_confirmation_time_matches_postgres_utc_json_precision(self):
        stamp = datetime.fromisoformat('2026-10-07T03:00:00.946000+09:00')
        self.assertEqual('2026-10-06T18:00:00.946+00:00', postgres_timestamp(stamp))
        capture, state = fixture()
        capture['captured_at'] = stamp.isoformat()
        capture['menu_items'][0].update(price=11000, price_text='11,000원')
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘',
            'action': 'upsert', 'price_krw': 10000}]
        self.assertEqual('2026-10-06T18:00:00.946+00:00',
            make_plan(capture, state)['override_updates'][0]['fields']['confirmed_at'])

    def test_verified_price_change_updates_display_price_together(self):
        capture, state = fixture()
        capture['menu_items'][0].update(price=11000, price_text='11,000원')
        fields = make_plan(capture, state)['updates'][0]['fields']
        self.assertEqual({'price_krw': 11000, 'price_text': '11,000원'}, fields)

    def test_missing_source_fields_preserve_metadata_and_featured(self):
        for description in (None, '', ' \n '):
            for stored_description in ('닭 육수', None):
                with self.subTest(description=description, stored_description=stored_description):
                    capture, state = fixture()
                    state['items'][0]['description'] = stored_description
                    capture['menu_items'][0].update(description=description, image_url=None)
                    capture['quality'].update(source_image_count=0, collected_image_count=0)
                    plan = make_plan(capture, state)
                    self.assertEqual([], plan['updates'])
                    self.assertEqual([], plan['pending'])

    def test_proxy_and_origin_photo_do_not_create_fake_changes(self):
        capture, state = fixture()
        state['items'][0]['image_url'] = 'https://search.pstatic.net/common/?src=https%3A%2F%2Fexample.com%2Fmenu.jpg'
        self.assertEqual([], make_plan(capture, state)['updates'])

    def test_content_description_updates_with_verified_price_and_image(self):
        capture, state = fixture()
        capture['menu_items'][0].update(description='생선 육수', price=11000, price_text='11,000원',
            image_url='https://example.com/latest.jpg', source_image_url='https://example.com/latest.jpg')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual('생선 육수', plan['expected'][0]['description'])
        self.assertEqual(11000, plan['expected'][0]['price_krw'])
        self.assertEqual('11,000원', plan['expected'][0]['price_text'])
        self.assertEqual('https://example.com/latest.jpg', plan['expected'][0]['image_url'])
        self.assertEqual('생선 육수', plan['updates'][0]['fields']['description'])

    def test_description_only_update_allows_verified_section_order(self):
        capture, state = fixture()
        other = copy.deepcopy(state['items'][0])
        other.update(id='00000000-0000-0000-0000-000000000004', name='미소라멘', display_order=1)
        state['items'].append(other)
        source = copy.deepcopy(capture['menu_items'][0])
        source.update(source_id='naver-item-2', name='미소라멘')
        capture['menu_items'][0].update(description='생선 육수', price=11000, price_text='11,000원')
        capture['menu_items'].insert(0, source)
        capture['scope'].insert(0, {'source_id': source['source_id'], 'name': source['name'],
            'decision': 'include', 'reason': '라멘 본품'})
        capture['quality'].update(source_count=2, collected_count=2,
            source_image_count=2, collected_image_count=2)
        plan = make_plan(capture, state)
        self.assertEqual({ROW: 1, other['id']: 0}, {r['id']: r['display_order'] for r in plan['expected']})
        self.assertEqual(11000, next(r for r in plan['expected'] if r['id'] == ROW)['price_krw'])

    def test_case_changes_are_not_treated_as_whitespace_only(self):
        capture, state = fixture()
        state['items'][0]['description'] = 'Shoyu Soup'
        capture['menu_items'][0]['description'] = 'SHOYU SOUP'
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual('SHOYU SOUP', plan['updates'][0]['fields']['description'])

    def test_verified_latest_price_synchronizes_menu_and_manual_record(self):
        capture, state = fixture()
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘',
            'action': 'upsert', 'price_krw': 10000}]
        capture['menu_items'][0].update(price=11000, price_text='11,000원')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual(11000, plan['expected'][0]['price_krw'])
        self.assertEqual(11000, plan['override_updates'][0]['fields']['price_krw'])
        self.assertEqual('admin_verified', plan['override_updates'][0]['fields']['source_type'])
        self.assertEqual(10000, state['overrides'][0]['price_krw'])

    def test_verified_manual_scope_allows_latest_fixed_price_without_new_approval(self):
        capture, state = fixture()
        capture['scope'][0].update(decision='manual', reason='이름·설명만으로 범위 불명')
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘',
            'action': 'upsert', 'source_type': 'admin_verified', 'price_krw': 10000}]
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual([], plan['updates'])
        self.assertEqual([{'name': '쇼유라멘', 'override_id': 'override-1'}], plan['scope_confirmed_by_override'])
        capture['menu_items'][0].update(price=11000, price_text='11,000원')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual(11000, plan['expected'][0]['price_krw'])
        self.assertEqual(11000, plan['override_updates'][0]['fields']['price_krw'])

    def test_verified_latest_photo_synchronizes_manual_photo_record(self):
        capture, state = fixture()
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘',
            'action': 'upsert', 'price_krw': 10000, 'image_url': state['items'][0]['image_url']}]
        capture['menu_items'][0]['image_url'] = 'https://example.com/latest.jpg'
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual('https://example.com/latest.jpg', plan['expected'][0]['image_url'])
        self.assertEqual('https://example.com/latest.jpg', plan['override_updates'][0]['fields']['image_url'])

    def test_description_only_override_difference_is_synchronized(self):
        capture, state = fixture()
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘',
            'action': 'upsert', 'price_krw': 10000, 'description': '別の 육수'}]
        capture['menu_items'][0].update(price=11000, price_text='11,000원')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual(11000, plan['expected'][0]['price_krw'])
        self.assertEqual(11000, plan['override_updates'][0]['fields']['price_krw'])
        self.assertEqual('닭 육수', plan['override_updates'][0]['fields']['description'])

    def test_representative_conflict_still_preserves_menu_and_manual_record(self):
        capture, state = fixture()
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘',
            'action': 'upsert', 'price_krw': 10000, 'is_featured': True}]
        capture['menu_items'][0].update(price=11000, price_text='11,000원')
        plan = make_plan(capture, state)
        self.assertTrue(plan['pending'])
        self.assertEqual([], plan['updates'])
        self.assertEqual([], plan['override_updates'])

    def test_new_price_side_boundary_does_not_reclassify_existing_menus(self):
        for price in (0, 7900, 8000, 8001):
            for registered in (False, True):
                with self.subTest(price=price, registered=registered):
                    capture, state = fixture()
                    capture['menu_items'][0].update(price=price, price_text=f'{price:,}원')
                    if not registered:
                        state['items'] = []
                    plan = make_plan(capture, state)
                    self.assertEqual([], plan['deletions'])
                    self.assertEqual([], plan['pending'])
                    if registered:
                        self.assertEqual(ROW, plan['expected'][0]['id'])
                        self.assertEqual(price, plan['expected'][0]['price_krw'])
                        self.assertEqual([], plan['excluded_new_by_price'])
                    else:
                        self.assertEqual(int(price > 8000), len(plan['additions']))
                        self.assertEqual(int(price <= 8000), len(plan['excluded_new_by_price']))

    def test_explicit_upsert_scope_precedes_new_price_side_threshold(self):
        capture, state = fixture()
        state['items'] = []
        capture['menu_items'][0].update(price=8000, price_text='8,000원')
        capture['scope'][0].update(decision='manual', reason='등록 범위 미확정')
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘',
            'action': 'upsert', 'price_krw': 8000}]
        plan = make_plan(capture, state)
        self.assertEqual(1, len(plan['additions']))
        self.assertEqual([], plan['excluded_new_by_price'])
        self.assertEqual([], plan['pending'])

    def test_new_automatic_ramen_type_still_obeys_price_side_threshold(self):
        capture, state = fixture()
        state['items'] = []
        capture['menu_items'][0].update(name='탄탄면', price=8000, price_text='8,000원')
        capture['scope'][0].update(name='탄탄면', decision='manual')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['additions'])
        self.assertEqual(1, len(plan['excluded_new_by_price']))

    def test_variable_new_price_is_not_invented_for_side_classification(self):
        capture, state = fixture()
        state['items'] = []
        capture['menu_items'][0].update(price=None, price_text='가격 변동')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['excluded_new_by_price'])
        self.assertEqual([], plan['additions'])
        self.assertTrue(plan['pending'])

    def test_suppressed_source_does_not_reappear(self):
        capture, state = fixture()
        state['items'] = []
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘', 'action': 'suppress'}]
        self.assertEqual([], make_plan(capture, state)['additions'])

    def test_existing_excluded_menu_requires_cleanup_instead_of_silent_preservation(self):
        capture, state = fixture()
        state['rules'] = [{'id': 'rule-1', 'shop_id': SHOP, 'match_type': 'name_contains',
            'match_value': '쇼유라멘', 'is_active': True}]
        capture['scope'][0].update(decision='exclude', reason='원본 설명에 볶음밥 명시')
        plan = make_plan(capture, state)
        self.assertEqual('cleanup_required', plan['status'])
        self.assertEqual([ROW], plan['deletion_handoff'][0]['ids'])
        with self.assertRaises(ValueError):
            transaction_sql(plan, '<li>unverified</li>')

    def test_affirmative_deletion_removes_only_exact_source_bound_existing_row(self):
        capture, state = fixture()
        state['items'][0].update(name='챠-항', description='한국인의 디저트, 볶음밥입니다',
            price_krw=5000, price_text='5,000')
        capture['menu_items'][0].update(name='챠-항', description='한국인의 디저트, 볶음밥입니다',
            price=5000, price_text='5,000원')
        capture['scope'][0].update(decision='exclude', reason='원본 설명에 볶음밥 명시',
            name='챠-항', delete_existing=True,
            deletion_evidence='동일 이름·고정 가격·행 ID 확인, 수동 보호 없음')
        state['rules'] = [{'id': 'rule-1', 'shop_id': SHOP, 'match_type': 'name_contains',
            'match_value': '챠-항', 'is_active': True}]
        plan = make_plan(capture, state)
        self.assertEqual('prepared', plan['status'])
        self.assertEqual([ROW], [r['id'] for r in plan['deletions']])
        self.assertEqual([], plan['expected'])
        raw = ('<li><a data-nlog-area="plc_bmv.menu" data-nlog-params="{&quot;place_id&quot;:&quot;123&quot;}">'
            '<span class="lZCdr">챠-항</span><span class="N4UFh">5,000원</span>'
            '<div class="NjVSW">한국인의 디저트, 볶음밥입니다</div><img src="https://example.com/menu.jpg"></a></li>')
        sql = transaction_sql(plan, raw)
        self.assertIn('DELETE FROM public.shop_menu_items WHERE id=', sql)
        self.assertIn("Deletion count mismatch", sql)

    def test_active_manual_upsert_preserves_registered_scope_over_collector_exclusion(self):
        capture, state = fixture()
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘', 'action': 'upsert'}]
        capture['scope'][0].update(decision='exclude', reason='범위 밖',
            delete_existing=True, deletion_evidence='원본 범위 밖')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['deletions'])
        self.assertEqual(1, len(plan['expected']))
        self.assertEqual([], plan['pending'])

    def test_active_exclusion_conflicting_with_upsert_requires_review(self):
        capture, state = fixture()
        state['overrides'] = [{'id': 'override-1', 'shop_id': SHOP, 'normalized_name': '쇼유라멘', 'action': 'upsert'}]
        state['rules'] = [{'id': 'rule-1', 'shop_id': SHOP, 'match_type': 'name_contains',
            'match_value': '쇼유라멘', 'is_active': True}]
        capture['scope'][0].update(decision='exclude', reason='확인된 제외 규칙',
            delete_existing=True, deletion_evidence='동일 이름·가격')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['deletions'])
        self.assertEqual(1, len(plan['expected']))
        self.assertTrue(plan['pending'])

    def test_hidden_event_and_mismatched_price_cannot_use_direct_deletion(self):
        for fields in ({'is_visible': False}, {'event_id': ROW}, {'price_krw': 9000}):
            capture, state = fixture()
            state['items'][0].update(fields)
            state['rules'] = [{'id': 'rule-1', 'shop_id': SHOP, 'match_type': 'name_contains',
                'match_value': '쇼유라멘', 'is_active': True}]
            capture['scope'][0].update(decision='exclude', reason='범위 밖',
                delete_existing=True, deletion_evidence='원본 범위 밖')
            plan = make_plan(capture, state)
            self.assertEqual('cleanup_required', plan['status'])
            self.assertEqual([], plan['deletions'])

    def test_db_only_row_is_preserved_after_source_order(self):
        capture, state = fixture()
        other = {**state['items'][0], 'id': '00000000-0000-0000-0000-000000000004',
            'name': '머쉬룸시오라멘', 'display_order': 0}
        state['items'][0]['display_order'] = 1
        state['items'].append(other)
        plan = make_plan(capture, state)
        self.assertEqual(['머쉬룸시오라멘'], plan['db_only_preserved'])
        self.assertEqual(2, len(plan['expected']))
        self.assertEqual([0, 1], [r['display_order'] for r in plan['expected']])

    def test_variable_price_cannot_replace_fixed_price(self):
        capture, state = fixture()
        capture['menu_items'][0].update(price=None, price_text='변동')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['updates'])
        self.assertEqual(10000, plan['expected'][0]['price_krw'])

    def test_variable_price_does_not_block_existing_photo_refresh(self):
        capture, state = fixture()
        capture['menu_items'][0].update(price=None, price_text='가격 변동', image_url='https://example.com/latest.jpg')
        plan = make_plan(capture, state)
        self.assertEqual(10000, plan['expected'][0]['price_krw'])
        self.assertEqual({'image_url': 'https://example.com/latest.jpg'}, plan['updates'][0]['fields'])
        self.assertTrue(plan['pending'])

    def test_existing_variable_price_preserves_absence_and_refreshes_known_fields(self):
        capture, state = fixture()
        state['items'][0].update(price_krw=None, price_text='변동')
        capture['menu_items'][0].update(price=None, price_text='가격 변동', description='닭\n육수')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertIsNone(plan['expected'][0]['price_krw'])
        self.assertEqual({'price_text': '가격 변동', 'description': '닭\n육수'}, plan['updates'][0]['fields'])

    def test_new_variable_price_is_not_inferred_from_another_menu(self):
        capture, state = fixture()
        state['items'] = []
        capture['menu_items'][0].update(price=None, price_text='가격 변동')
        plan = make_plan(capture, state)
        self.assertEqual([], plan['additions'])
        self.assertTrue(plan['pending'])

    def test_source_supported_photo_duplicate_uses_representative_row_once(self):
        capture, state = fixture()
        first = capture['menu_items'][0]
        first.update(is_featured=True, image_url='https://example.com/latest.jpg')
        second = {**first, 'source_id': 'naver-item-2', 'is_featured': False, 'image_url': 'https://example.com/older.jpg'}
        capture['menu_items'].append(second)
        capture['scope'][0].update(decision='manual', reason='동일 메뉴 사진 차이')
        capture['scope'].append({**capture['scope'][0], 'source_id': second['source_id']})
        capture['quality'].update(collected_count=2, source_count=2, source_image_count=2, collected_image_count=2)
        capture['duplicate_resolutions'] = [{'name': first['name'], 'source_ids': [first['source_id'], second['source_id']],
            'selected_source_id': first['source_id'], 'scope_verified': True, 'evidence': '이름·가격·구성 동일; 대표 원본 우선'}]
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual(1, len(plan['expected']))
        self.assertEqual(2, len(plan['source_items']))
        self.assertEqual('https://example.com/latest.jpg', plan['expected'][0]['image_url'])
        excluded = copy.deepcopy(capture)
        excluded['scope'][1].update(decision='exclude', duplicate_of=first['source_id'])
        self.assertEqual([], make_plan(excluded, state)['pending'])
        self.assertEqual([], make_plan(excluded, state)['deletions'])
        del excluded['scope'][1]['duplicate_of']
        with self.assertRaisesRegex(ValueError, 'conflict'):
            make_plan(excluded, state)
        for field, value in [('price', 12000), ('description', '굵은면 다른 구성'), ('category', '다른 종류')]:
            invalid = copy.deepcopy(capture)
            invalid['menu_items'][1][field] = value
            with self.assertRaisesRegex(ValueError, 'conflict'):
                make_plan(invalid, state)
        invalid = copy.deepcopy(capture)
        invalid['duplicate_resolutions'][0]['selected_source_id'] = second['source_id']
        with self.assertRaisesRegex(ValueError, 'representative'):
            make_plan(invalid, state)

    def test_same_name_multiple_prices_are_held(self):
        capture, state = fixture()
        capture['menu_items'].append({**capture['menu_items'][0], 'price': 12000, 'price_text': '12,000원'})
        capture['scope'].append(copy.deepcopy(capture['scope'][0]))
        capture['quality'].update(collected_count=2, source_count=2, source_image_count=2, collected_image_count=2)
        plan = make_plan(capture, state)
        self.assertEqual([], plan['updates'])
        self.assertEqual([], plan['additions'])
        self.assertEqual(2, len(plan['pending']))

    def test_excluded_duplicates_do_not_create_registration_conflicts(self):
        capture, state = fixture()
        for price in (2000, 2500):
            capture['menu_items'].append({**capture['menu_items'][0], 'source_id': str(price),
                'name': '콜라', 'price': price, 'price_text': str(price)})
            capture['scope'].append({'source_id': str(price), 'name': '콜라', 'decision': 'exclude', 'reason': '음료'})
        capture['quality'].update(collected_count=3, source_count=3, source_image_count=3, collected_image_count=3)
        self.assertEqual([], make_plan(capture, state)['pending'])

    def test_renamed_same_photo_menu_is_not_silently_duplicated(self):
        capture, state = fixture()
        capture['menu_items'][0]['name'] = '특제 쇼유라멘'
        capture['scope'][0]['name'] = '특제 쇼유라멘'
        plan = make_plan(capture, state)
        self.assertEqual([], plan['additions'])
        self.assertTrue(plan['pending'])

    def test_legacy_name_description_join_is_not_added_as_new_menu(self):
        for prefix, price in [('', 10000), ('[강추] ', 10000), ('', 11000)]:
            with self.subTest(prefix=prefix, price=price):
                capture, state = fixture()
                old = state['items'][0]
                old.update(name='쇼유라멘닭 육수', description=None, image_url=None)
                capture['menu_items'][0].update(name=prefix+'쇼유라멘', price=price,
                    price_text=f'{price:,}원')
                capture['scope'][0]['name'] = capture['menu_items'][0]['name']
                plan = make_plan(capture, state)
                self.assertEqual([], plan['additions'])
                self.assertEqual([old['name']], plan['pending'][0]['candidates'])
                self.assertEqual([old], plan['expected'])

    def test_coexisting_flavor_variants_are_added_even_with_shared_photo_and_price(self):
        capture, state = fixture()
        variant = copy.deepcopy(capture['menu_items'][0])
        variant.update(source_id='naver-item-2', name='쇼유라멘(연한맛)')
        capture['menu_items'].append(variant)
        capture['scope'].append({'source_id': variant['source_id'], 'name': variant['name'],
            'decision': 'include', 'reason': '원본에 기본 메뉴와 동시에 표시된 맛 옵션의 라멘 본품'})
        capture['quality'].update(source_count=2, collected_count=2,
            source_image_count=2, collected_image_count=2)
        plan = make_plan(capture, state)
        self.assertEqual([], plan['pending'])
        self.assertEqual(['쇼유라멘(연한맛)'], [row['name'] for row in plan['additions']])
        self.assertEqual(2, len(plan['expected']))

    def test_branch_identity_requires_verification(self):
        capture, state = fixture()
        capture['identity_verified'] = False
        with self.assertRaises(ValueError):
            make_plan(capture, state)

    def test_image_omission_and_misbound_scope_abort(self):
        capture, state = fixture()
        capture['quality']['source_image_count'] = 2
        with self.assertRaises(ValueError):
            make_plan(capture, state)
        capture, state = fixture()
        capture['scope'][0]['source_id'] = 'other-item'
        with self.assertRaises(ValueError):
            make_plan(capture, state)

    def test_broken_collector_cannot_produce_sql(self):
        capture, state = fixture()
        capture['quality']['passed'] = False
        plan = make_plan(capture, state)
        with self.assertRaises(ValueError):
            transaction_sql(plan, '<li>bad</li>')

    def test_plan_does_not_mutate_baseline(self):
        capture, state = fixture()
        old = copy.deepcopy(state)
        capture['menu_items'][0].update(price=11000, price_text='11,000원')
        make_plan(capture, state)
        self.assertEqual(old, state)

    def test_cutoff_is_exclusive_korean_midnight(self):
        sql = select_sql('2026-10-06', 10)
        self.assertIn("u.updated_at < timestamptz '2026-10-06T00:00:00+09:00'", sql)
        self.assertIn('limit 10', sql)

    def test_sql_strings_and_proxy_decode_preserve_original_data(self):
        self.assertEqual("'라멘 ''한정'' $tag$ \\path'", literal("라멘 '한정' $tag$ \\path"))
        self.assertEqual('https://example.com/a/b.jpg', image_identity('https://search.pstatic.net/common/?src=https%3A%2F%2Fexample.com%2Fa%2Fb.jpg'))


if __name__ == '__main__':
    unittest.main()
