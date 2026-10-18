---
name: menu-add
description: Register or update menu sections and items for an existing Ramap shop from a pasted menu, map URL, image, or structured prompt. Use for `/menu-add` and manual menu registration requests; do not use to create a shop.
---

# Menu Add

Add verified menu data to an existing `public.ramen_shops` row. Before every registration, read and apply [`docs/menu-registration-policy.md`](../../../docs/menu-registration-policy.md). It is the source of truth for menu scope, lifecycle status, duplicate handling, and safe mutation. Menu names and prices are required; descriptions and images are optional.

Read `AGENTS.md` and apply the available `supabase:supabase` skill for schema access and authenticated writes. The primary agent performs preview and approved registration; the separate reviewer owns only Phase 3. A menu registration request does not start a product-code implementation workflow.

## Usage

```text
/menu-add <shop name or Naver/Kakao URL> <menu list>
```

The user can optionally specify `카테고리`, one or more `대표 메뉴`, event information, descriptions, and image URLs.

When `db-add` submits independently approved menu previews, process up to three shops as one batch. Keep each shop's source objects, approval, section, transaction, and verification independent.

## Phase 1: Resolve and Preview

1. Resolve an existing shop. Prefer a supplied Naver/Kakao place URL, then an exact name and address match. If multiple branches remain, ask the user to choose. Never create a shop in this workflow.
2. Inspect the live `shop_menu_sections`, `shop_menu_items`, `visible_shop_menu_items`, latest `shop_menu_snapshots`, active `shop_menu_manual_overrides`, and applicable exclusion rules. Confirm that `shop_menu_items` stores `description` and `image_url` on the same menu row. Stop and report missing schema or source evidence.
3. Extract only source-supported menu name, integer won price, optional description, image URL, representative flag, section, and ordering. For Naver snapshots with both `추천 메뉴` and a regular list, use the displayed sequence of recommendations followed by regular-list items not already present with the same normalized name and price. Use that sequence after scope filtering as contiguous `display_order`; never derive it from insertion time, alphabetical order, or price. Preserve current source wording; do not invent price, image, event, category, or a replacement name.
4. Compare the latest source snapshot and operating menu read-only. Classify every candidate as `유지`, `추가`, `삭제`, `업데이트`, or `수동 검토` using the policy. Keep only ramen-scope items; leave ambiguous scope, semantic duplicates, price conflicts, parenthesized variants, and branch uncertainty as `수동 검토`.
5. Normalize section and menu names by trimming, lowercasing, and removing every whitespace character. Same normalized menu names with different prices are price conflicts, not additions. Detect semantic old/new duplicates only when the policy evidence is sufficient; otherwise do not delete.
6. Use `상시메뉴` only as the existing regular section. Create an event or limited section only with source evidence. Do not create a new regular section, and mark empty legacy sections for approved cleanup.
7. If `대표 메뉴` is supplied, resolve every named item from the preview or existing shop menu. Ask when any name is ambiguous. If omitted, leave current representative menus unchanged. Treat an explicit `대표 메뉴 해제` request as the only instruction to remove a representative designation.
8. Prepare a structured preview for the audit trail: shop ID/name/address, source URL and snapshot time, each source and DB value, lifecycle status, duplicate or price-conflict evidence, section cleanup, and representative-menu change. The user gave a standing instruction on 2026-10-07 to write source-verified menu price changes, source-order updates, additions, evidenced deletions, and same-menu image changes directly to the DB without asking for approval or waiting for preview approval. Keep the preview internal until after the write. Under the 2026-10-16 user instruction, a confirmed absence from the complete current Naver menu is deletion evidence. Preserve only an exact menu with evidence that the user directly registered it; existing DB presence, ordinary scope approval, and admin_verified alone do not establish that exception. An incomplete collection is not absence evidence. Continue to hold unresolved scope, duplicate identity, competing-source/active-override conflicts, substantive description changes, menu renames, and composition changes. This standing instruction removes the approval wait, not source verification or conflict checks.

Apply the policy's source priority before classifying snapshot differences. An active `suppress` prevents re-registration; an active `upsert` establishes the already verified registration scope. Source-absence protection requires exact evidence of direct user registration bound to the shop and menu UUID. An ordinary upsert does not protect an absent menu and must be revoked in the same deletion transaction to prevent re-registration. Do not expire an override just because `review_after` has passed. Include any proposed override action and its minimal evidence in the preview; never put personal contacts or DM/call transcripts in an override note.

