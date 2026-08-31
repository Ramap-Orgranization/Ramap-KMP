do $do$
declare
    view_name text;
    view_definition text;
    cancellation_reason_expression text;
begin
    foreach view_name in array array['active_events', 'active_shop_events', 'calendar_events']
    loop
        view_definition := pg_get_viewdef(format('public.%I', view_name)::regclass, true);

        if position('event.cancellation_reason' in view_definition) = 0 then
            raise exception 'expected cancellation_reason in public.% view definition', view_name;
        end if;

        cancellation_reason_expression :=
            'case when (current_timestamp at time zone ''Asia/Seoul'')::date = any(event.cancelled_dates) then event.cancellation_reason end as cancellation_reason';

        execute format(
            'create or replace view public.%I as %s',
            view_name,
            regexp_replace(
                view_definition,
                'event\.cancellation_reason',
                cancellation_reason_expression
            )
        );
    end loop;
end;
$do$;
