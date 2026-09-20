import { readFile, writeFile } from 'node:fs/promises';
import { resolve, join } from 'node:path';
const dir=resolve(process.argv[2]??'docs/performance/parsed-sql-tuning-2026-09-20');
const median=a=>{a=[...a].sort((x,y)=>x-y);return(a[Math.floor((a.length-1)/2)]+a[Math.floor(a.length/2)])/2;};
const versions={baseline:5,before:5,current:5,baseline_common8:3};
const all=new Map(),forks=[];let samples=0;
for(const [version,n] of Object.entries(versions))for(let fork=1;fork<=n;fork++){
 const lines=(await readFile(join(dir,version+'-'+fork+'.csv'),'utf8')).trim().split(/\r?\n/);const head=lines.shift().split(',');const groups=new Map();
 for(const line of lines){const row=Object.fromEntries(line.split(',').map((v,i)=>[head[i],v]));
  if(row.version!==version||+row.fork!==fork||+row.errors!==0)throw Error('Bad row '+line);
  if(['current','before'].includes(version)&&(row.semantic_match!=='true'||row.min_count!==row.expected||row.max_count!==row.expected))throw Error('Behavior mismatch '+line);
  const key=row.shape+'/'+row.size;if(!groups.has(key))groups.set(key,[]);groups.get(key).push(row);samples++;
 }
 if(groups.size!==40)throw Error('Expected 40 cases: '+version+'/'+fork);
 for(const [key,rows] of groups){if(rows.length!==9||new Set(rows.map(x=>x.round)).size!==9)throw Error('Missing rounds');
  const first=rows[0],r={version,fork,shape:first.shape,size:+first.size,ns:median(rows.map(x=>+x.ns_per_op)),bytes:median(rows.map(x=>+x.bytes_per_op)),count:+first.min_count,equivalent:rows.every(x=>x.semantic_match==='true')};forks.push(r);
  if(!all.has(key))all.set(key,{});const cases=all.get(key);(cases[version]??=[]).push(r);
 }
}
const summary=[];
for(const [key,vs] of all){const r={shape:vs.current[0].shape,size:vs.current[0].size,equivalent_results:Object.values(vs).flat().every(x=>x.equivalent)};
 for(const version of Object.keys(versions)){const rows=vs[version];r[version+'_ns']=median(rows.map(x=>x.ns));r[version+'_min_ns']=Math.min(...rows.map(x=>x.ns));r[version+'_max_ns']=Math.max(...rows.map(x=>x.ns));r[version+'_bytes']=median(rows.map(x=>x.bytes));r[version+'_count']=rows[0].count;}
 r.current_vs_baseline_pct=(r.current_ns/r.baseline_ns-1)*100;r.current_vs_before_pct=(r.current_ns/r.before_ns-1)*100;r.current_vs_common8_pct=(r.current_ns/r.baseline_common8_ns-1)*100;summary.push(r);
}
summary.sort((a,b)=>a.shape.localeCompare(b.shape)||a.size-b.size);
const csv=rows=>Object.keys(rows[0]).join(',')+'\r\n'+rows.map(r=>Object.values(r).map(v=>typeof v==='number'&&!Number.isInteger(v)?v.toFixed(3):v).join(',')).join('\r\n')+'\r\n';
await writeFile(join(dir,'summary.csv'),csv(summary));await writeFile(join(dir,'fork-summary.csv'),csv(forks));await writeFile(join(dir,'summary.json'),JSON.stringify({samples,summary},null,2)+'\r\n');
console.log(JSON.stringify({samples,cases:summary.length,fasterThanBaseline:summary.filter(x=>x.current_ns<x.baseline_ns).length,remaining:summary.filter(x=>x.current_ns>=x.baseline_ns).map(x=>({shape:x.shape,size:x.size,pct:x.current_vs_baseline_pct,equivalent:x.equivalent_results}))},null,2));
