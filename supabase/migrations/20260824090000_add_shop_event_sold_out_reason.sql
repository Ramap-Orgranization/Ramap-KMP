create or replace function public.merge_shop_event_status_dates(
    p_event_id uuid,
    p_status text,
    p_scope text,
    p_reason text,
    p_today date
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    target_dates date[];
begin
    if p_status not in ('cancelled', 'sold_out') or p_scope not in ('today', 'entire_period') or (p_status = 'cancelled' and nullif(trim(p_reason), '') is null) or (p_status = 'sold_out' and p_scope <> 'today') then
        raise exception 'invalid event status update';
    end if;

    select case
        when p_scope = 'today' then array[p_today]
        else array(select generate_series(start_date, coalesce(end_date, start_date), interval '1 day')::date)
    end
    into target_dates
    from public.shop_events
    where id = p_event_id
      and start_date <= p_today
      and coalesce(end_date, start_date) >= p_today;

    if target_dates is null then
        raise exception 'active event not found';
    end if;


    update public.shop_events
    set cancelled_dates = case
            when p_status = 'cancelled' then array(select distinct unnest(coalesce(cancelled_dates, '{}') || target_dates))
            when p_status = 'sold_out' then array(select existing_date from unnest(coalesce(cancelled_dates, '{}')) as existing(existing_date) where existing_date <> all(target_dates))
            else cancelled_dates
        end,
        sold_out_dates = case
            when p_status = 'sold_out' then array(select distinct unnest(coalesce(sold_out_dates, '{}') || target_dates))
            when p_status = 'cancelled' then array(select existing_date from unnest(coalesce(sold_out_dates, '{}')) as existing(existing_date) where existing_date <> all(target_dates))
            else sold_out_dates
        end,
        cancellation_reason = case when p_status = 'cancelled' then p_reason else cancellation_reason end
    where id = p_event_id;
end;
$$;

revoke all on function public.merge_shop_event_status_dates(uuid, text, text, text, date) from public;
grant execute on function public.merge_shop_event_status_dates(uuid, text, text, text, date) to service_role;
