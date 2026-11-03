import unittest

from deletion_guards import ORIGIN_CHECKS, evidence_hash, validate_origin


class MediaDeletionGuardsTest(unittest.TestCase):
    def test_Firebase_관리_사진은_등록_근거_조사_없이_삭제하지_않는다(self):
        row = {'id': 'menu', 'shop_id': 'shop', 'name': '라멘', 'image_url': 'https://storage.googleapis.com/ramap-media/menu-images/photo.jpg'}
        review = {
            'menu_item_id': 'menu', 'shop_id': 'shop', 'name': '라멘',
            'before_sha256': evidence_hash(row), 'overrides_sha256': evidence_hash([]),
            'outcome': 'not_direct_registration', 'reason': '확인 완료', 'unresolved_clues': [],
            'checked': list(ORIGIN_CHECKS), 'aliases_reviewed': True,
            'evidence_refs': [{'ref': 'source', 'sha256': 'a' * 64}],
            'clue_resolutions': {'registration_audit': '확인', 'human_input': '확인', 'aliases': '확인'},
        }
        with self.assertRaisesRegex(ValueError, 'registration clues'):
            validate_origin(row, [review], [])
        review['clue_resolutions']['stored_media'] = 'Firebase 사진의 등록 근거 확인 완료'
        self.assertIs(review, validate_origin(row, [review], []))
