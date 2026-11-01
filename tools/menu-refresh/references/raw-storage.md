# 메뉴 원문 gzip 보관

전체 원문은 `menu-snapshot-raw` 비공개 Storage 버킷에 gzip 파일로 보관한다. 파일 경로는 실제 원문 UTF-8 바이트의 SHA-256으로 정하며 동일한 원문을 재사용한다. DB 스냅샷은 `raw_object_sha256`으로 `menu_snapshot_raw_objects`의 검증 기록을 참조한다. 메뉴 JSON·수집 시각·출처·스냅샷 ID·기존 `source_hash`는 유지한다. 과거 `source_hash`가 실제 원문 해시와 달라도 감사 기록을 재작성하지 않는다.

## 신규 수집·수동 등록

`refresh_run.py run-cli`와 `run_with_tools.js`는 메뉴 트랜잭션 전에 원문을 업로드하고 실제 파일을 다운로드·복원해 검증한다. `refresh_plan.py prepare`도 SQL 파일을 내보내기 전에 같은 검증을 수행한다. 순수 계획 함수가 반환한 SQL을 직접 실행할 때는 별도로 먼저 원문을 보관해야 한다. 이전 중인 동결 실행·원문·기존 SQL을 새 코드에 맞춰 수정하지 않는다. 새 실행을 만든다.

수동 SQL·기존 검증 RPC를 사용할 때도 먼저 공통 도구를 호출한다.

```bash
python3 tools/menu-refresh/scripts/snapshot_storage.py put \
  --project-id clbhayqiwjawaawvrhcq \
  --raw-file <capture-dir>/raw-menu.html --output <capture-dir>/raw-storage.json
```

반환된 `raw_sha256`을 새 스냅샷의 `raw_object_sha256`으로 저장하고 `raw_dom`은 NULL로 둔다. 기존 RPC나 SQL이 원문 문자열을 전달하더라도, 같은 원문이 이미 검증돼 있으면 DB 트리거가 저장소 참조로 바꾼다. 검증되지 않은 인라인 원문을 새로 저장하려는 쓰기는 이전 완료 후 거절된다. Storage 장애나 해시 불일치 때 메뉴 트랜잭션을 진행하지 않는다.

## 원문 읽기·사후 검증

```bash
python3 tools/menu-refresh/scripts/snapshot_storage.py read \
  --project-id clbhayqiwjawaawvrhcq --snapshot-id <snapshot-uuid> \
  --output <review-dir>/restored-raw-menu.html
```

다운로드한 gzip 파일의 해시·압축 크기, 복원한 원문의 크기·SHA-256을 확인한다. 삭제 근거 검증에는 복원한 전체 문서를 전달한다. 메뉴 JSON이나 DB에 저장된 해시만으로 파일의 무결성을 검증했다고 보고하지 않는다. 원문을 메뉴 패널로 줄이거나 메뉴판 이미지를 포함한 증거를 누락하지 않는다.

## 기존 자료 이전·재개

```bash
python3 tools/menu-refresh/scripts/migrate_snapshot_storage.py \
  --project-id clbhayqiwjawaawvrhcq \
  --output .artifacts/menu-snapshot-storage/<run-id> --workers 8
```

업로드 → 다운로드·바이트 단위 복원 검증 → 검증 기록 등록 → DB에서 현재 원문 해시 재확인 → 원문을 참조로 교체하는 순서를 사용한다. 파일은 덮어쓰지 않으며 잘못된 기존 파일을 발견하면 중단한다. 참조 전환은 행 잠금과 해시 검사를 포함한 RPC로 수행한다. 실패한 행은 원문을 유지하고 다시 실행하면 아직 인라인으로 남은 행만 처리한다. 실행 증거와 다운로드한 gzip 복사본은 ignored `.artifacts/`에 남긴다.

스냅샷·정규 메뉴의 수와 메타데이터 해시가 이전 전후 같은지 확인한다. 모든 원문을 이전한 뒤 인라인 저장 금지 제약을 적용한다. 물리 용량 회수는 해당 테이블의 쓰기 상황과 잠금을 확인하고 별도로 수행한다. 비공개 원문 버킷·검증 기록에는 일반 회원과 비회원의 접근을 허용하지 않는다. 인증된 Supabase CLI의 서버 자격 증명은 실행 중 메모리에서만 사용하며 로그·산출물·소스에 저장하지 않는다.

여러 재개 실행을 한 폴더 아래 보관했다면 최종 상태를 다음 도구로 대조한다. 응답이 유실된 커밋은 실제 파일을 다시 다운로드·복원해 확인한다.

```bash
python3 tools/menu-refresh/scripts/verify_snapshot_storage.py \
  --project-id clbhayqiwjawaawvrhcq --artifacts .artifacts/menu-snapshot-storage/<group> \
  --baseline .artifacts/menu-snapshot-storage/<group>/<first-run>/before.json
```

기본 이전 배치는 20건이다. 자원이 작은 DB에서 큰 원문을 한꺼번에 읽으면 메모리와 질의 시간 제한에 영향을 줄 수 있으므로, 연결 지연이 발생하면 작업을 멈추고 서버 상태를 먼저 확인한다. 원문 파일 전송은 프로젝트의 전용 Storage 호스트를 사용한다.
