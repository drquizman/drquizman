const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert'),M=require('../assets/host-3d-v1/archive-doctor/animation-runtime.js');
const root=path.resolve(__dirname,'../assets/host-3d-v1');
function load(folder){const c={window:{}};for(const f of ['model-data.js','animations.js'])vm.runInNewContext(fs.readFileSync(path.join(root,folder,f),'utf8'),c);return{meshes:c.window.MODEL,bones:M.skeleton(c.window.ARCHIVE_BONES),clips:c.window.ARCHIVE_CLIPS};}
const old=load('archive-doctor-before-rerig'),next=load('archive-doctor');
let moved=0,changed=0;
next.bones.forEach((b,i)=>{if(Math.hypot(...M.sub(b.p,old.bones[i].p))>1e-6)moved++;});assert.equal(moved,10);
next.meshes.forEach((m,i)=>{assert.deepEqual(m.p,old.meshes[i].p);m.weights.forEach((w,k)=>{if(Math.abs(w-old.meshes[i].weights[k])>1e-6)changed++;});});assert(changed>0);
const results=[];for(const before of old.clips){const clip=next.clips.find(c=>c.name===before.name);assert(clip,'Missing original animation: '+before.name);let max=0;for(const t of [.2,.5,.8].map(f=>f*clip.duration)){const a=M.pose(old.bones,before,t),b=M.pose(next.bones,clip,t);next.meshes.forEach((m,i)=>{const pa=M.deform(old.meshes[i],a.skin).p,pb=M.deform(m,b.skin).p;for(let k=0;k<pa.length;k++)max=Math.max(max,Math.abs(pa[k]-pb[k]));});}assert(max>.001,clip.name+' did not change actual mesh motion');results.push({clip:clip.name,maxPositionChange:+max.toFixed(4)});}
console.log(JSON.stringify({movedJoints:moved,changedWeights:changed,meshBindShapePreserved:true,results},null,2));
