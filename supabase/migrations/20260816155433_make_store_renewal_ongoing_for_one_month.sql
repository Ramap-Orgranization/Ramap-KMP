begin;

create or replace function public.event_is_active_on(
    event_type text,
    start_date date,
    end_date date,
    today date
)
returns boolean
language sql
immutable
as $function$
    select case
        when event_type = 'store_renewal' then
            today < (start_date + interval '1 month')::date
        else end_date is null or end_date >= today
    end;
$function$;

create or replace function public.event_is_today_on(
    event_type text,
    start_date date,
    end_date date,
    today date
)
returns boolean
language sql
immutable
as $function$
    select today >= start_date
        and case
            when event_type = 'store_renewal' then today < (start_date + interval '1 month')::date
            else end_date is null or today <= end_date
        end;
$function$;

create or replace function public.event_overlaps_period(
    event_type text,
    start_date date,
    end_date date,
    period_start date,
    period_end date
)
returns boolean
language sql
immutable
as $function$
    select case
        when event_type = 'store_renewal' then
            start_date < period_end
            and (start_date + interval '1 month')::date > period_start
        else start_date < period_end and (end_date is null or end_date >= period_start)
    end;
$function$;

commit;
