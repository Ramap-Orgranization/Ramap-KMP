---
name: db-add
description: Add or conservatively update one or many ramen shops in Ramap Supabase from names or place links. Use for single-shop registration and batched discovery, validation, snapshots, and reporting.
---

# DB Add

Register or conservatively update one or many ramen shops in `public.ramen_shops`. The user may provide a name, place links, hours, or categories per shop. Discover missing fields from public place pages and the existing Ramap database, then write only data supported by a reliable source.

## Execution context

Read `AGENTS.md` and `.codex/commands/db-add.md`. The command delegates here for registration details; its older `public.shops` example is not the write target. `supabase/migrations/20261003013853_remove_legacy_shops_view.sql` removes that compatibility view. Inspect the current `public.ramen_shops` schema before querying or writing.

Apply the available `supabase:supabase` skill for connection and CLI guidance. Use the configured project and an authenticated Supabase MCP or linked CLI path for administrative writes. `local.properties` already contains the app configuration; do not print its keys or ask for them again. Discover CLI flags with `--help`. The primary agent owns registration; only `menu-add`'s final review requires its dedicated reviewer.

## Usage

```text
/db-add <shop name> [Kakao/Naver/Instagram URL] [hours] [categories]
/db-add <shop 1>
<shop 2>
...
/db-add <Kakao/Naver URLs, one per line>
```

Examples:

```text
/db-add 산미태
/db-add 멘지 위례중앙점 https://naver.me/xxxx 토리 쇼유
/db-add https://place.map.kakao.com/853448086 아부라소바
/db-add 산미태
멘지 위례중앙점 https://naver.me/xxxx
https://place.map.kakao.com/853448086
```

## Registration gate

`/db-add` is discovery-first. Shop input authorizes discovery and preview preparation; insert, update, profile-image upload, and snapshot persistence wait for explicit approval of that preview.

After read-only discovery, duplicate checks, and menu collection/review, show a registration preview. Collect the complete crawlable menu and prepare the ramen-scoped snapshot before displaying this preview; do not wait for registration approval to collect it. For each shop, include its batch index, official name, address, canonical map identifiers, normalized hours or a clearly marked missing schedule, categories, duplicate action (`create` or `update missing fields`), Instagram status, menu-source URL, and snapshot plan (`collected in preview; persist after shop write` or `no crawlable menu`). Coordinates remain an internal validation and database field; do not show them in the user-facing preview unless the user explicitly asks for diagnostics. For batches, show totals and one compact row per shop.

Wait for explicit approval that identifies the preview, such as `1~10 등록 승인`, `2, 4번만 등록`, or `방금 미리보기 전부 승인`. Do not treat a shop list, a request to collect data, silence, or a menu approval as registration approval. If source data changes after the preview, show a new preview and require fresh approval.

After approval, mutate only the approved rows, then persist and verify the already-collected immutable snapshot for every approved shop with a crawlable menu. Do not recollect unless the preview collection failed or the source changed. The same approval authorizes snapshot persistence; it never authorizes `menu-add` rows. Do not stop after creating shop rows or ask for another approval before persisting and verifying the planned snapshots. Complete those snapshots first, then show the separate menu-registration preview required by `menu-add`.

### Registration preview format

For batch previews, use this order and keep it compact:

1. Start with a totals line: `등록`, `업데이트`, `중복 입력 스킵`, `결정 필요`, `실패/미확인`, and `메뉴 스냅샷 예정`.
2. Show one Markdown table with one numbered row per input item. Prefer these columns: `#`, `매장 / 주소`, `지도 식별자`, `영업시간`, `카테고리`, and `메뉴 스냅샷`. Do not add a `좌표` column to the normal user-facing result.
3. Render the canonical Kakao/Naver URLs in the `지도 식별자` cell as compact labeled links, for example `[📍 Kakao](...) / [N Naver](...)`, instead of printing long URLs or coordinates. Use short status text in other cells, such as `수집 완료, 라멘 8종`, `미확인`, or `크롤 가능한 메뉴 없음`.
4. Put shared caveats below the table: Instagram/account-list status and any fields intentionally left null.
5. Put decision-required items in a separate short list after the caveats. End with one copyable approval sentence, such as `1~16 등록 승인` or `1, 4번만 등록 승인`.

For a single shop, use the same information in a compact block rather than forcing a table. Do not expose snapshot IDs, hashes, parser versions, raw counts, or internal diagnostics unless the user asks for diagnostics.

