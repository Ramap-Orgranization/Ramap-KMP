# 실행 산출물과 도구

스킬의 helper는 Python 표준 라이브러리만 사용한다. DB 인증은 연결된 Supabase MCP 또는 현재 저장소에 연결된 Supabase CLI로 수행하고 키를 출력하거나 산출물에 저장하지 않는다. CLI 명령은 현재 버전의 `--help`로 확인한다.

보류 상태 재점검·체크 승인표 생성·선택 반영은 [pending-review.md](pending-review.md)를 읽는다. 등록 범위와 개별 매장 결정은 Ramap의 `docs/menu-registration-policy.md`를 따른다. 아래 도구는 배치마다 새로 작성하지 않고 재사용한다.

| 도구 | 책임 |
| --- | --- |
| `capture_naver_menu.js` | 렌더링된 메뉴와 같은 항목의 공개 상태를 읽기 전용으로 캡처 |
| `source_dom.py` | 저장 DOM의 순서·필드와 수집 결과를 재대조 |
| `refresh_plan.py` | 대상 선정/상태 조회 SQL, 변경 계획과 가드 SQL 생성 |
| `refresh_run.py`, `run_with_tools.js` | 실행 고정·수집 제출·묶음 조회·매장별 쓰기·복구·집계 |
| `refresh_verify.py` | 반영 후 전체/노출 메뉴·스냅샷·보호 기록 대조 |
| `deletion_guards.py` | 정확한 삭제 대상의 등록 경위와 전체 페이지·메뉴판 검토를 계획/SQL/사후 검증에서 공통 검사 |
| `review_checklist.py`, `review-checklist.html` | 동결 검토 자료·체크 승인표 생성과 선택 추출. DB 쓰기는 수행하지 않음 |
| `collection_skips.py`, `data/collection-skips.json` | 최신 재수집 제외 조회와 검증된 수집 배정 캐시 |
| `replay_run.py`, `test_*.py` | 코드 변경 시 저장 자료로 실제 실행 경로를 모의 DB에서 검증 |

## 실행 파일

실행별 전용 디렉터리에 `selection.json`, 최초 `baseline.json`, 반영 직전 `prewrite-state.json`, 매장별 `result.json`/원본 캡처/`prepared/plan.json`/`prepared/apply.sql`, 커밋 결과와 독립 검토 결과를 보존한다. 저장소의 ignored `.artifacts/menu-refresh/<run-id>/`를 기본 위치로 사용하며 임시 디렉터리를 사용했다면 완료 전에 실행 기록을 이 위치로 옮긴다. `state-sql`은 선정 JSON 배열과 Supabase CLI의 `{rows:[...]}` 형식을 받는다. 동일 매장의 성공 상태와 스냅샷 ID는 실행 manifest에서 보존하며 실패한 매장 때문에 성공한 매장을 다시 쓰지 않는다.

```bash
python3 <skill-dir>/scripts/refresh_plan.py select-sql --before 2026-10-06 --limit 10 > selection.sql
supabase db query --linked --file selection.sql --output json > selection.json
python3 <skill-dir>/scripts/refresh_plan.py state-sql --selection selection.json > state.sql
supabase db query --linked --file state.sql --output json > baseline.json
```

연결 프로젝트가 Ramap인지 먼저 확인한다. 상태 JSON은 `rows[0].state`에 `shops`, `sections`, `items`, `visible`, `overrides`, `rules`, `menu_timestamps`, `collection_skips` 배열을 담는다. `items`는 `shop_menu_sections`를 JOIN해 `shop_id`를 붙인 원본 전체 행이다. `shop_menu_items` 자체에는 `shop_id`가 없다.

## 실행기

`refresh_run.py`는 같은 실행의 상태를 `manifest.json`에 원자적으로 보존한다. `context.json`에는 정책·실행 코드의 해시와 별도로 입력한 사용자 범위 확인이 있으며 각 매장의 `assignment.json`에 수집 경로와 계약이 있다. 전체 원본 JSON을 대화에 다시 출력할 필요가 없다. [모델별 작업자 입력 계약](agent-workflow.md)에 따라 Luna Low 수집자와 Sol High 판정자를 새 컨텍스트로 배정한다. 판정자에게 진입점인 `tools/menu-refresh/SKILL.md`를 전달하고 수집자는 수집 절차만 읽는다.

```bash
python3 tools/menu-refresh/scripts/refresh_run.py init \
  --run .artifacts/menu-refresh/<run-name> --selection selection.json \
  --project-id clbhayqiwjawaawvrhcq
python3 tools/menu-refresh/scripts/refresh_run.py submit \
  --run .artifacts/menu-refresh/<run-name> \
  --capture <shop-capture-dir>/result.json --collector /root/<collector-name>
python3 tools/menu-refresh/scripts/refresh_run.py run-cli \
  --run .artifacts/menu-refresh/<run-name>
```

수집자가 원본 DOM과 수집 증거를 저장하고 판정자가 같은 산출물 폴더에 검증된 파생 `result.json`을 작성한 뒤 주 에이전트가 `submit`한다. 실행기의 collector에는 실제 원문 수집자를 기록한다. 실행기는 필수 항목·원본 대조·품질 확인·지점 판정·경로·조사 시각을 확인한다. 라이브 캡처의 기본 유효 기간은 24시간이며 `--max-age-hours`로 더 짧게 정할 수 있다. 반영 직전에도 시각과 원본 해시를 다시 확인한다. 실행 중 이미 제출한 캡처를 편집하지 않는다. 새 원본/판정이 필요하면 새 실행에 새 캡처를 제출한다.

