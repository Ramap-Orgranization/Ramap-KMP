drop function if exists public.fetch_calendar_event_page(date);

create function public.fetch_calendar_event_page(requested_month date)
returns table (
    events jsonb,
    has_previous boolean,
    has_next boolean,
    notification_dates jsonb
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
        (date_trunc('month', requested_month) + interval '2 months')::date as month_after_next_start,
        (current_timestamp at time zone 'Asia/Seoul')::date as today
),
current_events as (
    select event.*
    from public.calendar_events event
    cross join bounds
    where event.start_date < bounds.next_month_start
        and (coalesce(event.end_date, event.start_date) >= bounds.month_start)
),
reminder_dates as (
    select greatest(event.start_date, bounds.month_start) as notification_date
    from public.shop_events event
    cross join bounds
    where event.start_date >= greatest(bounds.month_start, bounds.today)
        and event.start_date < bounds.next_month_start
        and event.event_type = any (array['collab', 'popup', 'limited_menu', 'summer_limited'])
        and (
            exists (
                select 1
                from public.shop_event_notification_subscriptions subscription
                where subscription.user_id = auth.uid()
                    and (
                        subscription.shop_id = event.shop_id
                        or subscription.shop_id in (
                            select participant.shop_id
                            from public.shop_event_participants participant
                            where participant.event_id = event.id
                                and participant.shop_id is not null
                        )
                    )
            )
            or exists (
                select 1
                from public.user_event_notification_overrides override
                where override.user_id = auth.uid()
                    and override.event_id = event.id
                    and override.enabled
            )
        )
        and not exists (
            select 1
            from public.user_event_notification_overrides override
            where override.user_id = auth.uid()
                and override.event_id = event.id
                and not override.enabled
        )
        and coalesce(
            (
                select preference.enabled
                from public.user_event_notification_preferences preference
                where preference.user_id = auth.uid()
                limit 1
            ),
            true
        )
    union
    select event.start_date - 1 as notification_date
    from public.shop_events event
    cross join bounds
    where event.start_date > greatest(bounds.month_start, bounds.today)
        and event.start_date <= bounds.next_month_start
        and event.event_type = any (array['collab', 'popup', 'limited_menu', 'summer_limited'])
        and (
            exists (
                select 1
                from public.shop_event_notification_subscriptions subscription
                where subscription.user_id = auth.uid()
                    and (
                        subscription.shop_id = event.shop_id
                        or subscription.shop_id in (
                            select participant.shop_id
                            from public.shop_event_participants participant
                            where participant.event_id = event.id
                                and participant.shop_id is not null
                        )
                    )
            )
            or exists (
                select 1
                from public.user_event_notification_overrides override
                where override.user_id = auth.uid()
                    and override.event_id = event.id
                    and override.enabled
            )
        )
        and not exists (
            select 1
            from public.user_event_notification_overrides override
            where override.user_id = auth.uid()
                and override.event_id = event.id
                and not override.enabled
        )
        and coalesce(
            (
                select preference.enabled
                from public.user_event_notification_preferences preference
                where preference.user_id = auth.uid()
                limit 1
            ),
            true
        )
),
visible_notification_dates as (
    select distinct reminder.notification_date
    from reminder_dates reminder
    cross join bounds
    where auth.uid() is not null
        and reminder.notification_date >= greatest(bounds.month_start, bounds.today)
        and reminder.notification_date < bounds.next_month_start
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
            and (coalesce(event.end_date, event.start_date) >= bounds.previous_month_start)
    ) as has_previous,
    exists (
        select 1
        from public.calendar_events event
        cross join bounds
        where event.start_date < bounds.month_after_next_start
            and (coalesce(event.end_date, event.start_date) >= bounds.next_month_start)
    ) as has_next,
    coalesce(
        (
            select jsonb_agg(to_jsonb(date.notification_date) order by date.notification_date)
            from visible_notification_dates date
        ),
        '[]'::jsonb
    ) as notification_dates;
$$;

revoke all on function public.fetch_calendar_event_page(date) from public;
grant execute on function public.fetch_calendar_event_page(date) to anon, authenticated;
