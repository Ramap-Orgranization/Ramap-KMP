select cron.unschedule(jobid)
from cron.job
where jobname = 'ramap-delete-expired-shop-events';

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
    venue.lng as venue_lng
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
    venue.lng as venue_lng
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
    venue.lng as venue_lng
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
