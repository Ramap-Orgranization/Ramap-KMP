create table public.shop_operating_anomalies (
    id bigint generated always as identity primary key,
    shop_id text not null,
    observed_at timestamptz not null default now(),
    anomaly_type text not null check (anomaly_type in ('unexpected_close', 'early_close')),
    status text not null,
    minutes_early integer,
    source_url text not null,
    check (
        (anomaly_type = 'early_close' and minutes_early > 0)
        or (anomaly_type = 'unexpected_close' and minutes_early is null)
    )
);

create index shop_operating_anomalies_shop_observed_at_idx
on public.shop_operating_anomalies (shop_id, observed_at desc);

alter table public.shop_operating_anomalies enable row level security;

grant all on table public.shop_operating_anomalies to service_role;
grant usage, select on sequence public.shop_operating_anomalies_id_seq to service_role;

create function public.record_shop_operating_observation(
    p_shop_id text,
    p_status text,
    p_source_url text,
    p_anomaly_type text default null,
    p_minutes_early integer default null,
    p_observed_at timestamptz default now()
)
returns table (
    check_interval_minutes integer,
    check_reason text,
    anomalies_7d bigint,
    anomalies_14d bigint,
    anomalies_30d bigint
)
language plpgsql
set search_path = public
as $$
declare
    recent_7d bigint;
    recent_14d bigint;
    recent_30d bigint;
begin
    if p_anomaly_type is not null and p_anomaly_type not in ('unexpected_close', 'early_close') then
        raise exception 'Unsupported operating anomaly type: %', p_anomaly_type;
    end if;

    -- ponytail: six-hour dedupe treats repeated polls of one incident as one event.
    if p_anomaly_type is not null and not exists (
        select 1
        from public.shop_operating_anomalies
        where shop_id = p_shop_id
          and anomaly_type = p_anomaly_type
          and status = p_status
          and observed_at >= p_observed_at - interval '6 hours'
          and observed_at <= p_observed_at
    ) then
        insert into public.shop_operating_anomalies (
            shop_id, observed_at, anomaly_type, status, minutes_early, source_url
        ) values (
            p_shop_id, p_observed_at, p_anomaly_type, p_status, p_minutes_early, p_source_url
        );
    end if;

    select
        count(*) filter (where observed_at >= p_observed_at - interval '7 days'),
        count(*) filter (where observed_at >= p_observed_at - interval '14 days'),
        count(*) filter (where observed_at >= p_observed_at - interval '30 days')
    into recent_7d, recent_14d, recent_30d
    from public.shop_operating_anomalies
    where shop_id = p_shop_id
      and observed_at <= p_observed_at;

    return query
    select
        case
            when recent_14d >= 2 or recent_30d >= 3 then 15
            when recent_7d >= 1 then 30
            else 60
        end,
        case
            when recent_14d >= 2 or recent_30d >= 3 then 'volatile'
            when recent_7d >= 1 then 'recent_anomaly'
            else 'stable'
        end,
        recent_7d,
        recent_14d,
        recent_30d;
end;
$$;

revoke all on function public.record_shop_operating_observation(text, text, text, text, integer, timestamptz) from public;
grant execute on function public.record_shop_operating_observation(text, text, text, text, integer, timestamptz) to service_role;
