begin;

alter table public.shop_events
    drop constraint if exists shop_events_event_type_check;

alter table public.shop_events
    add constraint shop_events_event_type_check
    check (
        event_type = any (array['collab', 'popup', 'limited_menu', 'summer_limited', 'new_menu', 'store_renewal'])
        and (event_type <> 'store_renewal' or end_date is null)
    );

do $do$
declare
    view_name text;
    view_definition text;
    function_definition text;
begin
    foreach view_name in array array['active_events', 'active_shop_events', 'calendar_events']
    loop
        view_definition := pg_get_viewdef(format('public.%I', view_name)::regclass, true);
        execute format(
            'create or replace view public.%I as %s',
            view_name,
            regexp_replace(
                view_definition,
                'event_type = ANY \(ARRAY\[[^]]*\]\)',
                'event_type = ANY (ARRAY[''collab'', ''popup'', ''limited_menu'', ''summer_limited'', ''new_menu'', ''store_renewal''])'
            )
        );
    end loop;

    function_definition := pg_get_functiondef('public.fetch_calendar_event_page(date)'::regprocedure);
    execute regexp_replace(
        function_definition,
        'event_type = ANY \(ARRAY\[[^]]*\]\)',
        'event_type = ANY (ARRAY[''collab'', ''popup'', ''limited_menu'', ''summer_limited'', ''new_menu'', ''store_renewal''])',
        'g'
    );
end;
$do$;

commit;
