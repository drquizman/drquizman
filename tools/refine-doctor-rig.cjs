// Actual bind-joint edits. Mesh stays in world bind coordinates; inverse binds
// and local offsets are rebuilt by animation-runtime and the GLB exporter.
module.exports=function refineRig(meshes,bones){
 const original=bones.map(b=>b.p.slice()),index=new Map(bones.map((b,i)=>[b.name,i]));
 const changes=[];
 function move(name,delta){const i=index.get(name),b=bones[i];b.p=original[i].map((v,k)=>v+delta[k]);b.bind.splice(12,3,...b.p);changes.push({name,from:original[i],to:b.p.slice()});}
 for(const side of ['l','r']){
  move('clavicle_'+side,[0,-.08,0]);move('upperarm_'+side,[0,-.08,0]);
  const e=original[index.get('lowerarm_'+side)],h=original[index.get('hand_'+side)],v=h.map((x,k)=>x-e[k]),len=Math.hypot(...v);
  move('lowerarm_'+side,v.map(x=>.10*x/len));
  move('calf_'+side,[0,-.08,0]);
  // Ankle is a rotation pivot inside the shoe, not a sole contact marker.
  move('foot_'+side,[0,-.06,0]);
 }
 let changedWeights=0;
 // Shift existing smooth blend zones with their joint, preserving influence
 // support, fingers, rigid shoe regions and normalization of all four weights.
 for(const side of ['l','r'])for(const [parent,child,end,width] of [
  ['clavicle_','upperarm_','lowerarm_',.24],
  ['upperarm_','lowerarm_','hand_',.22],
  ['thigh_','calf_','foot_',.28],
  ['calf_','foot_','ball_',.18]]){
  const a=index.get(parent+side),b=index.get(child+side),c=index.get(end+side),axis=original[c].map((v,k)=>v-original[b][k]),len=Math.hypot(...axis),shift=bones[b].p.reduce((s,v,k)=>s+(v-original[b][k])*axis[k]/len,0);
  for(const m of meshes)for(let v=0;v<m.weights.length;v+=4){let ai=-1,bi=-1;for(let k=0;k<4;k++){if(m.joints[v+k]===a)ai=v+k;if(m.joints[v+k]===b)bi=v+k;}if(ai<0||bi<0)continue;
   const wa=m.weights[ai],wb=m.weights[bi],sum=wa+wb;if(wa<1e-5||wb<1e-5)continue;const t=wb/sum,factor=Math.exp(-shift/width),next=t*factor/(1-t+t*factor);m.weights[bi]=sum*next;m.weights[ai]=sum*(1-next);changedWeights++;
  }
 }
 return{version:2,changes,changedWeights};
};
