create table public.shop_menu_update_timestamps (
    shop_id uuid primary key references public.shops(id) on delete cascade,
    updated_at timestamptz not null
);

alter table public.shop_menu_update_timestamps enable row level security;

create or replace function public.track_shop_menu_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    target_shop_id uuid;
begin
    if tg_table_name = 'shop_menu_sections' then
        target_shop_id := coalesce(new.shop_id, old.shop_id);
    else
        select section.shop_id into target_shop_id
        from public.shop_menu_sections section
        where section.id = coalesce(new.section_id, old.section_id);
    end if;

    if target_shop_id is null then
        raise exception 'Could not resolve shop for menu update';
    end if;

    insert into public.shop_menu_update_timestamps (shop_id, updated_at)
    values (target_shop_id, now())
    on conflict (shop_id) do update
    set updated_at = excluded.updated_at;

    return coalesce(new, old);
end;
$$;

create trigger track_shop_menu_section_update
after insert or update or delete on public.shop_menu_sections
for each row execute function public.track_shop_menu_update();

create trigger track_shop_menu_item_update
after insert or update or delete on public.shop_menu_items
for each row execute function public.track_shop_menu_update();

create or replace function public.fetch_shop_menu_updated_at(p_shop_id uuid)
returns timestamptz
language sql
stable
security definer
set search_path = ''
as $$
    select updated_at
    from public.shop_menu_update_timestamps
    where shop_id = p_shop_id;
$$;

grant execute on function public.fetch_shop_menu_updated_at(uuid) to anon, authenticated;
revoke all on function public.track_shop_menu_update() from public;
