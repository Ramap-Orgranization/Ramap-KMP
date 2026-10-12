---
name: menu-collect-review
description: Review a completed Naver Place ramen-menu collection for scope, description, and image completeness before it is reported. Use automatically at the end of menu-collect; do not collect or register menus.
---

# Menu Collect Review

Review the just-completed `menu-collect` result before the user sees the final collection report. This is a read-only quality gate: never modify Naver, Ramap, or the collected source values.

Apply `AGENTS.md` and `docs/menu-registration-policy.md`. The primary agent performs this collection check automatically; do not invoke the post-registration role in `.codex/agents/sol/menu-registration-reviewer.md` for a collection-only request.

## Inputs

Use the full extracted DOM menu list, the ramen-scoped collection list, excluded or ambiguous items, source URL and capture time from the same collection run. Match by item identifier when available; otherwise require the same normalized name and price within that menu surface and reconcile its displayed order. Do not join same-name records across different prices or surfaces. Reopen or inspect the source DOM only when those artifacts do not make a finding decidable.

## Checks

1. **Scope:** apply the current registration policy and verified exclusion rules to every reported item. Verify toppings, sides, drinks, and non-ramen main dishes were excluded. Keep all excluded items in the raw capture and list ambiguous ramen-combination sets separately; they must not be silently included. Identify conflicts with an active manual override without changing source values or the override.
2. **Source reconciliation:** verify the reported DOM order is the explicit `추천 메뉴` order followed by the fully expanded regular-menu order, with only same-name-and-price recommendation duplicates removed. Do not treat `대표메뉴` summary panels as recommendations or featured labels. For every reported item, compare the collected fields with the same source menu item. Read the description from `.okI98` and the image from `.place_thumb img`; use the source DOM's visible menu item, not `메뉴판 이미지로 보기`. Report any source/collection mismatch without repairing the collected value.
3. **Descriptions:** list each menu under description omission only when the source `.okI98` exists but the collected `description` is blank. Do not invent or normalize beyond trimming surrounding whitespace. Put any source/collection text difference under the source reconciliation exception. A missing description does not exclude an otherwise price-confirmed menu.
4. **Images:** independently inspect each reported item's visible menu row and matching serialized Naver `PlaceMenuItem` state. A same-item HTTPS `thumbnailUrl` or `images[].url` is a source image even when the row lacks `.place_thumb img`. For every item with either source signal, require a non-null matching `image_url`; retry the existing item's one permitted in-view image-load attempt, then reread its matching state record before listing it as missing. Do not treat placeholders, `data:` URLs, blank URLs, gallery, review, feed, or `메뉴판 이미지로 보기` photos as a menu image. Put any source/collection URL difference under the source reconciliation exception; do not replace the collected URL during review. A missing image does not exclude an otherwise price-confirmed menu.

## Report

Return the review immediately after collection, before the normal collection table and Review Gate. Include:

```text
### 수집 정합성 검토
- 필터링: 통과 | 확인 필요 — <names/reason>
- 메뉴 설명 누락: <names>
- 메뉴 이미지 누락: <names>
- 원본 대조 불일치: <names/reason>
```

Omit exception lines when there is no actual exception. Use `통과` only when scope, source reconciliation, and completeness checks pass. If any item needs confirmation or has missing source data, retain it exactly as collected, report the exception, and still wait for the user's review; never repair by inference or start registration.
