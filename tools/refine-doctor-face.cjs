// Shared reference-driven proportions in normalized bind coordinates.
module.exports=function refineFace(meshes){
 const smooth=(a,b,x)=>{const t=Math.max(0,Math.min(1,(x-a)/(b-a)));return t*t*(3-2*t)},g=(x,c,w)=>Math.exp(-(((x-c)/w)**2));
 function warp(p){let [x,y,z]=p;const front=smooth(.35,.85,z),lower=g(y,5.78,.43)*front,nose=g(x,0,.22)*g(y,6.18,.29)*smooth(.98,1.15,z);
  return[x,y+.30*Math.max(smooth(5.48,6.05,y),front),z];
 }
 const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
 for(const m of meshes){if(!m.name.startsWith('Plane.021'))continue;
  // Join split shading normals across coincident skin vertices, retaining UVs.
  if(m.texture==='head.png'){
   const groups=new Map(),key=i=>m.p.slice(i,i+3).map(v=>v.toFixed(5)).join(',');
   for(let i=0;i<m.p.length;i+=3){if(m.p[i+1]>6.9||m.p[i+2]<-.3)continue;let k=key(i),a=groups.get(k)||[];a.push(i);groups.set(k,a);}
   for(const indices of groups.values()){let n=[0,0,0];for(const i of indices)for(let k=0;k<3;k++)n[k]+=m.n[i+k];let len=Math.hypot(...n);if(len>1e-6)for(const i of indices)for(let k=0;k<3;k++)m.n[i+k]=n[k]/len;}
  }
  for(let i=0;i<m.p.length;i+=3){let p=m.p.slice(i,i+3),q=warp(p),jac=[];for(let k=0;k<3;k++){let t=p.slice();t[k]+=.0001;jac[k]=warp(t).map((v,j)=>(v-q[j])/.0001);}let cof=[cross(jac[1],jac[2]),cross(jac[2],jac[0]),cross(jac[0],jac[1])],n=[0,1,2].map(k=>cof.reduce((s,c,j)=>s+c[k]*m.n[i+j],0)),len=Math.hypot(...n);m.n.splice(i,3,...n.map(v=>v/len));m.p.splice(i,3,...q);}
 }
};

