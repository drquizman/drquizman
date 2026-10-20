const fs=require('fs'),path=require('path'),vm=require('vm'),{parse}=require('./inspect-doctor-fbx.cjs'),M=require('../assets/host-3d-v1/archive-doctor/animation-runtime.js');
const out=path.resolve(__dirname,'../assets/host-3d-v1/archive-doctor'),ctx={window:{}};vm.runInNewContext(fs.readFileSync(path.join(out,'model-data.js'),'utf8'),ctx);const target=M.skeleton(ctx.window.ARCHIVE_BONES),nameIndex=new Map(target.map((b,i)=>[b.name,i])),info=JSON.parse(fs.readFileSync(path.join(out,'source-info.json'),'utf8'));
const map={pelvis:'Hips',spine_01:'Spine',spine_02:'Spine1',spine_03:'Spine2',neck_01:'Neck',head:'Head'};
for(let side of ['l','r']){const prefix=side==='l'?'Left':'Right';for(let [a,b] of Object.entries({clavicle:'Shoulder',upperarm:'Arm',lowerarm:'ForeArm',hand:'Hand',thigh:'UpLeg',calf:'Leg',foot:'Foot',ball:'ToeBase'}))map[a+'_'+side]=prefix+b;for(let [a,b] of Object.entries({thumb:'Thumb',index:'Index',middle:'Middle',ring:'Ring',pinky:'Pinky'}))for(let k=1;k<=3;k++)map[a+'_0'+k+'_'+side]=prefix+'Hand'+b+k;}
const child=(n,key)=>n.children.find(n=>n.name===key),val=(n,key)=>child(n,key)?.props[0],props=n=>Object.fromEntries((child(n,'Properties70')?.children||[]).map(p=>[p.props[0],p.props.slice(4)])),clean=n=>n.props[1].split('\0')[0].split(':').at(-1);
function load(file){let d=parse(file),objects=d.nodes.find(n=>n.name==='Objects').children,connections=d.nodes.find(n=>n.name==='Connections').children.map(n=>n.props),byId=new Map(objects.map(n=>[n.props[0],n])),models=objects.filter(n=>n.name==='Model'),index=new Map(models.map((n,i)=>[n.props[0],i])),bones=models.map(n=>({name:clean(n),p:props(n),id:n.props[0],parent:index.get(connections.find(c=>c[0]==='OO'&&c[1]===n.props[0]&&index.has(c[2]))?.[2])??-1,tracks:{}}));
 for(let cn of objects.filter(n=>n.name==='AnimationCurveNode')){const link=connections.find(c=>c[0]==='OP'&&c[1]===cn.props[0]&&index.has(c[2]));if(!link)continue;const track={};for(let c of connections.filter(c=>c[0]==='OP'&&c[2]===cn.props[0]&&byId.get(c[1])?.name==='AnimationCurve')){let curve=byId.get(c[1]);track[c[3].slice(-1)]={t:val(curve,'KeyTime').map(v=>v/46186158000),v:val(curve,'KeyValueFloat')};}bones[index.get(link[2])].tracks[link[3]]=track;}
 const stack=objects.find(n=>n.name==='AnimationStack'),sp=props(stack),duration=(sp.LocalStop?.[0]||0)/46186158000;
 function sampleCurve(c,time,fallback){if(!c)return fallback;let lo=0,hi=c.t.length-1;if(time<=c.t[lo])return c.v[lo];if(time>=c.t[hi])return c.v[hi];while(hi-lo>1){let m=(lo+hi)>>1;if(c.t[m]<=time)lo=m;else hi=m;}return c.v[lo]+(c.v[hi]-c.v[lo])*(time-c.t[lo])/(c.t[hi]-c.t[lo]);}
 function sample(time,rest=false){let result=[];function visit(i){if(result[i])return;let b=bones[i],p=b.p,track=b.tracks,rot=(p['Lcl Rotation']||[0,0,0]).slice(),pos=(p['Lcl Translation']||[0,0,0]).slice();if(!rest)for(let k=0;k<3;k++){let a='XYZ'[k];rot[k]=sampleCurve(track['Lcl Rotation']?.[a],time,rot[k]);pos[k]=sampleCurve(track['Lcl Translation']?.[a],time,pos[k]);}let q=M.qm(M.qm(M.qe(p.PreRotation||[0,0,0]),M.qe(rot,['XYZ','XZY','YZX','YXZ','ZXY','ZYX'][p.RotationOrder?.[0]||0])),M.qi(M.qe(p.PostRotation||[0,0,0])));if(b.parent>=0){visit(b.parent);pos=M.add(result[b.parent].p,M.qr(result[b.parent].q,pos));q=M.qm(result[b.parent].q,q);}result[i]={p:pos,q};}bones.forEach((_,i)=>visit(i));return result;}
 return{bones,duration,sample,byName:new Map(bones.map((b,i)=>[b.name,i]))};
}
const clips=[];
for(let [file,label,download] of [['wait','Ожидание'],['hi','Приветствие'],['count','Счёт'],['dance','Танец'],['laughing','Смех'],['talking','Разговор 1','Talking.fbx'],['talking2','Разговор 2','Talking (1).fbx'],['talking3','Разговор 3','Talking (2).fbx'],['arguing','Спор','Standing Arguing.fbx'],...require('./doctor-gesture-clips.cjs')]){
 const source=load(download?'C:/Users/akhin/Downloads/'+download:'C:/Users/akhin/Documents/Quizman/public/models/Host/'+file+'.fbx'),rest=source.sample(0,true),match=target.map(b=>source.byName.get(map[b.name])),align=target.map((b,i)=>{if(match[i]===undefined)return[0,0,0,1];let child=target.findIndex((c,j)=>c.parentIndex===i&&match[j]!==undefined);if(b.name.startsWith('hand_'))child=nameIndex.get('middle_01_'+b.name.slice(-1));let parent=b.parentIndex;let tv,sv;if(child>=0){tv=M.sub(target[child].p,b.p);sv=M.sub(rest[match[child]].p,rest[match[i]].p);}else if(parent>=0&&match[parent]!==undefined){tv=M.sub(b.p,target[parent].p);sv=M.sub(rest[match[i]].p,rest[match[parent]].p);}else return[0,0,0,1];return M.between(tv,sv);});
 const hipIndex=nameIndex.get('pelvis'),sourceHip=match[hipIndex],sourceHipRest=rest[sourceHip].p,targetHip=target[hipIndex],translationScale=(targetHip.p[1]-target[nameIndex.get('foot_l')].p[1])/(sourceHipRest[1]-rest[source.byName.get('LeftFoot')].p[1]),fps=30,speed=file==='hi'?3:1,duration=source.duration*speed,frames=[];
 for(let f=0;f<=Math.round(duration*fps);f++){
  const sampled=source.sample(f/fps/speed),worldQ=[],locals=[],hipWorld=M.add(targetHip.p,M.sub(sampled[sourceHip].p,sourceHipRest).map(v=>v*translationScale));
  function visit(i){if(worldQ[i])return;let b=target[i];if(b.parentIndex>=0)visit(b.parentIndex);if(match[i]!==undefined){let delta=M.qm(sampled[match[i]].q,M.qi(rest[match[i]].q));worldQ[i]=M.qm(M.qm(delta,align[i]),b.q);}else worldQ[i]=b.parentIndex<0?b.q:M.qm(worldQ[b.parentIndex],b.localQ);locals[i]=b.parentIndex<0?worldQ[i]:M.qm(M.qi(worldQ[b.parentIndex]),worldQ[i]);}
  target.forEach((_,i)=>visit(i));
  // Anatomical clearance in the pelvis yaw frame; keep bone lengths and finger gestures.
  const dot=(a,b)=>a.reduce((s,v,k)=>s+v*b[k],0);
  const hipDelta=M.qm(worldQ[hipIndex],M.qi(targetHip.q));
  const axis=M.qr(hipDelta,[1,0,0]);axis[1]=0;const lateral=M.unit(axis);
  function positions(){let p=[];function walk(i){if(p[i])return p[i];let b=target[i];p[i]=i===hipIndex?hipWorld:b.parentIndex<0?b.p:M.add(walk(b.parentIndex),M.qr(worldQ[b.parentIndex],b.localP));return p[i];}target.forEach((_,i)=>walk(i));return p;}
  function rotateBranch(i,q){worldQ[i]=M.qm(q,worldQ[i]);target.forEach((b,j)=>{if(b.parentIndex===i)rotateBranch(j,q);});}
  function spread(name,childName,side,minimum,onlyDown=false){let i=nameIndex.get(name),j=nameIndex.get(childName),p=positions(),v=M.unit(M.sub(p[j],p[i]));let blend=onlyDown?Math.max(0,Math.min(1,(-v[1]-.05)/.3)):1;blend=blend*blend*(3-2*blend);if(!blend)return;let a=lateral.map(x=>x*side),d=dot(v,a),desired=.5*(d+minimum+Math.sqrt((d-minimum)**2+.025**2));desired=Math.min(.85,desired);if(desired<=d)return;let tangent=M.unit(v.map((x,k)=>x-a[k]*d)),newV=tangent.map((x,k)=>x*Math.sqrt(1-desired*desired)+a[k]*desired);rotateBranch(i,M.qs([0,0,0,1],M.between(v,newV),blend));}
  for(let side of ['l','r']){
   let sign=side==='l'?1:-1,foot=nameIndex.get('foot_'+side),footQ=worldQ[foot].slice();
   spread('thigh_'+side,'calf_'+side,sign,.15);
   spread('calf_'+side,'foot_'+side,sign,.12);
   if(file==='dance'){
    // Keep the knee in the thigh's sagittal plane. Independent spreading
    // of the two segments can otherwise create a sideways knee bend.
    const p=positions(),thigh=nameIndex.get('thigh_'+side),calf=nameIndex.get('calf_'+side);
    const forward=[-lateral[2],0,lateral[0]],upper=M.unit(M.sub(p[calf],p[thigh])),lower=M.unit(M.sub(p[foot],p[calf]));
    const upperAngle=Math.atan2(dot(upper,forward),-upper[1]),lowerAngle=Math.atan2(dot(lower,forward),-lower[1]);
    const raw=upperAngle-lowerAngle,flex=Math.max(0,Math.min(1.5,raw));
    const angle=upperAngle-flex,spreadAmount=dot(upper,lateral),sagittal=Math.sqrt(1-spreadAmount*spreadAmount);
    const desired=forward.map((v,k)=>v*Math.sin(angle)*sagittal+lateral[k]*spreadAmount+(k===1?-Math.cos(angle)*sagittal:0));
    rotateBranch(calf,M.between(lower,desired));
   }
   // Rebuild segment orientation from the doctor's bind pose, not Mixamo's
   // axial twist. Matching joint positions alone leaves the kneecaps turned in.
   const yaw=M.between([1,0,0],lateral);
   for(const [joint,end] of [['thigh_','calf_'],['calf_','foot_']]){
    const i=nameIndex.get(joint+side),j=nameIndex.get(end+side),p=positions();
    const direction=M.sub(p[j],p[i]),bindDirection=M.qr(yaw,M.sub(target[j].p,target[i].p));
    worldQ[i]=M.qm(M.qm(M.between(bindDirection,direction),yaw),target[i].q);
   }
   // Shoes follow the character heading with their original slight turnout.
   // Keep source heel/toe pitch, but discard inherited inward yaw and roll.
   const toe=nameIndex.get('ball_'+side),sourceToe=M.qr(footQ,target[toe].localP);
   const bindToe=M.qr(target[foot].q,target[toe].localP);
   const pitch=Math.asin(Math.max(-1,Math.min(1,M.unit(sourceToe)[1])))-Math.asin(M.unit(bindToe)[1]);
   const footRotation=M.qm(M.qm(yaw,M.qe([-pitch*180/Math.PI,0,0])),target[foot].q);
   rotateBranch(foot,M.qm(footRotation,M.qi(worldQ[foot])));
   if(file==='dance'){
    // The toe joint must share the corrected shoe frame. Keeping its source
    // rotation twists the forefoot independently of the corrected ankle.
    const toeRotation=M.qm(worldQ[foot],target[toe].localQ);
    rotateBranch(toe,M.qm(toeRotation,M.qi(worldQ[toe])));
   }
   if(file==='laughing')continue;
   spread('upperarm_'+side,'lowerarm_'+side,sign,.48,true);
   spread('lowerarm_'+side,'hand_'+side,sign,.23,true);
  }
  if(file==='wait'||file==='hi')rotateBranch(nameIndex.get('head'),M.qe([file==='wait'?24:18,0,0]));
  target.forEach((b,i)=>locals[i]=b.parentIndex<0?worldQ[i]:M.qm(M.qi(worldQ[b.parentIndex]),worldQ[i]));
  let parent=targetHip.parentIndex,hip=parent<0?hipWorld:M.qr(M.qi(worldQ[parent]),M.sub(hipWorld,target[parent].p));frames.push({q:locals.flat().map(v=>+v.toFixed(8)),hip:hip.map(v=>+v.toFixed(8))});
 }
 const clip={name:file,label,duration,fps,frames,mappedBones:match.filter(v=>v!==undefined).length};
 // Keep the retargeted shoe soles above the floor with one constant correction
 // per clip. A constant preserves the source's vertical motion and jumps.
 let floor=0;const soleMeshes=ctx.window.MODEL.map(m=>{let p=[],joints=[],weights=[],n=[];for(let i=0;i<m.p.length/3;i++)if(m.p[i*3+1]<.25){p.push(...m.p.slice(i*3,i*3+3));n.push(...m.n.slice(i*3,i*3+3));joints.push(...m.joints.slice(i*4,i*4+4));weights.push(...m.weights.slice(i*4,i*4+4));}return{p,n,joints,weights};}).filter(m=>m.p.length);
 for(let f=0;f<frames.length;f++){const pose=M.pose(target,clip,f/fps);for(const m of soleMeshes){let p=M.deform(m,pose.skin).p;for(let i=1;i<p.length;i+=3)floor=Math.min(floor,p[i]);}}
 const lift=M.qr(M.qi(target[targetHip.parentIndex].q),[0,-floor+.003,0]);for(let frame of frames)frame.hip=frame.hip.map((v,k)=>v+lift[k]);clip.floorCorrection=-floor+.003;
 clips.push(clip);console.log(file,duration,'seconds;',clip.mappedBones,'mapped bones');
}
fs.writeFileSync(path.join(out,'animations.js'),'window.ARCHIVE_CLIPS='+JSON.stringify(clips)+';');fs.writeFileSync(path.join(out,'retargeted-animations.json'),JSON.stringify({format:'QuizmanLocalTracks',version:1,bones:target.map(b=>({name:b.name,parent:b.parentIndex,translation:b.localP,rotation:b.localQ})),mapping:map,clips}));
