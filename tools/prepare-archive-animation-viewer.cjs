const fs=require('fs'),path=require('path');require('./prepare-archive-viewer.cjs');const out=path.resolve(__dirname,'../assets/host-3d-v1/archive-doctor');let html=fs.readFileSync(path.join(out,'index.html'),'utf8');
function sub(a,b){if(!html.includes(a))throw Error('Missing animation insertion '+a.slice(0,90));html=html.replace(a,b);}
sub('href="doctor-original.fbx"','href="doctor-animated.glb"');sub('Исходный FBX ↓','GLB с анимациями ↓');
sub('<script src="model-data.js"></script>','<script src="model-data.js"></script><script src="animation-runtime.js"></script><script src="animations.js"></script>');
sub('<div class="controls">',`<div class="controls"><div class="group"><select id="clip" aria-label="Анимация"><option value="">Исходная поза</option><option value="wait" selected>Ожидание</option><option value="hi">Приветствие</option><option value="count">Счёт</option><option value="dance">Танец</option><option value="laughing">Смех</option><option value="talking">Разговор 1</option><option value="talking2">Разговор 2</option><option value="talking3">Разговор 3</option><option value="arguing">Спор</option></select><button id="play" aria-pressed="true">Пауза</button><button id="repeat" aria-pressed="true">Повтор</button></div><input id="timeline" aria-label="Кадр анимации" type="range" min="0" max="1000" value="0"><output id="clip-time">0.0 с</output></div><div class="controls">`);
sub('  </style>','select {font:inherit;font-size:12px;border:0;background:transparent;padding:8px;color:#494239;max-width:150px} #timeline {width:130px;accent-color:#247d7d} #clip-time {font-size:11px;color:#80786e;min-width:60px} footer{gap:6px;padding-bottom:12px}\n  </style>');
sub('return {skinTint:','return {source:m,posedP:new Float32Array(m.p),posedN:new Float32Array(m.n),skinTint:');
sub('      if(shadowEnabled){','      function drawAnimatedShadow(){if(shadowEnabled){gl.bindFramebuffer(gl.FRAMEBUFFER,framebuffer);');
sub('      gl.bindFramebuffer(gl.FRAMEBUFFER,null);','      gl.bindFramebuffer(gl.FRAMEBUFFER,null);}');
sub('        if(!dirty&&!animation)return;dirty=false;','        if(!dirty&&!animation&&!playing)return;dirty=false;updateCharacter(now);drawAnimatedShadow();');
sub('        if(animation){dirty=true;requestRender();}','        if(animation||playing){dirty=true;requestRender();}');
sub('      new ResizeObserver(requestRender).observe(canvas);',`
      const A=window.ArchiveAnimation,rig=A.skeleton(window.ARCHIVE_BONES),clips=window.ARCHIVE_CLIPS,byId=id=>document.getElementById(id);let selectedClip=clips[0],playing=true,repeat=true,clipTime=0,lastTime=0;
      function updateCharacter(now){if(playing&&selectedClip){if(lastTime)clipTime+=(now-lastTime)/1000;if(clipTime>selectedClip.duration){if(repeat)clipTime%=selectedClip.duration;else{clipTime=selectedClip.duration;playing=false;byId('play').textContent='Пуск';byId('play').setAttribute('aria-pressed','false');}}}lastTime=now;const pose=A.pose(rig,selectedClip,clipTime);for(let i=0;i<rig.length;i++)window.ARCHIVE_BONES[i].p=pose.world[i].slice(12,15);for(const m of meshes){A.deform(m.source,pose.skin,m.posedP,m.posedN);gl.bindBuffer(gl.ARRAY_BUFFER,m.p);gl.bufferSubData(gl.ARRAY_BUFFER,0,m.posedP);gl.bindBuffer(gl.ARRAY_BUFFER,m.n);gl.bufferSubData(gl.ARRAY_BUFFER,0,m.posedN);}byId('timeline').value=selectedClip?clipTime/selectedClip.duration*1000:0;byId('clip-time').textContent=clipTime.toFixed(1)+' / '+(selectedClip?selectedClip.duration.toFixed(1):'0.0')+' с';}
      byId('clip').onchange=()=>{selectedClip=clips.find(c=>c.name===byId('clip').value)||null;clipTime=0;lastTime=0;playing=!!selectedClip;byId('play').textContent=playing?'Пауза':'Пуск';byId('play').setAttribute('aria-pressed',String(playing));requestRender();};
      byId('play').onclick=()=>{if(!selectedClip)return;playing=!playing;lastTime=0;byId('play').textContent=playing?'Пауза':'Пуск';byId('play').setAttribute('aria-pressed',String(playing));requestRender();};
      byId('repeat').onclick=()=>{repeat=!repeat;byId('repeat').setAttribute('aria-pressed',String(repeat));};
      byId('timeline').oninput=()=>{playing=false;clipTime=selectedClip?Number(byId('timeline').value)/1000*selectedClip.duration:0;byId('play').textContent='Пуск';byId('play').setAttribute('aria-pressed','false');requestRender();};
      new ResizeObserver(requestRender).observe(canvas);`);
