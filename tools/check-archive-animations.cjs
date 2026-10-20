const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert'),M=require('../assets/host-3d-v1/archive-doctor/animation-runtime.js');
const root=path.resolve(__dirname,'../assets/host-3d-v1/archive-doctor'),ctx={window:{}};for(let f of ['model-data.js','animations.js'])vm.runInNewContext(fs.readFileSync(path.join(root,f),'utf8'),ctx);const bones=M.skeleton(ctx.window.ARCHIVE_BONES),meshes=ctx.window.MODEL,clips=ctx.window.ARCHIVE_CLIPS;
const file=fs.readFileSync(path.join(root,'doctor-animated.glb')),jl=file.readUInt32LE(12),g=JSON.parse(file.toString('utf8',20,20+jl)),bin=file.subarray(28+jl);assert.equal(file.readUInt32LE(0),0x46546c67);assert.equal(file.readUInt32LE(8),file.length);
function read(id){const a=g.accessors[id],v=g.bufferViews[a.bufferView],dims={SCALAR:1,VEC2:2,VEC3:3,VEC4:4,MAT4:16}[a.type],size=a.componentType===5123?2:4,start=v.byteOffset||0;assert(start+a.count*dims*size<=bin.length);return Array.from({length:a.count*dims},(_,i)=>a.componentType===5126?bin.readFloatLE(start+i*size):a.componentType===5123?bin.readUInt16LE(start+i*size):bin.readUInt32LE(start+i*size));}
assert.equal(g.animations.length,9+require('./doctor-gesture-clips.cjs').length);for(const [id] of require('./doctor-gesture-clips.cjs'))assert(g.animations.some(a=>a.name===id),'Missing gesture '+id);assert.equal(g.skins[0].joints.length,58);const bind=M.pose(bones,null,0);let maxBindError=0,minFloor=Infinity;
for(let i=0;i<meshes.length;i++){const m=meshes[i],a=g.meshes[i].primitives[0].attributes,positions=read(a.POSITION),weights=read(a.WEIGHTS_0),joints=read(a.JOINTS_0);assert.equal(positions.length,m.p.length);positions.forEach((v,k)=>assert.equal(v,Math.fround(m.p[k])));for(let v=0;v<m.p.length/3;v++){let sum=0;for(let j=0;j<4;j++){let k=v*4+j;assert(joints[k]>=0&&joints[k]<bones.length);assert.equal(joints[k],m.joints[k]);assert.equal(weights[k],Math.fround(m.weights[k]));sum+=weights[k];}assert(Math.abs(sum-1)<1e-6);}const p=M.deform(m,bind.skin).p;p.forEach((v,k)=>maxBindError=Math.max(maxBindError,Math.abs(v-m.p[k])));}
assert(maxBindError<2e-5);
const ibm=read(g.skins[0].inverseBindMatrices);bones.forEach((b,i)=>{let identity=M.multiply(bind.world[i],ibm.slice(i*16,i*16+16));identity.forEach((v,k)=>assert(Math.abs(v-(k%5===0?1:0))<2e-5));});
for(let clip of clips){const animation=g.animations.find(a=>a.name===clip.name);assert(animation);assert.equal(clip.mappedBones,52);for(let ch of animation.channels){const sampler=animation.samplers[ch.sampler],times=read(sampler.input),values=read(sampler.output),stride=ch.target.path==='rotation'?4:3;assert.equal(values.length,times.length*stride);assert(Math.abs(times.at(-1)-clip.duration)<1e-5);for(let i=0;i<times.length;i++){if(i)assert(times[i]>times[i-1]);if(stride===4)assert(Math.abs(Math.hypot(...values.slice(i*4,i*4+4))-1)<1e-6);}}
 for(let f=0;f<clip.frames.length;f+=5){const pose=M.pose(bones,clip,f/clip.fps);for(let m of meshes){const deformed=M.deform(m,pose.skin);assert([...deformed.p,...deformed.n].every(Number.isFinite));for(let k=1;k<deformed.p.length;k+=3)minFloor=Math.min(minFloor,deformed.p[k]);}}
}
assert(minFloor>-.001,'Feet penetrate the floor');
console.log(JSON.stringify({clips:clips.map(c=>({name:c.name,duration:c.duration,mappedBones:c.mappedBones})),originalSkeletonNodes:bones.length,geometryPreserved:true,maxBindError,minFloor,embeddedTextures:g.images.length,glbBytes:file.length},null,2));

