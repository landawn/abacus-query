import {spawn} from 'node:child_process';
import {mkdir,readFile,writeFile} from 'node:fs/promises';
import {resolve,join} from 'node:path';
const work=resolve(process.argv[2]??'.audit/parsed-sql-tuning');
const old=resolve(process.argv[3]??'.audit/parsed-sql-vs-965f6afe');
const output=resolve(process.argv[4]??'docs/performance/parsed-sql-tuning-2026-09-20');
const java=process.argv[5]??'java';
await mkdir(output,{recursive:true});
const deps={baseline:(await readFile(join(old,'baseline-classpath.txt'),'utf8')).trim(),current:(await readFile(join(old,'current-classpath.txt'),'utf8')).trim()};
const runs=[];
for(let fork=1;fork<=5;fork++)for(const version of (fork%2?['baseline','current','before']:['before','current','baseline']))runs.push([version,fork]);
for(let fork=1;fork<=3;fork++)runs.push(['baseline_common8',fork]);
const status={started:new Date().toISOString(),completed:[]};
for(const [version,fork] of runs){
 const library=version==='current'?join(work,'tuned.jar'):join(old,version==='before'?'current.jar':'baseline.jar');
 const classpath=[join(old,'harness'),library,deps[version==='baseline'?'baseline':'current']].join(process.platform==='win32'?';':':');
 const started=new Date().toISOString();
 console.log('Starting '+version+' fork '+fork+' at '+started);
 const result=await new Promise((accept,reject)=>{
  const p=spawn(java,['-Xms512m','-Xmx1024m','-XX:+UseG1GC','-cp',classpath,'ParserBenchmark',version,String(fork)],{windowsHide:true});
  let stdout='',stderr='';p.stdout.on('data',b=>stdout+=b);p.stderr.on('data',b=>stderr+=b);p.on('error',reject);p.on('close',code=>accept({code,stdout,stderr}));
 });
 await writeFile(join(output,version+'-'+fork+'.csv'),result.stdout);
 await writeFile(join(output,version+'-'+fork+'.stderr.txt'),result.stderr);
 status.completed.push({version,fork,started,finished:new Date().toISOString(),code:result.code});
 await writeFile(join(output,'run-status.json'),JSON.stringify(status,null,2)+'\n');
 if(result.code!==0)throw Error('Failed '+version+'/'+fork+': '+result.stderr);
}