`run-cli`는 `supabase/.temp/project-ref`가 실행의 프로젝트와 같고 설치된 CLI의 `db query --help`가 필요한 옵션을 제공할 때만 동작한다. 다른 프로젝트로 자동 연결하지 않는다. 인증된 연결이 있는 MCP에서는 다음과 같이 로컬 실행기를 불러온다.

수집 자체가 실패한 매장은 `fail --shop-id <uuid> --reason <실제 원인>`으로 기록하고 다른 매장을 진행한다. 실패한 수집도 성공으로 표시하거나 매장 목록에서 지우지 않는다. 진단 후 한 번만 재시도할 수 있으며 캡처가 없는 경우 `waiting_capture`로 돌아간다.

```javascript
const source = await tools.exec_command({
  cmd: 'cat tools/menu-refresh/scripts/run_with_tools.js', max_output_tokens: 5000
});
if (source.exit_code !== 0) throw new Error('runner could not be loaded');
const run = new Function('return ' + source.output)();
const progress = await run({
  execCommand: args => tools.exec_command(args),
  executeSql: args => tools.mcp__codex_apps__supabase_execute_sql(args)
}, {
  script: '/absolute/Ramap-kmp/tools/menu-refresh/scripts/refresh_run.py',
  runDir: '/absolute/Ramap-kmp/.artifacts/menu-refresh/<run-name>',
  projectId: 'clbhayqiwjawaawvrhcq'
});
text(progress);
```

이 코드는 `functions.exec`에서 쓰는 로컬 코드이며 CUA 페이지 evaluator에 전달하지 않는다. 공개 페이지의 JavaScript나 DB 응답을 실행하지 않는다. 호출 한 번은 기본 45초 또는 40개 작업까지만 진행하고 `yielded`이면 같은 실행기를 이어 호출한다. `idle`의 `waiting_capture`가 있으면 다음 수집 결과를 제출한 뒤 진행한다. 이 실행 흐름이 DB 시작 감사, 준비된 매장들의 묶음 조회, 매장별 SQL 생성·ROLLBACK·COMMIT·재조회·검증을 연결한다.

삭제와 새 수동 확인 생성은 항상 ROLLBACK을 먼저 실행한다. 기본 `--rehearsal all`은 모든 SQL을 검증하며, 동일 코드 버전의 SQL 경로가 검증된 뒤 일반 변경에는 `init --rehearsal deletions`를 사용할 수 있다. 이 옵션에도 삭제·수동 확인 삽입의 ROLLBACK은 남는다. ROLLBACK에서도 실제 SQL의 변경 전/후 가드와 처리 수 검사가 동일하게 실행된다.

연결 실패로 커밋 결과가 불명확하면 실행기가 해당 스냅샷 ID를 먼저 조회한다. 존재하고 메뉴·보호·출처·해시가 모두 맞으면 성공으로 기록한다. 스냅샷 부재는 재실행 허가가 아니며 해당 매장을 `failed`로 남긴다. 원인을 진단한 뒤 `retry --shop-id <uuid> --diagnosis <reason>`로 해당 매장만 한 번 새 라이브 상태에서 준비한다. 사후 불일치에는 자동 재시도가 없다.

프로세스가 응답을 기록하기 전에 종료되면 `next`는 `await_result`를 반환하고 SQL을 재실행하지 않는다. 이전 실행이 끝난 것을 확인한 뒤 CLI의 `run-cli --resume` 또는 MCP 실행기의 `resume: true`를 사용한다. 같은 실행에 CLI와 MCP를 동시에 붙이지 않는다. 정책이나 코드 버전이 바뀌면 기존 manifest를 수정하지 말고 새 실행을 만든다.

### 독립 검토와 보고서

`review-handoff.json`에는 사후 자동 검증이 통과하고 아직 독립 검토를 받지 않은 매장만 포함한다. 배치 하나를 한 번 의뢰하고 검토자는 매장마다 원본·DB 메뉴 순서·가격·사진·보존 항목을 독립 확인한다. 해당 매장의 수집자와 반영자는 검토자가 될 수 없다. 검토 결과 형식은 다음과 같다.

```json
{"reviewer":"/root/independent-reviewer","shops":[
  {"shop_id":"uuid","snapshot_id":"uuid","plan_hash":"handoff의 값",
   "result":"pass","findings":[]}
]}
```

`review --run <run-dir> --report <review.json>`으로 기록한 뒤 실행기를 한 번 더 진행하면 감사 집계가 마무리된다. 독립 검토 전에는 `awaiting_independent_review`이며 `pass` 결과를 만들어 채우지 않는다. `report.md`·`summary.json`·`review-handoff.json`은 자동 생성된다. `events.jsonl`은 단계별 시각과 대기/실행 시간, `timings.json`은 단계별 합계를 기록한다. DB 단계 시간에는 도구·응답 대기가 포함되며 전체 브라우저 조사 시간과 사용자 응답 시간은 이 수치에서 추정하지 않는다.

