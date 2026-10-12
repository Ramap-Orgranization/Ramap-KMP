---
type: knowledge-candidate
authority: none
status: rejected
created_at: 2026-10-13
updated_at: 2026-10-13
target_path: null
decision_id: null
resolution_reason: "후속 구현과 검증으로 관찰된 누락을 수정했습니다. 확정 정책이 아니라 구현 사실이므로 대상 reference 페이지에 반영하고, 해결된 후보 파일은 미해결 후보로 유지하지 않습니다."
---

# 새 리뷰 삽입 시 매장 헤더 정보 누락

## 내용과 근거

2026-10-13(Asia/Seoul), `4736aa00 + working-tree`의 `1.1.9-debug`를 Android 16 실기기에서 검증했습니다. 내 리뷰 목록에서 매장 상세를 열어 사진 있는 임시 비공개 리뷰를 작성한 뒤 상세를 닫으면, 새 리뷰에는 매장 이름만 표시되고 썸네일·주소·카테고리가 빠졌습니다. 공개로 저장한 후 공개 필터에서 서버 조회를 거치면 해당 정보가 표시됐습니다. 사진 업로드·교체와 공개 설정 저장은 성공했지만 새 행의 헤더 표시를 완료로 판단하지 않았습니다.

- [ReviewMutationResponse](../../../data/src/commonMain/kotlin/com/peto/ramap/data/model/ReviewMutationResponse.kt)는 변경 응답에서 리뷰 projection·매장 이름·moderation 설명을 domain snapshot으로 옮깁니다.
- [ReviewChangeReducer](../../../domain/src/commonMain/kotlin/com/peto/ramap/domain/model/review/change/ReviewChangeReducer.kt)의 `myReviews`는 행 ID별 기존 metadata에서 `old?.shop`을 가져옵니다. 새 리뷰 ID에는 기존 행이 없으므로 `shop`이 null입니다. 같은 매장인 다른 행의 metadata도 재사용하지 않습니다.
- [ShopReviewCardHeader](../../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/review/ShopReviewCardHeader.kt)는 `shop == null`이면 매장 이름을 회색 텍스트로 표시합니다. 이때 매장 ID에 의한 상세 열기 콜백은 유지됩니다.

후속 수정에서 저장 응답의 매장 ID를 기준으로 필요한 매장 요약만 조회해 변경 snapshot과 이미지 보완 경로에 포함했습니다. reducer는 새 행에서도 매장 thumbnail·주소·카테고리를 즉시 표시하고 기존 사진·본문·매장 정보를 보존합니다. 매장 조회 실패나 취소가 저장 성공을 뒤집거나 저장을 재실행하지 않습니다. 단건 내 리뷰 조회도 같은 매장 요약을 포함합니다. 서버/API 계약 변경은 없습니다.

2026-10-13(Asia/Seoul)에 Android 16 실기기에서 기존 목록에 있던 매장에 새 리뷰를 작성하고 첫 복귀 화면에서 매장 사진·주소·카테고리를 확인했습니다. 새 리뷰 ID와 매장 ID의 조합이 새 행인 경우를 확인했습니다. 목록에 없던 매장은 단위 테스트로 확인했습니다. 상세 열기와 기존 사진 유지, 목록 전체 재조회 없이 매장 정보를 표시하는 경로를 검증했습니다. APK 해시와 비식별화된 상세 증거는 로컬 전용 `docs/change-history/review-new-shop-metadata-20261013/`에 보관합니다.

## 실제 사용 이력

| 작업 또는 이슈 | 날짜 | 판단에 사용한 맥락과 결과 |
| --- | --- | --- |
| 리뷰 사진·공개 설정·계정 전환 실기기 검증 | 2026-10-13 | 새 행에 이름만 보이는 단말 결과와 reducer의 `old?.shop`을 대조하고, 미수정 표시 문제로 기록 |

## 처리

후속 구현으로 해결했습니다. 정책으로 승격하지 않았으며 대상 reference 문서에 현재 구현과 검증 범위를 반영했습니다. 운영 API 변경은 없습니다.
