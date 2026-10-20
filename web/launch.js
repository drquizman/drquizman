'use strict';
(()=>{
  const origin='https://quizman-web-prototype.onrender.com';
  const preparing=document.getElementById('preparing'),studio=document.getElementById('studio'),status=document.getElementById('status'),retry=document.getElementById('retry'),spinner=document.getElementById('spinner');
  let generation=0,request,frameTimer;
  function failed(message){spinner.hidden=true;status.textContent=message;retry.hidden=false;}
  function openStudio(own){
    status.textContent='Сервер готов. Открываем студию…';
    studio.onload=()=>{if(own!==generation)return;clearTimeout(frameTimer);studio.hidden=false;preparing.hidden=true;document.getElementById('frame-help').hidden=false;};
    frameTimer=setTimeout(()=>{if(own===generation)failed('Сервер готов, но студия долго загружается. Попробуйте снова или откройте игру отдельно.');},30000);
    studio.src=origin+'/';
  }
  async function prepare(){
    const own=++generation;request?.abort();clearTimeout(frameTimer);studio.onload=null;studio.hidden=true;studio.removeAttribute('src');preparing.hidden=false;document.getElementById('frame-help').hidden=true;
    retry.hidden=true;spinner.hidden=false;status.textContent='Пробуждаем сервер игры. Обычно это занимает около минуты, если он отдыхал.';
    const deadline=Date.now()+150000;
    while(own===generation&&Date.now()<deadline){
      request=new AbortController();const timeout=setTimeout(()=>request.abort(),12000);
      try{
        const response=await fetch(origin+'/health',{mode:'cors',cache:'no-store',credentials:'omit',signal:request.signal});
        if(response.ok&&(await response.text()).trim()==='ok'){if(own===generation)openStudio(own);return;}
      }catch{/* Aborted requests still reach Render and can start a sleeping service. */}
      finally{clearTimeout(timeout);}
      if(own!==generation)return;
      status.textContent='Студия ещё запускается. Это может занять чуть больше минуты…';
      await new Promise(resolve=>setTimeout(resolve,3000));
    }
    if(own===generation)failed('Не удалось дождаться сервера. Проверьте интернет и попробуйте снова или откройте игру отдельно.');
  }
  retry.addEventListener('click',prepare);
  addEventListener('pagehide',()=>{generation++;request?.abort();clearTimeout(frameTimer);});
  addEventListener('pageshow',event=>{if(event.persisted&&studio.hidden)prepare();});
  prepare();
})();
