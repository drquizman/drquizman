import assert from 'node:assert/strict';
import {createHandler} from '../supabase/functions/website-stats/index.mjs';
const env={SUPABASE_URL:'https://db.test',SUPABASE_SERVICE_ROLE_KEY:'test-secret'};
const origin='https://www.drquizman.ru';
let visits=0, calls=[], githubDown=false;
const stats={visits:0,downloads:7,downloads_checked_at:null,started_at:new Date().toISOString()};
const mock=async(url,options)=>{
 calls.push({url,options});
 if(url.includes('api.github.com')) return githubDown?new Response('',{status:503}):Response.json([{assets:[{id:101,name:'DrQuizman.apk',download_count:13},{id:102,name:'source.zip',download_count:99}]},{assets:[{id:103,name:'old.apk',download_count:5}]}]);
 if(url.endsWith('rpc/qm_website_visit')){visits++;return Response.json(null);}
 if(url.endsWith('rpc/qm_website_sync_downloads')){
  assert.deepEqual(JSON.parse(options.body),{p_assets:[{asset_id:'101',downloads:13},{asset_id:'103',downloads:5}]});
  Object.assign(stats,{downloads:18,downloads_checked_at:new Date().toISOString()});
  return Response.json({downloads:stats.downloads,downloads_checked_at:stats.downloads_checked_at});
 }
 return Response.json([{...stats}]);
};
const handler=createHandler(env,mock);
const req=(method='GET',body,requestOrigin=origin)=>new Request('https://db.test/functions/v1/website-stats',{method,headers:{Origin:requestOrigin,'Content-Type':'application/json'},...(body===undefined?{}:{body:JSON.stringify(body)})});
assert.equal((await handler(req('POST',{session:'bad'}))).status,400);
assert.equal((await handler(req('POST',{session:crypto.randomUUID()},'https://evil.test'))).status,403);
assert.equal(calls.length,0);
const preflight=await handler(req('OPTIONS'));
assert.equal(preflight.status,204);assert.equal(preflight.headers.get('Access-Control-Allow-Origin'),origin);
let response=await handler(req());
assert.equal((await response.json()).downloads,18);assert.equal(visits,0);
assert(!calls.filter(c=>c.url.includes('api.github.com')).some(c=>c.options.headers.Authorization));
calls=[];
response=await handler(req('POST',{session:crypto.randomUUID()}));
assert.equal(response.status,200);assert.equal(visits,1);
assert.equal(calls.filter(c=>c.url.includes('api.github.com')).length,0);
const body=JSON.parse(calls.find(c=>c.url.endsWith('rpc/qm_website_visit')).options.body);
assert.match(body.p_session,/^[a-f0-9]{64}$/);assert.match(body.p_client,/^[a-f0-9]{64}$/);
stats.downloads_checked_at=null;githubDown=true;
assert.equal((await (await handler(req())).json()).downloads,18);
const unavailable=createHandler(env,async()=>new Response('',{status:503}));
assert.equal((await unavailable(req())).status,503);
console.log('Website stats: origin checks, validation, anonymous GET, hashed sessions, APK-only totals, caching, upstream failure passed.');
