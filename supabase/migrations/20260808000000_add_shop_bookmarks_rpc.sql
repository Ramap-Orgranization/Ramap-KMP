create or replace function public.add_shop_bookmarks(p_shop_ids uuid[])
returns void
language sql
set search_path to ''
as $function$
  insert into public.user_shop_bookmarks (user_id, shop_id)
  select (select auth.uid()), input.shop_id
  from unnest(p_shop_ids) as input(shop_id)
  on conflict (user_id, shop_id) do nothing;
$function$;

revoke all on function public.add_shop_bookmarks(uuid[]) from anon, public;
grant execute on function public.add_shop_bookmarks(uuid[]) to authenticated;
