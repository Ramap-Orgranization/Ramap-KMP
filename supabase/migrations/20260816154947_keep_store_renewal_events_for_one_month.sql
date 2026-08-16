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

commit;