### 기존 사용자 범위 확인의 재사용

주 에이전트가 실제 사용자 범위 확인을 아래 형식으로 기록하고 `init --confirmations <file.json>`으로 전달한다. 기존 범위 확인 삽입은 공통 준비 함수를 재사용하며 수집 작업자에게 승인 기록 생성을 맡기지 않는다. 체크 승인표에서 추출한 값을 사용할 때에도 실제 사용자 승인과 정확한 원본 항목에 결속한다.

```json
[{"shop_id":"확인한 매장의 uuid","name":"확인한 정확한 메뉴명",
  "source_id":"현재 원본의 ID 또는 null","confirmed_at":"확인 시각 + timezone",
  "evidence":"해당 매장·메뉴에 대한 실제 사용자 범위 확인 근거"}]
```

현재 원본 하나와 고정 가격이 맞을 때만 활성 `admin_verified` upsert를 생성한다. 메타데이터는 고정하지 않고 가격·설명·사진·순서는 최신 원본과 기존 등록 정책을 따른다. 이미 같은 활성 upsert가 있으면 다시 삽입하지 않는다. 활성 suppress 해제나 판매 상태 변경은 이 기능으로 처리하지 않는다.

### 재수집 제외

재수집 제외는 사용자 지시로 확정한 정확한 매장·메뉴에만 적용한다. 활성 기록과 건수는 DB 감사 및 `data/collection-skips.json`에서 읽으며 과거 배치의 미선택 건수를 상수로 사용하지 않는다. DB 감사의 `collection_skip_authority=human_user`와 `collection_skip_bindings`가 원본이며 매장 ID에 결속한 실제 `source_ids` 또는 정확한 `normalized_names`로 대응한다. 기존 메뉴를 숨기거나 삭제하는 `suppress`와 혼동하지 않는다. `state-sql`과 매장 선정 SQL은 최신 활성 제외를 읽고, `prepare`는 제외 항목을 새 보류 없이 보존한다. 제외 정책이 준비 뒤 바뀌면 트랜잭션 가드가 쓰기를 중단한다.

캐시와 최신 DB 기록이 달라졌거나 사용자가 제외를 해제한 경우, `scripts/collection_skips.py`의 `collection_skips_sql()`로 최신 활성 기록을 읽어 검증한 뒤 캐시를 갱신한다. 새 실행에는 `init --collection-skips <registry.json>`으로 검증된 기록을 명시할 수 있다. 수집 배정 파일의 `collection_skips`를 읽고 해당 항목의 추가 원본·사진 조회와 범위·가격 충돌 판정을 하지 않는다. 전체 DOM에 기계적으로 포함된 원문은 보존한다. 미선택 보류의 원래 기록은 통합 목록에 유지하고 향후 수집 제외라는 처리 상태를 별도로 표시한다.

캐시는 `schema_version=1`, `entries` 배열을 사용한다. 각 항목은 고유 UUID `id`, `kind=menu|shop`, `status=active|inactive`, `shop_id`, `authority=human_user`와 사용자 지시 근거를 가진다. 메뉴 제외에는 실제 `source_ids` 또는 공백 제거·소문자 정규화한 정확한 `normalized_names`가 필요하다. 승인 자료의 `bundle_id`, `entry_id`, 원래 보류 ID·값과 원본 스냅샷/해시도 보존한다. 최신 DB 상태에 `collection_skips`가 있으면 빈 배열도 유효한 최신 결과이며 과거 캐시로 제외를 되살리지 않는다. 전체 매장 제외는 실행기 `init`에서 배정하지 않고 메뉴 제외는 매장별 배정으로 전달한다. 해제는 같은 ID의 최신 비활성 감사 기록과 캐시를 함께 갱신한다.

### 저장 자료 재현

```bash
python3 tools/menu-refresh/scripts/replay_run.py \
  --run .artifacts/menu-refresh/<replay-name> --selection selection.json \
  --state saved-current-state.json --captures saved-captures.json
```

`saved-captures.json`은 `[{"capture":"/saved/shop/result.json","raw":"/saved/shop/raw-menu.html","collector":"원래 수집자"}]` 형식이다. 원본은 수정하지 않으며 이동된 과거 경로는 임시 입력 폴더에서만 재연결한다. 이 모드는 모의 DB로 실제 큐·검증·집계 코드를 실행한다. 실제 DB 접속과 SQL 실행, 새 브라우저 조사 속도를 검증한 것으로 보고하지 않는다. 라이브 CLI/MCP 연결기는 replay 모드의 작업을 거부한다.

## 브라우저 캡처

### 원본 부재와 직접 등록 보호

최신 DB 조회는 `manual_menu_protections`를 포함해야 한다. 보호는 DB 감사의 `manual_menu_protection_authority=human_user`와 `manual_menu_protection_bindings`에서 읽으며, 정확한 `shop_id`, `menu_item_id`, 이름, 직접 등록 근거를 대조한다. 일반 upsert나 `admin_verified`는 직접 등록 증거가 아니다. 새 직접 등록 확인을 반영할 때 실제 사용자 확인 또는 등록 감사의 참조를 결속해 DB 감사와 검증된 캐시를 갱신한다. 목록이 비어 있는 것과 보호 조회가 누락된 것을 구분한다.

