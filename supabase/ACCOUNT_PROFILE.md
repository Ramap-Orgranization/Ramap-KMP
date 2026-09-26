# Account profile migration

`20260923232146_account_profile.sql` adds the account profile API and private avatars using the existing Supabase Auth UID and `public.public_profiles`. It does not create an additional identity or accept community guidelines on behalf of a user. Nicknames need not be unique; first creation chooses a Korean adjective and ramen word, then persists that name. Concurrent creation uses the UID primary key and row locks.

## Apply order

Applied to the linked `ramap` project (`clbhayqiwjawaawvrhcq`) as `20260923232146_account_profile` on 2026-09-24 KST after explicit deployment approval. The local filename matches the server-generated migration version; the recorded SQL matches exactly. This was a standalone deployment; the pending review community migration was not applied.

- On the existing deployed schema without the pending review community feature, the profile migration can be applied by itself. It creates the compatible identity table and needs only Supabase Auth and Storage schemas.

Existing 11–20 character nicknames remain unchanged/readable. The new account profile API uses 2–10 Hangul syllables, Latin letters, digits or underscore. The existing community onboarding API and table retain their historical 2–20 character rule, so unchanged legacy names can still renew consent. The profile update RPC requires a compliant nickname even for photo-only changes; it does not silently truncate legacy names. Existing community acceptance timestamps and versions are preserved. `accepted_at` retains its default for the community onboarding RPC; account-only creation explicitly inserts null.

## Optional bio follow-up

`20260923233040_account_profile_bio.sql` was applied to the same `ramap` server on 2026-09-24 KST for the requested editable introduction. It adds `bio text not null default ''` and backfills existing profiles with an empty bio. The original applied migration is unchanged; the new filename matches its server-generated version and SQL exactly.

Bio is optional, single-line, and limited to 50 Unicode codepoints (including emoji as one codepoint). The client domain rule and database constraint reject LF, CR, vertical tab, form feed, NEL, and Unicode line/paragraph separators. The table constraint covers all write paths. Existing nickname, photo, consent, ownership, and suspension behavior remains intact.

The bio migration introduced one five-argument signature: `update_my_profile(p_user_id, p_nickname, p_avatar_path default null, p_remove_photo default false, p_bio default null)`. The old four-argument public wrapper is dropped before its private implementation is replaced, avoiding PostgREST overload ambiguity. Omitted or explicit-null `p_bio` preserves the stored bio; an empty string clears it. Fetch/update JSON responses include `bio`. Existing Kotlin clients ignore unknown JSON keys; the new DTO defaults missing `bio` to empty.

Bio remains readable through the authenticated own-profile RPC only; no direct column grants are added. Changes to bio reset moderation approval alongside nickname/photo changes, while no-op saves preserve approval and consent.

Server verification passed in a rolled-back transaction: save/read/clear, four-argument and explicit-null preservation, 50/51 emoji boundary, newline rejection, owner mismatch and anonymous denial. Security advisor findings were unchanged. Evidence is in `.artifacts/profile-bio/server-verification.json`. Full app login and HTTP Storage flows remain outside this database-role verification.

## Optional Instagram follow-up

`20260923234115_account_profile_instagram.sql` was applied to the same `ramap` server on 2026-09-24 KST for the requested Instagram registration. It adds `instagram_username text not null default ''`, preserving existing profiles with an empty account link. The two earlier migrations remain unchanged; the filename and SQL match the server migration record exactly.

At deployment, the client accepted a plain handle, `@handle`, or an Instagram profile URL and normalized valid values to lowercase handles. This only checked format, not account existence or ownership. On 2026-09-26, profile Instagram registration and display were removed from the app. The database column and existing values remain for now.

The update RPC has one six-argument signature, appending `p_instagram_username text default null` to the previous five arguments. The migration drops the former public wrapper before replacing its private implementation, avoiding overloaded RPCs. Current app calls omit the Instagram argument, preserving any stored value. Fetch/update JSON still includes `instagram_username`, which the app ignores.

Instagram remains accessible through the authenticated own-profile RPC, with no new direct column grants. Changes clear moderation approval through the identity trigger; unchanged values preserve approval, and consent/bio/photo behavior is retained.

Live database-role checks passed for save/read/clear, four/five/null-six argument compatibility, 30/31-character bounds, invalid and reserved handles, owner/anonymous protection, and preserved bio/column privacy. All test writes were rolled back. Security advisor findings were unchanged. Evidence is in `.artifacts/profile-instagram/server-verification.json`; external app/browser opening and account ownership are not verified by these database tests.

