---
name: menu-collect
description: Collect menu names, won prices, descriptions, and image URLs from a Naver Place restaurant menu page for user review. Use for `/menu-collect` or requests to crawl a shop's Naver menu; never write directly to the database.
---

# Menu Collect

Read the visible menu list from Naver Place without changing Naver or Ramap data.

Read `AGENTS.md` and `docs/menu-registration-policy.md` for scope and source-priority rules. Collection and the automatic `menu-collect-review` pass belong to the primary agent; `.codex/agents/sol/menu-registration-reviewer.md` is a different role used only after an approved `menu-add` database transaction.

## Usage

```text
/menu-collect <Naver Place URL>
/menu-collect
```

When no URL is supplied, use the currently open Naver Place page in Chrome and reuse the user's existing Chrome tab. If the tab is not already on the menu page, open the `메뉴` tab before extraction.

## Extraction

1. Work inside the Naver Place entry frame when the map page embeds the restaurant page. Wait for the menu list to render, scroll its full height to the bottom, then back to the top so every lazy-loaded menu image is requested. Do not extract until image loading has settled.
   - When invoked by `db-add` for independent shops, collect up to three resolved Naver place IDs concurrently. A public Naver restaurant-menu response fetched with a normal browser user agent and `https://map.naver.com/` referer is an allowed fast path; use the rendered menu as fallback whenever that response is unavailable, incomplete, or cannot provide the required raw capture.
   - Parse response records independently. Support both `PlaceMenuItem` and `PlaceDetail_BaeminMenu`; do not use a document-wide pattern that can join one item's name to another item's image or price.
2. Find the `.place_section_content` whose list items contain menu names and prices. Do not collect the separate `메뉴판 이미지로 보기` section, advertisements, reviews, or recommended nearby shops.
3. Extract every menu item in the order Naver presents them. Expand every `더보기` control in the selected menu surface before extracting or assessing images. Build the sequence from the section explicitly titled `추천 메뉴`, followed by the regular menu section after removing items with the same normalized name and price already shown in recommendations. Do not treat a separate `대표메뉴` summary panel as `추천 메뉴`, and do not use it to reorder or duplicate the regular list. If multiple ordering/fulfilment surfaces are shown, use only the surface represented by the supplied source URL and report any conflicting surface as a review exception. Prefer these current selectors, but fall back to the equivalent text/image structure if Naver changes generated class names:

```text
item          li.E2jtL
name          .lPzHi
price         .p2H02 em
description   .okI98
image         .place_thumb img
featured      .place_blind whose trimmed text is "대표"
```

4. Return one object per item with this shape:

```json
{
  "name": "이츠모라멘",
  "price": 10500,
  "description": "갖가지 돼지뼈로 오랜시간 우려낸 돈코츠 라멘",
  "image_url": "https://search.pstatic.net/common/?...",
  "source_image_url": "https://ldb-phinf.pstatic.net/.../1000039851.jpg",
  "is_featured": true
}
```

- Remove commas, spaces, and `원` from a fixed numeric price and store it as an integer in won.
- If Naver shows a non-numeric price such as `변동`, keep the visible value in `price_text` and set `price` to `null`.
- Preserve menu names and descriptions as displayed after trimming surrounding whitespace. Use `null` for a missing description or image; never infer either.
- Preserve a source empty description as an empty value rather than converting it into an invented description. Distinguish it from a missing (`null`) description in the review.
- A missing description or image is informational only: keep the menu when its name and numeric price are available. Do not exclude it from the collection or later registration candidate list for that reason.
- Collect the description from the same menu item's `.okI98` and the image from its `.place_thumb img` before applying the ramen scope filter. Do not use images outside the menu item or from `메뉴판 이미지로 보기`. For each menu item, use the first non-empty, non-placeholder URL in this order: `currentSrc`, `src`, `data-src`, `data-lazy-src`, then the first URL in `srcset` or `data-srcset`. A URL present in these DOM attributes is source data, not an inference.
- Naver can omit a rendered menu-row image while exposing it in the same page's serialized `PlaceMenuItem` state. For every menu item, also inspect that item's own `thumbnailUrl` and `images[].url`. Match the state record to its visible row by an item identifier when available; otherwise require the same normalized name and price within the current menu list. Treat a non-empty HTTPS value from those fields as the source image. Never use gallery, review, feed, or `메뉴판 이미지로 보기` photos as a substitute.
- If `hasImage` is true but neither the rendered row nor its matching `PlaceMenuItem` exposes an acceptable URL, report the menu under `사진 미수집`; do not report it as a source item without a photo.
- Treat an image as loaded only when its `img.complete` is true and `naturalWidth > 0`. If it has not loaded, scroll that item into view, wait for it to settle, and reread the URL attributes. Never accept a known placeholder, `data:` URI, or blank URL as a menu photo.
- Set `image_url` to the loaded URL. When it is a Naver image proxy URL, URL-decode its `src` query parameter into `source_image_url`; otherwise use the same URL for both fields.
- Set `is_featured` only from the visible `대표` label inside the same menu row; a `대표메뉴` section heading or summary panel never marks its children as featured.
- Skip an item only when its name is missing. Keep zero-price items.