원본 부재 후보가 있으면 판정자가 전체 메뉴·더보기·로딩·메뉴판 안내·별도 메뉴·이름 변종 대응을 확인한다. `raw-menu.html`은 수집기의 `raw_html` 전체 문서로 저장한다. 텍스트 카드 섹션 합본에는 섹션 밖의 메뉴판 갤러리가 빠질 수 있으므로 삭제 근거로 쓰지 않는다. 전체 문서가 잘리거나 이미지 안내·다른 메뉴 표면을 확인하지 못하면 삭제를 중단한다. 메뉴판 원본 URL을 개별 메뉴 사진으로 등록하지 않는다.

삭제 계약은 `direct_registration_only_v2`다. 새 파생 캡처에 다음 증거를 결속하고 실제 파일/감사와 대조한다. 값만 채워 완전성을 선언하지 않는다.

- `source_absence_review`: `schema_version=2`, `complete=true`, 실제 `source_url`, `captured_at`, 전체 문서의 `raw_sha256`, 판단 `reason`. `coverage`는 `scope=full_document`, `expanded=true`, `loading_complete=true`, `other_menu_surfaces_reviewed=true`, 해시 있는 `collection_evidence={ref,sha256}`를 가진다.
- `menu_board_reviews`: DOM의 메뉴판 이미지 원본 URL마다 한 건씩 `image_url`, `outcome=all_candidates_checked|not_current`, `reason`, `unresolved_candidates=[]`, 실제 검토 파일의 `evidence={ref,sha256}`를 저장한다. 이미지가 없으면 빈 배열이다. 이미지에만 남은 후보, 오래된 가격, 별칭·구성 충돌은 확인 없이 부재로 처리하지 않는다. 공통 가드는 실제 전체 문서의 메뉴판 URL 집합과 대조한다.
- `registration_origin_reviews`: 모든 삭제 후보의 정확한 `shop_id`, `menu_item_id`, `name`, `before_sha256`, 현재 오버라이드 전체의 `overrides_sha256`를 저장한다. 해시는 공통 `evidence_hash`로 계산한다. `checked`는 `registration_audit`, `human_input`, `manual_overrides`, `stored_media`, `aliases` 전체이며, `aliases_reviewed=true`, `unresolved_clues=[]`, 실제 조사 근거 `evidence_refs=[{ref,sha256}]`, `reason`과 항목별 `clue_resolutions`를 포함한다. 직접 등록이 아님을 조사한 경우에만 `outcome=not_direct_registration`으로 판정한다. 과거 원문/감사가 없거나 단서가 해결되지 않으면 이 판정을 만들지 않는다.

직접 등록이 확인되면 삭제 리뷰를 만들지 않고 정확한 직접 등록 보호를 먼저 감사에 결속한다. 예전 note·현장 확인·사용자 첨부·업로드 이미지·별칭은 조사 단서이며 그 자체로 모든 메뉴를 보호하지 않는다. 일반 수집 결과의 범위 승인과 실제 사용자의 직접 등록 요청을 구분한다. 직접 등록 메뉴에 대한 별도 명시적 삭제 지시는 정확한 인간 근거를 갖춘 좁은 `menu-add` 계획으로 처리한다.

같은 매장의 전체 원본에 없는 행은 직접 등록 보호 항목을 제외하고 삭제 계획에 넣는다. 원본 부재 메뉴의 일반 활성 upsert는 같은 트랜잭션에서 `revoked`로 해제해 재등록을 막는다. 재수집 제외와 충돌한 항목은 삭제 보호가 아니라 처리 불가로 분리한다. 동일 SQL의 ROLLBACK·COMMIT·전체 재조회 후 실제 원본 미대응 행마다 직접 등록 근거가 있는지 확인하며, 기대 상태에 잘못 남겨 둔 행도 실패다.

CUA 최초 초기화는 해당 도구의 문서대로 수행한다. 새 Chrome 탭 바인딩이 반환한 **브라우저별 전체 문서**에서 `tab.playwright.evaluate` 지원을 확인한 뒤 공개 메뉴를 읽는다. 초기 공통 접근성 문서에 DOM API가 없다는 이유만으로 기능 미지원으로 판정하지 않는다. 실행 전 모든 메뉴 더보기를 확장하고 메뉴 사진을 스크롤해 로드한다.

공통 CUA Tab과 브라우저의 DOM Tab은 다른 바인딩일 수 있다. 현재 문서에 advanced `agent.browsers` API가 제공되면 실제 선택된 browser ID로 `agent.browsers.get(browserId)`를 얻고, 그 브라우저의 `tabs.get(ownedTab.id)`로 같은 전용 탭을 바인딩한 뒤 문서에 명시된 읽기 전용 evaluator를 사용한다. 공통 Tab에 `playwright` 속성이 없다는 사실만으로 권한 제한을 선언하지 않는다. Chrome에는 지원되는 `sessionName` 옵션을 사용하며, 실제 메뉴 더보기 선택자는 `[data-nlog-area="plc_bmv.more"]`다.

