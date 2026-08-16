begin;

alter table public.shop_events
    add column if not exists cancellation_reason text,
    add column if not exists cancellation_source_url text;

create or replace view public.active_events as
select
    event.id,
    event.event_type,
    event.title,
    event.description,
    event.start_date,
    event.end_date,
    event.source_url,
    public.event_is_today_on(
        event.event_type,
        event.start_date,
        event.end_date,
        (current_timestamp at time zone 'Asia/Seoul')::date
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
    event.image_paths,
    event.sold_out_dates,
    ((current_timestamp at time zone 'Asia/Seoul')::date = any(event.sold_out_dates))
        as is_sold_out_today,
    event.cancellation_reason,
    event.cancellation_source_url
from shop_events event
join shops venue on venue.id = event.shop_id
where event.event_type = any (
        array['collab', 'popup', 'limited_menu', 'summer_limited', 'store_renewal']
    )
    and public.event_is_active_on(
        event.event_type,
        event.start_date,
        event.end_date,
        (current_timestamp at time zone 'Asia/Seoul')::date
    )
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
    public.event_is_today_on(event.event_type, event.start_date, event.end_date, current_date)
        as is_today,
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
    event.image_paths,
    event.sold_out_dates,
    (current_date = any(event.sold_out_dates)) as is_sold_out_today,
    event.cancellation_reason,
    event.cancellation_source_url
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
    and public.event_is_active_on(event.event_type, event.start_date, event.end_date, current_date)
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
    public.event_is_today_on(
        event.event_type,
        event.start_date,
        event.end_date,
        (current_timestamp at time zone 'Asia/Seoul')::date
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
    event.image_paths,
    event.sold_out_dates,
    ((current_timestamp at time zone 'Asia/Seoul')::date = any(event.sold_out_dates))
        as is_sold_out_today,
    event.cancellation_reason,
    event.cancellation_source_url
from shop_events event
join shops venue on venue.id = event.shop_id
where event.event_type = any (
        array['collab', 'popup', 'limited_menu', 'summer_limited', 'store_renewal']
    )
order by
    public.event_is_today_on(
        event.event_type,
        event.start_date,
        event.end_date,
        (current_timestamp at time zone 'Asia/Seoul')::date
    ) desc,
    event.start_date,
    event.created_at;

commit;
