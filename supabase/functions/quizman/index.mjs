const encoder = new TextEncoder();
export async function hash(value) {
  const result = await crypto.subtle.digest('SHA-256', encoder.encode(value));
  return [...new Uint8Array(result)].map(byte => byte.toString(16).padStart(2, '0')).join('');
}
export function randomToken() {
  return [...crypto.getRandomValues(new Uint8Array(32))].map(byte => byte.toString(16).padStart(2, '0')).join('');
}
export function randomCode() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  return Array.from(crypto.getRandomValues(new Uint8Array(8)), byte => alphabet[byte % alphabet.length]).join('');
}
export function normalizeCode(value) { return String(value).replace(/[\s-]/g, '').toUpperCase(); }
export function validCode(value) { return /^[A-Z2-9]{8}$/.test(value); }
export function safeEqual(left, right) {
  if (!left || !right || left.length !== right.length) return false;
  let result = 0; for (let i = 0; i < left.length; i++) result |= left.charCodeAt(i) ^ right.charCodeAt(i);
  return result === 0;
}
function json(value, status = 200) {
  return new Response(JSON.stringify(value), {status, headers: {'Content-Type':'application/json', 'Cache-Control':'no-store'}});
}
class HttpError extends Error { constructor(status) { super('Request failed'); this.status = status; } }

async function hmac(key, value) {
  const cryptoKey = await crypto.subtle.importKey('raw', typeof key === 'string' ? encoder.encode(key) : key,
    {name:'HMAC',hash:'SHA-256'}, false, ['sign']);
  return new Uint8Array(await crypto.subtle.sign('HMAC', cryptoKey, encoder.encode(value)));
}
export async function telegramUser(initData, botToken, now = Math.floor(Date.now()/1000)) {
  if (!botToken || typeof initData !== 'string' || initData.length > 16384) throw new HttpError(401);
  const data = new URLSearchParams(initData);
  if ([...data.keys()].length !== new Set(data.keys()).size) throw new HttpError(401);
  const supplied = data.get('hash'); data.delete('hash');
  const check = [...data.entries()].sort(([a],[b]) => a < b ? -1 : a > b ? 1 : 0).map(([k,v])=>`${k}=${v}`).join('\n');
  const signature = await hmac(await hmac('WebAppData',botToken),check);
  const expected = [...signature].map(b=>b.toString(16).padStart(2,'0')).join('');
  const date = Number(data.get('auth_date'));
  if (!safeEqual(supplied,expected) || !Number.isInteger(date) || date > now+30 || now-date > 3600) throw new HttpError(401);
  let user; try { user = JSON.parse(data.get('user')); } catch { throw new HttpError(401); }
  if (!user || !Number.isSafeInteger(user.id) || user.id <= 0 || user.is_bot) throw new HttpError(401);
  return user;
}

