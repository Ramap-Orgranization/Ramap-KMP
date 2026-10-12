# 메뉴 최신화 작업 도구

이 폴더에 메뉴 최신화 과정에서 만든 재사용 도구와 작업 코드 보관본을 모았다. 다음 작업은 [SKILL.md](SKILL.md)의 기준을 적용하고 공통 도구를 재사용한다. 기존 `skills/menu-refresh`는 이 폴더로 연결된다.

| 위치 | 내용 |
| --- | --- |
| `scripts/` | 수집, 원본 대조, 대상 선정, 가드 SQL, 실행·재개, 사후 검증, 체크 승인 추출과 테스트 |
| `assets/review-checklist.html` | 보류 메뉴 체크 승인표 템플릿 |
| `data/collection-skips.json` | 최신 DB 감사로 검증한 재수집 제외 캐시 |
| [references/workflow.md](references/workflow.md) | 배치 실행·수집·복구 명령과 입력 계약 |
| [references/pending-review.md](references/pending-review.md) | 보류 점검, 승인표 생성, 선택만 반영, 통합 목록 갱신 |
| `work-files/menu-refresh/`, `menu-review/`, `shop-maintenance/` | 수집 중 작성한 배치별 코드와 실험 코드 보관본 |
| `work-files/output/` | 당시 파일럿 보고서와 스킬 ZIP |
| [work-files/manifest.json](work-files/manifest.json) | 보관본의 원래 경로·SHA-256·용도 |

Ramap 루트에서 실행한다.

```bash
python3 tools/menu-refresh/scripts/refresh_plan.py select-sql --before 2026-10-06 --limit 10
python3 tools/menu-refresh/scripts/refresh_run.py --help
python3 tools/menu-refresh/scripts/review_checklist.py build --help
```

`work-files/`는 당시 입력·매장·승인·실행에 연결된 코드 보관본이다. 필요한 구현을 참고할 때 원래 경로와 근거를 함께 확인한다. 새 수집과 DB 반영은 `scripts/`의 공통 도구에 새 실행 폴더와 최신 DB 상태를 전달한다. 과거 완료 배치의 SQL을 새 승인처럼 재실행하지 않는다. 원본 DOM, 동결 승인 자료, 실행 감사와 DB 결과는 원래 `.artifacts/` 경로에 보존했다.

검증된 기존 도구를 배치마다 다시 만들지 않는다. 코드 변경 시에는 다음 테스트와 [저장 자료 replay](references/workflow.md#저장-자료-재현)를 수행한다.

```bash
python3 -m unittest discover -s tools/menu-refresh/scripts -p 'test_*.py'
```

이동 전에 고정한 실행은 기존 버전 기록을 보존한다. 도구 경로·정책 버전이 달라졌다면 완료 대상을 재실행하지 않고 필요한 미완료 대상만 새 실행으로 이어간다.
