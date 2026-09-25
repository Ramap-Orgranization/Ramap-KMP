-- Run only in the disposable profile fixture, after the nickname migration.
set role authenticated;
select set_config('request.jwt.claim.sub', '22222222-2222-2222-2222-222222222222', false);
select public.profile_test_assert(public.is_profile_nickname_available('기존차슈'), 'own nickname remains available');
select public.profile_test_assert(not public.is_profile_nickname_available('춤추는라멘'), 'another nickname is unavailable');
select public.profile_test_reject($q$select public.update_my_profile('22222222-2222-2222-2222-222222222222', '춤추는라멘')$q$);
select public.update_my_profile('22222222-2222-2222-2222-222222222222', 'NewRamen');
select set_config('request.jwt.claim.sub', '33333333-3333-3333-3333-333333333333', false);
select public.profile_test_assert(not public.is_profile_nickname_available('newramen'), 'case-insensitive duplicate check');
select public.profile_test_reject($q$select public.update_my_profile('33333333-3333-3333-3333-333333333333', 'newramen')$q$);
select public.profile_test_assert(public.fetch_or_create_my_profile('33333333-3333-3333-3333-333333333333')->>'nickname' ~ '^[가-힣0-9]{2,10}$', 'generated nickname remains valid');
select set_config('request.jwt.claim.sub', '', false);
select public.profile_test_reject($q$select public.is_profile_nickname_available('free')$q$);
set role anon;
select public.profile_test_reject($q$select public.is_profile_nickname_available('free')$q$);
reset role;
