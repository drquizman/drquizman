import assert from 'node:assert/strict';
import {createHandler} from '../supabase/functions/quizman/index.mjs';
const calls=[];
const env={SUPABASE_URL:'https://db.test',SUPABASE_SERVICE_ROLE_KEY:'test',TELEGRAM_BOT_TOKEN:'test',TELEGRAM_WEBHOOK_SECRET:'secret',QUIZMAN_OWNER_TELEGRAM_ID:'179204494'};
const mock=async(url,options)=>{
 calls.push({url,body:JSON.parse(options.body)});
 if(url.includes('api.telegram.org')) return Response.json({ok:true});
 if(url.endsWith('qm_user_stats')) return Response.json({total:5,new_day:1,new_week:2,active_day:3,active_week:4,linked_users:2,devices:3,test_orders:7,test_buyers:2});
 return Response.json(true);
};
const handler=createHandler(env,mock);
const request=(id,secret='secret',type='private')=>new Request('https://db.test/functions/v1/quizman/telegram',{
 method:'POST',headers:{'X-Telegram-Bot-Api-Secret-Token':secret},
 body:JSON.stringify({message:{from:{id},chat:{id,type},text:'/stats'}})
});
assert.equal((await handler(request(179204494,'forged'))).status,401);
assert.equal(calls.length,0);
await handler(request(179204494,'secret','group'));assert.equal(calls.length,0);
assert.equal((await handler(request(123))).status,200);
assert(!calls.some(c=>c.url.endsWith('qm_user_stats')));
assert.equal(calls.at(-1).body.text,'Команда недоступна.');
calls.length=0;
assert.equal((await handler(request(179204494))).status,200);
assert.equal(calls.filter(c=>c.url.endsWith('qm_user_stats')).length,1);
assert.deepEqual(calls.find(c=>c.url.endsWith('qm_touch_user')).body,{p_telegram_id:179204494});
assert.match(calls.at(-1).body.text,/Всего известных пользователей: 5/);
assert.match(calls.at(-1).body.text,/Тестовых покупок: 7/);
calls.length=0;
await createHandler({...env,QUIZMAN_OWNER_TELEGRAM_ID:undefined},mock)(request(179204494));
assert(!calls.some(c=>c.url.endsWith('qm_user_stats')));
console.log('Statistics checks passed: owner-only, default deny, webhook authentication, group exclusion, authenticated tracking.');
