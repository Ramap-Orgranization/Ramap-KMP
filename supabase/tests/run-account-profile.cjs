// Disposable PostgreSQL WASM only; never connects to Supabase or a live database.
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const { PGlite } = require(resolve(process.argv[2] || '.artifacts/profile-implementation/node_modules/@electric-sql/pglite'));
const read = path => readFileSync(resolve('supabase', path), 'utf8');
(async () => {
  const db = new PGlite();
  try {
    await db.exec(read('tests/profile-fixture.sql'));
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
    console.log('PASS: standalone migration and RPC/RLS assertions');
  } finally { await db.close(); }
})().catch(error => { console.error(error.message); process.exitCode = 1; });
