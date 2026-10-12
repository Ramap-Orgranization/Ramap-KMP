---
authority: reference
status: active
last_verified: 2026-10-13
source_ref: c559e52c3248076dacab2f2928622d9d787bc1e5
---

# 디자인시스템 탐색

공통 Compose 표현은 [core/designsystem](../../core/designsystem)에 있습니다. UI 작성 규칙은 로컬 `ramap-coding-conventions`와 `ramap-design-patterns` 스킬이 소유합니다.

| 필요 | 기존 구현 |
| --- | --- |
| 테마, Preview의 navigation dispatcher | [RamapTheme](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/theme/Theme.kt) |
| 텍스트 스타일·폰트·색상 | [AppTextStyle](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/theme/AppTextStyle.kt), [Typography](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/theme/Typography.kt), [Colors](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/theme/Colors.kt) |
| 스타일을 지정하는 텍스트 | [AppText](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/text/AppText.kt) |
| 일반·로딩·비활성 버튼 | [AppButton](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/button/AppButton.kt) |
| 상단 바·다이얼로그·바텀시트 | [CommonTopBar](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/topbar/CommonTopBar.kt), [CommonDialog](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/dialog/CommonDialog.kt), [CommonBottomSheet](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/bottomsheet/CommonBottomSheet.kt) |
| 리뷰·프로필·영업 공지 표현 | [review](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/review), [profile](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/profile), [notice](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/notice) |
| 문자열·이미지 리소스 | [composeResources](../../core/designsystem/src/commonMain/composeResources) |
| Preview용 데이터 | [core/preview](../../core/preview) |

`AppButton`은 로딩 중 클릭을 막고 진행 표시를 렌더링합니다. `RamapTheme`은 앱 타이포그래피를 제공하며 Preview에서 필요한 navigation dispatcher를 보완합니다. 정확한 인자와 표현은 연결한 구현에서 확인합니다.

## Resource mapper의 실제 위치

현재 mapper는 다음 두 모듈이 소유합니다.

| 소유 모듈 | 구현된 표현 |
| --- | --- |
| [core/ui/resource/area](../../core/ui/src/commonMain/kotlin/com/peto/ramap/ui/resource/area) | 행정구역 UI 모델, 지역 필터 mapper |
| [designsystem/resource](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/resource) | UiText·formatter, 카테고리, 이벤트, 영업시간, 가게 정보, 운영 공지, 대기 방식, 신고 사유 |

[RankingFilters](../../feature/main/ranking/src/commonMain/kotlin/com/peto/ramap/ui/main/ranking/component/RankingFilters.kt)는 두 모듈의 mapper를 함께 사용합니다. 디자인시스템 컴포넌트는 자신이 소유한 mapper를 직접 사용하며 `core/ui`에 의존하지 않습니다. 테스트의 package 이름만으로 소유 모듈을 판단하지 않습니다.

이 배치를 현재 기준으로 확정하고 아키텍처·모듈·패턴 스킬에 반영했습니다([DEC-0002](decisions.md#dec-0002-현재-구현을-기준으로-개발-규칙-정합성-확정)). 상세 배치 규칙의 원천은 해당 스킬입니다.

## 가게 리뷰 목록 카드

2026-10-12 작업 트리에서 확인한 범위는 공통 가게 리뷰 카드와 두 리뷰 목록의 사용 경로입니다. 2026-10-13에 최종 PR 코드 커밋 `c559e52c3248076dacab2f2928622d9d787bc1e5` 및 관련 카드 통합을 다시 확인했습니다. 나머지 설명의 기존 확인 기준은 유지합니다.

[ShopReviewCard](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/review/ShopReviewCard.kt)는 기본 [ReviewCard](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/review/ReviewCard.kt)에 [ShopReviewCardHeader](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/review/ShopReviewCardHeader.kt)를 조합합니다. 내 리뷰와 다른 사람 리뷰 목록이 같은 가게 요약을 사용하며, 가게가 없으면 내 리뷰는 기존 가게 이름을, 다른 사람 리뷰는 기존 상세 오류 문구를 표시합니다. 수정·삭제 등의 메뉴는 작성자 헤더와 가게 헤더가 [ReviewCardOverflowMenu](../../core/designsystem/src/commonMain/kotlin/com/peto/ramap/designsystem/review/ReviewCardOverflowMenu.kt)를 공유합니다.

내 리뷰는 카드 선택과 사진 확대를 각각 지원합니다. 다른 사람 리뷰의 기존 사진 선택 동작은 카드 선택으로 유지하며, 사진 보기 여부는 카드 인자로 지정합니다. 본문 더보기와 메뉴 같은 내부 클릭은 카드 선택과 독립적으로 처리합니다.
