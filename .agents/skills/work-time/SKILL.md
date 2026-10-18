---
name: work-time
description: Read a ramen shop's weekly business hours from Naver Map or Kakao Map, normalize break times, last orders, and closed days, and conservatively save them to Supabase. Use when the user invokes `/work-time` or asks to collect/update a shop's opening hours.
---

# Work Time

Collect and update the structured business hours for an existing Ramap shop.

Read `AGENTS.md` and the app's business-hours contracts in `domain/src/commonMain/kotlin/com/peto/ramap/domain/model/businesshour` and the current `public.ramen_shops` schema. Source extraction and database updates belong to the primary agent; an hours request does not start an implementation-agent workflow.

## Usage

```text
/work-time <kakao place url> [naver place url]
/work-time <shop name>
```

## Source priority

1. Prefer the provided Naver Map restaurant page.
2. Fall back to the provided Kakao Map place page.
3. Inspect the map page in an available signed-in browser. If a page is already open in Chrome, reuse that signed-in browser session.

For Naver and Kakao pages, expand the business-hours section using its right-side toggle before extracting data. Do not rely only on the compact “영업 중” summary. Capture every weekday, multiple operating intervals, break times, multiple last-order times, and closed-day labels.

## Workflow

1. Apply the available `supabase:supabase` skill and discover the authenticated MCP or linked CLI path. Inspect CLI commands with `--help` when using them; reuse configured credentials without printing them.
2. Resolve the `ramap` project and read the current `public.ramen_shops` schema when needed.
3. Resolve the target shop by Kakao place URL first, then Naver URL, then an exact name match. Do not create a shop from `/work-time`; use `/db-add` for that.
4. Open the source page and expand the complete weekly schedule. When both maps expose complete schedules, compare all weekdays after normalization. If they conflict, show the conflicting values and ask which schedule to use; do not choose from an unverified claim of recency. If only one complete schedule is available, use it and report the other source's absence or incompleteness.
5. Store the existing app-facing summary as `business_hours`. Do not store a separate raw-hours column; the source URL and extracted values are enough for the update report.
6. Normalize the schedule into these columns:

```text
business_hours_weekly       jsonb  {"mon":{"closed":false,"open":"11:30","close":"21:00","close_next_day":false,"label":null}, ...}
business_hours_break_times  jsonb  {"mon":[{"start":"15:30","end":"17:00"}], ...}
business_hours_last_orders  jsonb  {"mon":["15:00", "20:30"], ...}
business_hours_notice       text   nullable
business_hours_notice_type  text   nullable
```

Use `mon`, `tue`, `wed`, `thu`, `fri`, `sat`, and `sun` keys. Each weekly value must be a typed object with `closed`, `open`, `close`, `close_next_day`, and `label` fields. Keep every day explicitly shown by the source, including closed days. Keep multiple last-order entries in source order. A closed day has `closed: true`, null `open`/`close`, and no operating interval.

Each `business_hours_break_times` item must be an object with string `start` and `end` fields, for example `{"start":"15:30","end":"17:00"}`. Do not store the human-readable string `"15:30 - 17:00 브레이크타임"` in this column; that belongs only in `business_hours`. Validate the final JSON shape before writing and abort if any item is not an object with both fields.

Each `business_hours_last_orders` item must be a time-only string in zero-padded `HH:mm` format (00:00–23:59). The app adds the last-order label when rendering, so store `"20:30"` rather than `"20:30 라스트오더"`. Extract only the verified time from source labels such as `"점심시간 라스트오더 14:20"`, preserve multiple entries in source order, and keep human-readable labels only in `business_hours`. Validate every final last-order item against `^([01][0-9]|2[0-3]):[0-5][0-9]$` before writing; if a time cannot be resolved unambiguously, preserve the existing field and report the unresolved value.

Check the current schema and app model before representing multiple operating intervals. Do not collapse disjoint intervals into continuous opening hours or invent an unsupported JSON field; preserve verified source details in the summary and report any structured-model limitation. Set `close_next_day` only when the source establishes overnight hours.

7. Build `business_hours` in the app's existing compact format, for example:

```text
일 / 11:30 - 21:00 / 15:30 - 17:00 브레이크타임 / 15:00, 20:30 라스트오더 / 월 / ... / 수 / 정기휴무 (매주 수요일)
```

8. Before writing, show a concise preview containing source URL, every weekday's normalized values, and fields that will change. Ask for confirmation when the source is ambiguous, incomplete, or has conflicting schedules. A normal `/work-time` request may update a clearly verified schedule directly.
9. Update only business-hours columns and `updated_at`; preserve all shop identity, menu, visibility, and Instagram fields.
10. Verify the final row with a select query and report the updated shop id, source, and normalized schedule. A partial extraction is a preview for a decision, not a replacement schedule; preserve existing structured values until the missing weekdays or the explicitly requested limited update are resolved.

## Update shape

```sql
update public.ramen_shops
set business_hours = '<compact summary>',
    business_hours_weekly = '<weekly json>'::jsonb,
    business_hours_break_times = '<break json>'::jsonb,
    business_hours_last_orders = '<last-order json>'::jsonb,
    business_hours_notice = '<notice or null>',
    business_hours_notice_type = '<general|schedule_rule|external|mixed or null>',
    updated_at = now()
where id = '<shop_uuid>';
```

Never overwrite an existing schedule with an empty or partial extraction. Do not infer holidays, break times, or last orders from reviews, search snippets, or a single “영업 중” status.
