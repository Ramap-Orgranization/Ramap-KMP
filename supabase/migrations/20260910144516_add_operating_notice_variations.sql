alter table public.shop_operating_notices
    add column if not exists schedule_override jsonb,
    add column if not exists manually_released_at timestamptz;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'shop_operating_notices_schedule_override_type_check') then
        alter table public.shop_operating_notices add constraint shop_operating_notices_schedule_override_type_check
            check (
                schedule_override is null or
                    (notice_type = 'operating_notice' and jsonb_typeof(schedule_override) = 'object')
            ) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'shop_operating_notices_early_close_end_time_check') then
        alter table public.shop_operating_notices add constraint shop_operating_notices_early_close_end_time_check
            check (notice_type <> 'early_close' or end_time is not null) not valid;
    end if;
    if not exists (select 1 from pg_constraint where conname = 'shop_operating_notices_manual_release_type_check') then
        alter table public.shop_operating_notices add constraint shop_operating_notices_manual_release_type_check
            check (manually_released_at is null or notice_type = 'late_opening') not valid;
    end if;
end $$;

create unique index if not exists shop_operating_notices_schedule_override_once
    on public.shop_operating_notices (shop_id, notice_date)
    where notice_type = 'operating_notice' and schedule_override is not null;

create index if not exists shop_operating_notices_delayed_opening_index
    on public.shop_operating_notices (notice_date, manually_released_at)
    where notice_type = 'late_opening';

create or replace function public.release_shop_operating_notice(notice_id uuid)
returns timestamptz
language sql
security definer
set search_path = public
as $$
    update shop_operating_notices
    set manually_released_at = coalesce(manually_released_at, now())
    where id = notice_id and notice_type = 'late_opening'
    returning manually_released_at;
$$;

revoke all on function public.release_shop_operating_notice(uuid) from public;
grant execute on function public.release_shop_operating_notice(uuid) to service_role;
