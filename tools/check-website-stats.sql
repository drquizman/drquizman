begin;
do $$
declare before_count bigint;
begin
 if has_table_privilege('anon','public.qm_website_stats','select')
 or has_table_privilege('anon','public.qm_website_sessions','select')
 or has_function_privilege('anon','public.qm_website_visit(text,text)','execute') then
  raise exception 'Public database permissions too broad';
 end if;
 select visits into before_count from public.qm_website_stats where id;
 perform public.qm_website_visit(repeat('a',64),repeat('b',64));
 perform public.qm_website_visit(repeat('a',64),repeat('b',64));
 if (select visits from public.qm_website_stats where id)<>before_count+1 then raise exception 'Reload deduplication failed'; end if;
 update public.qm_website_sessions set last_seen=now()-interval '31 minutes' where session_hash=repeat('a',64);
 perform public.qm_website_visit(repeat('a',64),repeat('b',64));
 if (select visits from public.qm_website_stats where id)<>before_count+2 then raise exception 'Session renewal failed'; end if;
end $$;
rollback;
select visits,downloads,started_at from public.qm_website_stats;
