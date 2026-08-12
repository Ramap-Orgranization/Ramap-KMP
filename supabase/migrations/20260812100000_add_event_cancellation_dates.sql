alter table public.shop_events
    add column if not exists cancelled_dates date[] not null default '{}'::date[];

create or replace view public.calendar_events
with (security_invoker = true)
as
select
    event.id,
    event.event_type,
    event.title,
    event.description,
    event.start_date,
    event.end_date,
    event.source_url,
    (current_timestamp at time zone 'Asia/Seoul')::date >= event.start_date
        and (
            event.end_date is null
            or (current_timestamp at time zone 'Asia/Seoul')::date <= event.end_date
        ) as is_today,
    true as is_venue,
    event.shop_id as venue_shop_id,
    venue.name as venue_shop_name,
    venue.address as venue_address,
    collaborator.shop_id as collaborator_shop_id,
    coalesce(
        collaborator_shop.name,
        collaborator.external_name::character varying
    ) as collaborator_name,
    collaborator.external_instagram_url as collaborator_instagram_url,
    event.waiting_method,
    event.waiting_url,
    venue.instagram_profile_image_path as venue_profile_image_url,
    venue.kakao_place_url as venue_kakao_place_url,
    venue.naver_place_url as venue_naver_place_url,
    venue.lat as venue_lat,
    venue.lng as venue_lng,
    event.cancelled_dates,
    (current_timestamp at time zone 'Asia/Seoul')::date = any(event.cancelled_dates) as is_cancelled_today
from public.shop_events event
join public.shops venue on venue.id = event.shop_id
left join lateral (
    select
        participant.shop_id,
        participant.external_name,
        participant.external_instagram_url
    from public.shop_event_participants participant
    where participant.event_id = event.id
    order by participant.created_at
    limit 1
) collaborator on true
left join public.shops collaborator_shop on collaborator_shop.id = collaborator.shop_id
where event.event_type = any (array['collab', 'popup', 'limited_menu', 'summer_limited'])
order by
    (
        (current_timestamp at time zone 'Asia/Seoul')::date >= event.start_date
        and (
            event.end_date is null
            or (current_timestamp at time zone 'Asia/Seoul')::date <= event.end_date
        )
    ) desc,
    event.start_date,
    event.created_at;

create or replace view public.active_events
with (security_invoker = true)
as
select
    event.id,
    event.event_type,
    event.title,
    event.description,
    event.start_date,
    event.end_date,
    event.source_url,
    (current_timestamp at time zone 'Asia/Seoul')::date >= event.start_date
        and (
            event.end_date is null
            or (current_timestamp at time zone 'Asia/Seoul')::date <= event.end_date
        ) as is_today,
    true as is_venue,
    event.shop_id as venue_shop_id,
    venue.name as venue_shop_name,
    venue.address as venue_address,
    collaborator.shop_id as collaborator_shop_id,
    coalesce(
        collaborator_shop.name,
        collaborator.external_name::character varying
    ) as collaborator_name,
    collaborator.external_instagram_url as collaborator_instagram_url,
    event.waiting_method,
    event.waiting_url,
    venue.instagram_profile_image_path as venue_profile_image_url,
    venue.kakao_place_url as venue_kakao_place_url,
    venue.naver_place_url as venue_naver_place_url,
    venue.lat as venue_lat,
    venue.lng as venue_lng,
    event.cancelled_dates,
    (current_timestamp at time zone 'Asia/Seoul')::date = any(event.cancelled_dates) as is_cancelled_today
from public.shop_events event
join public.shops venue on venue.id = event.shop_id
left join lateral (
    select
        participant.shop_id,
        participant.external_name,
        participant.external_instagram_url
    from public.shop_event_participants participant
    where participant.event_id = event.id
    order by participant.created_at
    limit 1
) collaborator on true
left join public.shops collaborator_shop on collaborator_shop.id = collaborator.shop_id
where event.event_type = any (array['collab', 'popup', 'limited_menu', 'summer_limited'])
    and (
        event.end_date is null
        or event.end_date >= (current_timestamp at time zone 'Asia/Seoul')::date
    )
order by
    (
        (current_timestamp at time zone 'Asia/Seoul')::date >= event.start_date
        and (
            event.end_date is null
            or (current_timestamp at time zone 'Asia/Seoul')::date <= event.end_date
        )
    ) desc,
    event.start_date,
    event.created_at;

create or replace view public.active_shop_events
with (security_invoker = true)
as
select
    event_context.shop_context_id,
    event_context.shop_context_id = event.shop_id as is_venue,
    event.id,
    event.event_type,
    event.title,
    event.description,
    event.start_date,
    event.end_date,
    event.source_url,
    current_date >= event.start_date
        and (event.end_date is null or current_date <= event.end_date) as is_today,
    event.shop_id as venue_shop_id,
    venue.name as venue_shop_name,
    venue.address as venue_address,
    collaborator.shop_id as collaborator_shop_id,
    coalesce(
        collaborator_shop.name,
        collaborator.external_name::character varying
    ) as collaborator_name,
    collaborator.external_instagram_url as collaborator_instagram_url,
    event.waiting_method,
    event.waiting_url,
    venue.instagram_profile_image_path as venue_profile_image_url,
    venue.kakao_place_url as venue_kakao_place_url,
    venue.naver_place_url as venue_naver_place_url,
    venue.lat as venue_lat,
    venue.lng as venue_lng,
    event.cancelled_dates,
    (current_timestamp at time zone 'Asia/Seoul')::date = any(event.cancelled_dates) as is_cancelled_today
from public.shop_events event
join public.shops venue on venue.id = event.shop_id
cross join lateral (
    select event.shop_id as shop_context_id
    union
    select participant.shop_id
    from public.shop_event_participants participant
    where participant.event_id = event.id
        and participant.shop_id is not null
) event_context
left join lateral (
    select
        participant.shop_id,
        participant.external_name,
        participant.external_instagram_url
    from public.shop_event_participants participant
    where participant.event_id = event.id
        and participant.shop_id is distinct from event_context.shop_context_id
    order by participant.created_at
    limit 1
) collaborator on true
left join public.shops collaborator_shop on collaborator_shop.id = collaborator.shop_id
where event.event_type = any (array['collab', 'popup', 'limited_menu', 'summer_limited'])
    and (event.end_date is null or event.end_date >= current_date)
order by event.start_date, event.created_at;

revoke all on public.calendar_events, public.active_events, public.active_shop_events
from public, anon, authenticated;

grant select on public.calendar_events, public.active_events, public.active_shop_events
to anon, authenticated;

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
        and (event.end_date is null or event.end_date >= bounds.month_start)
),
reminder_dates as (
    select greatest(event.start_date, bounds.month_start) as notification_date
    from public.shop_events event
    cross join bounds
    where event.start_date >= greatest(bounds.month_start, bounds.today)
        and event.start_date < bounds.next_month_start
        and event.start_date <> all(event.cancelled_dates)
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
        and event.start_date <> all(event.cancelled_dates)
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
            and (event.end_date is null or event.end_date >= bounds.previous_month_start)
    ) as has_previous,
    exists (
        select 1
        from public.calendar_events event
        cross join bounds
        where event.start_date < bounds.month_after_next_start
            and (event.end_date is null or event.end_date >= bounds.next_month_start)
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