## Menu Scope

Collect ramen-centered main dishes only. Include ramen, tsukemen, mazemen/abura-soba, cold ramen, and a set meal only when ramen is its main component.

Before the scope filter, read active `public.shop_menu_exclusion_rules` for the resolved shop through the available authenticated read path. Exclude an item when an active `name_contains` rule matches its displayed name; report it as an excluded, rule-matched item. If no existing shop can be resolved or the read is unavailable, still collect the public source and mark the rule check as unverified.

Retain source items in the full unfiltered capture even when an exclusion or manual override affects their registration eligibility. Report active override conflicts separately; do not replace Naver's observed values with database values or imply that collection revokes an override.

Exclude the following by default:

- Additional toppings: eggs, extra noodles or meat, rice, and similar add-ons.
- Sides and drinks: fried items, dumplings, okonomiyaki, snacks, desserts, soft drinks, and alcohol.
- Non-ramen main dishes: rice bowls (for example 차슈덮밥, 부타동, 규동, 오야꼬동), curry dishes, soba/memil, and jjamppong. Apply the menu-name suffix rule in `docs/menu-registration-policy.md` before this default filter: a trimmed menu name ending in `라멘` is ramen-scope, including `나가사키짬뽕라멘`; a missing description does not make its scope ambiguous. Price validation, lifecycle, active overrides, and exclusion rules still apply separately.

Do not generalize a user-requested exclusion of one named dish to every shop. When a dish is not clearly covered above, or when it is a ramen-combination set, show it separately and ask the user before including it in the review result or registration.

## Verification and Output

Before reporting, compare the extracted count with the menu list's DOM item count and check that each item has a name and either a numeric `price` or `price_text`. Verify the final sequence is exactly `추천 메뉴` items first, then the expanded regular-list order with only same-name-and-price recommendation duplicates removed. For every DOM menu item that exposes `.place_thumb img` **or** a matching `PlaceMenuItem` with `hasImage=true`, verify that the corresponding collected item has a non-null `image_url`; for a mismatch, scroll that exact item into view and retry its image load once, then reread the matching serialized item state. Report the number of source-exposed menu images and the number of collected image URLs. If any exposed image still cannot be collected, list its menu name under `사진 미수집` instead of silently omitting it; this is not an exclusion reason. If the list is absent, report that Naver exposes no crawlable menu on the supplied page instead of guessing from photos or reviews.

After extraction and verification, always run [menu-collect-review](../menu-collect-review/SKILL.md) on the same run's full DOM list, included list, and excluded or ambiguous items. Do this automatically before reporting; do not wait for the user to request a review. Put its `수집 정합성 검토` result before the collection table.

Report the shop name, source URL, collection time, item count, the automatic review result, and a concise table containing name, price, description, featured status, and image URLs. Include machine-readable JSON only when the user requests it.

## Review Gate

For a standalone `/menu-collect` request, always stop after showing the collected result and wait for the user to review it. Never download images, write files, call `/menu-add`, or mutate Supabase in that standalone collection turn, even when the original `/menu-collect` request also asks for registration.

When `menu-collect` is invoked as a substep of `/db-add`, return the same complete raw DOM list and review artifacts for its shop-registration preview. After the user approves that preview and the shop row exists, `db-add` persists and verifies the immutable `public.shop_menu_snapshots` capture before waiting for the separate menu-row approval. Collection itself remains read-only and snapshot persistence does not authorize `/menu-add`.

Start registration only when the user explicitly approves the latest displayed result and asks to register it. Then hand the exact approved item objects—including each `description`, `image_url`, and `source_image_url`—to `/menu-add`; do not split metadata into a later request. Follow its preview, approval, registration, and verification workflow. `menu-collect` must never write database rows itself, infer approval from silence, or reuse approval for a changed or newly collected result.
