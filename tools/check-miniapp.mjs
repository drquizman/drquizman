import assert from 'node:assert/strict';
import {createHmac} from 'node:crypto';
import {telegramUser,createHandler} from '../supabase/functions/quizman/index.mjs';
const token='123456:TEST_ONLY';
const now=Math.floor(Date.now()/1000);
function sign(user,date=now){
 const p=new URLSearchParams({auth_date:String(date),query_id:'test',user:JSON.stringify({id:user,first_name:'Test'})});
 const key=createHmac('sha256','WebAppData').update(token).digest();
 p.set('hash',createHmac('sha256',key).update([...p.entries()].sort(([a],[b])=>a.localeCompare(b)).map(([k,v])=>`${k}=${v}`).join('\n')).digest('hex'));
 return p.toString();
}
assert.equal((await telegramUser(sign(11),token)).id,11);
for(const raw of [sign(11).replace('Test','Fake'),sign(11,now-3601),sign(11,now+100),sign(11)+'&auth_date='+now,'',sign(-1)]) {
 await assert.rejects(()=>telegramUser(raw,token));
}
let queries=[];
const handler=createHandler({SUPABASE_URL:'https://db.test',SUPABASE_SERVICE_ROLE_KEY:'test',TELEGRAM_BOT_TOKEN:token,MINIAPP_URL:'https://catalog.test'},async url=>{
 queries.push(url);
 if(url.includes('rpc/'))return Response.json(true);
 if(url.includes('qm_entitlements'))return Response.json(url.includes('eq.11&')?[{pack_id:'owned'}]:[]);
 return Response.json([{id:'free',is_free:true,title:'Free',language:'ru',question_count:750,topics:['A']},{id:'owned',is_free:false,title:'Owned',language:'ru',question_count:750,topics:['A']}]);
});
async function request(initData,origin='https://catalog.test'){
 return handler(new Request('https://db.test/functions/v1/quizman/v1/miniapp/catalog',{method:'POST',headers:{Origin:origin,'Content-Type':'application/json'},body:JSON.stringify({initData})}));
}
const owner=await request(sign(11));assert.equal(owner.status,200);assert.equal(owner.headers.get('Access-Control-Allow-Origin'),'https://catalog.test');
assert.deepEqual((await owner.json()).packs.map(p=>p.status),['free','owned']);
assert.deepEqual((await (await request(sign(12))).json()).packs.map(p=>p.status),['free','paid']);
queries=[];assert.equal((await request('forged')).status,401);assert.equal(queries.length,0);
assert.equal((await request(sign(11),'https://evil.test')).status,403);
console.log('Mini App checks passed: valid signatures, tampering, expiry, duplicate fields, account isolation, CORS, no payment path.');
let purchaseCalls=[];
const purchaseHandler=createHandler({SUPABASE_URL:'https://db.test',SUPABASE_SERVICE_ROLE_KEY:'test',TELEGRAM_BOT_TOKEN:token,MINIAPP_URL:'https://catalog.test'},async (url,options)=>{
 if(url.includes('qm_test_purchase'))purchaseCalls.push(JSON.parse(options.body));
 return Response.json(true);
});
const buy=(body)=>purchaseHandler(new Request('https://db.test/functions/v1/quizman/v1/miniapp/purchase',{method:'POST',headers:{Origin:'https://catalog.test','Content-Type':'application/json'},body:JSON.stringify(body)}));
assert.equal((await buy({initData:'forged',packId:'extra_ru_01'})).status,401);
assert.equal(purchaseCalls.length,0);
assert.equal((await buy({initData:sign(11),packId:'new_750_questions_ru'})).status,400);
const purchased=await buy({initData:sign(11),packId:'extra_ru_01',telegram_id:999,price:999});
assert.equal(purchased.status,200);assert.equal((await purchased.json()).amountStars,0);
assert.deepEqual(purchaseCalls,[{p_telegram_id:11,p_pack_id:'extra_ru_01'}]);
console.log('Test purchase API: forged identities denied, pack allowlist, identity derived only from Telegram, no client price accepted.');
