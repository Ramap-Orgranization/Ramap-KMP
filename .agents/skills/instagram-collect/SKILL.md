---
name: instagram-collect
description: Collect posts and stories from configured ramen-shop Instagram accounts, retain raw event, sold-out, and operating evidence, and save event candidates as pending in Ramap Supabase. Use for `/instagram-collect` or a manual account scan; do not collect reels or register approved events and notices automatically.
---

# Instagram Collect

The collector source of truth is `https://github.com/Ramap-Orgranization/Instagram-Collector`. Set `COLLECTOR_ROOT` to an accessible local checkout and run its documented commands there. Do not recreate the collector under `Ramap-kmp/tools`.

Read `AGENTS.md` and apply the available `supabase:supabase` skill. The primary agent runs the existing collector and explains its verified output; it does not start the product-code implementation workflow. The collector owns browser traversal, classification, and deduplication. Collection saves raw evidence and pending event candidates, with no automatic writes to `public.shop_events` or `public.shop_operating_notices`.

## Preflight and database access

1. Confirm the collector checkout and inspect its current README, `collect.py`, account schema, and supported flags before running it. If the source is unavailable, report that prerequisite; do not invent a replacement script or unsupported mode.
2. Use the collector's configured linked project and `supabase db query --linked` for database operations. Verify CLI availability and flags with `--help`, linkage, table columns, accepted statuses, and constraints. Do not print configured credentials, ask for a service-role key, or switch to ad-hoc REST/database clients.
3. Inspect `public.instagram_posts_raw`, `public.shop_event_candidates`, and `public.shop_operating_notices` read-only. `public.shop_operating_notice_candidates` was removed by `supabase/migrations/20261003012055_remove_shop_operating_notice_candidates.sql`. It is not a required table or write target. If the collector still requires it, stop before collection and report the code/schema incompatibility rather than recreating it or silently redirecting writes into approved notices.
4. Restrict collection writes to raw rows and pending event candidates. Later event review may update the candidate's supported review fields. Approved notices are read-only evidence for duplicate and cleanup checks during collection.

## Accounts and execution

`$COLLECTOR_ROOT/accounts.csv` is the account source of truth. Collect only active rows and use their verified `username`, `shop_id`, `instagram_url`, and `is_active` mapping. Do not hardcode an account list or copy one branch's identity to another.

Run `python3 "$COLLECTOR_ROOT/collect.py"` with the supported defaults. Reuse its dedicated Chrome profile; use its documented login command only when the session actually needs login. Retain the default narrow post scan and expand it only when the requested scan requires it. Do not use Apify, collect reels, or bypass private accounts, CAPTCHA, or access controls.

## Capture and classification contract

Check these behaviors in the current collector and use its artifacts for the report; do not independently replay browser collection or inserts after the process has already done them.

1. Collect the monitored account's own post links and canonicalize them before duplicate checks. Exclude other accounts' links and reels. Traverse story items until the sequence ends or a media fingerprint repeats. A profile page with follower/post thumbnails but no story-viewer controls is not a story capture.
2. Preserve raw caption, publication or observation time, media reference, OCR/accessibility text, and the supported provenance fields. When a post lacks reliable dates, only the monitored shop's own replies can supply official date evidence; ordinary customer comments cannot.
3. Determine provenance before keyword classification. Original content can become `event`, `sold_out`, or `operating_notice`. A pure reshare remains raw-only. Ambiguous provenance remains raw-only with an explicit verification note, including sold-out evidence; never lose the raw evidence or label it approved.
4. Use source-supported event, sold-out, and operating signals, not a keyword alone. Retain the named menu for a sold-out notice; do not mark the entire shop sold out when one item is named. Quantities and times come from the source, and vague `점심` or `저녁` terms do not become exact times.
5. Combine related slides from the same story run using the collector's supported grouping. Resolve relative dates for posts from publication time and for stories from observation time in `Asia/Seoul`, recording that basis. Never fabricate a missing date or time.
6. Exclude expired event candidates only when the source establishes an expired date or an already-passed explicit end time. Keep undated possible events for review. Same-day operating notices and explicitly future planned notices remain relevant; immediate-closing posts use their publication date and stories use their observation date. Pure sold-out evidence remains available for later status review.
7. Save only event candidates as `pending` in `public.shop_event_candidates`, linked to their raw row. Preserve sold-out and operating classification in supported raw metadata and the collection report; do not insert them into the removed candidate table or approved notices. If the current collector cannot retain that evidence without those writes, report the incompatibility before execution.

## Cleanup and verification

Use only the current collector's scoped cleanup after checking its compatibility. It may remove unreferenced raw stories older than the established two-day retention threshold, after checking every existing referencing table, including event candidates and approved operating notices. Do not issue a query against the removed notice-candidate table, delete posts or current stories manually, or remove pending/approved evidence.

Verify output counts with a follow-up CLI read: scanned accounts, raw posts/stories, pending events, raw sold-out/operating evidence, expired or date-skipped items, duplicates, reshares, cleaned rows, and inaccessible accounts. Distinguish process exit status from verified stored results. Reprocessing legacy rows is a separate scoped action, not a reason to replay successful captures.

## Second-pass event review

After collection, read the scan's pending event candidates with their raw caption, OCR, source URL/type, provenance, and existing event matches. Apply the conversation's event-review rules to identify expired events, non-events, price changes, ordinary operating/sold-out notices, reviews, polls, merchandise, cancellations, winner notices, duplicates, reintroductions, and renewals.

Keep candidate status `pending`. Record a recommendation only in the supported `review_note`, prefixed with `2차 검증:`, including evidence, possible date/title corrections, and uncertainty. A changed post URL can still describe the same registered event; an explicit reintroduction with a new period can be a new candidate. Do not approve, reject, or register an event in this pass.

Show a text-only preview in the conversation with shop, title, dates/times, recommendation, evidence, uncertainty, and source URL. Do not launch a review UI server. User approval of candidate review applies only to that review decision; `event-add` must read the original source and obtain approval of its registration preview. Operating notices use the separate `operating-notice-add` manual workflow, and sold-out status changes require a separately authorized review action.

## Completion report

Report verified collection totals and actionable blockers, identify any pending review, and preserve account-level failures without blocking unrelated readable accounts. Do not call pending candidates registered events, treat a reshare as the shop's announcement, or claim the collector is compatible without checking its source and current schema.
