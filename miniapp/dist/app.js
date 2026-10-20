'use strict';
const api='https://xoerzqgvcpjrbuwtbtdo.supabase.co/functions/v1/quizman/v1/miniapp/catalog';
const tg=window.Telegram?.WebApp;
tg?.ready(); tg?.expand();
const languageNames={ru:'Русский',en:'English',de:'Deutsch',es:'Español',he:'עברית'};
const statusNames={free:'Бесплатный',owned:'В коллекции',paid:'Продажи скоро'};
let purchasing=false;
const keys=['language','topic','count','status','price'];
const $=id=>document.getElementById(id);
const escapeHTML=value=>String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
let rows=[];
const metadata=window.QUIZMAN_CATALOG || [];
function render(){
 const filters=Object.fromEntries(keys.map(key=>[key,$(key).value]));
 const filtered=rows.filter(row=>keys.every(key=>filters[key]==='all'||(key==='topic'?row.topics.some(topic=>topic.id===filters.topic):String(row[key])===filters[key])));
 $('pack-rows').innerHTML=filtered.map(row=>`<tr><td><span class="language"><span class="language-code" aria-label="${languageNames[row.language]||escapeHTML(row.language)}">${escapeHTML(row.language.toUpperCase())}</span><span class="language-name">${languageNames[row.language]||''}</span></span></td><td><span class="topic-name">${escapeHTML(/^new_750_questions_/.test(row.id)?'Пак №1':row.title)}</span><span class="topic-total">${row.topics.length>1?'Мульти':escapeHTML(row.topics[0]?.title||'—')}</span></td><td class="number">${row.count}</td><td><span class="access ${row.status}">${row.status==='paid'&&row.testPurchase?'Тест · 0 ★':statusNames[row.status]}</span>${row.status==='paid'&&row.testPurchase?`<button class="buy" data-pack="${escapeHTML(row.id)}" ${purchasing?'disabled':''}>Купить</button>`:''}</td><td class="price included">${row.status==='free'||row.testPurchase?'0 ★':'—'}</td></tr>`).join('');
 $('empty').hidden=filtered.length>0;
 $('result-count').textContent=`Показано ${filtered.length} из ${rows.length} паков`;
 $('reset').disabled=keys.every(key=>filters[key]==='all');
 keys.forEach(key=>$(key).classList.toggle('active',filters[key]!=='all'));
}
function reset(){keys.forEach(key=>$(key).value='all');render();}
keys.forEach(key=>$(key).addEventListener('change',render));
$('reset').addEventListener('click',reset);$('empty-reset').addEventListener('click',reset);
$('retry').addEventListener('click',load);
$('pack-rows').addEventListener('click',event=>{const button=event.target.closest('button[data-pack]');if(button)buy(button.dataset.pack);});
async function buy(id){
 if(purchasing)return;
 const pack=rows.find(p=>p.id===id);if(!pack?.testPurchase||pack.status!=='paid')return;
 purchasing=true;render();
 const message=pack.title+' · '+languageNames[pack.language]+' · '+pack.count+' вопросов. Тестовая покупка за 0 звёзд. Списания не будет. Добавить пакет в коллекцию?';
 try {
 const confirmed=tg?.isVersionAtLeast?.('6.2')?await new Promise(resolve=>tg.showConfirm(message,resolve)):window.confirm(message);
 if(!confirmed)return;
 $('purchase-feedback').textContent='Оформляем тестовую покупку…';
 const response=await fetch(api.replace('/catalog','/purchase'),{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({initData:tg.initData,packId:id}),signal:AbortSignal.timeout(15000)});
 if(!response.ok)throw Error(response.status===401?'session':'purchase');
 pack.status='owned';pack.price='owned';
 $('summary').textContent=rows.length+' паков · '+rows.filter(p=>p.status!=='paid').length+' доступны вам';
 $('purchase-feedback').textContent='Пакет добавлен! В игре нажмите «Проверить доступные пакеты», затем «Загрузить все». Списано 0 звёзд.';
 }catch(error){$('purchase-feedback').textContent=error.message==='session'?'Откройте миниап заново из бота — сессия истекла.':'Не удалось подтвердить покупку. Попробуйте ещё раз: повторное оформление безопасно.';}
 finally{purchasing=false;render();}
}
async function load(){
 $('catalog').hidden=true; $('notice').hidden=false; $('retry').hidden=true;
 if(!tg?.initData){
  $('account').textContent='Вход через Telegram';
  $('notice-text').textContent='Откройте каталог кнопкой в @DrQuizman_bot — так мы покажем ваши пакеты.';
  $('open-bot').hidden=false; return;
 }
 $('open-bot').hidden=true; $('notice-text').textContent='Загружаем ваши пакеты…';
 try {
  const response=await fetch(api,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({initData:tg.initData}),signal:AbortSignal.timeout(15000)});
  if(response.status===401) throw new Error('session');
  if(!response.ok) throw new Error('network');
  const data=await response.json();
  rows=data.packs.map(pack=>{
   const known=metadata.find(item=>item.id===pack.id);
   return {...pack,price:pack.status==='owned'?'owned':pack.status==='free'||pack.testPurchase?'0':'soon',topics:pack.topics.map(title=>{
    const topic=known?.topics.find(item=>item.title===title);
    return {id:topic?.id||title,title,label:topic?.label||title,icon:topic?.icon||'❓'};
   })};
  });
  $('topic').replaceChildren(new Option('Все темы','all'));
  for(const topic of [...new Map(rows.flatMap(p=>p.topics.map(t=>[t.id,t]))).values()])$('topic').add(new Option(topic.label,topic.id));
  $('count').replaceChildren(new Option('Все','all'));
  for(const count of [...new Set(rows.map(p=>p.count))].sort((a,b)=>a-b))$('count').add(new Option(String(count),String(count)));
  $('account').textContent='Telegram подключён ✓';
  $('summary').textContent=`${rows.length} паков · ${rows.filter(p=>p.status!=='paid').length} доступны вам`;
  $('notice').hidden=true; $('catalog').hidden=false; render();
 } catch(error){
  $('notice-text').textContent=error.message==='session'?'Сессия истекла. Закройте миниап и откройте каталог из бота снова.':'Не удалось загрузить каталог. Проверьте интернет и повторите.';
  $('retry').hidden=error.message==='session';
 }
}
load();
