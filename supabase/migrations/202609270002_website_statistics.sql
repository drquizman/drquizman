begin;
create table public.qm_website_stats (
 id boolean primary key default true check (id),
 visits bigint not null default 0,
 downloads bigint,
 downloads_checked_at timestamptz,
 started_at timestamptz not null default now()
);
insert into public.qm_website_stats(id) values(true);
create table public.qm_website_sessions (
 session_hash text primary key,
 client_hash text not null,
 last_seen timestamptz not null default now()
);
create index qm_website_sessions_last_seen on public.qm_website_sessions(last_seen);
alter table public.qm_website_stats enable row level security;
alter table public.qm_website_sessions enable row level security;
revoke all on public.qm_website_stats,public.qm_website_sessions from public,anon,authenticated;
grant all on public.qm_website_stats,public.qm_website_sessions to service_role;

-- One visit per browser session, renewed after 30 minutes without a page load.
-- Keep only keyed hashes for at most a day; never store raw IP addresses.
create function public.qm_website_visit(p_session text,p_client text)
returns void language plpgsql security definer set search_path = '' as $$
declare previous timestamptz;
begin
 if p_session !~ '^[a-f0-9]{64}$' or p_client !~ '^[a-f0-9]{64}$' then
  raise exception 'Invalid session';
 end if;
 perform 1 from public.qm_website_stats where id for update;
 delete from public.qm_website_sessions where last_seen < now()-interval '1 day';
 select last_seen into previous from public.qm_website_sessions where session_hash=p_session;
 if previous is null and (select count(*) from public.qm_website_sessions
   where client_hash=p_client and last_seen>now()-interval '1 hour') >= 30 then return; end if;
 if previous is null or previous < now()-interval '30 minutes' then
  update public.qm_website_stats set visits=visits+1 where id;
 end if;
 insert into public.qm_website_sessions(session_hash,client_hash,last_seen) values(p_session,p_client,now())
 on conflict(session_hash) do update set last_seen=now();
end $$;
revoke all on function public.qm_website_visit(text,text) from public,anon,authenticated;
grant execute on function public.qm_website_visit(text,text) to service_role;
commit;
