// Disposable PostgreSQL WASM only; never connects to Supabase or a live database.
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { PGlite } = require(resolve(process.argv[2] || '.artifacts/profile-implementation/node_modules/@electric-sql/pglite'));
const read = path => readFileSync(resolve('supabase', path), 'utf8');
(async () => {
  const db = new PGlite();
  try {
    await db.exec(read('tests/profile-fixture.sql'));
    await db.exec(read('tests/fixtures/historical-review/20260905100001_create_shop_reviews.sql'));
    await db.exec(read('tests/fixtures/historical-review/20260905100002_add_shop_review_images.sql'));
    await db.exec(read('migrations/20260923232146_account_profile.sql'));
    await db.exec(`set role authenticated;
      select set_config('request.jwt.claim.sub','22222222-2222-2222-2222-222222222222',false);
      select public.fetch_or_create_my_profile('22222222-2222-2222-2222-222222222222');
      select public.update_my_profile('22222222-2222-2222-2222-222222222222','기존차슈');
      reset role;`);
    await db.exec(read('migrations/20260923233040_account_profile_bio.sql'));
    await db.exec(`select public.profile_test_assert((select nickname = '기존차슈' and bio = '' from public.public_profiles where user_id = '22222222-2222-2222-2222-222222222222'), 'bio migration preserves existing identity and backfills empty bio');`);
    await db.exec(read('migrations/20260923234115_account_profile_instagram.sql'));
    await db.exec(`select public.profile_test_assert((select nickname = '기존차슈' and bio = '' and instagram_username = '' from public.public_profiles where user_id = '22222222-2222-2222-2222-222222222222'), 'Instagram migration preserves existing identity and backfills empty username');`);
    await db.exec(read('tests/account_profile.sql'));
    await db.exec(read('tests/account_profile_bio.sql'));
    await db.exec(read('tests/account_profile_instagram.sql'));
    await db.exec(read('migrations/20260924060646_unique_profile_nickname.sql'));
    await db.exec(read('tests/account_profile_nickname.sql'));
    await db.exec(read('migrations/20260925143632_account_profile_visibility.sql'));
    await db.exec(read('tests/account_profile_visibility.sql'));
    await db.exec(`
      insert into public.ramen_shops(id) values ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa');
      insert into public.shop_reviews(id, shop_id, user_id, body, image_paths)
      values ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
        '22222222-2222-2222-2222-222222222222', '기존 공개 리뷰', '{}');
    `);
    await db.exec(read('migrations/20260926022746_review_community_safety.sql'));
    await db.exec(read('tests/review_community_safety.sql'));
    await db.exec(read('migrations/20260926175557_instant_review_publication.sql'));
    await db.exec(read('tests/instant_review_publication.sql'));
    await db.exec(read('migrations/20260927094254_profile_nickname_availability_volatile.sql'));
    await db.exec(read('tests/account_profile_nickname_after_community.sql'));
    // Model the verified production precondition without changing historical 50-codepoint tests.
    await db.exec(`update public.public_profiles set bio = '' where char_length(bio) > 30;`);
    await db.exec(read('migrations/20260927095300_profile_bio_30_codepoints.sql'));
    await db.exec(read('tests/account_profile_bio_30_codepoints.sql'));
    await db.exec(read('migrations/20260927100000_profile_daily_edit_limit.sql'));
    await db.exec(read('tests/account_profile_daily_edit_limit.sql'));
    await db.exec(read('migrations/20260927100100_profile_request_rate_limit.sql'));
    await db.exec(read('migrations/20260927100200_profile_nickname_check_daily_30.sql'));
    await db.exec(read('tests/account_profile_request_rate_limit.sql'));
    console.log('PASS: standalone migration and RPC/RLS assertions');
  } finally { await db.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