## API and access

Authenticated clients call `fetch_or_create_my_profile(p_user_id)` and `update_my_profile(p_user_id, p_nickname, p_avatar_path, p_remove_photo, p_bio)`. The server's optional Instagram argument is omitted. Both RPCs reject a supplied UID that differs from `auth.uid()`. Private security-definer implementations perform the writes, with empty search paths and explicit grants; public wrappers use invoker security. No client receives table write privileges or access to private consent/moderation/avatar columns. Existing community nickname read policies remain intact.

Updating nickname, avatar, bio, or Instagram clears `approved_at` through a table trigger, including writes through other entry points. Consent is unchanged. Suspended accounts cannot use the update RPC. Profile row locks serialize concurrent profile writes and existing review submission locks.

The `profile-avatars` bucket is private, limited to JPEG/PNG and 5 MiB. Client validation checks bytes against the declared JPEG/PNG signature and byte limit, using the independent profile image rule. It is signature validation, not full image decoding. Storage API enforces bucket MIME and size restrictions; SQL cannot inspect object bytes. Paths are `<auth UID>/<random UUID>.jpg|png`, immutable after upload. Own files can be read and signed for 30 minutes. A server policy rejects deleting a currently referenced photo.

Uploads precede the profile RPC. The client captures the UID, sends it to the RPC, checks the session before and after asynchronous operations, and discards responses for a different session. Cancellation propagates. Upload failures clean only the new path under a non-cancellable best-effort block. Confirmed replacement/removal cleans the previous path; a failed RPC can have committed, so its new upload is retained.

For residual orphan files after lost responses or failed cleanup, an operator should reconcile old `profile-avatars` objects against all `public_profiles.avatar_path` values, allow an age window for in-flight operations, and delete only confirmed unreferenced objects through the **Storage API**. Do not delete `storage.objects` rows directly. No cleanup scheduler is included.

## Local verification

Run `node supabase/tests/run-account-profile.cjs` with `@electric-sql/pglite` installed at `.artifacts/profile-implementation/node_modules/@electric-sql/pglite`, or pass its absolute module directory as the first argument. This branch runs only the standalone profile sequence; cross-feature migration compatibility belongs to the review branch. The runner only creates disposable in-memory databases; it cannot connect to a remote database. Fixtures in `profile-fixture.sql` must never be applied to a real project.

Verified with PGlite 0.3.14 / PostgreSQL 17.5 (including the bio and Instagram follow-ups): the standalone migration sequence, stable first nickname, UID mismatch rejection, anonymous denial, restricted columns, invalid nicknames, own/cross-user path policies, immutable uploads, referenced-photo cleanup protection, missing/cross-owner avatar rejection, consent preservation, approval reset/no-op behavior, suspension, nullable account-only consent. Bio checks cover existing-row backfill, empty/default values, Unicode length limits, every prohibited line separator, persistence, clearing, old four-argument and explicit-null five-argument clients, one RPC signature, owner/suspension/anonymous restrictions, column privacy, and approval reset/no-op behavior.

Instagram checks cover existing-row backfill, canonical handle and length constraints, reserved routes, save/read/remove, four/five-argument and explicit-null six-argument preservation, unchanged bio/consent, approval reset/no-op, column privacy, ownership, suspension, and anonymous denial.

This local suite executes PostgreSQL functions, grants, triggers and RLS with authenticated/anonymous roles. It does **not** exercise an actual Supabase Storage HTTP service, JWT verification, signed URL delivery/expiry, concurrent network sessions, or native Android/iOS image pickers. Those remain integration/device checks.

## Server verification

After deployment, verified both public RPCs use invoker security, allow authenticated execution and deny anonymous execution. RLS is enabled; the avatar bucket is private with JPEG/PNG and 5 MiB limits, and all three owner policies are installed.

A transaction using an existing Auth UID with database-local authenticated claims verified stable first nickname, nickname update, 2–10 length validation, owner mismatch rejection, missing avatar rejection, unchanged consent and anonymous denial. All test writes were rolled back; no test account was created. This is a database-role check, not end-to-end JWT authentication.

Security advisors added only the expected informational `rls_enabled_no_policy` finding for `public_profiles`: standalone profile access is deliberately through the guarded RPCs, with direct reads denied by RLS. No new warning or error was introduced; pre-existing unrelated findings remain. See [RLS advisor documentation](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy). Local deployment evidence is saved in `.artifacts/profile-implementation/server-migration-verification.json`.