탭 생성이 공통 접근성 문서만 반환하면 최초 브라우저 선택의 `cua.getBrowser(...)`가 제공하는 브라우저별 문서를 확인한다. 이후에는 유효한 바인딩을 재사용하며 탭 오류 때문에 브라우저를 다시 선택하지 않는다.

Place ID를 확정했으면 `https://m.place.naver.com/restaurant/<place-id>/menu/list`를 전용 탭에 직접 연다. PC 지도 진입 URL의 iframe을 거쳐 수집하지 않는다. CLI에서 내장 브라우저가 없으면 제공되는 Chrome 바인딩을 사용하고, 지원되지 않는 내장 브라우저를 반복 초기화하지 않는다.

직접 페이지 요청이 HTTP 429로 실패하면 같은 요청을 반복하지 않고 새 브라우저 탭의 렌더링된 메뉴로 확인한다. 매장별 작업자는 전용 탭을 사용하고 공용 탭의 페이지를 바꾸지 않는다.

```javascript
// Local filesystem operations below only save/read task artifacts.
var artifactFs = await import('node:fs/promises');
var collector = await artifactFs.readFile(skillDir + '/scripts/capture_naver_menu.js', 'utf8');
var capture = await tab.playwright.evaluate('(' + collector + ')()', undefined, {timeoutMs: 30000});
await artifactFs.writeFile(shopDir + '/dom-capture.json', JSON.stringify(capture, null, 2));
await artifactFs.writeFile(shopDir + '/raw-menu.html', capture.raw_html);
```

내부 평가기의 30초와 파일 저장 시간에 여유를 주도록 이 CUA 도구 호출에는 `timeout_ms: 60000`을 지정한다.

로컬 산출물 저장은 위 예제의 `node:fs/promises`로 수행한다. 실제 import·쓰기 오류를 확인하기 전에 저장 기능이 없다고 판정하지 않는다. 클립보드 경유나 도구 응답의 수기 재구성으로 원문을 대체하지 않으며, 공통 수집기 문자열도 직접 읽은 내용을 그대로 실행한다.

수집·탭 종료·부재 확인 시각은 각각 실제 실행 시 측정한다. 나중에 입력한 시각을 과거 실행 시각으로 쓰지 않는다. 수집자·모델·브라우저·탭 ID가 누락된 과거 자료는 원문을 바꾸지 않고 별도 인계 근거와 확인 한계를 남긴다. 사진 수는 비어 있지 않은 유효 HTTPS URL만 세며, `images: [""]` 같은 배열의 존재만으로 사진이 있다고 판단하지 않는다.

### 탭 종료

- 완료된 매장의 웹 페이지는 반드시 즉시 종료한다. 수집자는 DOM·원문·수집 증거 저장을 마치면 자신의 전용 탭을 닫는다. 판정자·주 에이전트·독립 검토자도 해당 페이지의 확인과 증거 저장을 마치는 즉시 직접 연 탭을 닫는다. 다음 단계는 저장 파일로 인계한다.
- 배치가 끝날 때까지 완료된 페이지를 남기거나, 다음 매장을 열기 위해 완료된 탭을 보관·재사용하지 않는다. 완료된 페이지에는 `markHandoff()`·`markDeliverable()`을 호출하지 않는다. 보류·실패로 해당 페이지 작업을 끝낸 경우도 근거와 재개 사유를 저장하고 닫는다.
- 실시간 재확인이 필요한 경우에만 해당 매장 페이지를 새로 열고, 재확인 후 다시 닫는다. 진행 중인 다른 작업자의 탭과 이 작업과 무관한 사용자 탭은 건드리지 않는다.
- 종료 후 탭 목록에서 해당 탭이 사라졌는지 확인한다. 닫기 실패는 대상 탭 ID·URL·오류를 기록해 주 에이전트에게 인계하며 종료 성공으로 보고하지 않는다. 작업 재개·배치 전환·최종 보고 전에도 자신이 연 완료 매장 탭이 남아 있는지 점검한다.

### 작업자별 병렬 수집

- 먼저 새 실행 환경의 전용 탭 한 개에서 실제 메뉴 DOM·직렬화 상태·원문 HTML 수집을 사전 확인한다. 사용자가 동시 수집 검증을 요청한 경우에만 이미 완료된 매장도 읽기 전용 검증 대상으로 다시 연다.
- 각 작업자는 자신의 CUA 초기화와 전용 탭 생성부터 원문 저장까지 수행한다. 다른 작업자의 탭을 이동하거나 부모가 만든 캡처를 자신의 수집 결과로 사용하지 않는다. 탭 ID·원본 URL·수집자·시작/종료 시각과 수집기·DOM·원문 파일 해시를 남긴다.
- 여러 작업자를 배정할 때는 각 탭의 메뉴 목록이 준비된 뒤 수집을 시작하도록 조정한다. 실제 활성 작업자 수와 서로 다른 탭 ID를 확인하고, 각 작업자가 저장한 결과를 개별 대조한다. 활성 수와 페이지 수만으로 원본 수집 성공을 선언하지 않는다.
- `Debugger unattached`, selector deadline, isolated-world 오류는 실행 오류로 기록한다. 기존 브라우저 바인딩을 유지하고 해당 작업자만 새 전용 탭에서 한 번 재시도한다. 명시적 브라우저 연결 해제에는 도구가 제공한 복구 문서를 따른다. REPL 전체 초기화·공유 브라우저 재선택으로 다른 작업자의 세션을 바꾸지 않는다.
- 문서에 DOM 읽기 API가 실제 없거나 한 번의 진단 재시도도 실패하면 해당 작업자의 수집을 중단하고 정확한 오류를 남긴다. 별도 Playwright import, 비공개 연결 방식, 선언값 수정으로 통과시키지 않는다. 부모의 대체 수집은 출처를 별도로 표시하며 작업자의 직접 수집 성공 수에 포함하지 않는다.

