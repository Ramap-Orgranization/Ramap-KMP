alter table public.shop_reviews
    drop constraint if exists shop_reviews_body_byte_length,
    add constraint shop_reviews_body_character_length check (
        body = btrim(body)
        and char_length(body) between 5 and 150
    ),
    add column if not exists image_paths text[] not null default '{}',
    add constraint shop_reviews_image_paths_limit check (cardinality(image_paths) <= 3);

create or replace function public.shop_review_image_paths_belong_to_user(
    paths text[],
    owner_id uuid
)
returns boolean
language sql
immutable
set search_path = ''
as $$
    select array_position(paths, null) is null
        and coalesce(
            bool_and(
                path ~ (
                    '^' || owner_id::text ||
                    '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}[.](jpg|png)$'
                )
            ),
            true
        )
    from unnest(paths) as path;
$$;

alter table public.shop_reviews
    add constraint shop_reviews_image_paths_owner
    check (public.shop_review_image_paths_belong_to_user(image_paths, user_id));

grant select (id, shop_id, body, created_at, image_paths) on table public.shop_reviews to anon, authenticated;
grant insert (shop_id, body, image_paths) on table public.shop_reviews to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'shop-review-images',
    'shop-review-images',
    true,
    5242880,
    array['image/jpeg', 'image/png']
)
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create policy "Authenticated users upload own shop review images"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'shop-review-images'
    and (storage.foldername(name))[1] = (select auth.uid()::text)
);

create policy "Authenticated users delete own shop review images"
on storage.objects for delete to authenticated
using (
    bucket_id = 'shop-review-images'
    and (storage.foldername(name))[1] = (select auth.uid()::text)
    and not exists (
        select 1
        from public.shop_reviews
        where name = any(image_paths)
    )
);

create policy "Authenticated users read own shop review images"
on storage.objects for select to authenticated
using (
    bucket_id = 'shop-review-images'
    and (storage.foldername(name))[1] = (select auth.uid()::text)
);
;
