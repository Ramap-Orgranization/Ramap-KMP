begin;

alter table public.shop_events
    add column if not exists image_paths text[] not null default '{}'::text[];

update public.shop_events
set description = ''
where description is null;

alter table public.shop_events
    alter column description set default '',
    alter column description set not null;

alter table public.shop_events
    drop constraint if exists shop_events_event_type_check;

alter table public.shop_events
    add constraint shop_events_event_type_check
    check (
        event_type = any (array['collab', 'popup', 'limited_menu', 'summer_limited', 'store_renewal'])
        and (event_type <> 'store_renewal' or end_date is null)
    );

alter table public.shop_events
    add constraint shop_events_image_paths_check
    check (
        cardinality(image_paths) <= 5
        and array_position(image_paths, null::text) is null
        and array_position(image_paths, ''::text) is null
    );

alter table public.shop_events
    add constraint shop_events_description_or_image_check
    check (length(trim(description)) > 0 or cardinality(image_paths) > 0);

insert into storage.buckets (
    id,
    name,
    public,
    file_size_limit,
    allowed_mime_types
)
values (
    'event-images',
    'event-images',
    true,
    5242880,
    array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update
set name = excluded.name,
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create or replace view public.active_events as
select
    event.id,
    event.event_type,
    event.title,
    event.description,
    event.start_date,
    event.end_date,
    event.source_url,
    (
        (current_timestamp at time zone 'Asia/Seoul')::date between
            event.start_date and coalesce(event.end_date, event.start_date)
    ) as is_today,
    true as is_venue,
    to_jsonb(venue.*) as venue_shop,
    coalesce(
        (
            select jsonb_agg(to_jsonb(collaborator_shop.*) order by participant.created_at)
            from shop_event_participants participant
            join shops collaborator_shop on collaborator_shop.id = participant.shop_id
            where participant.event_id = event.id
                and participant.shop_id is distinct from event.shop_id
        ),
        '[]'::jsonb
    ) as collaborator_shops,
    coalesce(
        (
            select jsonb_agg(
                jsonb_build_object(
                    'name', participant.external_name,
                    'instagram_url', participant.external_instagram_url
                ) order by participant.created_at
            )
            from shop_event_participants participant
            where participant.event_id = event.id
                and participant.shop_id is null
        ),
        '[]'::jsonb
    ) as external_participants,
    event.waiting_method,
    event.waiting_url,
    event.cancelled_dates,
    ((current_timestamp at time zone 'Asia/Seoul')::date = any(event.cancelled_dates))
        as is_cancelled_today,
    event.image_paths
from shop_events event
join shops venue on venue.id = event.shop_id
where event.event_type = any (
        array['collab', 'popup', 'limited_menu', 'summer_limited', 'store_renewal']
    )
    and coalesce(event.end_date, event.start_date) >= (current_timestamp at time zone 'Asia/Seoul')::date
order by event.created_at desc, event.id desc;

create or replace view public.active_shop_events as
select
    event_context.shop_context_id,
    (event_context.shop_context_id = event.shop_id) as is_venue,
    event.id,
    event.event_type,
    event.title,
    event.description,
    event.start_date,
    event.end_date,
    event.source_url,
    (
        current_date between event.start_date and coalesce(event.end_date, event.start_date)
    ) as is_today,
    to_jsonb(venue.*) as venue_shop,
    coalesce(
        (
            select jsonb_agg(to_jsonb(collaborator_shop.*) order by participant.created_at)
            from shop_event_participants participant
            join shops collaborator_shop on collaborator_shop.id = participant.shop_id
            where participant.event_id = event.id
                and participant.shop_id is distinct from event.shop_id
        ),
        '[]'::jsonb
    ) as collaborator_shops,
    coalesce(
        (
            select jsonb_agg(
                jsonb_build_object(
                    'name', participant.external_name,
                    'instagram_url', participant.external_instagram_url
                ) order by participant.created_at
            )
            from shop_event_participants participant
            where participant.event_id = event.id
                and participant.shop_id is null
        ),
        '[]'::jsonb
    ) as external_participants,
    event.waiting_method,
    event.waiting_url,
    event.cancelled_dates,
    (current_date = any(event.cancelled_dates)) as is_cancelled_today,
    event.image_paths
from shop_events event
join shops venue on venue.id = event.shop_id
cross join lateral (
    select event.shop_id as shop_context_id
    union
    select participant.shop_id
    from shop_event_participants participant
    where participant.event_id = event.id
        and participant.shop_id is not null
) event_context
where event.event_type = any (
        array['collab', 'popup', 'limited_menu', 'summer_limited', 'store_renewal']
    )
    and coalesce(event.end_date, event.start_date) >= current_date
order by event.start_date, event.created_at;

create or replace view public.calendar_events as
select
    event.id,
    event.event_type,
    event.title,
    event.description,
    event.start_date,
    event.end_date,
    event.source_url,
    (
        (current_timestamp at time zone 'Asia/Seoul')::date between
            event.start_date and coalesce(event.end_date, event.start_date)
    ) as is_today,
    true as is_venue,
    to_jsonb(venue.*) as venue_shop,
    coalesce(
        (
            select jsonb_agg(to_jsonb(collaborator_shop.*) order by participant.created_at)
            from shop_event_participants participant
            join shops collaborator_shop on collaborator_shop.id = participant.shop_id
            where participant.event_id = event.id
                and participant.shop_id is distinct from event.shop_id
        ),
        '[]'::jsonb
    ) as collaborator_shops,
    coalesce(
        (
            select jsonb_agg(
                jsonb_build_object(
                    'name', participant.external_name,
                    'instagram_url', participant.external_instagram_url
                ) order by participant.created_at
            )
            from shop_event_participants participant
            where participant.event_id = event.id
                and participant.shop_id is null
        ),
        '[]'::jsonb
    ) as external_participants,
    event.waiting_method,
    event.waiting_url,
    event.cancelled_dates,
    ((current_timestamp at time zone 'Asia/Seoul')::date = any(event.cancelled_dates))
        as is_cancelled_today,
    event.image_paths
from shop_events event
join shops venue on venue.id = event.shop_id
where event.event_type = any (
        array['collab', 'popup', 'limited_menu', 'summer_limited', 'store_renewal']
    )
order by
    (
        (current_timestamp at time zone 'Asia/Seoul')::date between
            event.start_date and coalesce(event.end_date, event.start_date)
    ) desc,
    event.start_date,
    event.created_at;

create or replace function public.fetch_calendar_event_page(requested_month date)
returns table(events jsonb, has_previous boolean, has_next boolean, notification_dates jsonb)
language sql
stable
set search_path to 'public'
as $function$
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
        and coalesce(event.end_date, event.start_date) >= bounds.month_start
),
reminder_dates as (
    select greatest(event.start_date, bounds.month_start) as notification_date
    from public.shop_events event
    cross join bounds
    where event.start_date >= greatest(bounds.month_start, bounds.today)
        and event.start_date < bounds.next_month_start
        and event.event_type = any (
            array['collab', 'popup', 'limited_menu', 'summer_limited', 'store_renewal']
        )
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
        and event.event_type = any (
            array['collab', 'popup', 'limited_menu', 'summer_limited', 'store_renewal']
        )
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
            and coalesce(event.end_date, event.start_date) >= bounds.previous_month_start
    ) as has_previous,
    exists (
        select 1
        from public.calendar_events event
        cross join bounds
        where event.start_date < bounds.month_after_next_start
            and coalesce(event.end_date, event.start_date) >= bounds.next_month_start
    ) as has_next,
    coalesce(
        (
            select jsonb_agg(to_jsonb(date.notification_date) order by date.notification_date)
            from visible_notification_dates date
        ),
        '[]'::jsonb
    ) as notification_dates;
$function$;

commit;
