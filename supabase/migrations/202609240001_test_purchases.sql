begin;
alter table public.qm_packs add column test_purchase_enabled boolean not null default false;
create table public.qm_test_orders (
 id uuid primary key default gen_random_uuid(),
 telegram_id bigint not null check(telegram_id>0),
 pack_id text not null references public.qm_packs(id),
 amount_stars integer not null default 0 check(amount_stars=0),
 created_at timestamptz not null default now(),
 unique(telegram_id,pack_id)
);
alter table public.qm_test_orders enable row level security;
revoke all on public.qm_test_orders from anon,authenticated;
grant all on public.qm_test_orders to service_role;
create function public.qm_test_purchase(p_telegram_id bigint,p_pack_id text)
returns boolean language plpgsql security definer set search_path='' as $$
begin
 if p_telegram_id is null or p_telegram_id<=0 then return false; end if;
 perform 1 from public.qm_packs where id=p_pack_id and active and test_purchase_enabled and not is_free for share;
 if not found then return false; end if;
 insert into public.qm_test_orders(telegram_id,pack_id) values(p_telegram_id,p_pack_id) on conflict do nothing;
 insert into public.qm_entitlements(telegram_id,pack_id,source) values(p_telegram_id,p_pack_id,'test-zero-stars') on conflict do nothing;
 return true;
end $$;
revoke all on function public.qm_test_purchase(bigint,text) from public,anon,authenticated;
grant execute on function public.qm_test_purchase(bigint,text) to service_role;
commit;
