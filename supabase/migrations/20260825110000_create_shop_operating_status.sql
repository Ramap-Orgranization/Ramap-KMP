create table public.shop_operating_status (
    shop_id text primary key,
    naver_place_id text,
    status text,
    source_url text not null,
    checked_at timestamptz not null default now(),
    last_error text
);

create index shop_operating_status_checked_at_idx
on public.shop_operating_status (checked_at desc);

alter table public.shop_operating_status enable row level security;

create policy "Anyone can read shop operating status"
on public.shop_operating_status
for select
to anon, authenticated
using (true);
