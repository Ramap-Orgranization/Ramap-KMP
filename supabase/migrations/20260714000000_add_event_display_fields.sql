alter table public.shop_events
    add column if not exists event_type text not null default 'collab'
        check (event_type in ('collab', 'popup', 'limited_menu')),
    add column if not exists waiting_method text,
    add column if not exists waiting_url text;

create table if not exists public.shop_event_participants (
    id uuid primary key default gen_random_uuid(),
    event_id uuid not null references public.shop_events(id) on delete cascade,
    shop_id uuid references public.shops(id) on delete cascade,
    external_name text,
    external_instagram_url text,
    created_at timestamptz not null default now(),
    constraint shop_event_participant_identity check (
        (shop_id is not null and external_name is null) or
        (shop_id is null and external_name is not null)
    ),
    unique (event_id, shop_id)
);

alter table public.shop_event_participants enable row level security;

grant select on public.shop_event_participants to anon, authenticated;

create policy "Public can read event participants"
    on public.shop_event_participants for select
    using (true);

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
    current_date between event.start_date and event.end_date as is_today,
    event.shop_id as venue_shop_id,
    venue.name as venue_shop_name,
    venue.address as venue_address,
    collaborator.shop_id as collaborator_shop_id,
    coalesce(collaborator_shop.name, collaborator.external_name) as collaborator_name,
    collaborator.external_instagram_url as collaborator_instagram_url,
    event.waiting_method,
    event.waiting_url
from public.shop_events event
join public.shops venue on venue.id = event.shop_id
cross join lateral (
    select event.shop_id as shop_context_id
    union
    select participant.shop_id
    from public.shop_event_participants participant
    where participant.event_id = event.id and participant.shop_id is not null
) event_context
left join lateral (
    select participant.shop_id, participant.external_name, participant.external_instagram_url
    from public.shop_event_participants participant
    where participant.event_id = event.id
      and participant.shop_id is distinct from event_context.shop_context_id
    order by participant.created_at
    limit 1
) collaborator on true
left join public.shops collaborator_shop on collaborator_shop.id = collaborator.shop_id
where event.event_type in ('collab', 'popup', 'limited_menu')
  and event.end_date >= current_date
order by event.start_date, event.created_at;

grant select on public.active_shop_events to anon, authenticated;
