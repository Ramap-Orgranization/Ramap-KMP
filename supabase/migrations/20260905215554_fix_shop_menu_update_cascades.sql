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
        return coalesce(new, old);
    end if;

    insert into public.shop_menu_update_timestamps (shop_id, updated_at)
    values (target_shop_id, now())
    on conflict (shop_id) do update
    set updated_at = excluded.updated_at;

    return coalesce(new, old);
end;
$$;
