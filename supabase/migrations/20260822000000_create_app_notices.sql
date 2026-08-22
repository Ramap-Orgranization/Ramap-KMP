create table public.app_notices (
    platform text primary key check (platform in ('android', 'ios')),
    notice_id uuid not null default gen_random_uuid(),
    is_enabled boolean not null default false,
    title text not null default '',
    message text not null default '',
    constraint app_notices_enabled_content_check check (
        not is_enabled or (length(trim(title)) > 0 and length(trim(message)) > 0)
    )
);

alter table public.app_notices enable row level security;

create policy "Anyone can read app notices"
on public.app_notices
for select
to anon, authenticated
using (true);

insert into public.app_notices (platform, notice_id, is_enabled, title, message)
values
    (
        'android',
        '00000000-0000-4000-8000-000000000215',
        true,
        '공지 기능 테스트',
        '이 공지는 feat/215-app-notice 브랜치의 전역 공지 테스트입니다.'
    ),
    (
        'ios',
        '00000000-0000-4000-8000-000000000215',
        true,
        '공지 기능 테스트',
        '이 공지는 feat/215-app-notice 브랜치의 전역 공지 테스트입니다.'
    );
