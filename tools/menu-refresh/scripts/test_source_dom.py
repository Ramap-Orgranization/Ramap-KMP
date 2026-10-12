import copy
import json
import unittest
from html import escape

from refresh_plan import make_plan, transaction_sql
from source_dom import reconcile_source
from test_refresh_plan import fixture


URL = 'https://m.place.naver.com/restaurant/123/menu/list'


def item(name='쇼유라멘', price=10000, image='https://example.com/menu.jpg'):
    return {'name': name, 'price': price, 'price_text': f'{price:,}원' if price is not None else '가격 변동',
        'description': '닭 육수', 'image_url': image, 'is_featured': False}


def row(source, recommended=False, place_id='123'):
    params = escape(json.dumps({'place_id': place_id}), quote=True)
    area = 'recmenu' if recommended else 'menu'
    image = f'<img src="{escape(source["image_url"], quote=True)}">' if source['image_url'] else ''
    return (f'<li><a data-nlog-area="plc_bmv.{area}" data-nlog-params="{params}">'
        f'<span class="lZCdr">{escape(source["name"])}</span>'
        f'<span class="N4UFh">{escape(source["price_text"])}</span>'
        f'<div class="NjVSW">{escape(source["description"])}</div>{image}</a></li>')


class SourceDOMTest(unittest.TestCase):
    def test_reordered_arrays_cannot_generate_sql_against_unchanged_dom(self):
        capture, state = fixture()
        first, second = item(), item('시오라멘', 11000, 'https://example.com/shio.jpg')
        raw = '<ul>' + row(first) + row(second) + '</ul>'
        capture['menu_items'] = [second, first]
        capture['scope'] = [{'name': i['name'], 'source_id': None, 'decision': 'include', 'reason': '라멘'} for i in capture['menu_items']]
        capture['quality'].update(source_count=2, collected_count=2, source_image_count=2, collected_image_count=2)
        with self.assertRaisesRegex(ValueError, 'order/name'):
            transaction_sql(make_plan(capture, state), raw)

    def test_recommendation_first_removes_only_repeated_regular_recommendation(self):
        first, second = item(), item('시오라멘')
        raw = row(second) + row(first) + row(first, recommended=True)
        result = reconcile_source([first, second], raw, URL)
        self.assertEqual(1, result['recommendation_duplicates'])

    def test_regular_duplicate_photos_are_preserved(self):
        first, second = item(), item(image='https://example.com/alternate.jpg')
        raw = row(first) + row(second)
        self.assertEqual(2, reconcile_source([first, second], raw, URL)['canonical_rows'])
        with self.assertRaisesRegex(ValueError, 'count'):
            reconcile_source([first], raw, URL)

    def test_price_and_photo_swaps_are_rejected_even_with_matching_counts(self):
        original = item()
        for key, value in [('price', 11000), ('image_url', 'https://example.com/other.jpg')]:
            changed = {**original, key: value}
            with self.assertRaisesRegex(ValueError, 'price|image'):
                reconcile_source([changed], row(original), URL)

    def test_variable_price_label_cannot_be_replaced_with_an_inferred_fixed_price(self):
        variable = item(price=None)
        with self.assertRaisesRegex(ValueError, 'price'):
            reconcile_source([item()], row(variable), URL)

    def test_summary_is_excluded_only_when_actual_categories_have_same_complete_item(self):
        original = item()
        summary = '<div class="place_section"><h2 class="place_section_header_title">대표메뉴</h2>' + row(original) + '</div>'
        result = reconcile_source([original], summary + row(original), URL)
        self.assertEqual(1, result['summary_rows'])
        with self.assertRaisesRegex(ValueError, 'missing from actual categories'):
            reconcile_source([original], summary, URL)

    def test_legacy_duplicate_complete_panels_do_not_remove_duplicate_rows(self):
        first, second = item(), item(image='https://example.com/alternate.jpg')
        panel = '<ul>' + row(first) + row(second) + '</ul>'
        raw = json.dumps({'menu_sections': [panel, panel]})
        result = reconcile_source([first, second], raw, URL)
        self.assertEqual(1, result['duplicate_complete_panels'])
        self.assertEqual(2, result['canonical_rows'])

    def test_raw_branch_id_and_description_must_match(self):
        original = item()
        with self.assertRaisesRegex(ValueError, 'branch ID'):
            reconcile_source([original], row(original, place_id='456'), URL)
        changed = copy.deepcopy(original)
        changed['description'] = '다른 육수'
        with self.assertRaisesRegex(ValueError, 'description'):
            reconcile_source([changed], row(original), URL)


if __name__ == '__main__':
    unittest.main()
