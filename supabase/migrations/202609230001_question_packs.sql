begin;
create table public.qm_packs (
 id text primary key check (id ~ '^[a-zA-Z0-9_-]{1,100}$' and id <> 'base'),
 title text not null, language text not null check (language in ('ru','en','de','es','he')),
 question_count integer not null check(question_count > 0), topic_count integer not null check(topic_count > 0),
 topics jsonb not null default '[]', storage_path text not null unique,
 sha256 text not null check(sha256 ~ '^[a-f0-9]{64}$'),
 is_free boolean not null default false, price_stars integer check(price_stars > 0),
 active boolean not null default true, created_at timestamptz not null default now()
);
create table public.qm_entitlements (
 telegram_id bigint not null check(telegram_id > 0), pack_id text not null references public.qm_packs(id),
 source text not null default 'manual-test', created_at timestamptz not null default now(),
 primary key(telegram_id,pack_id)
);
create table public.qm_device_links (
 code text primary key check(code ~ '^[A-Z2-9]{8}$'), poll_hash text not null unique,
 device_hash text not null unique, expires_at timestamptz not null,
 telegram_id bigint, approved_at timestamptz, created_at timestamptz not null default now()
);
create table public.qm_devices (
 token_hash text primary key, telegram_id bigint not null check(telegram_id > 0),
 revoked boolean not null default false, created_at timestamptz not null default now()
);
create table public.qm_rate_limits (
 key text primary key, window_start timestamptz not null, requests integer not null
);
alter table public.qm_packs enable row level security;
alter table public.qm_entitlements enable row level security;
alter table public.qm_device_links enable row level security;
alter table public.qm_devices enable row level security;
alter table public.qm_rate_limits enable row level security;
revoke all on public.qm_packs,public.qm_entitlements,public.qm_device_links,public.qm_devices,public.qm_rate_limits from anon,authenticated;
grant all on public.qm_packs,public.qm_entitlements,public.qm_device_links,public.qm_devices,public.qm_rate_limits to service_role;

create function public.qm_rate_limit(p_key text,p_limit integer,p_seconds integer)
returns boolean language plpgsql security definer set search_path = '' as $$
declare n integer;
begin
 insert into public.qm_rate_limits as r(key,window_start,requests) values(p_key,now(),1)
 on conflict(key) do update set
 requests = case when r.window_start < now()-make_interval(secs=>p_seconds) then 1 else r.requests+1 end,
 window_start = case when r.window_start < now()-make_interval(secs=>p_seconds) then now() else r.window_start end
 returning requests into n;
 return n <= p_limit;
end $$;
create function public.qm_approve_link(p_code text,p_telegram_id bigint)
returns boolean language plpgsql security definer set search_path = '' as $$
declare link public.qm_device_links;
begin
 if p_telegram_id <= 0 then return false; end if;
 select * into link from public.qm_device_links where code=p_code for update;
 if not found or link.expires_at<=now() then return false; end if;
 if link.telegram_id is not null then return link.telegram_id=p_telegram_id; end if;
 insert into public.qm_devices(token_hash,telegram_id) values(link.device_hash,p_telegram_id);
 update public.qm_device_links set telegram_id=p_telegram_id,approved_at=now() where code=p_code;
 return true;
end $$;
revoke all on function public.qm_rate_limit(text,integer,integer),public.qm_approve_link(text,bigint) from public,anon,authenticated;
grant execute on function public.qm_rate_limit(text,integer,integer),public.qm_approve_link(text,bigint) to service_role;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values('question-packs','question-packs',false,10485760,array['application/json'])
on conflict(id) do update set public=false,file_size_limit=10485760,allowed_mime_types=array['application/json'];
-- No public object policies: only the server's service role can read/write these files.
commit;
