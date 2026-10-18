import copy
import json
import tempfile
import unittest
import uuid
from datetime import datetime, timezone
from html import escape
from pathlib import Path

from refresh_plan import digest, prepare_capture
from refresh_run import accept, drain, fail_collection, initialize, load, next_action, record_review, recover, retry, save, submit
from refresh_verify import VISIBLE_FIELDS, expected_visible, result_rows, verify_plan
from replay_run import ReplayDatabase
from test_refresh_plan import SHOP, fixture


def raw_for(capture):
    rows = []
    for item in capture['menu_items']:
        description = '' if item.get('description') is None else '<div class="NjVSW">' + escape(item['description']) + '</div>'
        image = '<img src="' + escape(item['image_url'], quote=True) + '">' if item.get('image_url') else ''
        featured = '<span class="place_blind">대표</span>' if item.get('is_featured') else ''
        rows.append('<li><a data-nlog-area="plc_bmv.menu" data-nlog-params="{&quot;place_id&quot;:&quot;123&quot;}">'
            '<span class="lZCdr">' + escape(item['name']) + '</span><span class="N4UFh">' + escape(item['price_text'])
            + '</span>' + description + image + featured + '</a></li>')
    return ''.join(rows)


def state_fixture():
    capture, state = fixture()
    state['visible'] = [{k: row.get(k) for k in VISIBLE_FIELDS} for row in state['items']]
    state['menu_timestamps'] = []
    return capture, state


class RefreshRunTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.run = self.root / 'run'
        self.capture, self.state = state_fixture()
        self.selection = [self.state['shops'][0]]

    def setup_run(self, rehearsal='deletions', confirmations=(), mode='replay', second=None, description_approvals=()):
        selection = self.selection + ([second] if second else [])
        initialize(self.run, selection, 'offline', mode=mode, rehearsal=rehearsal,
            confirmations=confirmations, description_approvals=description_approvals)
        self.db = ReplayDatabase(self.run, self.state)

    def submit_capture(self, capture=None, name='input', collector='/collector'):
        capture = copy.deepcopy(capture or self.capture)
        directory = self.root / name
        directory.mkdir()
        capture['raw_html_path'] = str(directory / 'raw-menu.html')
        (directory / 'raw-menu.html').write_text(raw_for(capture))
        if capture.get('source_absence_review'):
            capture['source_absence_review']['raw_sha256'] = digest(raw_for(capture))
        save(directory / 'result.json', capture)
        submit(self.run, directory / 'result.json', collector)

    def manifest(self):
        return load(self.run / 'manifest.json')

    def shop(self, shop_id=SHOP):
        return self.manifest()['shops'][shop_id]

    def process(self):
        return drain(self.run, self.db)

    def test_잘못된_기대값에도_원본에_없는_비보호_잔존_행을_검출한다(self):
        plan, _ = prepare_capture(self.capture, self.state, raw_for(self.capture))
        extra = {**self.state['items'][0], 'id': str(uuid.uuid4()), 'name': '과거냉라멘'}
        self.state['items'].append(extra)
        self.state['visible'].append({k: extra.get(k) for k in VISIBLE_FIELDS})
        plan['expected'].append(extra)
        plan['expected_visible'] = self.state['visible']
        result = verify_plan(plan, self.state, [])
        self.assertIn('unprotected menu absent from Naver: ' + extra['id'], result['issues'])

    def test_잘못된_기대값에도_직접_등록_보호_행의_삭제를_검출한다(self):
        extra = {**self.state['items'][0], 'id': str(uuid.uuid4()), 'name': '직접등록냉라멘'}
        self.state['items'].append(extra)
        self.state['manual_menu_protections'] = [{'id': str(uuid.uuid4()), 'shop_id': SHOP,
            'menu_item_id': extra['id'], 'name': extra['name'], 'authority': 'human_user',
            'kind': 'direct_registration', 'status': 'active',
            'evidence_ref': 'user-message:direct-registration', 'reason': '사용자 직접 등록 확인'}]
        self.capture['source_absence_review']['raw_sha256'] = digest(raw_for(self.capture))
        plan, _ = prepare_capture(self.capture, self.state, raw_for(self.capture))
        self.state['items'] = [row for row in self.state['items'] if row['id'] != extra['id']]
        plan['expected'] = copy.deepcopy(self.state['items'])
        plan['expected_visible'] = self.state['visible']
        result = verify_plan(plan, self.state, [])
        self.assertIn('directly registered menu deleted: ' + extra['id'], result['issues'])

    def test_verified_price_is_applied_without_approval_and_second_drain_does_not_replay(self):
        self.setup_run()
        self.capture['menu_items'][0].update(price=11000, price_text='11,000원')
        self.submit_capture()
        result = self.process()
        self.assertEqual('awaiting_independent_review', result['status'])
        self.assertEqual(11000, self.db.state['items'][0]['price_krw'])
        before = len(self.db.actions)
        self.process()
        self.assertEqual(before, len(self.db.actions))
        self.assertEqual(1, sum(a['kind'] == 'commit' for a in self.db.actions))

    def test_completed_collection_is_processed_while_other_shop_is_still_collecting(self):
        self.setup_run(second={**self.selection[0], 'id': str(uuid.uuid4()), 'name': '두 번째 매장'})
        self.submit_capture()
        result = self.process()
        self.assertEqual('verified', self.shop()['status'])
        self.assertEqual(1, len(result['waiting_capture']))
        self.assertEqual(1, result['verified_shops'])

    def test_ready_shops_share_one_prewrite_read(self):
        second_id = str(uuid.uuid4())
        second_capture, second_state = state_fixture()
        second_capture['shop_id'] = second_id
        second_state['shops'][0]['id'] = second_id
        for key in ('sections', 'items', 'visible'):
            for row in second_state[key]:
                row['shop_id'] = second_id
                row['id'] = str(uuid.uuid4())
        second_state['items'][0]['section_id'] = second_state['sections'][0]['id']
        second_state['visible'][0] = {k: second_state['items'][0].get(k) for k in VISIBLE_FIELDS}
        for key in self.state:
            self.state[key] += second_state[key]
        self.setup_run(second=second_state['shops'][0])
        self.submit_capture()
        self.submit_capture(second_capture, name='input-two')
        result = self.process()
        self.assertEqual(2, result['verified_shops'])
        reads = [a for a in self.db.actions if a['kind'] == 'prewrite']
        self.assertEqual(1, len(reads))
        self.assertEqual(2, len(reads[0]['shop_ids']))

    def test_commit_acknowledgement_loss_recovers_without_a_second_commit(self):
        self.setup_run()
        self.submit_capture()
        lost = False
        def query(action, sql):
            nonlocal lost
            result = self.db(action, sql)
            if action['kind'] == 'commit' and not lost:
                lost = True
                raise TimeoutError('ack lost after commit')
            return result
        result = drain(self.run, query)
        self.assertEqual(1, result['verified_shops'])
        self.assertEqual(1, sum(a['kind'] == 'commit' for a in self.db.actions))
        self.assertEqual(1, sum(a['kind'] == 'recover' for a in self.db.actions))

    def test_interrupted_action_is_not_reissued_until_explicit_recovery(self):
        self.setup_run()
        self.submit_capture()
        drain(self.run, self.db, max_actions=2)
        action = next_action(self.run)
        self.assertEqual('commit', action['kind'])
        self.assertEqual('await_result', next_action(self.run)['kind'])
        self.db(action, Path(action['sql_path']).read_text())
        recover(self.run)
        self.process()
        self.assertEqual('verified', self.shop()['status'])
        self.assertEqual(1, sum(a['kind'] == 'commit' for a in self.db.actions))

    def test_missing_commit_requires_diagnosis_and_only_one_retry(self):
        self.setup_run()
        self.submit_capture()
        drain(self.run, self.db, max_actions=2)
        self.assertEqual('commit', next_action(self.run)['kind'])
        recover(self.run)
        self.process()
        self.assertEqual('failed', self.shop()['status'])
        with self.assertRaises(ValueError):
            retry(self.run, SHOP, '')
        retry(self.run, SHOP, 'transport stopped before executing commit; snapshot confirmed absent')
        self.process()
        self.assertEqual('verified', self.shop()['status'])
        with self.assertRaises(ValueError):
            retry(self.run, SHOP, 'try again')

    def test_explicit_exclusion_deletion_runs_exact_guards_with_rollback_first(self):
        self.state['rules'] = [{'id': 'rule-1', 'shop_id': SHOP, 'match_type': 'name_contains',
            'match_value': '쇼유라멘', 'is_active': True}]
        self.setup_run()
        self.capture['scope'][0].update(decision='exclude', reason='확인된 범위 밖',
            delete_existing=True, deletion_evidence='동일 원본·DB 이름과 가격 및 수동 보호 부재 확인')
        self.submit_capture()
        self.process()
        self.assertEqual([], self.db.state['items'])
        kinds = [a['kind'] for a in self.db.actions]
        self.assertLess(kinds.index('rollback'), kinds.index('commit'))
        rollback_action = next(a for a in load_events(self.run) if a.get('stage') == 'rollback' and a['kind'] == 'query_issued')
        sql = (self.run / 'actions' / (rollback_action['action_id'] + '.sql')).read_text()
        self.assertIn('Deletion count mismatch', sql)
        self.assertIn('Post-write menu mismatch', sql)
        self.assertIn('\nROLLBACK;\nSELECT ', sql)
        self.assertNotIn('\nCOMMIT;\n', sql)

    def test_scope_confirmation_and_new_menu_share_one_transaction(self):
        self.capture['scope'][0].update(decision='manual', reason='이름·설명만으로 범위 불명')
        self.state['items'], self.state['visible'] = [], []
        confirmation = {'shop_id': SHOP, 'name': '쇼유라멘', 'source_id': 'naver-item-1',
            'confirmed_at': '2026-10-07T11:00:00+09:00', 'evidence': '사용자가 해당 메뉴의 등록 범위를 확정함'}
        self.setup_run(confirmations=[confirmation])
        self.submit_capture()
        result = self.process()
        self.assertEqual(1, result['added_rows'])
        self.assertEqual(1, len(self.db.state['overrides']))
        self.assertEqual('admin_verified', self.db.state['overrides'][0]['source_type'])
        self.assertEqual('2026-10-07T02:00:00+00:00', self.db.state['overrides'][0]['confirmed_at'])
        shop = self.shop()
        sql = (self.run / shop['directory'] / 'prepared/apply.sql').read_text()
        self.assertIn('INSERT INTO public.shop_menu_manual_overrides', sql)
        self.assertIn('INSERT INTO public.shop_menu_items', sql)
        self.assertEqual(1, sum(a['kind'] == 'commit' for a in self.db.actions))

    def test_separate_root_description_approval_is_used_by_queue(self):
        self.capture['menu_items'][0]['description'] = '닭 육수와 차슈 2pcs'
        approval = {'shop_id':SHOP, 'menu_item_id':self.state['items'][0]['id'],
            'name':'쇼유라멘', 'source_id':'naver-item-1', 'before_description':'닭 육수',
            'source_description':'닭 육수와 차슈 2pcs', 'authority':'human_user',
            'reason':'해당 문구 변경을 사용자가 승인'}
        self.setup_run(description_approvals=[approval])
        self.submit_capture()
        self.process()
        self.assertEqual('닭 육수와 차슈 2pcs',self.db.state['items'][0]['description'])
        self.assertEqual([],self.shop()['summary']['pending'])

    def test_scope_confirmation_rejects_wrong_source_or_existing_suppression(self):
        confirmation = {'shop_id': SHOP, 'name': '쇼유라멘', 'source_id': 'wrong-id',
            'confirmed_at': '2026-10-07T02:00:00Z', 'evidence': '해당 메뉴 범위 확인'}
        with self.assertRaises(ValueError):
            prepare_capture(self.capture, self.state, raw_for(self.capture), confirmations=[confirmation])
        confirmation['source_id'] = 'naver-item-1'
        self.state['overrides'] = [{'id': str(uuid.uuid4()), 'shop_id': SHOP, 'normalized_name': '쇼유라멘', 'action': 'suppress'}]
        with self.assertRaises(ValueError):
            prepare_capture(self.capture, self.state, raw_for(self.capture), confirmations=[confirmation])

    def test_changed_raw_capture_cannot_generate_or_execute_sql(self):
        self.setup_run()
        self.submit_capture()
        path = self.run / self.shop()['directory'] / 'raw-menu.html'
        path.write_text(path.read_text().replace('10,000원', '9,000원'))
        self.process()
        self.assertEqual('blocked', self.shop()['status'])
        self.assertFalse(any(a['kind'] == 'commit' for a in self.db.actions))

    def test_unsafe_raw_path_rejected_before_db_access(self):
        self.setup_run()
        directory = self.root / 'unsafe'
        directory.mkdir()
        path = self.root / 'outside.html'
        path.write_text(raw_for(self.capture))
        self.capture['raw_html_path'] = str(path)
        save(directory / 'result.json', self.capture)
        with self.assertRaises(ValueError):
            submit(self.run, directory / 'result.json', '/collector')
        self.assertEqual(0, self.manifest()['db_calls'])

    def test_old_capture_rejected_in_live_mode_and_replay_transport_is_separate(self):
        self.setup_run(mode='live')
        self.capture['captured_at'] = '2020-01-01T00:00:00Z'
        with self.assertRaises(ValueError):
            self.submit_capture()
        self.assertEqual('waiting_capture', self.shop()['status'])

    def test_actual_post_write_menu_mismatch_stops_without_auto_retry(self):
        self.setup_run()
        self.submit_capture()
        def query(action, sql):
            result = self.db(action, sql)
            if action['kind'] == 'verify':
                result['rows'][0]['verification']['state']['items'][0]['price_krw'] = 999
            return result
        drain(self.run, query)
        self.assertEqual('verification_failed', self.shop()['status'])
        with self.assertRaises(ValueError):
            retry(self.run, SHOP, 'ignore mismatch')
        self.assertEqual(1, sum(a['kind'] == 'commit' for a in self.db.actions))

    def test_independent_review_is_bound_to_writer_collector_snapshot_and_plan(self):
        self.setup_run()
        self.submit_capture()
        self.process()
        handoff = load(self.run / 'review-handoff.json')['shops'][0]
        entry = {k: handoff[k] for k in ('shop_id', 'snapshot_id', 'plan_hash')}
        entry.update(result='pass', findings=[])
        for reviewer in ('/root', '/collector'):
            with self.assertRaises(ValueError):
                record_review(self.run, {'reviewer': reviewer, 'shops': [entry]})
        with self.assertRaises(ValueError):
            record_review(self.run, {'reviewer': '/reviewer', 'shops': [{**entry, 'snapshot_id': str(uuid.uuid4())}]})
        record_review(self.run, {'reviewer': '/reviewer', 'shops': [entry]})
        result = self.process()
        self.assertEqual('completed', result['status'])
        self.assertEqual(1, result['reviewed_shops'])
        self.assertEqual([], load(self.run / 'review-handoff.json')['shops'])

    def test_duplicate_response_cannot_advance_the_queue_twice(self):
        self.setup_run()
        self.submit_capture()
        action = next_action(self.run)
        response = self.db(action, Path(action['sql_path']).read_text())
        accept(self.run, action['id'], response)
        with self.assertRaises(ValueError):
            accept(self.run, action['id'], response)

    def test_collection_failure_is_reported_and_can_return_to_collection_once(self):
        self.setup_run()
        result = fail_collection(self.run, SHOP, 'rendered source has no usable menu after fallback')
        self.assertEqual(1, result['failures'])
        self.assertEqual(0, result['verified_shops'])
        retry(self.run, SHOP, 'source became available; collect a new complete capture')
        self.assertEqual('waiting_capture', self.shop()['status'])
        self.submit_capture()
        self.process()
        self.assertEqual('verified', self.shop()['status'])

    def test_plan_changed_after_commit_is_not_used_for_a_false_verification(self):
        self.setup_run()
        self.submit_capture()
        drain(self.run, self.db, max_actions=3)
        self.assertEqual('verifying', self.shop()['status'])
        plan_path = self.run / self.shop()['directory'] / 'prepared/plan.json'
        plan = load(plan_path)
        plan['snapshot_id'] = str(uuid.uuid4())
        save(plan_path, plan)
        self.process()
        self.assertEqual('verification_failed', self.shop()['status'])
        self.assertFalse(any(a['kind'] == 'verify' for a in self.db.actions))


