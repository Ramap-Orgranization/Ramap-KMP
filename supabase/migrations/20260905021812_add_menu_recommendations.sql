create table public.user_shop_menu_recommendations (
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    menu_id uuid not null references public.shop_menu_items(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, menu_id)
);
create index user_shop_menu_recommendations_menu_id_idx
    on public.user_shop_menu_recommendations (menu_id);
alter table public.user_shop_menu_recommendations enable row level security;
revoke all on table public.user_shop_menu_recommendations from anon, authenticated;
grant select on table public.user_shop_menu_recommendations to anon, authenticated;
grant insert, delete on table public.user_shop_menu_recommendations to authenticated;
create policy "Users can view their menu recommendations"
on public.user_shop_menu_recommendations
for select
to authenticated
using ((select auth.uid()) = user_id);
create policy "Users can add their menu recommendations"
on public.user_shop_menu_recommendations
for insert
to authenticated
with check ((select auth.uid()) = user_id);
create policy "Users can remove their menu recommendations"
on public.user_shop_menu_recommendations
for delete
to authenticated
using ((select auth.uid()) = user_id);
create or replace function public.fetch_menu_recommendation_counts(p_menu_ids uuid[])
returns table(menu_id uuid, recommendation_count bigint)
language sql
security definer
set search_path = ''
as $$
    select recommendation.menu_id, count(*)::bigint
    from public.user_shop_menu_recommendations recommendation
    where recommendation.menu_id = any(p_menu_ids)
    group by recommendation.menu_id;
$$;
revoke all on function public.fetch_menu_recommendation_counts(uuid[]) from public;
grant execute on function public.fetch_menu_recommendation_counts(uuid[]) to anon, authenticated;
