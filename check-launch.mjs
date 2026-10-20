import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFile} from 'node:fs/promises';
const code=await readFile(new URL('./web/launch.js',import.meta.url),'utf8');
function setup(responses){
 let clock=0,id=0,calls=0;const timers=new Map(),listeners={},elements={};
 for(const name of ['preparing','studio','status','retry','spinner','frame-help'])elements[name]={hidden:['studio','retry','frame-help'].includes(name),textContent:'',removeAttribute(key){delete this[key];},addEventListener(name,fn){this[name]=fn;}};
 const context=vm.createContext({document:{getElementById:name=>elements[name]},AbortController,Date:{now:()=>clock},addEventListener:(name,fn)=>listeners[name]=fn,setTimeout:(fn,ms)=>{const key=++id;timers.set(key,{fn,at:clock+ms});return key;},clearTimeout:key=>timers.delete(key),fetch:async(url,options)=>{calls++;assert.equal(url,'https://quizman-web-prototype.onrender.com/health');assert.equal(options.credentials,'omit');const value=responses.shift();if(value instanceof Error)throw value;return {ok:value==='ok'||value==='html',text:async()=>value==='html'?'<html>Render is starting</html>':value||'busy'};}});
 vm.runInContext(code,context);
 async function flush(){for(let i=0;i<12;i++)await Promise.resolve();}
 async function advance(ms){const end=clock+ms;for(;;){await flush();const entry=[...timers].filter(([,v])=>v.at<=end).sort((a,b)=>a[1].at-b[1].at)[0];if(!entry)break;clock=entry[1].at;timers.delete(entry[0]);entry[1].fn();}clock=end;await flush();}
 return {elements,listeners,timers,flush,advance,get calls(){return calls;}};
}
let env=setup(['ok']);await env.flush();assert.equal(env.elements.studio.src,'https://quizman-web-prototype.onrender.com/');assert.equal(env.elements.preparing.hidden,false,'loading remains until frame loads');env.elements.studio.onload();assert.equal(env.elements.studio.hidden,false);assert.equal(env.elements.preparing.hidden,true);await env.advance(60000);assert.equal(env.calls,1,'no keepalive requests after readiness');
env=setup([new Error('offline'),'html','ok']);await env.advance(6000);assert.equal(env.calls,3);assert.ok(env.elements.studio.onload,'retries network errors and Render loading HTML until readiness');
env=setup([]);await env.advance(150000);assert.equal(env.elements.retry.hidden,false);assert.equal(env.elements.spinner.hidden,true);const prior=env.calls;env.listeners.pagehide();await env.advance(30000);assert.equal(env.calls,prior,'leaving cancels wakeup');
env=setup(['ok']);await env.advance(30000);assert.equal(env.elements.retry.hidden,false,'frame timeout offers retry');
env=setup([]);await env.advance(150000);env.elements.retry.click();await env.flush();assert.equal(env.elements.retry.hidden,true,'manual retry restores loading');env.listeners.pagehide();await env.advance(3000);
console.log('PASS: ready/frame loading, cold-start retries, non-ready HTML, finite timeout, manual retry, navigation cancellation and no polling after readiness');
