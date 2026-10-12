# /db-add

Add a ramen shop to the Ramap Supabase database from a Kakao Map place URL.

## Usage

```text
/db-add <kakao place url> [instagram url] <menu categories>
```

Examples:

```text
/db-add https://place.map.kakao.com/853448086 https://www.instagram.com/_shin_ramen_ 아부라소바
/db-add https://place.map.kakao.com/853448086 아부라소바 마제소바
```

## Instructions

Use the `db-add` skill.

The command must:

1. Follow the Supabase workflow for DB writes.
2. Fetch Kakao place metadata for name, address, and coordinates.
3. Map Korean menu category names to Ramap category ids.
4. Check `public.shops` for duplicates by Kakao place id, Kakao URL, or Instagram URL.
5. Insert the shop when no duplicate exists.
6. Verify the final row with a `select`.
7. Report the row id, inserted fields, and any null fields.