## Batch input and execution

Treat each non-empty input line as one shop request. A line may contain a shop name plus its URLs and optional hours/categories; a URL-only line is also one request. If the user supplies a table, CSV, or numbered list, preserve each row/item as one request. Keep the input order and assign a stable batch index for reporting.

Single-shop input uses the same workflow below. For two or more requests:

1. Normalize and deduplicate input first by confirmed place identity: canonical Kakao ID or resolved Naver place ID. An exact normalized Instagram URL alone is not a duplicate when different branches share an official account; confirm the same shop/address before skipping a profile-only match. Keep the first confirmed occurrence and report later duplicates as skipped; do not merge same-name shops without an address/region match.
2. Resolve sources and validate identity, coordinates, hours, and categories independently. Reuse already-resolved chain Instagram mappings and place metadata within the batch, but never copy a branch's address, coordinates, or hours to another branch.
3. Do read-only discovery in batches where the source/tool permits it. Before mutations, query existing `ramen_shops` rows for all resolved identifiers in chunks small enough for the active client; do not run a full-table name scan.
4. Prepare ready rows in input order. One blocked or failed shop must not block unrelated ready shops. Do not parallelize conflicting writes to the same resolved shop identity.
5. Group only decision-required items into one concise question. Include batch index, proposed official name/address, sources, and the exact ambiguity or hours conflict. Include unambiguous rows in the registration preview; do not register any row before approval.
6. Before showing the registration preview, collect independent menus in batches of up to three shops, run `menu-collect-review`, and show one combined ramen-scoped snapshot preview. After registration approval, run Instagram-image upload and persist/verify one immutable snapshot per approved crawlable menu using those collected artifacts, then require explicit approval for that exact group before invoking `menu-add`. A menu approval is needed only for writing menu rows, not for collection or snapshot persistence.

Use chunks of up to 10 shops by default. For a larger list, finish and report each chunk before moving to the next; retain resolved, verified identifiers to avoid repeated discovery. Stop only for a systemic failure (for example, lost database access), a user decision that affects remaining rows, or an explicit user request to stop.

## Operating principle

Prepare a registration preview when shop identity and a complete weekly schedule are clear. Ask only when a decision can materially change the proposed row:

- multiple place candidates match the name;
- Kakao/Naver sources disagree on identity; or
- Naver and Kakao expanded schedules differ;
- address or coordinates cannot be verified;
- the schedule is incomplete or only a compact “영업 중” summary is available.

Do not block preview merely because a category, rating, or Instagram image cannot be verified. Leave that field empty and report it.

## Automatic discovery

1. Parse every input URL regardless of order. Accept Kakao URLs with query parameters, Naver `naver.me` short URLs and `map.naver.com/p/entry/place/<id>` URLs, `http` or `https` Instagram URLs, trailing slashes, and Instagram query strings.
2. If a source is missing, search in this order:
   - existing `public.ramen_shops` rows, especially same-brand/chain rows;
   - Naver place search/page;
   - Kakao place search/page;
   - the shop’s official Instagram or official site.
3. Use a place candidate only when name, region/address, and shop context agree. If several candidates remain plausible, stop and ask instead of guessing.
4. Use Kakao metadata for identity and coordinates when available:
   - `og:title` → source name;
   - `og:description` → address;
   - `twitter:image` static map `m=<lng>,<lat>` → coordinates.
   For every Naver place page, store its resolved canonical entry URL in `naver_place_url` as `https://map.naver.com/p/entry/place/<id>`. When the source is a `naver.me` short URL, follow its redirect and store the resolved `map.naver.com` URL. Do not store the short URL. If a stable place ID cannot be resolved, leave `naver_place_url` null and report it rather than fabricating a URL.
5. If Kakao is unavailable, use a verified Naver address and geocode it with Nominatim only as an approximate fallback. Report that coordinates are approximate. Never invent an address.
6. If a user branch name differs from the official place-page name, store the place-page name and report the difference. Distinguish same-name branches by Kakao ID/address.

### Shared chain Instagram

When the user says a branch shares another chain’s Instagram, or supplies no Instagram URL for a recognizable chain:

1. Query existing `public.ramen_shops` rows by normalized brand/name and inspect their Instagram URLs.
2. Reuse an existing exact profile URL only when the chain match is clear. Keep separate shop-to-profile mappings for each branch.
3. If no existing mapping is trustworthy, discover the official profile from the place page or official web presence. Do not select a similarly named personal or unrelated account.

