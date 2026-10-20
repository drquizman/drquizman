update public.qm_packs set title = case language
  when 'ru' then 'Дополнительный пакет №1'
  when 'en' then 'Extra question pack #1'
  when 'de' then 'Zusätzliches Fragenpaket Nr. 1'
  when 'es' then 'Paquete adicional n.º 1'
  when 'he' then 'חבילת שאלות נוספת מס׳ 1'
end
where id = 'new_750_questions_' || language;