메뉴 원문은 `raw_html` 전체 문서로 저장하고 `sections`와 `menu_board_images`도 보존한다. 페이지 전체 `outerHTML` 반환이 도구 길이 제한으로 잘렸다면 완전한 증거로 사용하지 않는다. `dom-capture.json`의 수와 전체 문서·패널·메뉴판 목록·수집 목록을 함께 대조한다. 원본/수집 값이 이미 검증된 자료로 판정되면 같은 페이지를 다시 열지 않는다. 과거 텍스트 패널 캡처는 원래 해시를 보존하며 삭제 근거로 승격하지 않는다.

이 표현식은 DOM에서 확인한 메뉴 카드와 같은 페이지의 `script.textContent`에 노출된 `__APOLLO_STATE__` JSON만 읽는다. 사이트 JavaScript를 eval하지 않으며 페이지나 네트워크를 변경하지 않는다. 현재 두 가지 카드 선택자를 지원하지만 네이버 클래스나 가격 구조가 바뀌면 캡처 결과를 직접 확인해 수정한다. 직렬화 데이터만으로 다른 주문 표면의 메뉴를 일반 목록에 섞지 않는다.

반환하는 `sections`는 제목을 포함한 원본 섹션 전체다. 자동으로 대표 요약을 삭제하지 않는다. `PlaceMenuCategory.itemIds`, 같은 항목 ID/이름·가격, 표시된 제목과 실제 메뉴 행 수로 대표 요약과 실제 카테고리를 구분한다. 별도 실제 카테고리에도 `대표메뉴`라는 이름이 붙을 수 있다. 같은 메뉴 사진의 여러 `images[].url` 중 DOM 카드 사진을 우선하고, 다른 사진을 임의로 선택하지 않는다.

DOM 카드에 사진이 없어도 같은 실제 메뉴 ID의 공개 `PlaceMenuItem.thumbnailUrl` 또는 `images[].url`에 유효한 HTTPS 메뉴 사진이 있으면 수집 대상이다. 해당 행의 한 번 허용된 이미지 로드와 동일 항목 상태를 확인한 뒤 누락 여부를 판정한다. 플레이스홀더·리뷰·갤러리·메뉴판 전체 사진을 개별 메뉴 사진으로 사용하지 않는다.

이미지 프록시 URL과 그 `src`가 가리키는 동일 원본 URL은 같은 사진으로 비교한다. URL 표기만 다르다는 이유로 사진 변경을 만들지 않는다. 캡처에는 표시 URL과 확인한 원본 `source_image_url`을 각각 보존한다.

SQL 생성 시 `source_dom.py`가 메뉴 링크의 원본 Place ID와 canonical 목록의 순서·이름·가격·설명·사진·대표 표시를 대조한다. `대표메뉴` 제목의 행은 다른 실제 카테고리에 전체 필드가 같은 행이 있어야 요약으로 제외한다. 단독 실제 카테고리라면 자동 처리를 중단하고 검토한다. 과거 JSON형 `menu_sections` 캡처는 바이트가 완전히 같은 패널의 중복만 제거하며, 일반 행의 같은 이름·가격을 합치지 않는다. 새 캡처는 제목을 보존한 HTML로 저장한다. DOM과 상태만으로 결정할 수 없는 사진이나 요약 구조는 별도 원본 검증이 필요하며 선언값을 고쳐 가드를 우회하지 않는다.

## result.json 계약

```json
{
  "shop_id": "Ramap UUID",
  "shop_name": "DB 매장명",
  "status": "collected",
  "source_url": "https://m.place.naver.com/restaurant/123/menu/list",
  "captured_at": "2026-10-07T02:00:00Z",
  "raw_html_path": "/task-run/shop-01/raw-menu.html",
  "parser_version": "naver-menu-rendered-v1",
  "source_shop": {"id": "123", "name": "원본 매장명", "address": "원본 도로명 주소"},
  "identity_verified": true,
  "menu_items": [
    {"source_id": "원본 ID 또는 null", "name": "쇼유라멘", "price": 11000,
     "price_text": "11,000원", "description": null, "image_url": null,
     "source_image_url": null, "is_featured": false}
  ],
  "scope": [{"source_id": "원본 ID 또는 null", "name": "쇼유라멘", "decision": "include", "reason": "원본 라멘 본품"}],
  "quality": {"passed": true, "issues": [], "source_count": 1, "collected_count": 1,
    "source_image_count": 0, "collected_image_count": 0, "sequence_evidence": "DOM/항목별 상태/카테고리 순서 일치"},
  "findings": [], "db_only_preserved": []
}
```

