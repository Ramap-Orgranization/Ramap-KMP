---
name: insta-add
description: Attach an Instagram profile URL and its public profile image to one existing Ramap shop without creating a shop or changing its other fields. Use when the user invokes `/insta-add` or asks to add only Instagram information to an existing shop.
---

# Instagram Add

Update only `public.ramen_shops.instagram_url` and, when available, `public.ramen_shops.instagram_profile_image_path` for an existing shop.

Read `AGENTS.md` and apply the available `supabase:supabase` skill before database operations. Resolve the configured project without printing credentials and use an authenticated MCP or linked CLI path for writes. This existing-shop update belongs to the primary agent and does not invoke the staged implementation workflow.

## Usage

```text
/insta-add <shop name> <instagram profile url>
```

Example:

```text
/insta-add 멘탄 https://www.instagram.com/mentan.kr/
```

## Workflow

1. Resolve the configured Supabase project and inspect the current `public.ramen_shops` schema and row before writing. The app's configured anon key is not an administrative write credential.
2. Validate the supplied URL identifies a profile on `instagram.com` or `www.instagram.com`, not a post, reel, story, or login route. Normalize a valid profile to `https://www.instagram.com/<username>/` without query parameters or a fragment; show any normalization in the result.
3. Find existing shops by exact `name`, selecting `id`, `name`, `instagram_url`, and `instagram_profile_image_path`.
   - If no row exists, stop; never create a shop from this command.
   - If multiple rows match, stop and ask for a more specific identifier such as the shop UUID or Kakao URL.
   - If the URL is already attached to another shop, verify whether it is the same chain's shared official account. Reuse it only with clear branch/account evidence or an explicit user instruction; report unrelated or ambiguous matches as conflicts.
   - Compare existing and supplied URLs by their canonical profile identity. If the matched row has a different non-null profile, stop instead of overwriting it unless the user explicitly requested that replacement.
4. Inspect the public profile through an available browser or HTTP tool and extract `og:image`. Decode HTML entities and accept only HTTPS URLs on `cdninstagram.com` or its subdomains. If extraction fails, preserve the explicitly user-supplied profile URL update and report that its image was skipped; do not substitute an unrelated image.
5. If the matched row already has `instagram_profile_image_path`, keep it. Otherwise retrieve the verified original image for upload and accept only JPEG, PNG, or WebP files up to 10 MiB, based on both MIME type and actual file type.
6. Upload a new image as `<shop_uuid>.<ext>` to the public `shop-profile-images` bucket using the linked Supabase CLI. Verify its public URL returns HTTP 200 before saving the path.
7. Patch only these columns on the matched row:

```json
{
  "instagram_url": "<normalized supplied profile url>",
  "instagram_profile_image_path": "<verified object path or existing path>"
}
```

If the image was skipped and no existing path exists, patch only `instagram_url`.
8. Verify the final row. Do not insert or modify `name`, `address`, coordinates, Kakao/Naver URLs, hours, menu categories, visibility, or any other field.

For the image validation and upload details, reuse the **Instagram Profile Image** procedure in `.agents/skills/db-add/SKILL.md`.

## Report

Report the existing row ID, the Instagram URL, the final profile-image path, and any skipped image reason. State that no other shop fields were changed.
