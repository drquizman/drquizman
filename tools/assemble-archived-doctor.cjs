const fs=require('fs'),path=require('path'),{parse}=require('./inspect-doctor-fbx.cjs');
const source='C:/Users/akhin/Documents/Quizman/public/models/DoctorQuizz',out=path.resolve(__dirname,'../assets/host-3d-v1/archive-doctor');fs.mkdirSync(out,{recursive:true});
const d=parse(path.join(source,'doctor_rigged_hand_fix.fbx')),objects=d.nodes.find(n=>n.name==='Objects').children,connections=d.nodes.find(n=>n.name==='Connections').children.map(n=>n.props),byId=new Map(objects.map(n=>[n.props[0],n]));
const child=(n,key)=>n.children.find(n=>n.name===key),value=(n,key)=>child(n,key)?.props[0],name=n=>n.props[1].split('\0')[0],bind=new Map();
for(let pose of objects.filter(n=>n.name==='Pose'))for(let n of pose.children.filter(n=>n.name==='PoseNode'))bind.set(value(n,'Node'),value(n,'Matrix'));
const transform=(m,p,w=1)=>[0,1,2].map(k=>m[k]*p[0]+m[4+k]*p[1]+m[8+k]*p[2]+w*m[12+k]);
const textures={body:'doctor texture_doctor_complete_Diffuse.png',head:'texture head_head_low_Diffuse.png',hand:'hand_diff.png',face:'MouthMAP_doctor.png'};
for(let [key,file] of Object.entries(textures))fs.copyFileSync(path.join(source,file),path.join(out,key+'.png'));
fs.copyFileSync(path.join(source,'doctor_rigged_hand_fix.fbx'),path.join(out,'doctor-original.fbx'));
const groups=[],bounds={min:[Infinity,Infinity,Infinity],max:[-Infinity,-Infinity,-Infinity]};
const boneObjects=objects.filter(n=>n.name==='Model'&&(n.props[2]==='LimbNode'||n.props[2]==='Null')),boneIndex=new Map(boneObjects.map((b,i)=>[b.props[0],i]));
for(let geo of objects.filter(n=>n.name==='Geometry'&&n.props[2]==='Mesh')){
 const model=byId.get(connections.find(c=>c[0]==='OO'&&c[1]===geo.props[0]&&byId.get(c[2])?.name==='Model')[2]),matrix=bind.get(model.props[0]);if(!matrix)throw Error('Missing bind matrix');
 const mats=connections.filter(c=>c[0]==='OO'&&c[2]===model.props[0]&&byId.get(c[1])?.name==='Material').map(c=>byId.get(c[1]));
 const skinId=connections.find(c=>c[2]===geo.props[0]&&byId.get(c[1])?.props[2]==='Skin')?.[1],influences=new Map();
 for(const link of connections.filter(c=>c[2]===skinId&&byId.get(c[1])?.props[2]==='Cluster')){const cluster=byId.get(link[1]),joint=connections.find(c=>c[2]===link[1]&&boneIndex.has(c[1]))?.[1],ii=value(cluster,'Indexes')||[],ww=value(cluster,'Weights')||[];if(joint===undefined)continue;for(let k=0;k<ii.length;k++){if(ww[k]<=0)continue;if(!influences.has(ii[k]))influences.set(ii[k],[]);influences.get(ii[k]).push([boneIndex.get(joint),ww[k]]);}}
 const positions=value(geo,'Vertices'),indices=value(geo,'PolygonVertexIndex'),norm=child(geo,'LayerElementNormal'),uv=child(geo,'LayerElementUV'),material=child(geo,'LayerElementMaterial'),normalData=value(norm,'Normals'),uvData=value(uv,'UV'),uvIndex=value(uv,'UVIndex'),materialData=value(material,'Materials'),groupsByMat=new Map();
 let corners=[],poly=0;
 for(let corner=0;corner<indices.length;corner++){
  const index=indices[corner]<0?-indices[corner]-1:indices[corner];corners.push({index,corner});if(indices[corner]>=0)continue;
  const mi=materialData[value(material,'MappingInformationType')==='AllSame'?0:poly],matName=name(mats[mi]),tex=/hand/i.test(matName)?'hand':/head/i.test(matName)?'head':/Mouth|Eyes|Brows/i.test(matName)?'face':'body';
  let m=groupsByMat.get(mi);if(!m){m={name:name(model)+' / '+matName,p:[],n:[],uv:[],idx:[],joints:[],weights:[],color:'#ffffff',texture:tex+'.png',face:tex==='face',materialName:matName};groupsByMat.set(mi,m);groups.push(m);}
  const base=m.p.length/3;
  for(let c of corners){const weights=(influences.get(c.index)||[[0,1]]).sort((a,b)=>b[1]-a[1]).slice(0,4),sum=weights.reduce((s,w)=>s+w[1],0);for(let k=0;k<4;k++){m.joints.push(weights[k]?.[0]||0);m.weights.push((weights[k]?.[1]||0)/sum);}let p=transform(matrix,positions.slice(c.index*3,c.index*3+3)),ni=value(norm,'MappingInformationType')==='ByVertice'?c.index:c.corner,n=transform(matrix,normalData.slice(ni*3,ni*3+3),0),l=Math.hypot(...n);n=n.map(x=>x/l);let ui=uvIndex?uvIndex[c.corner]:c.corner;m.p.push(...p);m.n.push(...n);let uu=uvData[ui*2],vv=uvData[ui*2+1];if(/Eyes/.test(matName)){uu-=.2;vv+=.5;}if(/Mouth/.test(matName))vv=.56+(vv-.6476605534553528)*1.15;m.uv.push(uu,vv);for(let k=0;k<3;k++){bounds.min[k]=Math.min(bounds.min[k],p[k]);bounds.max[k]=Math.max(bounds.max[k],p[k]);}}
  for(let k=1;k<corners.length-1;k++)m.idx.push(base,base+k,base+k+1);corners=[];poly++;
 }
}
const scale=7.45/(bounds.max[1]-bounds.min[1]),centerX=(bounds.max[0]+bounds.min[0])/2,centerZ=(bounds.max[2]+bounds.min[2])/2,normalize=p=>[(p[0]-centerX)*scale,(p[1]-bounds.min[1])*scale,(p[2]-centerZ)*scale];
for(let m of groups)for(let i=0;i<m.p.length;i+=3)m.p.splice(i,3,...normalize(m.p.slice(i,i+3)));
const bones=objects.filter(n=>n.name==='Model'&&(n.props[2]==='LimbNode'||n.props[2]==='Null')).map(n=>({id:n.props[0],name:name(n),parent:connections.find(c=>c[0]==='OO'&&c[1]===n.props[0]&&byId.get(c[2])?.name==='Model')?.[2]||0,p:normalize(bind.get(n.props[0]).slice(12,15)),bind:bind.get(n.props[0]).map((v,i)=>i>=12&&i<=14?normalize(bind.get(n.props[0]).slice(12,15))[i-12]:v)}));
require('./refine-doctor-face.cjs')(groups);
const rigChanges=require('./refine-doctor-rig.cjs')(groups,bones);
fs.writeFileSync(path.join(out,'rig-changes.json'),JSON.stringify(rigChanges,null,2));
fs.writeFileSync(path.join(out,'model-data.js'),'window.MODEL='+JSON.stringify(groups)+';window.ARCHIVE_BONES='+JSON.stringify(bones)+';');
fs.writeFileSync(path.join(out,'source-info.json'),JSON.stringify({source,fbx:'doctor_rigged_hand_fix.fbx',bindBounds:bounds,displayScale:scale,materials:groups.map(m=>({name:m.name,texture:m.texture,triangles:m.idx.length/3})),skeletonNodes:bones.length,triangles:groups.reduce((s,m)=>s+m.idx.length/3,0)},null,2));
console.log(JSON.stringify({out,groups:groups.map(m=>[m.materialName,m.texture,m.idx.length/3]),bones:bones.length,bounds}));
