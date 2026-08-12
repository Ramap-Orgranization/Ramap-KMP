create or replace function public.fetch_calendar_event_page(requested_month date)
returns table (
    events jsonb,
    has_previous boolean,
    has_next boolean
)
language sql
stable
set search_path = public
as $$
with bounds as (
    select
        date_trunc('month', requested_month)::date as month_start,
        (date_trunc('month', requested_month) - interval '1 month')::date as previous_month_start,
        (date_trunc('month', requested_month) + interval '1 month')::date as next_month_start,
        (date_trunc('month', requested_month) + interval '2 months')::date as month_after_next_start
),
current_events as (
    select event.*
    from public.calendar_events event
    cross join bounds
    where event.start_date < bounds.next_month_start
        and (event.end_date is null or event.end_date >= bounds.month_start)
)
select
    coalesce(
        (
            select jsonb_agg(to_jsonb(event) order by event.start_date, event.id)
            from current_events event
        ),
        '[]'::jsonb
    ) as events,
    exists (
        select 1
        from public.calendar_events event
        cross join bounds
        where event.start_date < bounds.month_start
            and (event.end_date is null or event.end_date >= bounds.previous_month_start)
    ) as has_previous,
    exists (
        select 1
        from public.calendar_events event
        cross join bounds
        where event.start_date < bounds.month_after_next_start
            and (event.end_date is null or event.end_date >= bounds.next_month_start)
    ) as has_next;
$$;

revoke all on function public.fetch_calendar_event_page(date) from public;
grant execute on function public.fetch_calendar_event_page(date) to anon, authenticated;