`identity_verified`는 판정자가 수집된 원본 ID·이름·주소와 현재 DB를 대조해 지점을 실제 확인한 뒤 설정한다. DB UUID를 `source_shop.id`에 넣지 않는다. 이름이 지점명을 생략해도 같은 원본 ID와 도로명·호수가 확인되면 그 근거를 기록한다. 주소를 DB에서 복사해 원본에서 확인했다고 표시하지 않는다.

`menu_items`는 추천 중복만 정리한 **필터 전 전체** 메뉴다. `scope`의 `include|exclude|manual`은 같은 순서로 한 항목씩 대응한다. `price`는 고정 원 가격 또는 null이고 범위·변동은 `price_text`에 그대로 둔다. 원본 증거에는 null 설명과 실제 빈 문자열을 구분해 보존하되, 설명이 없거나 공백뿐이면 기존 DB 설명을 삭제하거나 빈 문자열로 바꾸지 않는다. 새 이름이 기존 메뉴의 단순 이름 변경인지 확실하지 않으면 `manual`로 둔다. 구성·한정·판매 상태 증거가 있으면 일반 섹션 자동 추가로 넘기지 않는다.

사용자가 기존 메뉴의 등록 범위를 확정한 경우 해당 메뉴의 활성 `admin_verified` upsert를 적용한다. 이름·설명 부족에 따른 `manual` 범위 보류는 해제한다. 검증된 최신 가격·사진·설명이 과거 upsert와 다르면 등록 정책에 따라 정규 메뉴와 함께 동기화하며 구성·판매 상태와 숨김·이벤트·중복 검사는 계속 수행한다. 기존 등록 메뉴와 신규 가격 기준의 우선순위는 `docs/menu-registration-policy.md`를 따른다. 리틀훅라멘의 기존 `정구지로`, `온비빔`은 2026-10-07 사용자 확인 대상이다.

기존 등록 메뉴는 무조건 라멘 범위이며 수집자의 `exclude`나 신규 저가 기준만으로 삭제하지 않는다. 사용자 명시 제외·확인된 판매 종료·중복은 정확한 원본/기존 UUID 대응 근거로 처리한다. 완전한 최신 원본의 부재는 별도의 `source_absence_review`로 결속하며 정확한 직접 등록 근거가 없는 기존 행을 삭제한다. 일반 upsert의 재등록 방지도 같은 트랜잭션에 처리한다. 수집 불완전·정체 불확실·정확 재수집 제외와 충돌하면 삭제를 중단하고 해당 값만 미해결로 남긴다.

수동 SQL 확장에서도 전체 행 비교를 유지한다. `jsonb_agg(... ORDER BY row->>'id')`와 비교하는 JSON 배열은 UUID 순서로 직렬화하며, 화면 `display_order`와 원본 순서는 바꾸지 않는다. 이름·섹션 등 지원 밖 변경은 실제 SQL의 변경 이벤트와 validation audit 상세에 정확히 결속하고, 검증기의 이벤트 수를 실제 INSERT 조건과 대조한다. 실패한 ROLLBACK·검증 자료를 보존한 뒤 원인을 수정하며 성공한 SQL을 다시 실행하지 않는다.

원본 ID가 없으면 실제 null을 저장한다. 캡처 행 순번은 별도 `capture_row_index`로 표시할 수 있지만 메뉴 식별 근거를 대신하지 못한다. 원본에서 사진이 노출된 메뉴 수와 수집한 사진 URL 수는 전체 `menu_items` 기준으로 같아야 한다. 추천 때문에 원시 화면에 사진 5개, 중복 제거한 목록에는 4개라면 canonical 수는 4/4이며 raw DOM 사진 수 5는 별도다. 일반 목록의 같은 이름·가격 두 행이 다른 사진을 가진다면 원본에서는 두 행 모두 보존한다. 구성·설명·카테고리도 동일하면 등록 후보는 원본의 대표 표시→정보 충실도→첫 등장 순서에 따라 1개로 결정한다.

이 경우 파생 캡처에 `duplicate_resolutions: [{"name": "돈코츠라멘", "source_ids": ["원본 ID 1", "원본 ID 2"], "selected_source_id": "원본 ID 1", "scope_verified": true, "evidence": "동일 이름·고정 가격·설명·카테고리·구성; 대표 항목 우선"}]`를 추가한다. 중복 원본 행의 scope를 `exclude`로 둔다면 `duplicate_of: "선택 원본 ID"`도 기록해 범위 밖 메뉴 삭제와 구분한다. 모든 실제 ID가 포함되고 선택이 원본의 대표·정보·순서와 일치해야 하며, 가격·설명·카테고리 차이가 있으면 도우미가 거부한다. DOM 대조와 스냅샷에는 필터 전 전체 메뉴를 사용한다.

기존 변동 가격 행은 `price_krw=NULL`과 변동 표기를 유지하면서 확인된 설명·사진·순서를 갱신한다. 원본의 숫자가 없다는 이유로 다른 필드를 막지 않는다. 과거 고정 가격이 있는데 새 숫자가 없으면 가격은 유지하고 그 값만 미확정으로 기록한다. 신규 변동 메뉴의 고정 가격을 추측해서 추가하지 않는다.

## 준비·실행·복구

```bash
python3 <skill-dir>/scripts/refresh_plan.py prepare \
  --capture shop-01/result.json --state prewrite-state.json --output shop-01/prepared
supabase db query --linked --file shop-01/prepared/apply.sql --output json > shop-01/commit-result.json
```