def load_events(run):
    return [json.loads(line) for line in (run / 'events.jsonl').read_text().splitlines()]


class RefreshVerificationTest(unittest.TestCase):
    def test_mcp_untrusted_wrapper_is_parsed_as_data(self):
        data = [{'value': '$(touch /tmp/never-executed)'}]
        wrapper = {'content': [{'type': 'text', 'text': json.dumps({'result': '<untrusted-data-test>\n'
            + json.dumps(data) + '\n</untrusted-data-test>'})}]}
        self.assertEqual(data, result_rows(wrapper))

    def test_error_and_non_row_results_are_rejected(self):
        for response in ({'isError': True, 'content': []}, {'result': 'not JSON'}, {'rows': None}):
            with self.assertRaises((ValueError, json.JSONDecodeError)):
                result_rows(response)

    def test_visible_rows_are_checked_independently_of_regular_items(self):
        capture, state = state_fixture()
        plan, sql = prepare_capture(capture, state, raw_for(capture))
        plan['expected_visible'] = expected_visible(plan, state['visible'])
        snapshot = {k: plan[k] for k in ('shop_id', 'captured_at', 'source_url', 'parser_version', 'source_hash', 'menu_fingerprint')}
        snapshot.update(id=plan['snapshot_id'], raw_hash=plan['source_hash'], menu_items=plan['source_items'])
        self.assertTrue(verify_plan(plan, state, [snapshot])['passed'])
        state['visible'][0]['image_url'] = None
        self.assertIn('visible menu mismatch', verify_plan(plan, state, [snapshot])['issues'])

    def test_snapshot_raw_hash_source_order_and_item_count_are_checked(self):
        capture, state = state_fixture()
        plan, sql = prepare_capture(capture, state, raw_for(capture))
        plan['expected_visible'] = expected_visible(plan, state['visible'])
        snapshot = {k: plan[k] for k in ('shop_id', 'captured_at', 'source_url', 'parser_version', 'source_hash', 'menu_fingerprint')}
        snapshot.update(id=plan['snapshot_id'], raw_hash='wrong', menu_items=[])
        result = verify_plan(plan, state, [snapshot])
        self.assertFalse(result['passed'])
        self.assertIn('snapshot raw source hash mismatch', result['issues'])
        self.assertIn('snapshot source items mismatch', result['issues'])


if __name__ == '__main__':
    unittest.main()
