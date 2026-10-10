---
authority: reference
status: active
last_verified: 2026-10-11
source_ref: 33a0d49c2731e42144f2c79704748d7c7e0cf68b
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
