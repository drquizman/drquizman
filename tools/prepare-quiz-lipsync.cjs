const fs=require('fs'),path=require('path'),{execFileSync}=require('child_process');
const root=path.resolve(__dirname,'../assets/host-3d-v1/archive-doctor/audio');
execFileSync('ffmpeg',['-hide_banner','-loglevel','error','-y','-i',path.join(root,'quiz-red-planet.wav'),'-f','s16le','-ac','1','-ar','16000',path.join(root,'speech.pcm')]);
const pcm=fs.readFileSync(path.join(root,'speech.pcm')),levels=[];
for(let i=0;i<pcm.length/2;i+=320){let sum=0,n=0;for(let j=i;j<Math.min(i+320,pcm.length/2);j++){const x=pcm.readInt16LE(j*2)/32768;sum+=x*x;n++;}levels.push(Math.sqrt(sum/n));}
const peak=Math.max(...levels)||1;
fs.writeFileSync(path.join(root,'lipsync.js'),'window.QUIZ_LIPSYNC='+JSON.stringify({fps:50,levels:levels.map(v=>+Math.max(0,Math.min(1,(v/peak-.025)*2.5)).toFixed(4))})+';');
