// Produce an Android-only copy of the current rig. Never rewrite the source GLB.
// glTF morph targets give Filament the same mouth / blink deformation as the web viewer.
const fs=require('fs'),path=require('path'),crypto=require('crypto');
const root=path.resolve(__dirname,'..');
const source=fs.readFileSync(path.join(root,'assets/host-3d-v1/archive-doctor/doctor-animated.glb'));
const jsonLength=source.readUInt32LE(12),g=JSON.parse(source.subarray(20,20+jsonLength));
let bin=Buffer.from(source.subarray(28+jsonLength,28+jsonLength+source.readUInt32LE(20+jsonLength)));
function values(index){const a=g.accessors[index],v=g.bufferViews[a.bufferView];if(a.componentType!==5126)throw Error('Float accessor required');const n={VEC2:2,VEC3:3,VEC4:4}[a.type],offset=(v.byteOffset||0)+(a.byteOffset||0);return Array.from({length:a.count*n},(_,i)=>bin.readFloatLE(offset+Math.floor(i/n)*(v.byteStride||n*4)+(i%n)*4));}
function accessor(data,n){const pad=(4-bin.length%4)%4;bin=Buffer.concat([bin,Buffer.alloc(pad)]);const offset=bin.length,b=Buffer.alloc(data.length*4);data.forEach((v,i)=>b.writeFloatLE(v,i*4));bin=Buffer.concat([bin,b]);const view=g.bufferViews.push({buffer:0,byteOffset:offset,byteLength:b.length,target:34962})-1;const min=Array(n).fill(Infinity),max=Array(n).fill(-Infinity);data.forEach((v,i)=>{min[i%n]=Math.min(min[i%n],v);max[i%n]=Math.max(max[i%n],v)});return g.accessors.push({bufferView:view,componentType:5126,count:data.length/n,type:'VEC'+n,min,max})-1;}
for(const mesh of g.meshes){
 if(!/Mouth|Eyes/.test(mesh.name))continue;
 for(const p of mesh.primitives){
  const original=values(p.attributes.POSITION),base=original.slice(),delta=Array(base.length).fill(0);
  if(/Mouth/.test(mesh.name)){
   const ys=original.filter((_,i)=>i%3===1),anchor=(Math.min(...ys)+Math.max(...ys))/2+.11;
   for(let i=0;i<base.length;i+=3){base[i+1]=anchor+(original[i+1]-anchor)*.12;base[i+2]+=.045;delta[i+1]=(original[i+1]-anchor)*.90;}
   // GLB UVs are flipped relative to the WebGL viewer's original FBX UVs.
   const uv=values(p.attributes.TEXCOORD_0);for(let i=1;i<uv.length;i+=2)uv[i]-=.6476605534553528-.56;
   p.attributes.TEXCOORD_0=accessor(uv,2);
  }else{
   for(const sign of [-1,1]){const ys=original.filter((_,i)=>i%3===1&&Math.sign(original[i-1])===sign),bottom=Math.min(...ys);for(let i=0;i<base.length;i+=3)if(Math.sign(base[i])===sign){base[i+1]=bottom+(base[i+1]-bottom)*.82;base[i+2]+=.018;}}
   const ys=base.filter((_,i)=>i%3===1),center=(Math.min(...ys)+Math.max(...ys))/2;
   for(let i=0;i<base.length;i+=3)delta[i+1]=(center-base[i+1])*.96;
  }
  p.attributes.POSITION=accessor(base,3);p.targets=[{POSITION:accessor(delta,3)}];
 }
 mesh.weights=[0];mesh.extras={...mesh.extras,targetNames:[/Mouth/.test(mesh.name)?'SpeechOpen':'Blink']};
}
g.buffers[0].byteLength=bin.length;bin=Buffer.concat([bin,Buffer.alloc((4-bin.length%4)%4)]);
let json=Buffer.from(JSON.stringify(g));json=Buffer.concat([json,Buffer.alloc((4-json.length%4)%4,32)]);
const head=Buffer.alloc(20);head.writeUInt32LE(0x46546c67);head.writeUInt32LE(2,4);head.writeUInt32LE(28+json.length+bin.length,8);head.writeUInt32LE(json.length,12);head.writeUInt32LE(0x4e4f534a,16);
const bh=Buffer.alloc(8);bh.writeUInt32LE(bin.length);bh.writeUInt32LE(0x004e4942,4);
const target=path.join(root,'tv/src/main/assets/host');fs.mkdirSync(target,{recursive:true});
fs.writeFileSync(path.join(target,'doctor.glb'),Buffer.concat([head,json,bh,bin]));
fs.writeFileSync(path.join(target,'source.json'),JSON.stringify({source:'assets/host-3d-v1/archive-doctor/doctor-animated.glb',sha256:crypto.createHash('sha256').update(source).digest('hex'),rig:JSON.parse(fs.readFileSync(path.join(root,'assets/host-3d-v1/archive-doctor/rig-changes.json'))).version,animations:g.animations.map(a=>a.name),addedMorphTargets:['SpeechOpen','Blink']},null,2));
console.log('Android model prepared; '+g.animations.length+' animations; original rig and clips preserved.');