sub('<script src="animations.js"></script>','<script src="animations.js"></script><script src="audio/lipsync.js"></script>');
sub('<div class="controls">','<div class="controls"><button id="speech-play">Озвучить вопрос</button><input id="speech-time" type="range" min="0" max="18.4" step="0.02" value="0" aria-label="Время озвучки"><output id="speech-status">0.0 / 18.4 с</output><span style="font-size:12px">Вопрос о Красной планете · липсинк</span></div><div class="controls">');
sub('!dirty&&!animation&&!playing','!dirty&&!animation&&!playing&&voice.paused');
sub('if(animation||playing)','if(animation||playing||!voice.paused)');
sub('      function updateCharacter(now)',`      const voice=new EventTarget();let audioCtx=null,audioBuffer=null,audioSource=null,audioOffset=0,audioStart=0,audioPlaying=false;
      Object.defineProperties(voice,{currentTime:{get:()=>audioPlaying?Math.min(audioBuffer.duration,audioOffset+audioCtx.currentTime-audioStart):audioOffset},paused:{get:()=>!audioPlaying},ended:{get:()=>audioBuffer&&audioOffset>=audioBuffer.duration}});
      function stopSpeech(){audioOffset=voice.currentTime;audioPlaying=false;if(audioSource){audioSource.onended=null;audioSource.stop();audioSource=null;}byId('speech-play').textContent='Продолжить вопрос';voice.dispatchEvent(new Event('pause'));}
      async function startSpeech(){try{if(!audioCtx)audioCtx=new AudioContext();await audioCtx.resume();if(!audioBuffer){const response=await fetch('audio/speech.pcm');if(!response.ok)throw Error('audio');const pcm=new DataView(await response.arrayBuffer());audioBuffer=audioCtx.createBuffer(1,pcm.byteLength/2,16000);const channel=audioBuffer.getChannelData(0);for(let i=0;i<channel.length;i++)channel[i]=pcm.getInt16(i*2,true)/32768;byId('speech-time').max=audioBuffer.duration;}if(audioOffset>=audioBuffer.duration)audioOffset=0;audioSource=audioCtx.createBufferSource();audioSource.buffer=audioBuffer;audioSource.connect(audioCtx.destination);audioStart=audioCtx.currentTime;audioPlaying=true;audioSource.onended=()=>{audioOffset=audioBuffer.duration;audioPlaying=false;byId('speech-play').textContent='Повторить вопрос';voice.dispatchEvent(new Event('ended'));};audioSource.start(0,audioOffset);byId('speech-play').textContent='Пауза озвучки';voice.dispatchEvent(new Event('play'));}catch(error){byId('speech-status').textContent='Не удалось включить звук';}}
      byId('speech-play').onclick=()=>audioPlaying?stopSpeech():startSpeech();
      byId('speech-time').oninput=()=>{if(audioPlaying)stopSpeech();audioOffset=Number(byId('speech-time').value);voice.dispatchEvent(new Event('seeked'));};
      const mouthMesh=meshes.find(m=>/Mouth/.test(m.source.name)),eyeMesh=meshes.find(m=>/Eyes/.test(m.source.name)),lipData=window.QUIZ_LIPSYNC;
      const baseUV=mouthMesh.source.uv.slice(),mouthUV=new Float32Array(baseUV.length);
      const lipSource={...mouthMesh.source,p:mouthMesh.source.p.slice()},baseMouthP=mouthMesh.source.p;
      const blinkSource=eyeMesh?{...eyeMesh.source,p:eyeMesh.source.p.slice()}:null,baseEyeP=eyeMesh?.source.p;
      const eyeCenter=eyeMesh?[0,1,2].map(k=>{let a=[];for(let i=k;i<baseEyeP.length;i+=3)a.push(baseEyeP[i]);return (Math.min(...a)+Math.max(...a))/2;}):[0,0,0];
      const mx=(Math.min(...baseMouthP.filter((_,i)=>i%3===0))+Math.max(...baseMouthP.filter((_,i)=>i%3===0)))/2,my=(Math.min(...baseMouthP.filter((_,i)=>i%3===1))+Math.max(...baseMouthP.filter((_,i)=>i%3===1)))/2;
      let lipLevel=0,lipClock=0;
      function updateLips(now){
       if(blinkSource){const t=now/1000,cycle=(t+0.17)%4.8,close=Math.max(0,1-Math.abs(cycle-3.35)/.12),close2=Math.max(0,1-Math.abs(cycle-3.39)/.12);for(let i=0;i<baseEyeP.length;i+=3){const side=baseEyeP[i]<0?close:close2,scale=1-.96*side;blinkSource.p[i]=baseEyeP[i];blinkSource.p[i+1]=eyeCenter[1]+(baseEyeP[i+1]-eyeCenter[1])*scale;blinkSource.p[i+2]=baseEyeP[i+2];}}
       byId('speech-time').value=voice.currentTime;byId('speech-status').textContent=voice.currentTime.toFixed(1)+' / 18.4 с';const index=Math.floor(voice.currentTime*lipData.fps),active=!voice.ended&&(!voice.paused||voice.currentTime>0);
       const desired=active?(lipData.levels[index]||0):0;
       const dt=Math.min(.1,Math.max(.001,(now-lipClock)/1000));lipClock=now;
       if(voice.paused)lipLevel=desired;else lipLevel+=(desired-lipLevel)*(1-Math.exp(-dt*22));
       // Keep one coherent smile silhouette; change opening continuously.
       // UV stays in the original open-smile tile, avoiding unrelated atlas cells.
       for(let i=0;i<baseUV.length;i+=2){mouthUV[i]=baseUV[i];mouthUV[i+1]=.6476605534553528+(baseUV[i+1]-.56);}
       const mouthHeight=.12+.72*Math.min(1,lipLevel),anchor=my+.11;
       for(let i=0;i<baseMouthP.length;i+=3){lipSource.p[i]=baseMouthP[i];lipSource.p[i+1]=active?anchor+(baseMouthP[i+1]-anchor)*mouthHeight:baseMouthP[i+1];lipSource.p[i+2]=baseMouthP[i+2]+(active?.045:0);}
       gl.bindBuffer(gl.ARRAY_BUFFER,mouthMesh.uv);gl.bufferSubData(gl.ARRAY_BUFFER,0,active?mouthUV:new Float32Array(baseUV));
      }
      for(const event of ['play','pause','seeked','ended','loadeddata'])voice.addEventListener(event,()=>{lipClock=0;requestRender();});
      function updateCharacter(now)`);
sub('const pose=A.pose(rig,selectedClip,clipTime);','updateLips(now);const pose=A.pose(rig,selectedClip,clipTime);');
sub('A.deform(m.source,pose.skin','A.deform(m===mouthMesh?lipSource:m===eyeMesh?blinkSource:m.source,pose.skin');
sub('</select>',require('./doctor-gesture-clips.cjs').map(([id,label])=>'<option value="'+id+'">'+label+'</option>').join('')+'</select>');
fs.writeFileSync(path.join(out,'index.html'),html);
