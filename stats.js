(() => {
  const endpoint = 'https://xoerzqgvcpjrbuwtbtdo.supabase.co/functions/v1/website-stats';
  const formatter = new Intl.NumberFormat('ru-RU');
  const render = (id, value) => {
    const element = document.getElementById(id);
    if (Number.isSafeInteger(value) && value >= 0) element.textContent = formatter.format(value);
    else { element.textContent = '—'; element.title = 'Статистика временно недоступна'; }
  };
  async function load() {
    let session;
    try {
      session = localStorage.getItem('quizman-visit-session');
      if (!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(session || '')) {
        session = crypto.randomUUID();
        localStorage.setItem('quizman-visit-session', session);
      }
    } catch { /* Reading totals still works when browser storage is blocked. */ }
    // Local previews never register production visits.
    const production = ['www.drquizman.ru', 'drquizman.ru', 'drquizman.github.io'].includes(location.hostname);
    const options = {signal:AbortSignal.timeout(20000)};
    if (production && session) Object.assign(options, {method:'POST', headers:{'Content-Type':'application/json'},body:JSON.stringify({session})});
    try {
      let response = await fetch(endpoint, options);
      if (!response.ok && options.method === 'POST') response = await fetch(endpoint,{signal:AbortSignal.timeout(15000)});
      if (!response.ok) throw new Error('Statistics unavailable');
      const stats = await response.json();
      render('visit-count', stats.visits);
      render('download-count', stats.downloads);
      const started = new Date(stats.started_at);
      if (!Number.isNaN(started.getTime())) document.getElementById('visits-stat').title = `Посещения с ${started.toLocaleDateString('ru-RU')}. Повторное открытие в течение 30 минут не добавляет посещение.`;
    } catch {
      render('visit-count', null);
      render('download-count', null);
    }
  }
  load();
})();
