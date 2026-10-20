begin;
do $$
declare base bigint; actual bigint;
begin
 if has_table_privilege('anon','public.qm_website_download_assets','select')
 or has_function_privilege('anon','public.qm_website_sync_downloads(jsonb)','execute') then
  raise exception 'Download history is publicly accessible';
 end if;
 select coalesce(sum(downloads),0) into base from public.qm_website_download_assets;
 -- These test changes are fully rolled back, including the total.
 update public.qm_website_stats set downloads=base where id;
 perform public.qm_website_sync_downloads('[{"asset_id":"900000000000001","downloads":2}]');
 perform public.qm_website_sync_downloads('[{"asset_id":"900000000000001","downloads":2}]');
 select downloads into actual from public.qm_website_stats where id;
 if actual<>base+2 then raise exception 'Repeated snapshot double counted'; end if;
 perform public.qm_website_sync_downloads('[{"asset_id":"900000000000002","downloads":1}]');
 select downloads into actual from public.qm_website_stats where id;
 if actual<>base+3 then raise exception 'Replacement lost previous downloads'; end if;
 perform public.qm_website_sync_downloads('[{"asset_id":"900000000000002","downloads":4}]');
 perform public.qm_website_sync_downloads('[{"asset_id":"900000000000002","downloads":1}]');
 perform public.qm_website_sync_downloads('[]');
 select downloads into actual from public.qm_website_stats where id;
 if actual<>base+6 then raise exception 'Stale snapshot or deletion reduced total'; end if;
end $$;
rollback;
select 'Download history tests passed; test data rolled back' as result;