Verify a newly discovered profile read-only before including its URL in the registration preview. If it cannot be verified, leave that discovered field null and report it. An explicitly supplied profile URL can remain in the preview with its verification status shown; do not turn a failed image fetch into evidence that the user supplied an invalid account.

## Hours discovery and normalization

Use the `work-time` skill for the source and JSON shape. Prefer a complete expanded Naver weekly schedule, then Kakao. Compact search snippets or a single “영업 중” line are not enough.

Priority when values differ:

1. explicit complete hours supplied by the user;
2. expanded Naver schedule;
3. expanded Kakao schedule.

When both Naver and Kakao expanded schedules are available, compare every weekday's open/close time, break interval, last order, and closed-day status after normalization. If any value differs, do not write that shop before asking the user which schedule to use. In a batch, group these conflicts while continuing with unrelated shops. Show the conflicting values by source; do not silently apply the priority order above. If only one map provides a complete schedule, use that source and report the other source was unavailable or incomplete.

Capture all seven keys: `mon`, `tue`, `wed`, `thu`, `fri`, `sat`, `sun`. Preserve closed days, holidays or recurring-closure notes, multiple intervals, break times, and multiple last orders.

```text
business_hours_weekly       jsonb  {"mon":{"closed":false,"open":"11:00","close":"21:00","close_next_day":false,"label":null}, ...}
business_hours_break_times  jsonb  {"mon":[{"start":"15:00","end":"17:00"}], ...}
business_hours_last_orders  jsonb  {"mon":["20:30"], ...}
```

Every break item must be an object with string `start` and `end`. Keep human-readable break text only in `business_hours`. Validate the final JSON before writing. A closed day has `closed: true`, null `open`/`close`, and an appropriate label.

Keep last-order arrays as time-only `HH:mm` strings and validate them using `work-time`. Last-order labels belong only in the human-readable `business_hours` summary; the app adds its own label to structured values.

If the user provides a dated seven-day schedule, normalize it to recurring weekdays only when all seven weekdays are represented and no date-specific exception is implied. Preserve an explicit holiday or closure note in `business_hours_notice` when supplied.

Never guess missing weekdays, break times, last orders, or holiday rules. If the schedule is partial, leave business-hours fields null for a new row or preserve existing values for an existing row, and report the missing portion.

## Category discovery

Use user-provided categories first. If absent, inspect the Naver/Kakao menu, official profile, or official site. Map only categories supported by the source; do not infer a category from the shop name alone.

```text
돈코츠 -> tonkotsu
쇼유 -> shoyu
시오 -> shio
미소 -> miso
토리 -> tori
츠케멘 -> tsukemen
마제소바 -> mazesoba
아부라소바 -> aburasoba
지로계 -> jiro
니보시/교카이/어패류 -> niboshi_gyokai
이에케/이에케이 -> iekei
토마토라멘 -> tomato
```

If no category is confidently supported, use an empty `text[]` and report that categories were not verified. Do not fabricate categories.

## Menu snapshot and registration

During registration-preview generation, always run the menu workflow when a crawlable Naver or Kakao menu is exposed. Prefer Naver and invoke `menu-collect` with the resolved Naver place URL when available. A standalone `menu-collect` remains read-only; this `db-add` orchestration collects and reviews the menu before registration approval, then persists the immutable snapshot only after the approved shop row exists.

1. Collect the complete visible menu before filtering. Scroll the menu to the bottom and back to the top, wait for lazy images, retain the full DOM-order list, and run `menu-collect-review`. The snapshot source must include every menu item, including excluded sides, drinks, rice, and ambiguous items; do not save only the ramen-filtered subset.
   - Extract item images from the page's serialized menu data first (`thumbnailUrl`, `images[].url`, or the equivalent source field). Do not rely on accessibility-tree text alone; it does not expose image attributes.
   - If serialized data has no image URL, fall back to the item's rendered DOM image attributes in this order: `currentSrc`, `src`, `data-src`, `data-lazy-src`, then the first usable URL from `srcset`.
   - Decode escaped URLs and keep the first non-empty HTTPS image URL. Preserve `null` only when both serialized data and the DOM fallback expose no item image.
   - Before showing the preview, compare the rendered/source image count with collected `image_url` values. If the source exposes an item image but the collected object has `image_url = null`, treat collection as failed and retry using the fallback path; do not persist the snapshot or continue to menu registration with that mismatch.
