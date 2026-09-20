// Run: node docs/performance/builder-tuning-2026-09-20/summarize.cjs
const fs=require('node:fs'),path=require('node:path');
const dir=path.join(__dirname,'final');
const median=values=>{const x=[...values].sort((a,b)=>a-b);return x.length%2?x[x.length>>1]:(x[x.length/2-1]+x[x.length/2])/2;};
const caseCount=fs.readFileSync(path.join(__dirname,"reference.txt"),"utf8").trim().split(/\r?\n/).length;
const groups=new Map();let samples=0;
for(const name of fs.readdirSync(dir).filter(x=>/^(baseline|current|before|baseline-control)-\d+\.csv$/.test(x))){
 const lines=fs.readFileSync(path.join(dir,name),'utf8').trim().split(/\r?\n/);
 if(lines.shift()!=='version,fork,case,round,operations,ns_per_op,bytes_per_op,equivalent')throw Error('Bad header '+name);
 const rows=lines.map(x=>x.split(','));if(rows.length!==caseCount*9)throw Error('Incomplete '+name);
 for(const row of rows){if(row.length!==8)throw Error('Bad row '+name);const [version,fork,workload,round,ops,ns,bytes,same]=row;
  if(!(Number(ops)>0&&Number(ns)>0&&Number(bytes)>=0))throw Error('Invalid sample '+row);
  if(['current','before'].includes(version)&&same!=='true')throw Error('Behavior mismatch '+row);
  const key=version+'|'+workload;let g=groups.get(key);if(!g)groups.set(key,g={version,workload,forks:new Map(),same:same==='true'});
  let f=g.forks.get(fork);if(!f)g.forks.set(fork,f={times:[],bytes:[],durations:[],rounds:new Set()});if(f.rounds.has(round))throw Error('Duplicate round');f.rounds.add(round);f.times.push(+ns);f.bytes.push(+bytes);f.durations.push(+ns*+ops/1e6);samples++;
 }
}
const summary=[],forks=[];
for(const g of groups.values()){
 const f=[...g.forks].map(([fork,v])=>({version:g.version,case:g.workload,fork:Number(fork),ns:median(v.times),bytes:median(v.bytes),median_batch_ms:median(v.durations)}));
 if(f.some(x=>!Number.isFinite(x.ns)))throw Error('Invalid summary');forks.push(...f);
 summary.push({version:g.version,case:g.workload,forks:f.length,median_ns:median(f.map(x=>x.ns)),min_fork_ns:Math.min(...f.map(x=>x.ns)),max_fork_ns:Math.max(...f.map(x=>x.ns)),bytes_per_op:median(f.map(x=>x.bytes)),equivalent_results:g.same});
}
summary.sort((a,b)=>a.case.localeCompare(b.case)||a.version.localeCompare(b.version));
const csv=(rows,keys)=>[keys.join(','),...rows.map(r=>keys.map(k=>r[k]).join(','))].join('\r\n')+'\r\n';
fs.writeFileSync(path.join(dir,'per-fork.csv'),csv(forks,['version','case','fork','ns','bytes','median_batch_ms']));
fs.writeFileSync(path.join(dir,'summary.csv'),csv(summary,['version','case','forks','median_ns','min_fork_ns','max_fork_ns','bytes_per_op','equivalent_results']));
const cases=summary.filter(r=>r.version==='current').map(current=>{
 const lookup=v=>summary.find(r=>r.case===current.case&&r.version===v);
 const baseline=lookup('baseline'),before=lookup('before'),control=lookup('baseline-control');
 return {case:current.case,baseline,current,before,control,speedup:baseline.median_ns/current.median_ns,percent_less_time:100*(1-current.median_ns/baseline.median_ns),allocation_change:current.bytes_per_op-baseline.bytes_per_op};
});
const result={samples,cases,shortestMedianBatchMs:Math.min(...forks.map(x=>x.median_batch_ms)),longestMedianBatchMs:Math.max(...forks.map(x=>x.median_batch_ms)),latencyWins:cases.filter(x=>x.speedup>1).length,allocationLower:cases.filter(x=>x.allocation_change<0).length,allocationEqual:cases.filter(x=>x.allocation_change===0).length,allocationHigher:cases.filter(x=>x.allocation_change>0).length};
fs.writeFileSync(path.join(dir,'summary.json'),JSON.stringify(result,null,2)+'\r\n');
console.log(JSON.stringify({samples,cases:cases.length,latencyWins:result.latencyWins,allocationLower:result.allocationLower,allocationEqual:result.allocationEqual,allocationHigher:result.allocationHigher}));
