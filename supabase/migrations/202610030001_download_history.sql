begin;
create table public.qm_website_download_assets (
 asset_id text primary key,
 downloads bigint not null check(downloads >= 0),
 last_seen_at timestamptz not null default now()
);
alter table public.qm_website_download_assets enable row level security;
revoke all on public.qm_website_download_assets from public,anon,authenticated;
grant all on public.qm_website_download_assets to service_role;

-- Verified by GitHub API on 2026-09-28: the old v2.0 APK had 2 downloads.
-- It was replaced on 2026-10-03 by asset 607745371. Its deleted asset ID
-- is unknown; retain this documented lower bound separately from live IDs.
insert into public.qm_website_download_assets(asset_id,downloads)
values('legacy:v2.0:before-2026-10-03',2);

create function public.qm_website_sync_downloads(p_assets jsonb)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare item jsonb; result jsonb;
begin
 if p_assets is null or jsonb_typeof(p_assets)<>'array' then raise exception 'Invalid assets'; end if;
 for item in select value from jsonb_array_elements(p_assets) loop
  if jsonb_typeof(item)<>'object' or item->>'asset_id' is null
     or item->>'asset_id' !~ '^[1-9][0-9]{0,18}$'
     or item->>'downloads' is null or item->>'downloads' !~ '^[0-9]{1,18}$' then
   raise exception 'Invalid asset';
  end if;
 end loop;
 -- Serialize complete snapshots; retain removed assets and ignore stale lower counts.
 perform 1 from public.qm_website_stats where id for update;
 insert into public.qm_website_download_assets as stored(asset_id,downloads,last_seen_at)
 select value->>'asset_id',max((value->>'downloads')::bigint),now()
 from jsonb_array_elements(p_assets) group by value->>'asset_id'
 on conflict(asset_id) do update set downloads=greatest(stored.downloads,excluded.downloads),last_seen_at=now();
 update public.qm_website_stats set
  downloads=greatest(coalesce(downloads,0),(select coalesce(sum(downloads),0) from public.qm_website_download_assets)),
  downloads_checked_at=now()
 where id returning jsonb_build_object('downloads',downloads,'downloads_checked_at',downloads_checked_at) into result;
 return result;
end $$;
revoke all on function public.qm_website_sync_downloads(jsonb) from public,anon,authenticated;
grant execute on function public.qm_website_sync_downloads(jsonb) to service_role;
-- Refresh on the first request after the upgraded function is deployed.
update public.qm_website_stats set downloads_checked_at=null where id;
commit;