가격 변동·메뉴 순서 갱신·메뉴 추가·삭제·이미지 변경과 같은 메뉴의 설명 갱신에는 등록 정책의 상시 실행 지시를 적용한다. 승인을 다시 요청하지 않고 plan.status가 `prepared`인지 확인한 뒤 마지막 명령을 실행하고 결과를 사후 보고한다. 삭제가 있으면 COMMIT을 ROLLBACK으로 바꾼 동일 SQL을 먼저 검증하고, 실제 삭제 처리 수·기대 상태 검사 통과를 확인한 뒤 원래 SQL을 실행한다. 도우미는 읽기 전용 준비 명령이며 실행 SQL에 부모 매장과 관련 행 잠금, 변경 전 JSON 비교, 처리 수와 변경 후 전체 메뉴 비교, 스냅샷 재실행 차단을 넣는다.

같은 메뉴의 실제 설명 내용 변경은 원본 그대로 갱신하고 `description_updates_by_policy`에 메뉴 ID·원본 ID·정확한 문구 쌍을 기록한다. 설명 차이만으로 보류하거나 기존 섹션 순서 갱신을 막지 않는다. 활성 upsert의 저장된 설명도 정규 메뉴와 함께 동기화한다. 개별 설명 동등성 결정과 사용자 설명 승인은 정확한 문구 쌍에만 적용한다. 원본 설명 부재만으로 기존 설명을 지우지 않는다. 메뉴 대응·다중 가격·판매 상태 등 다른 불확실성이 보류된 섹션은 기존 순서를 유지한다. 사용자가 특정 체크 선택 외의 보류를 유지하라고 지정한 배치에서는 새 일반 규칙으로 그 미선택 항목까지 처리하지 않는다.

활성 수동 확인과의 실제 구성 충돌은 별도로 검사한다. 새 메뉴와 기존 메뉴의 가격·사진/설명이 같거나 기존 이름에 원본 설명이 붙어 있으면 이름 변경/중복 후보로 보류한다. 과거 설명·사진이 비어 있으면 원본으로 보강할 수 있다.

단순 신규 추가로 처리하기 전에 기존 이름의 강조·괄호·분류 표기와 원본 이름+설명 결합을 대조한다. 같은 기존 ID의 정체가 확정되면 이름·가격·사진을 해당 ID에 반영하고 별도의 신규 행을 만들지 않는다. 실제 대응이 불명확한 후보만 `identity_mapping`으로 남기며, 이름 차이만으로 기존 메뉴의 라멘 범위를 다시 묻지 않는다. 이름 대응 갱신이 일반 helper의 지원 범위를 넘으면 `menu-add`로 처리한다.

실행 중 연결 오류가 나면 `plan.snapshot_id`로 새 스냅샷과 현재 전체 메뉴를 먼저 읽는다. 존재하고 기대 상태와 맞으면 성공으로 기록하고 재실행하지 않는다. 스냅샷이 없더라도 라이브 보호/메뉴 값이 달라졌다면 새 계획이 필요하다. 성공한 트랜잭션은 단순히 prepare를 다시 실행하지 않는다. 실패한 대상만 진단하고 재시도는 오류 원인을 해소한 경우 한 번으로 제한한다.

독립 검토가 범위 밖 기존 행의 잔존을 발견하면 기존 성공 SQL을 재실행하지 않는다. 원인과 삭제 근거를 확인한 별도 `remediation/` 폴더에서 현재 DB 기준의 새 보정 계획을 만든다. 사전 승인에 포함된 근거 확인 삭제는 동일 SQL의 ROLLBACK 검증 후 1회 반영하고 독립 재검토를 받는다. 동일 캡처를 재사용할 때 원본 조사 시각을 새 조사처럼 바꾸지 않는다. 기존 스냅샷은 보존하고 보정 트랜잭션의 추가 스냅샷도 최종 개수에 포함한다.

지원 범위는 상시 섹션 하나의 검증된 변경과 관련 upsert 가격·사진·설명 동기화다. 해당 값을 바꾼 확인 기록은 `admin_verified`로 전환하고 원본 확인 시각과 출처 근거를 note에 남긴다. `override_updates`와 변경 후 확인 기록도 검사하며 updated_at 트리거 값만 비교에서 제외한다. 섹션 생성·정리, 이벤트, 의미상 중복 삭제, 수동 확인의 범위·판매 상태 편집은 `menu-add`에 정확한 원본/사용자 지시/삭제 근거를 넘긴다. 새 스키마나 cron 설치는 이 스킬 실행에 포함되지 않는다.

실행 코드를 바꾼 경우 `python3 -m unittest discover -s <skill-dir>/scripts -p 'test_*.py'`와 저장 자료 replay로 검증한다. 동일 코드의 운영 실행마다 테스트를 반복하지 않는다. 실제 운영 적용 후에는 별도 read-only 조회와 독립 검토를 수행한다. `visible`은 `items`의 기술적 검사와 별개로 사용자에게 노출되는 최종 메뉴를 확인하는 근거다. 스냅샷/메뉴 일치만으로 범위 보류를 해결됐다고 보고하지 않는다.
