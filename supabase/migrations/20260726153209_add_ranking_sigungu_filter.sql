create or replace function public.fetch_shop_rankings(
  p_area text,
  p_sigungu text,
  p_category_ids text[],
  p_cursor_like_count bigint,
  p_cursor_name text,
  p_cursor_id uuid,
  p_limit integer
)
returns table(
  id uuid,
  kakao_place_id varchar,
  name varchar,
  address text,
  lat numeric,
  lng numeric,
  kakao_place_url text,
  naver_place_url text,
  phone varchar,
  business_hours text,
  instagram_url varchar,
  instagram_profile_image_path text,
  kakao_rating numeric,
  menu_category_ids text[],
  is_visible boolean,
  created_at timestamptz,
  updated_at timestamptz,
  like_count bigint
)
language sql
stable
set search_path = ''
as $function$
  with candidates as (
    select
      shops.*,
      coalesce(counts.like_count, 0)::bigint as normalized_like_count,
      split_part(btrim(shops.address), ' ', 2) as normalized_sigungu,
      case split_part(btrim(shops.address), ' ', 1)
        when '서울' then 'SEOUL'
        when '서울특별시' then 'SEOUL'
        when '부산' then 'BUSAN'
        when '부산광역시' then 'BUSAN'
        when '대구' then 'DAEGU'
        when '대구광역시' then 'DAEGU'
        when '인천' then 'INCHEON'
        when '인천광역시' then 'INCHEON'
        when '광주' then 'GWANGJU'
        when '광주광역시' then 'GWANGJU'
        when '대전' then 'DAEJEON'
        when '대전광역시' then 'DAEJEON'
        when '울산' then 'ULSAN'
        when '울산광역시' then 'ULSAN'
        when '세종' then 'SEJONG'
        when '세종특별자치시' then 'SEJONG'
        when '경기' then 'GYEONGGI'
        when '경기도' then 'GYEONGGI'
        when '충북' then 'CHUNGBUK'
        when '충청북도' then 'CHUNGBUK'
        when '충남' then 'CHUNGNAM'
        when '충청남도' then 'CHUNGNAM'
        when '전남' then 'JEONNAM'
        when '전라남도' then 'JEONNAM'
        when '경북' then 'GYEONGBUK'
        when '경상북도' then 'GYEONGBUK'
        when '경남' then 'GYEONGNAM'
        when '경상남도' then 'GYEONGNAM'
        when '강원' then 'GANGWON'
        when '강원도' then 'GANGWON'
        when '강원특별자치도' then 'GANGWON'
        when '전북' then 'JEONBUK'
        when '전라북도' then 'JEONBUK'
        when '전북특별자치도' then 'JEONBUK'
        when '제주' then 'JEJU'
        when '제주도' then 'JEJU'
        when '제주특별자치도' then 'JEJU'
        else null
      end as normalized_area
    from public.shops as shops
    left join public.shop_bookmark_counts as counts
      on counts.shop_id = shops.id
    where shops.is_visible = true
  )
  select
    candidates.id,
    candidates.kakao_place_id,
    candidates.name,
    candidates.address,
    candidates.lat,
    candidates.lng,
    candidates.kakao_place_url,
    candidates.naver_place_url,
    candidates.phone,
    candidates.business_hours,
    candidates.instagram_url,
    candidates.instagram_profile_image_path,
    candidates.kakao_rating,
    candidates.menu_category_ids,
    candidates.is_visible,
    candidates.created_at,
    candidates.updated_at,
    candidates.normalized_like_count
  from candidates
  where (p_area is null or candidates.normalized_area = p_area)
    and (
      p_sigungu is null
      or (
        candidates.normalized_sigungu ~ '(시|군|구)$'
        and candidates.normalized_sigungu = p_sigungu
      )
    )
    and (
      coalesce(cardinality(p_category_ids), 0) = 0
      or candidates.menu_category_ids && p_category_ids
    )
    and (
      (
        p_cursor_like_count is null
        and p_cursor_name is null
        and p_cursor_id is null
      )
      or candidates.normalized_like_count < p_cursor_like_count
      or (
        candidates.normalized_like_count = p_cursor_like_count
        and candidates.name > p_cursor_name
      )
      or (
        candidates.normalized_like_count = p_cursor_like_count
        and candidates.name = p_cursor_name
        and candidates.id > p_cursor_id
      )
    )
  order by
    candidates.normalized_like_count desc,
    candidates.name asc,
    candidates.id asc
  limit least(greatest(coalesce(p_limit, 20), 1), 50) + 1;
$function$;

create or replace function public.fetch_ranking_sigungu(p_area text)
returns table(sigungu text)
language sql
stable
set search_path = ''
as $function$
  with visible_shop_areas as (
    select
      split_part(btrim(shops.address), ' ', 2) as sigungu,
      case split_part(btrim(shops.address), ' ', 1)
        when '서울' then 'SEOUL'
        when '서울특별시' then 'SEOUL'
        when '부산' then 'BUSAN'
        when '부산광역시' then 'BUSAN'
        when '대구' then 'DAEGU'
        when '대구광역시' then 'DAEGU'
        when '인천' then 'INCHEON'
        when '인천광역시' then 'INCHEON'
        when '광주' then 'GWANGJU'
        when '광주광역시' then 'GWANGJU'
        when '대전' then 'DAEJEON'
        when '대전광역시' then 'DAEJEON'
        when '울산' then 'ULSAN'
        when '울산광역시' then 'ULSAN'
        when '세종' then 'SEJONG'
        when '세종특별자치시' then 'SEJONG'
        when '경기' then 'GYEONGGI'
        when '경기도' then 'GYEONGGI'
        when '충북' then 'CHUNGBUK'
        when '충청북도' then 'CHUNGBUK'
        when '충남' then 'CHUNGNAM'
        when '충청남도' then 'CHUNGNAM'
        when '전남' then 'JEONNAM'
        when '전라남도' then 'JEONNAM'
        when '경북' then 'GYEONGBUK'
        when '경상북도' then 'GYEONGBUK'
        when '경남' then 'GYEONGNAM'
        when '경상남도' then 'GYEONGNAM'
        when '강원' then 'GANGWON'
        when '강원도' then 'GANGWON'
        when '강원특별자치도' then 'GANGWON'
        when '전북' then 'JEONBUK'
        when '전라북도' then 'JEONBUK'
        when '전북특별자치도' then 'JEONBUK'
        when '제주' then 'JEJU'
        when '제주도' then 'JEJU'
        when '제주특별자치도' then 'JEJU'
        else null
      end as area
    from public.shops as shops
    where shops.is_visible = true
  )
  select distinct visible_shop_areas.sigungu
  from visible_shop_areas
  where visible_shop_areas.area = p_area
    and visible_shop_areas.sigungu ~ '(시|군|구)$'
  order by visible_shop_areas.sigungu;
$function$;
