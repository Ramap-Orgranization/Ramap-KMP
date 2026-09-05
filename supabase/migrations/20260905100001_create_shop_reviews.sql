create table public.shop_reviews (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references public.ramen_shops(id) on delete cascade,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    body text not null,
    created_at timestamptz not null default now(),
    constraint shop_reviews_body_byte_length check (
        body = btrim(body)
        and octet_length(body) between 10 and 300
    )
);

create index shop_reviews_shop_id_created_at_id_index
    on public.shop_reviews (shop_id, created_at desc, id desc);

alter table public.shop_reviews enable row level security;

revoke all on table public.shop_reviews from public, anon, authenticated;
grant select (id, shop_id, body, created_at) on table public.shop_reviews to anon, authenticated;
grant insert (shop_id, body) on table public.shop_reviews to authenticated;

create policy "Public shop reviews are readable"
on public.shop_reviews
for select
to anon, authenticated
using (true);

create policy "Authenticated users can create shop reviews"
on public.shop_reviews
for insert
to authenticated
with check (user_id = (select auth.uid()));
