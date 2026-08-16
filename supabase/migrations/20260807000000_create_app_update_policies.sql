create table public.app_update_policies (
    platform text primary key check (platform in ('android', 'ios')),
    minimum_build_number bigint not null check (minimum_build_number >= 1),
    store_url text not null check (store_url ~ '^https?://')
);

alter table public.app_update_policies enable row level security;

grant select on public.app_update_policies to anon, authenticated;

create policy "Anyone can read app update policies"
on public.app_update_policies
for select
to anon, authenticated
using (true);
