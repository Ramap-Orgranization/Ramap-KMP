create or replace function public.claim_due_shop_operating_statuses(batch_limit integer default 10)
returns table (
    id text,
    name text,
    naver_place_url text,
    business_hours_weekly jsonb,
    business_hours_break_times jsonb,
    check_interval_minutes integer,
    check_reason text,
    consecutive_failures integer
)
language plpgsql set search_path = public as $$
declare safe_limit integer := least(greatest(coalesce(batch_limit, 10), 1), 50);
begin
    insert into public.shop_operating_status (shop_id, source_url, next_check_at, check_interval_minutes, check_reason, consecutive_failures)
    select shops.id::text, shops.naver_place_url, now(), 60, 'initial', 0
    from public.shops shops where shops.is_visible and shops.naver_place_url is not null
    on conflict (shop_id) do nothing;

    return query
    with due as (
        select status.shop_id from public.shop_operating_status status
        join public.shops shops on shops.id::text = status.shop_id
        where shops.is_visible and shops.naver_place_url is not null and status.next_check_at <= now()
        order by status.next_check_at, status.shop_id
        for update of status skip locked limit safe_limit
    ), claimed as (
        update public.shop_operating_status status set next_check_at = now() + interval '60 minutes', check_reason = 'running'
        from due where status.shop_id = due.shop_id
        returning status.shop_id, status.consecutive_failures
    )
    select shops.id::text, shops.name::text, shops.naver_place_url::text, shops.business_hours_weekly, shops.business_hours_break_times, status.check_interval_minutes, status.check_reason, claimed.consecutive_failures
    from claimed join public.shops shops on shops.id::text = claimed.shop_id
    join public.shop_operating_status status on status.shop_id = claimed.shop_id;
end;
$$;

revoke all on function public.claim_due_shop_operating_statuses(integer) from public;
grant execute on function public.claim_due_shop_operating_statuses(integer) to service_role;