export function createHandler(env, fetcher = fetch) {
  const base = env.SUPABASE_URL?.replace(/\/$/, '');
  const key = env.SUPABASE_SERVICE_ROLE_KEY;
  const miniappUrl = env.MINIAPP_URL;
  const catalogKeyboard = miniappUrl ? {reply_markup:{inline_keyboard:[[{text:'Открыть каталог',web_app:{url:miniappUrl}}]]}} : {};
  async function db(path, method = 'GET', body) {
    const response = await fetcher(`${base}/rest/v1/${path}`, {method, headers: {
      apikey:key, Authorization:`Bearer ${key}`, 'Content-Type':'application/json', Prefer:'return=representation',
    }, ...(body === undefined ? {} : {body:JSON.stringify(body)})});
    if (!response.ok) throw new HttpError(503);
    return response.status === 204 ? null : await response.json();
  }
  async function rate(key, limit, seconds) {
    if (!await db('rpc/qm_rate_limit','POST',{p_key:key,p_limit:limit,p_seconds:seconds})) throw new HttpError(429);
  }
  async function telegram(method, body) {
    if (!env.TELEGRAM_BOT_TOKEN) throw new HttpError(503);
    const response = await fetcher(`https://api.telegram.org/bot${env.TELEGRAM_BOT_TOKEN}/${method}`, {
      method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),
    });
    if (!response.ok || !(await response.json()).ok) throw new HttpError(503);
  }
  const send = (id, text, extra = {}) => telegram('sendMessage',{chat_id:id,text,...extra});
  async function device(request) {
    const token = request.headers.get('Authorization')?.replace(/^Bearer /,'') || '';
    if (!/^[a-f0-9]{64}$/.test(token)) throw new HttpError(401);
    const rows = await db(`qm_devices?token_hash=eq.${await hash(token)}&revoked=eq.false&select=telegram_id`);
    if (!rows.length) throw new HttpError(401);
    return rows[0].telegram_id;
  }
  async function entitled(user, language) {
    const [packs, rights] = await Promise.all([
      db(`qm_packs?active=eq.true${language ? `&language=eq.${language}` : ''}&order=id`),
      db(`qm_entitlements?telegram_id=eq.${user}&select=pack_id`),
    ]);
    const owned = new Set(rights.map(row => row.pack_id));
    return packs.filter(pack => pack.is_free || owned.has(pack.id));
  }
  async function webhook(request) {
    if (!safeEqual(request.headers.get('X-Telegram-Bot-Api-Secret-Token'),env.TELEGRAM_WEBHOOK_SECRET)) throw new HttpError(401);
    if (Number(request.headers.get('content-length') || 0)>65536) throw new HttpError(413);
    const raw = await request.text(); if (raw.length>65536) throw new HttpError(413);
    const update = JSON.parse(raw);
    const callback = update.callback_query;
    const message = callback?.message || update.message;
    const user = callback?.from || message?.from;
    // Linking is only accepted in a private conversation with the authenticated Telegram sender.
    if (!user || user.is_bot || message?.chat?.type !== 'private' || message.chat.id !== user.id) return json({ok:true});
    await rate(`bot:${user.id}`,30,60);
    await db('rpc/qm_touch_user','POST',{p_telegram_id:user.id});
    if (callback) {
      const code = String(callback.data || '').replace(/^link:/,'');
      if (!String(callback.data).startsWith('link:') || !validCode(code)) return json({ok:true});
      const approved = await db('rpc/qm_approve_link','POST',{p_code:code,p_telegram_id:user.id});
      await telegram('answerCallbackQuery',{callback_query_id:callback.id,text:approved?'Игра подключена':'Код недействителен или истёк'});
      await send(user.id,approved?'Готово! Игра связана с вашим аккаунтом. Вернитесь в игру и нажмите «Загрузить все».':'Запросите новый код в игре.');
      return json({ok:true});
    }
    const text = String(message.text || '').trim();
    if (/^\/stats(?:@DrQuizman_bot)?$/i.test(text)) {
      if (String(user.id) !== env.QUIZMAN_OWNER_TELEGRAM_ID) {
        await send(user.id,'Команда недоступна.');
      } else {
        const stats = await db('rpc/qm_user_stats','POST',{});
        await send(user.id,[
          'Статистика Dr. Quizman',
          `Всего известных пользователей: ${stats.total}`,
          `Новых за 24 часа / 7 дней: ${stats.new_day} / ${stats.new_week}`,
          `Активных за 24 часа / 7 дней: ${stats.active_day} / ${stats.active_week}`,
          '',
          `Подключили игру: ${stats.linked_users}`,
          `Подключений устройств: ${stats.devices}`,
          `Тестовых покупок: ${stats.test_orders}`,
          `Тестовых покупателей: ${stats.test_buyers}`,
          '',
          'Активность — сообщения боту и посещения мини-приложения. Ваш аккаунт тоже учитывается.',
          'Учёт посещений с 27.09.2026. Ранее известные аккаунты включены в общее число; история посещений до этой даты неполная.'
        ].join('\n'));
      }
    } else if (/^\/start(?:@DrQuizman_bot)?$/i.test(text)) {
      await send(user.id,'Dr. Quizman\n\nОткройте каталог паков кнопкой ниже. Ваш аккаунт определяется автоматически.\n\nЧтобы подключить игру: «Выбрать вопросы» → «Подключить Telegram». Отправьте код сюда и подтвердите привязку.\n\n/packs — доступные пакеты\n/catalog — каталог паков\n\nПродажи пока не включены.',catalogKeyboard);
    } else if (/^\/catalog(?:@DrQuizman_bot)?$/i.test(text)) {
      await send(user.id,'Каталог паков вопросов. Бесплатные и ваши пакеты доступны для загрузки в подключённой игре.',catalogKeyboard);
    } else if (/^\/id(?:@DrQuizman_bot)?$/i.test(text)) {
      await send(user.id,`Ваш Telegram ID: ${user.id}`);
    } else if (/^\/packs(?:@DrQuizman_bot)?$/i.test(text)) {
      const packs = await entitled(user.id);
      await send(user.id,packs.length ? packs.map(pack=>`${pack.title} · ${pack.language.toUpperCase()} · ${pack.question_count}`).join('\n') : 'Доступных пакетов пока нет. Продажи ещё не включены.',catalogKeyboard);
    } else {
      const code = normalizeCode(text.replace(/^\/link(?:@DrQuizman_bot)?\s+/i,''));
      if (!validCode(code)) { await send(user.id,'Отправьте код из игры. Команды: /packs, /id.'); return json({ok:true}); }
      const links = await db(`qm_device_links?code=eq.${code}&expires_at=gt.${encodeURIComponent(new Date().toISOString())}&telegram_id=is.null&select=code`);
      if (!links.length) await send(user.id,'Код недействителен или истёк. Запросите новый код в игре.');
      else await send(user.id,`Подключить игру с кодом ${code} к вашему аккаунту? Подтверждайте только код, который видите на своём экране.`,
        {reply_markup:{inline_keyboard:[[{text:'Подключить мою игру',callback_data:`link:${code}`}]]}});
    }
    return json({ok:true});
  }
  const handle = async request => {
    try {
      if (!base || !key) throw new HttpError(503);
      const url = new URL(request.url);
      const path = url.pathname.replace(/^\/functions\/v1\/quizman/,'').replace(/^\/quizman/,'');
      if (path === '/admin/configure-bot' && request.method === 'POST') {
        if (!env.QUIZMAN_ADMIN_TOKEN || !safeEqual(request.headers.get('Authorization'),`Bearer ${env.QUIZMAN_ADMIN_TOKEN}`)) throw new HttpError(401);
        if (!miniappUrl?.startsWith('https://')) throw new HttpError(503);
        await telegram('setChatMenuButton',{menu_button:{type:'web_app',text:'Каталог паков',web_app:{url:miniappUrl}}});
        await telegram('setMyCommands',{commands:[{command:'start',description:'Начать и подключить игру'},{command:'catalog',description:'Каталог паков вопросов'},{command:'packs',description:'Мои доступные пакеты'}]});
        return json({ok:true});
      }
      if (['/v1/miniapp/catalog','/v1/miniapp/purchase'].includes(path) && request.method === 'POST') {
        if (Number(request.headers.get('content-length')||0)>20000) throw new HttpError(413);
        const raw=await request.text(); if(raw.length>20000) throw new HttpError(413);
        let payload; try { payload=JSON.parse(raw); } catch {throw new HttpError(400);}
        const user=await telegramUser(payload?.initData,env.TELEGRAM_BOT_TOKEN);
        if(path === '/v1/miniapp/purchase') {
          if(typeof payload.packId!=='string'||!/^extra_(ru|en|de|es|he)_0[1-5]$/.test(payload.packId)) throw new HttpError(400);
          await rate(`purchase:${user.id}`,30,60);
          await db('rpc/qm_touch_user','POST',{p_telegram_id:user.id});
          const granted=await db('rpc/qm_test_purchase','POST',{p_telegram_id:user.id,p_pack_id:payload.packId});
          if(!granted) throw new HttpError(403);
          return json({ok:true,test:true,amountStars:0,packId:payload.packId});
        }
        await rate(`catalog:${user.id}`,60,60);
        await db('rpc/qm_touch_user','POST',{p_telegram_id:user.id});
        const [packs,rights]=await Promise.all([db('qm_packs?active=eq.true&order=id'),db(`qm_entitlements?telegram_id=eq.${user.id}&select=pack_id`)]);
        const owned=new Set(rights.map(row=>row.pack_id));
        return json({paymentsEnabled:false,packs:packs.map(pack=>({id:pack.id,title:pack.title,language:pack.language,
          count:pack.question_count,topics:pack.topics,status:pack.is_free?'free':owned.has(pack.id)?'owned':'paid',
          testPurchase:!!pack.test_purchase_enabled,price:pack.is_free||pack.test_purchase_enabled?0:null}))});
      }
      if (path === '/health' && request.method === 'GET') return json({ok:true,bot:'DrQuizman_bot',paymentsEnabled:false});
      if (path === '/telegram' && request.method === 'POST') return await webhook(request);
      if (path === '/v1/device-links' && request.method === 'POST') {
        const ip = request.headers.get('x-forwarded-for')?.split(',')[0]?.trim() || 'unknown';
        await rate(`create:${await hash(ip)}`,10,3600); await rate('create:global',500,3600);
        const pollToken = randomToken(); const deviceToken = await hash(`device:${pollToken}`);
        const code = randomCode(); const expires = new Date(Date.now()+600000);
        await db('qm_device_links','POST',{code,poll_hash:await hash(pollToken),device_hash:await hash(deviceToken),expires_at:expires.toISOString()});
        return json({code,pollToken,expiresAt:Math.floor(expires.getTime()/1000)});
      }
      if (path === '/v1/device-links/status' && request.method === 'GET') {
        const token = request.headers.get('Authorization')?.replace(/^Bearer /,'') || '';
        if (!/^[a-f0-9]{64}$/.test(token)) throw new HttpError(401);
        const links = await db(`qm_device_links?poll_hash=eq.${await hash(token)}&select=telegram_id,expires_at`);
        if (!links.length || new Date(links[0].expires_at).getTime() <= Date.now()) return json({status:'expired'});
        if (!links[0].telegram_id) return json({status:'pending'});
        const deviceToken=await hash(`device:${token}`);
        const devices=await db(`qm_devices?token_hash=eq.${await hash(deviceToken)}&revoked=eq.false&select=token_hash`);
        if (!devices.length) throw new HttpError(401);
        return json({status:'approved',deviceToken});
      }
      if (path === '/v1/packs' && request.method === 'GET') {
        const user = await device(request); const language = url.searchParams.get('language');
        if (!['ru','en','de','es','he'].includes(language)) throw new HttpError(400);
        const packs = await entitled(user,language);
        return json({packs:packs.map(pack=>({id:pack.id,title:pack.title,language:pack.language,count:pack.question_count,topics:pack.topic_count,sha256:pack.sha256}))});
      }
      const match = path.match(/^\/v1\/packs\/([a-zA-Z0-9_-]{1,100})\/download$/);
      if (match && request.method === 'GET') {
        const user=await device(request);
        const pack=(await entitled(user)).find(pack=>pack.id===match[1]);
        if (!pack) throw new HttpError(403);
        await rate(`download:${user}`,100,3600);
        const objectPath=pack.storage_path.split('/').map(encodeURIComponent).join('/');
        const response=await fetcher(`${base}/storage/v1/object/authenticated/question-packs/${objectPath}`,{headers:{apikey:key,Authorization:`Bearer ${key}`}});
        if (!response.ok) throw new HttpError(503);
        return new Response(response.body,{headers:{'Content-Type':'application/json','Cache-Control':'no-store','Content-Disposition':`attachment; filename="${pack.id}.json"`}});
      }
      return json({error:'Not found'},404);
    } catch(error) {
      return json({error:'Request failed'},error instanceof HttpError ? error.status : 500);
    }
  };
  return async request => {
    const path=new URL(request.url).pathname;
    if(!/\/v1\/miniapp\/(catalog|purchase)$/.test(path)) return handle(request);
    const origin=request.headers.get('Origin');
    const allowed=miniappUrl ? new URL(miniappUrl).origin : null;
    if(origin && origin!==allowed) return json({error:'Origin denied'},403);
    const response=request.method==='OPTIONS'?new Response(null,{status:204}):await handle(request);
    if(origin===allowed && origin) {
      response.headers.set('Access-Control-Allow-Origin',origin);
      response.headers.set('Access-Control-Allow-Methods','POST, OPTIONS');
      response.headers.set('Access-Control-Allow-Headers','Content-Type');
      response.headers.set('Vary','Origin');
    }
    return response;
  };
}
if (typeof Deno !== 'undefined') Deno.serve(createHandler(Deno.env.toObject()));
