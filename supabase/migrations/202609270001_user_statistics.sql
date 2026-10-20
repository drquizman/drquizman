begin;
create table public.qm_users (
 telegram_id bigint primary key check(telegram_id>0),
 first_seen_at timestamptz not null default now(),
 last_seen_at timestamptz,
 historical boolean not null default false
);
alter table public.qm_users enable row level security;
revoke all on public.qm_users from public,anon,authenticated;
grant all on public.qm_users to service_role;
-- Historical records establish known accounts, not visits or registration dates.
insert into public.qm_users(telegram_id,first_seen_at,historical)
select telegram_id,min(created_at),true from (
 select telegram_id,created_at from public.qm_devices
 union all select telegram_id,created_at from public.qm_entitlements
 union all select telegram_id,created_at from public.qm_test_orders
) known group by telegram_id;
create function public.qm_touch_user(p_telegram_id bigint)
returns void language sql security definer set search_path='' as $$
 insert into public.qm_users(telegram_id,last_seen_at) values(p_telegram_id,now())
 on conflict(telegram_id) do update set last_seen_at=greatest(qm_users.last_seen_at,excluded.last_seen_at);
$$;
create function public.qm_user_stats()
returns jsonb language sql stable security definer set search_path='' as $$
 select jsonb_build_object(
  'total',count(*),
  'new_day',count(*) filter(where not historical and first_seen_at>=now()-interval '24 hours'),
  'new_week',count(*) filter(where not historical and first_seen_at>=now()-interval '7 days'),
  'active_day',count(*) filter(where last_seen_at>=now()-interval '24 hours'),
  'active_week',count(*) filter(where last_seen_at>=now()-interval '7 days'),
  'linked_users',(select count(distinct telegram_id) from public.qm_devices where not revoked),
  'devices',(select count(*) from public.qm_devices where not revoked),
  'test_orders',(select count(*) from public.qm_test_orders),
  'test_buyers',(select count(distinct telegram_id) from public.qm_test_orders)
 ) from public.qm_users;
$$;
revoke all on function public.qm_touch_user(bigint),public.qm_user_stats() from public,anon,authenticated;
grant execute on function public.qm_touch_user(bigint),public.qm_user_stats() to service_role;
commit;
