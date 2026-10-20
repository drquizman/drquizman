begin;
do $$
declare before_count bigint; after_count bigint; original_first timestamptz;
begin
 if has_function_privilege('anon','public.qm_user_stats()','execute')
 or has_function_privilege('authenticated','public.qm_touch_user(bigint)','execute')
 or has_table_privilege('anon','public.qm_users','select') then
  raise exception 'Analytics permissions are too broad';
 end if;
 select count(*) into before_count from public.qm_users;
 perform public.qm_touch_user(9000000000000000);
 select first_seen_at into original_first from public.qm_users where telegram_id=9000000000000000;
 perform public.qm_touch_user(9000000000000000);
 select count(*) into after_count from public.qm_users;
 if after_count<>before_count+1 or not exists (
  select 1 from public.qm_users where telegram_id=9000000000000000 and first_seen_at=original_first and last_seen_at is not null and not historical
 ) then raise exception 'User deduplication failed'; end if;
 if (public.qm_user_stats()->>'total')::bigint<>after_count then raise exception 'Stats total incorrect'; end if;
end $$;
rollback;
select public.qm_user_stats();
