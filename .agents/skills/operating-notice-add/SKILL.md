---
name: operating-notice-add
description: Recognize and manually register a ramen shop operating notice from Instagram, Naver Place, or a screenshot after an explicit preview confirmation. Use for /operating-notice-add or a manual request to add an approved operating change.
---

# Operating Notice Add

Register verified closure notices through Ramap's service-only `public.admin_shop_operating_notice_records`; `public.shop_operating_notices` is the public read-only projection. Previews are text-only in the conversation; never launch a review site for this workflow.

Read `AGENTS.md`, apply the available `supabase:supabase` skill, and use configured authenticated access without printing credentials. The primary agent owns this manual registration. Inspect the current schema and the notice contracts in `supabase/functions/register-event/index.ts` and `supabase/functions/_shared/operating-notice-schedule.ts` before preparing a payload.

Naver registration uses authenticated SQL directly; the current `register-event` function and administrator form accept Instagram URLs only. Do not send Naver notices through that endpoint or deploy app/server changes as part of manual registration. The existing app shows `source_url` as a generic web link labeled “원문 보기”.

## Invocation

- `/operating-notice-add <instagram-url>`
- A bare Instagram post, reel, or story URL when the user asks to register an operating change
- A Naver Place URL or a shop name plus a Naver holiday/operating-notice screenshot
- A shop name plus caption or screenshot when no URL is available

Do not use this skill for weekly business hours; use `/work-time`. Do not use it for automatic account scanning; use `/instagram-collect`.

## Phase 1: Recognize and preview

1. Resolve the linked `ramap` Supabase project and read the current schemas for `public.ramen_shops`, `public.shop_operating_notices`, and `public.admin_shop_operating_notice_records` with `supabase db query --linked`. Use only columns and enum/check values that the live schema exposes.
2. Canonicalize an Instagram URL by removing query parameters and fragments and retaining one trailing slash. For Naver, preserve place IDs, notice paths, and meaningful query parameters; remove tracking only. Resolve short share links through the browser. Never invent a source URL. With only a shop or screenshot, use the exact stored `instagram_url` for Instagram evidence or `naver_place_url` for Naver evidence when present, and mark it as a profile/place fallback. Do not replace a Naver source with Instagram merely because the shop has an Instagram account. A user-approved profile/place fallback may be used while retaining an observed direct notice/story URL in raw evidence.
3. Resolve the shop by the supplied name, posting account or Naver place identity, and the matching `instagram_url` or `naver_place_url`. For Naver, compare the name and address in the source with the stored shop; an Instagram account is not required. If multiple shops match, ask the user to choose. If no shop exists, stop and recommend `/db-add`.
4. Open the source in an available browser, signed in when required. Read the full caption and relevant carousel/story media, or the Naver holiday entry/notice. Do not bypass private, deleted, inaccessible, or CAPTCHA-protected content; use supplied text/screenshots and disclose unavailable source verification.
5. Extract only information supported by the source:
   - notice type: the app supports only `full_close` (임시 휴무) and `early_close` (조기 마감). ‘영업 변동’ is the overall feature name, not a notice type. Confirm the live deployment accepts the payload; do not register the removed `operating_notice` or `late_opening` types even if historical rows/checks retain them.
   - exact title and original caption/description
   - notice date, end date, start time, and end time
   - canonical source URL
   - raw source type and source text when those columns exist
   - explicit dates for monthly full-day closures. Every stored closure row has null `start_time` and `schedule_override`, and empty `daily_schedules`
6. Preserve the original caption or Naver notice wording, emojis, punctuation, and line breaks in `description`. A dated Naver holiday entry such as `금(10/9) 한글날 휴무` is a `full_close` notice; recurring `정기휴무 (매주 일요일)` belongs to `/work-time`. Do not copy unrelated search results or ordinary weekly hours into the notice. Do not rewrite or infer an exact date/time. Resolve `오늘`, `내일`, and similar relative dates only from the post publication date or source observation date, and state that basis in the preview. If a month/year or an end date required by the registration contract is missing, stop and ask rather than inventing one.
   - When one source lists multiple full-close periods with no opening day between them, present and, after confirmation, register them as one continuous `full_close` interval. Preserve each stated reason in the original description.
   - If OCR is garbled or the user supplies a correction, show the captured OCR verbatim first, then use the user's corrected text verbatim (including line breaks) for `description` and `ocr_text`. Keep the collector's raw row unchanged and record that the text was user-corrected in `review_note`; never silently repair OCR.
   - `early_close` needs a verified exact closing time and takes effect at that time. “점심만 영업”, “저녁 휴무”, and “지금 마감” without a clock time require clarification before preview/registration; do not convert regular lunch-session hours into a confirmed closing time. `full_close` has null `end_time`. General time changes and opening delays are outside the supported notice types.
   - A monthly calendar is reviewed together but saved as separate `full_close` rows for explicitly closed dates, sharing the real source/raw evidence. Each row has the same notice/end date and empty `daily_schedules`; normal/open or unmentioned dates do not create closure rows. The administrator endpoint can accept a `full_close` calendar draft and inserts those individual rows in one statement. Apply `validClosureSchedules` in `supabase/functions/_shared/operating-notice-schedule.ts`; do not store one month-long `full_close` row because older app versions interpret that as continuous closure.
