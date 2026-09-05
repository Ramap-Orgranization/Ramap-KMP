begin;

alter table public.shops
    add column if not exists waiting_provider text,
    add column if not exists waiting_provider_url text;

update public.shops shop
set
    waiting_provider = waiting.provider,
    waiting_provider_url = waiting.provider_url
from public.shop_waiting_systems waiting
where shop.id::text = waiting.shop_id::text
  and shop.waiting_provider is null
  and shop.waiting_provider_url is null;

alter table public.shops rename to ramen_shops;

alter table public.ramen_shops
    drop column phone,
    drop column kakao_place_id;

drop table public.shop_waiting_systems;

create view public.shops
with (security_invoker = true)
as
select
    shop.*,
    null::text as phone,
    substring(
        shop.kakao_place_url from '^https?://place\.map\.kakao\.com/([0-9]+)([/?#]|$)'
    ) as kakao_place_id
from public.ramen_shops shop;

create view public.shop_waiting_systems
with (security_invoker = true)
as
select
    shop.id,
    shop.id as shop_id,
    shop.waiting_provider as provider,
    shop.waiting_provider_url as provider_url
from public.ramen_shops shop
where shop.waiting_provider is not null;

grant select on public.shops, public.shop_waiting_systems to anon, authenticated;

commit;
