const origins = new Set(['https://www.drquizman.ru', 'https://drquizman.ru', 'http://www.drquizman.ru', 'http://drquizman.ru', 'https://drquizman.github.io']);
const encoder = new TextEncoder();
export function createHandler(env, fetcher = fetch) {
  const key = env.SUPABASE_SERVICE_ROLE_KEY;
  const base = env.SUPABASE_URL;
  let refreshing;
  async function db(path, method = 'GET', body) {
    const result = await fetcher(`${base}/rest/v1/${path}`, {method,
      headers: {apikey:key, Authorization:`Bearer ${key}`, 'Content-Type':'application/json'},
      ...(body === undefined ? {} : {body:JSON.stringify(body)}), signal:AbortSignal.timeout(8000)});
    if (!result.ok) throw new Error('Database unavailable');
    return result.status === 204 ? null : result.json();
  }
  async function hash(value) {
    const hmacKey = await crypto.subtle.importKey('raw', encoder.encode(key), {name:'HMAC',hash:'SHA-256'}, false, ['sign']);
    const bytes = await crypto.subtle.sign('HMAC', hmacKey, encoder.encode(value));
    return Array.from(new Uint8Array(bytes), b=>b.toString(16).padStart(2,'0')).join('');
  }
  async function refreshDownloads() {
    const assets = [];
    for (let page=1; page<=10; page++) {
      const response = await fetcher(`https://api.github.com/repos/drquizman/drquizman/releases?per_page=100&page=${page}`, {
        headers:{Accept:'application/vnd.github+json','User-Agent':'DrQuizman-Website-Stats'}, signal:AbortSignal.timeout(8000)});
      if (!response.ok) throw new Error('GitHub unavailable');
      const releases = await response.json();
      if (!Array.isArray(releases)) throw new Error('Invalid releases');
      for (const release of releases) {
        if (release.draft) continue;
        for (const asset of release.assets || []) {
          if (!/\.apk$/i.test(asset.name)) continue;
          if (!Number.isSafeInteger(asset.id) || asset.id <= 0 || !Number.isSafeInteger(asset.download_count) || asset.download_count < 0) throw new Error('Invalid asset');
          assets.push({asset_id:String(asset.id), downloads:asset.download_count});
        }
      }
      if (!(response.headers.get('link') || '').includes('rel="next"')) {
        return await db('rpc/qm_website_sync_downloads', 'POST', {p_assets:assets});
      }
    }
    throw new Error('Too many release pages');
  }
  return async request => {
    const origin = request.headers.get('Origin');
    const headers = {'Content-Type':'application/json', 'Cache-Control':'no-store', Vary:'Origin'};
    if (origins.has(origin)) Object.assign(headers, {'Access-Control-Allow-Origin':origin,
      'Access-Control-Allow-Methods':'GET, POST, OPTIONS', 'Access-Control-Allow-Headers':'Content-Type'});
    const json = (value,status=200)=>new Response(JSON.stringify(value),{status,headers});
    if (origin && !origins.has(origin)) return json({error:'Origin not allowed'},403);
    if (request.method === 'OPTIONS') return new Response(null,{status:204,headers});
    if (!['GET','POST'].includes(request.method)) return json({error:'Method not allowed'},405);
    try {
      if (request.method === 'POST') {
        if (!origins.has(origin)) return json({error:'Origin required'},403);
        if (Number(request.headers.get('Content-Length'))>256) return json({error:'Too large'},413);
        const text = await request.text();
        if (text.length>256) return json({error:'Too large'},413);
        let body; try { body=JSON.parse(text); } catch { return json({error:'Invalid JSON'},400); }
        if (!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(body?.session || '')) return json({error:'Invalid session'},400);
        // A daily keyed hash limits simple counter inflation without retaining IP addresses.
        const ip = request.headers.get('x-forwarded-for')?.split(',')[0].trim() || 'unknown';
        const day = new Date().toISOString().slice(0,10);
        await db('rpc/qm_website_visit','POST',{p_session:await hash(`session:${body.session}`),p_client:await hash(`client:${day}:${ip}`)});
      }
      const [stats] = await db('qm_website_stats?id=eq.true&select=visits,downloads,downloads_checked_at,started_at');
      if (!stats) throw new Error('Statistics unavailable');
      if (!stats.downloads_checked_at || Date.now()-Date.parse(stats.downloads_checked_at)>900000) {
        try {
          refreshing ||= refreshDownloads().finally(()=>{refreshing=undefined;});
          Object.assign(stats, await refreshing);
        } catch { /* Preserve the last verified total during an upstream outage. */ }
      }
      return json(stats);
    } catch { return json({error:'Statistics temporarily unavailable'},503); }
  };
}
if (typeof Deno !== 'undefined') Deno.serve(createHandler(Deno.env.toObject()));
