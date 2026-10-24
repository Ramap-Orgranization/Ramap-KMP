-- Run against the disposable fixture described in fixtures/review_visit_numbers.sql.
begin;
do $$
declare
    owner_id uuid := gen_random_uuid();
    viewer_id uuid := gen_random_uuid();
    shop_id uuid := gen_random_uuid();
    private_id uuid := gen_random_uuid();
    public_id uuid := gen_random_uuid();
    later_id uuid := gen_random_uuid();
    projected jsonb;
    row_number integer;
begin
    insert into public.ramen_shops values (shop_id, '회차 검증 매장');
    insert into public.public_profiles(user_id, nickname, is_public) values (owner_id, '회차 검증 작성자', true);
    insert into public.shop_reviews(id, shop_id, user_id, body, created_at, is_public, visit_number) values
        (private_id, shop_id, owner_id, '비공개 첫 방문', '2026-01-01', false, 1),
        (public_id, shop_id, owner_id, '공개 두 번째 방문', '2026-01-02', true, 2),
        (later_id, shop_id, owner_id, '삭제된 기록 이후 방문', '2026-01-07', true, 7);
    perform set_config('request.jwt.claim.sub', owner_id::text, true);
    projected := community_private.review_mutation_snapshot(private_id);
    assert (projected->>'visit_number')::int = 1, '비공개 작성 응답의 첫 방문은 1이다';
    assert (projected->>'author_review_count')::int = 2, '작성자 공개 리뷰 수는 방문 회차와 별개다';
    assert not exists (
        select 1 from public.fetch_shop_reviews(shop_id) p join public.shop_reviews r using (id)
        where p.visit_number <> r.visit_number
    ), '매장 목록은 저장된 회차를 사용한다';
    assert not exists (
        select 1 from public.fetch_profile_reviews(owner_id) p join public.shop_reviews r using (id)
        where p.visit_number <> r.visit_number
    ), '프로필 목록은 저장된 회차를 사용한다';
    projected := public.fetch_my_reviews();
    assert (projected->>'total_count')::int = 3, '내 리뷰 집계는 유지된다';
    assert not exists (
        select 1 from jsonb_array_elements(projected->'reviews') p join public.shop_reviews r on r.id=(p->>'id')::uuid
        where (p->>'visit_number')::int <> r.visit_number
    ), '내 리뷰 목록은 비공개 리뷰도 저장된 회차를 사용한다';
    projected := public.fetch_shop_reviews_page(shop_id);
    assert (projected->>'total_count')::int = 3, '본인 매장 목록의 집계는 유지된다';
    assert not exists (
        select 1 from jsonb_array_elements(projected->'reviews') p join public.shop_reviews r on r.id=(p->>'id')::uuid
        where (p->>'visit_number')::int <> r.visit_number
    ), '페이지 조회도 저장된 회차를 사용한다';
    update public.shop_reviews set is_public = true, body = '공개 범위를 바꾼 첫 방문' where id=private_id;
    assert (community_private.review_mutation_snapshot(private_id)->>'visit_number')::int = 1,
        '수정과 공개 전환은 방문 회차를 바꾸지 않는다';
    delete from public.shop_reviews where id=public_id;
    assert (community_private.review_mutation_snapshot(later_id)->>'visit_number')::int = 7,
        '다른 리뷰 삭제는 기존 방문 회차를 다시 매기지 않는다';
    update public.shop_reviews set is_public = false where id=private_id;
    perform set_config('request.jwt.claim.sub', viewer_id::text, true);
    assert community_private.review_mutation_snapshot(private_id) is null, '다른 사용자는 비공개 저장 응답을 볼 수 없다';
    assert not exists(select 1 from public.fetch_shop_reviews(shop_id) where id=private_id), '비공개 리뷰는 다른 사용자에게 노출하지 않는다';
    assert not exists(select 1 from public.fetch_profile_reviews(owner_id) where id=private_id), '프로필 비공개 리뷰도 노출하지 않는다';
    select visit_number into row_number from public.fetch_shop_reviews(shop_id) where id=later_id;
    assert row_number=7, '다른 사용자의 공개 리뷰에도 동일한 방문 회차를 표시한다';
end;
$$;
rollback;
