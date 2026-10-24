-- Disposable empty PostgreSQL database only. Minimal dependencies for projection tests.
-- Load this fixture, then the preserve_review_visit_numbers migration, then
-- supabase/tests/review_visit_numbers.sql with psql -v ON_ERROR_STOP=1.
create schema auth;
create schema community_private;
create function auth.uid() returns uuid language sql stable as $$
    select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
$$;
create function community_private.require_live_user() returns uuid language plpgsql as $$
begin
    if auth.uid() is null then raise exception 'Authentication required'; end if;
    return auth.uid();
end;
$$;
create table public.ramen_shops(id uuid primary key, name text);
create table public.public_profiles(user_id uuid primary key, nickname text, avatar_path text, is_public boolean, suspended_at timestamptz);
create table public.shop_reviews(
    id uuid primary key, shop_id uuid, user_id uuid, body text, created_at timestamptz,
    image_paths text[] default '{}', moderation_status text default 'published',
    moderation_note text, is_public boolean, visit_number integer not null
);
create table public.user_blocks(blocker_id uuid, blocked_id uuid);
create table public.account_deletion_requests(user_id uuid);
create table public.review_likes(review_id uuid, user_id uuid);
create function public.fetch_public_community_profile(uuid) returns jsonb language sql stable as $$
    select to_jsonb(p) from public.public_profiles p where user_id=$1 and is_public;
$$;
create function community_private.can_read_follow_content(uuid,text) returns boolean language sql stable as $$ select false; $$;