Determine registration provenance from the actual request. A user-provided menu list, attachment or owner-confirmed menu submitted for direct registration needs exact direct-registration bindings; approving a source-collected candidate's scope does not. Record the minimal original human evidence reference and hash with the resolved shop/menu UUIDs. Missing protection records are not proof of automatic import. Before deleting, investigate original registration inputs, audit history, uploaded media, notes and aliases using the common `tools/menu-refresh/scripts/deletion_guards.py` contract. Stop that deletion when provenance or whole-source coverage is unresolved.

Never write menu rows during Phase 1.

## Phase 2: Register and Verify

1. Continue under the registration policy's standing approval or explicit approval referring to the latest preview; do not ask again for an already authorized change.
2. Recheck the shop, relevant sections, items, representative item, and active manual overrides immediately before mutation. If a value relevant to the preview has changed, revise the affected preview before applying it.
3. Apply only approved `추가`, `업데이트`, `삭제`, section-cleanup, and manual-override actions in one transaction. Revalidate every target ID, shop identity, normalized name, and approved price immediately before mutation. An approved manual confirmation and its regular-menu changes must be applied together; a `suppress` alone does not create a section. For deletes, run the exact SQL with `ROLLBACK` first; commit only when planned and actual counts match.
4. Create missing approved sections and items with the normalized-name unique conflict target and `ON CONFLICT DO NOTHING`, then requery. Persist each approved item's `name`, `price_krw`, `price_text`, `description`, `image_url`, representative flag, and source `display_order` together. When a verified fixed price changes, keep `price_krw` and any non-null `price_text` synchronized to the source value. Leave `shop_menu_items.source_url` null unless the user explicitly requests a per-menu source link; never copy a map page or snapshot source URL into it. Keep required extraction provenance in `shop_menu_snapshots.source_url`. Update only approved changed fields. Never overwrite conflicting description, image, source-link, representative metadata, or source order.
5. When approved, remove confirmed out-of-scope or obsolete duplicate rows and empty legacy sections. For an event menu, attach `event_id` only after resolving one existing event; never create or infer an event here.
   For a direct user registration or confirmed recovery, persist the exact `manual_menu_protection_bindings` under `manual_menu_protection_authority=human_user` in the same transaction as the menu rows. Use `manual_menu_protections.validate_binding`; preserve prior bindings and distinguish inactive events from source absence. Requery authoritative bindings, then refresh the validated cache. A recovery preserves original UUIDs and values, supersedes the old deletion audit without rewriting it, and never replays old successful SQL.
6. Query `visible_shop_menu_items` after commit and verify no approved deletion remains, all approved kept/added/updated rows persist, no unintended duplicates remain, and no empty legacy section remains. Report the final statuses, metadata, representative menus, skipped duplicates, and items hidden because their attached event is inactive. Stop and report any mismatch; do not claim completion.

## Phase 3: Independent Final Review

After Phase 2's database transaction succeeds, delegate the final read-only check to the named role `menu_registration_reviewer` defined in [`.codex/agents/menu-registration-reviewer.toml`](../../../.codex/agents/menu-registration-reviewer.toml). This standalone TOML includes the runtime settings and `developer_instructions` for the review. If the named role is unavailable, spawn an agent with the TOML's runtime settings. Give it the exact approved source objects, source URL and snapshot time, scope/exclusion and manual-override decisions, and the post-transaction `visible_shop_menu_items` rows. The assigned reviewer does not delegate this review again. Wait for its result before claiming registration is complete; the database commit does not authorize a Git commit.

The reviewer must independently confirm the source-presented ramen-scope sequence maps one-to-one to the visible DB rows in the same `display_order`, and that every source menu item with a valid image has the same non-empty DB `image_url`. A source item without an image is informational, not a failure. The reviewer must report source-only, DB-only, order, name/price, and image mismatches separately. Every remaining DB-only row needs exact evidence of direct user registration; otherwise the review fails even when it matches the prepared expected state. If source evidence is insufficient or any mismatch remains, stop and report it; do not retry, mutate, or claim completion.

For an approved multi-shop batch, do the read-only precheck for all shops first, then write one shop at a time. A successful shop must not be replayed after another shop fails. Run the independent final review for each successfully committed shop. Finish with one aggregate verification of item count, image count, and description count per shop; retry only the failed shop once after diagnosing the Phase 2 mismatch. Never retry a Phase 3 mismatch automatically.

Never expose service-role secrets or place them in app code. Never modify shop identity, hours, categories, Instagram, or collector files in this workflow.