// Regression checks for crossed shins, narrow stance and the upward idle gaze,
// sampled between keys as well as on keys.
const byName=new Map(bones.map((b,i)=>[b.name,i])),dot=(a,b)=>a.reduce((s,v,k)=>s+v*b[k],0);
let minStance=Infinity,maxIdleGaze=-Infinity,minIdleHand=Infinity;
for(const clip of clips.filter(c=>c.name!=='laughing'))for(let f=0;f<clip.frames.length-1;f+=.5){
 const pose=M.pose(bones,clip,f/clip.fps),pos=n=>pose.world[byName.get(n)].slice(12,15),hip=byName.get('pelvis');
 const delta=M.qm(M.fromMatrix(pose.world[hip]),M.qi(bones[hip].q)),a=M.qr(delta,[1,0,0]);a[1]=0;const lateral=M.unit(a);
 const width=dot(M.sub(pos('foot_l'),pos('foot_r')),lateral);minStance=Math.min(minStance,width);assert(width>1.1,clip.name+' narrow feet at '+f);
 for(const side of ['l','r']){const sign=side==='l'?1:-1;assert(sign*dot(M.sub(pos('foot_'+side),pos('calf_'+side)),lateral)>.15,clip.name+' inward shin at '+f);}
 if(clip.name==='wait'){
  const h=byName.get('head'),forward=M.qr(M.qm(M.fromMatrix(pose.world[h]),M.qi(bones[h].q)),[0,0,1]);maxIdleGaze=Math.max(maxIdleGaze,forward[1]);assert(forward[1]<.2,'Idle gaze too high');
  for(const side of ['l','r']){const clear=(side==='l'?1:-1)*dot(M.sub(pos('hand_'+side),pos('pelvis')),lateral);minIdleHand=Math.min(minIdleHand,clear);assert(clear>1.15,'Idle hand too close to torso');}
 }
}
assert.equal(clips.find(c=>c.name==='hi').duration,1.6);
console.log({minStance,maxIdleGaze,minIdleHand});

// Joint spacing does not detect axial twist: compare each leg's orientation
// with a swing-only transport of its own bind frame, including half-key samples.
let maxLegTwist=0;
for(const clip of clips)for(let f=0;f<clip.frames.length-1;f+=.5){
 const pose=M.pose(bones,clip,f/clip.fps),hip=byName.get('pelvis');
 const a=M.qr(M.qm(M.fromMatrix(pose.world[hip]),M.qi(bones[hip].q)),[1,0,0]);a[1]=0;
 const yaw=M.between([1,0,0],M.unit(a));
 for(const side of ['l','r'])for(const [joint,end] of [['thigh_','calf_'],['calf_','foot_']]){
  const i=byName.get(joint+side),j=byName.get(end+side),dir=M.sub(pose.world[j].slice(12,15),pose.world[i].slice(12,15));
  const reference=M.qm(M.qm(M.between(M.qr(yaw,M.sub(bones[j].p,bones[i].p)),dir),yaw),bones[i].q);
  const actual=M.fromMatrix(pose.world[i]),angle=2*Math.acos(Math.min(1,Math.abs(dot(reference,actual))));
  maxLegTwist=Math.max(maxLegTwist,angle);assert(angle<.03,clip.name+' axial leg twist at '+f);
 }
}
console.log({maxLegTwistDegrees:maxLegTwist*180/Math.PI});

let maxDanceSideBend=0;
const dance=clips.find(c=>c.name==='dance');
for(let f=0;f<dance.frames.length-1;f+=.5){
 const w=M.pose(bones,dance,f/dance.fps).world,hip=byName.get('pelvis');
 let axis=M.qr(M.qm(M.fromMatrix(w[hip]),M.qi(bones[hip].q)),[1,0,0]);axis[1]=0;axis=M.unit(axis);
 const forward=[-axis[2],0,axis[0]],pos=n=>w[byName.get(n)].slice(12,15);
 for(const side of ['l','r']){
  const upper=M.unit(M.sub(pos('calf_'+side),pos('thigh_'+side))),lower=M.unit(M.sub(pos('foot_'+side),pos('calf_'+side)));
  const sideBend=Math.abs(dot(upper,axis)-dot(lower,axis));maxDanceSideBend=Math.max(maxDanceSideBend,sideBend);assert(sideBend<.005,'Dance knee bends sideways');
  const flex=Math.atan2(dot(upper,forward),-upper[1])-Math.atan2(dot(lower,forward),-lower[1]);assert(flex>-.003&&flex<1.503,'Dance knee hyperextension');
 }
}
console.log({maxDanceSideBend});


for(const frame of dance.frames)for(const side of ['l','r']){
 const i=byName.get('ball_'+side),q=frame.q.slice(i*4,i*4+4);
 assert(Math.abs(dot(q,bones[i].localQ))>1-1e-6,'Dance forefoot twists relative to ankle');
}