2. Keep the reviewed full capture in memory for the registration preview. After the shop row is created or resolved and only for an approved row, insert one immutable capture into `public.shop_menu_snapshots`, even when the menu is unchanged from an earlier capture. Use the existing schema:

```text
shop_id, source_url, parser_version, source_hash, raw_dom,
menu_items, menu_fingerprint, captured_at (default now())
```

   - `source_url`: the resolved menu page URL used for extraction;
   - `parser_version`: the current collector version, currently `naver-menu-html-v3` unless the extraction format has changed;
   - `raw_dom`: the complete menu-list DOM captured after lazy loading;
   - `menu_items`: the complete raw DOM-order item list, preserving `name`, numeric `price` or `price_text`, `description`, `image_url`, `source_image_url` when available, and `is_featured`;
   - `source_hash`: SHA-256 of `raw_dom`;
   - `menu_fingerprint`: SHA-256 of a deterministic JSON serialization of `menu_items` in DOM order.

   Verify the inserted snapshot by `id`, item count, source URL, and hashes before reporting it as saved. If the insert fails, inspect the error and retry once; do not claim the snapshot was saved. Never replace or delete an earlier snapshot. Keep snapshot IDs, hashes, raw-item counts, parser versions, and source URLs internal unless the user asks for diagnostic details. User-facing progress should say only that the snapshot was saved, then present the same ramen-scoped menu approval preview already collected during registration preview.
3. Show the ramen-scoped snapshot during registration preview and wait for the user's explicit approval of the registration preview before writing the shop. Saving the snapshot after shop write does not approve menu rows; a general approval to register the shop does not approve menu items.
4. On approval, invoke `menu-add` with the exact approved menu objects. Let `menu-add` recheck the shop, sections, and items, write descriptions and image URLs atomically with each item, and verify `visible_shop_menu_items`.
5. If no usable crawlable menu page is available, finish the shop registration and report that no snapshot could be saved. Do not invent menu items or prices.

Keep menu collection, snapshot persistence, and menu-row registration separate from shop identity, hours, categories, Instagram, and collector-list writes. `db-add` may update user-supplied shop categories, but menu rows must be written only through `menu-add` after the previewed snapshot receives explicit menu approval.

## Direct menu throughput

For a resolved Naver place ID, `menu-collect` may use Naver's public restaurant-menu response as a fast path. Fetch independent shops concurrently in groups of at most three with a normal browser user agent and `https://map.naver.com/` referer; fall back to the rendered Naver menu when the response is unavailable or incomplete.

- Parse each menu object independently. Naver currently exposes both `PlaceMenuItem` and `PlaceDetail_BaeminMenu` shapes; never use a broad match that can merge fields from adjacent objects.
- Prefer image URLs from the serialized menu object (`thumbnailUrl`, `images[].url`, or the equivalent field). If absent, inspect the matching rendered menu card's `currentSrc`, `src`, `data-src`, `data-lazy-src`, and `srcset` in that order. Never infer an item image from a generic menu-board image.
- Decode escaped image URLs, preserve a source `null` description as null, and preserve a source empty description as empty. Do not invent either value.
- Validate before preview and before snapshot persistence: if the rendered/source menu contains item images but the collected item list contains fewer non-null `image_url` values, fail or retry collection instead of saving partial image data. `menu-add` must receive the exact reviewed `image_url` values.
- Keep the full unfiltered menu response/list for snapshot capture. Filter to ramen scope only for the approval preview.
- After group approval, let `menu-add` write each shop atomically and verify all shops with one aggregate read. Retry only the failed shop once; never replay a successful shop or create a second regular section.

## Supabase workflow

1. Resolve the `ramap` project and read the current `public.ramen_shops` schema when needed. Perform all discovery and duplicate queries read-only before showing the registration preview.
2. Resolve all missing public fields before mutation. Use canonical Kakao URL `https://place.map.kakao.com/<place_id>` and the resolved Naver entry URL `https://map.naver.com/p/entry/place/<id>`.
3. Check duplicates by Kakao place ID/URL first, then the stored resolved Naver entry URL, then exact normalized Instagram URL. For a batch, make this a chunked set query over all non-null resolved identifiers and map matches back to their batch index. An Instagram match is only an identity candidate: a verified shared chain account can belong to several distinct branches. Do not treat it as a duplicate without matching place identity or address/region. A name match alone is not a duplicate unless address/region also agrees.

