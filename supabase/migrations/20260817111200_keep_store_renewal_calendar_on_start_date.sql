begin;

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
            start_date >= period_start and start_date < period_end
        else start_date < period_end and (end_date is null or end_date >= period_start)
    end;
$function$;

commit;