7. Check duplicates before preview using the shop, canonical source URL, notice date, and notice type, plus any additional identity constraint exposed by the live schema. Do not require a title column that the table does not have. For a calendar, check every resulting closure date rather than only the calendar period start.
8. Show a text-only preview containing the exact database payload:
   - shop id and name
   - notice type and its Korean meaning
   - exact description to be stored
   - notice/end dates and times
   - every resulting closure row for a calendar, including its exact dates and nullable fields
   - source URL and source type
   - source fallback status
   - linked raw evidence payload (new row or existing id), its source type, and any observation/date-confirmation note
   - material uncertainty, missing fields, or unavailable media
   - columns omitted because the live schema does not expose them, such as `title` or `status`
9. Do not write any database row in this phase. Wait for explicit confirmation referring to the latest preview.

## Phase 2: Register and verify

1. Continue only after confirmation such as `등록해줘`, `진행해`, or `확인`. Treat confirmation as applying only to the latest preview.
2. Re-read the live schema and recheck the duplicate identity immediately before writing.
   - Inspect `instagram_posts_raw` as well: `shop_operating_notices.raw_post_id` currently requires a linked raw row. Naver uses `source_type = 'naver'`, only when the live check constraint allows it. Keep the real Instagram username if known; otherwise use `naver:<verified-place-id>` in the required legacy `instagram_username` column as an internal source identifier, not an Instagram account. Do not create or modify the shop's Instagram fields.
   - Retain the observed source URL and exact source text in the raw row. For a new row, set `source_key` to that URL and record manual registration, observation date, and any user-confirmed year/month in `raw_payload`; do not guess `posted_at`. Insert the raw evidence and notice atomically.
   - `source_url` and `source_key` are unique on raw evidence. Prefer an observed direct notice/story URL to a shared profile/place URL. When only a shared URL exists, reuse the matching shop's raw row without overwriting its original caption or payload; store the current exact text in the notice's `description`/`ocr_text` and record the observation and shared-source reuse in `review_note`. Disclose reuse in the preview. Never link a different shop's raw row or invent a per-notice URL to avoid uniqueness.
3. Insert only columns present in `public.admin_shop_operating_notice_records`; do not assume `title`, `status`, or a source-type column exists. Set an approval status only when the live schema supports it and the user confirmed the latest preview. Preserve `raw_post_id`, `review_note`, confidence, and source metadata only when those columns exist and values are available.
4. Keep `source_url`, `notice_date`, nullable times, original description, and the resolved shop id. Do not upload screenshots or carousel images unless the inspected schema explicitly supports an image-path column and the user asks for it; never add source images to the repository.
5. Use `returning` on the insert and then run a follow-up `select` to verify the stored text, id, shop, notice type, dates/times, `schedule_override`, `daily_schedules`, status when present, and source URL. If the schema lacks an approval status or does not accept an approved value, omit that column rather than inventing it; manual approval is the workflow decision, not an assumed database status field.
   Verify the linked raw row's shop, source type, and URL too. Do not select absent columns; `daily_schedules` is optional and must be omitted on deployments without that column.

## Guardrails

- Never write before explicit confirmation.
- Never expose or place a Supabase service-role key in app code or conversation output.
- Never guess dates, times, shop identity, notice type, or database columns.
- Keep automatic collection separate from manual approval. `public.shop_operating_notice_candidates` was removed by `supabase/migrations/20261003012055_remove_shop_operating_notice_candidates.sql`; do not recreate it or invent candidate statuses. Retain collector raw evidence and use this skill for an explicitly approved notice registration.
- Do not change `public.ramen_shops` business hours; use `/work-time` for that.