```sql
select id, name, address, naver_place_url,
  instagram_url, instagram_profile_image_path, menu_category_ids
from public.ramen_shops
where kakao_place_url = 'https://place.map.kakao.com/<place_id>'
   or naver_place_url = '<resolved_naver_entry_url>'
   or instagram_url = '<normalized_instagram_url>';
```

4. If a confirmed duplicate exists, do not create a second row. Include only missing fields or fields explicitly supplied by the user in the preview; preserve identity, visibility, categories, hours, and Instagram data outside scope. After approval, update only approved fields and verify the final row. If identity still resolves to more than one row after distinguishing shared accounts and branch addresses, skip mutation for that request and report it for review.
5. If no duplicate exists, insert only after approval:

```sql
insert into public.ramen_shops (
  name, address, lat, lng,
  kakao_place_url, naver_place_url, instagram_url,
  business_hours, business_hours_weekly, business_hours_break_times,
  business_hours_last_orders, business_hours_notice,
  business_hours_notice_type, menu_category_ids, is_visible
)
values (
  '<name>', '<address>', <lat>, <lng>,
  '<kakao_url_or_null>', '<resolved_naver_entry_url_or_null>', '<instagram_url_or_null>',
  '<compact_hours_or_null>', '<weekly_json>'::jsonb,
  '<break_json>'::jsonb, '<last_order_json>'::jsonb,
  '<notice_or_null>', '<general|schedule_rule|external|mixed_or_null>',
  array[<category_ids>]::text[], true
)
returning id, name, address, lat, lng,
  kakao_place_url, naver_place_url, business_hours,
  instagram_url, menu_category_ids,
  instagram_profile_image_path, is_visible, created_at, updated_at;
```

The SQL above is a payload shape, not a command with literal placeholders. Use SQL `NULL` for missing values, never the string `'null'`. Bind values where the client supports it; otherwise safely quote source values in an exact SQL file passed with `--file`, without interpolating captions or URLs into shell code.

## Instagram profile image

Run after the row exists. Skip when `instagram_url` is null. If an existing row already has a profile image path, keep it unless the user explicitly requests a refresh.

1. Parse the username from the normalized profile URL.
2. Fetch the public profile with an available browser or HTTP tool and extract `og:image`.
   A missing or rejected `og:image` prevents image upload; it is not proof that the account is invalid. Newly discovered URLs must already have been verified during preview preparation. Preserve the approved profile URL and any existing image path if the later image fetch fails, and report that failure.
3. Decode HTML entities. Accept only HTTPS URLs hosted by `cdninstagram.com` or a subdomain. Reject login placeholders, generic images, unavailable/private profiles, and non-image responses.
4. Retrieve the verified original image through an available tool into a temporary directory for the approved upload. Accept only JPEG, PNG, or WebP files up to 10 MiB after checking MIME type and actual file type.
5. Upload to the public `shop-profile-images` bucket as `<shop_uuid>.<ext>`:

```text
supabase storage cp <local_image> \
  ss:///shop-profile-images/<shop_uuid>.<ext> \
  --linked --content-type <mime_type> \
  --workdir <repository>
```

6. Verify the object exists in `storage.objects` and its public URL returns HTTP 200 before updating:

```sql
update public.ramen_shops
set instagram_profile_image_path = '<shop_uuid>.<ext>', updated_at = now()
where id = '<shop_uuid>'
  and exists (
    select 1 from storage.objects
    where bucket_id = 'shop-profile-images'
      and name = '<shop_uuid>.<ext>'
  );
```

Shared profiles still get one UUID-named object per shop. A failed image fetch or upload must not roll back a valid shop row; report the skipped image.

## Completion report

For one shop, report only:

- registered or updated shop name and row ID;
- duplicate rows skipped, if any;
- source-discovered fields and any official-name difference;
- normalized hours/closures and categories;
- Instagram URL/image status;
- menu snapshot save status only; include IDs, hashes, sources, parser details, and raw capture counts only when the user explicitly requests diagnostics;
- intentionally null or unverified fields;

For a batch, start with totals for `registered`, `updated`, `duplicate input skipped`, `needs decision`, `unverified/failed`, and `menu snapshots saved`. Then give one compact row per batch index with shop name, row ID or status, and only the reason/action needed. Do not expose snapshot IDs, hashes, parser details, sources, or raw capture counts unless the user explicitly requests diagnostics. Do not claim a batch is complete while any approved crawlable menu lacks a verified snapshot or any item is waiting on a user decision; identify those pending indices.
