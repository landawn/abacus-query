import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = path.dirname(fileURLToPath(import.meta.url));
const forks = Number(process.argv[2] || 3);
const completedLogs = new Set();
const median = values => [...values].sort((a, b) => a - b)[Math.floor(values.length / 2)];
const decode = file => {
  const bytes = fs.readFileSync(file);
  return bytes.toString(bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf16le' : 'utf8')
    .replace(/^\uFEFF/, '').replace(/\r\n/g, '\n');
};

function read(prefix) {
  const cases = new Map();
  for (let fork = 1; fork <= forks; fork++) {
    const file = path.join(dir, `${prefix}-${fork}.csv`);
    const raw = decode(file);
    fs.writeFileSync(file, raw);
    completedLogs.add(`${prefix}-${fork}.log`);
    if (!raw.startsWith('fork,case,scale,')) throw new Error(`Invalid CSV: ${file}`);
    for (const line of raw.trim().split('\n').slice(1)) {
      const [reported, name, scale, chars, round, operations, ns, bytes, result] = line.split(',');
      if (+reported !== fork || !Number.isFinite(+ns) || !Number.isFinite(+bytes)) throw new Error(`Invalid row: ${line}`);
      const key = `${name},${scale}`;
      if (!cases.has(key)) cases.set(key, { name, scale: +scale, sqlChars: +chars, result, samples: {} });
      const entry = cases.get(key);
      if (entry.result !== result || entry.sqlChars !== +chars) throw new Error(`Inconsistent ${key}`);
      (entry.samples[fork] ??= []).push({ round: +round, operations: +operations, ns: +ns, bytes: +bytes });
    }
  }
  return [...cases.values()].map(entry => {
    const summaries = Object.entries(entry.samples).map(([fork, samples]) => {
      if (samples.length !== 7 || new Set(samples.map(r => r.round)).size !== 7) throw new Error(`Incomplete ${entry.name}`);
      return { fork: +fork, ns: median(samples.map(r => r.ns)), bytes: median(samples.map(r => r.bytes)) };
    });
    if (summaries.length !== forks) throw new Error(`Incomplete forks for ${entry.name}`);
    return { name: entry.name, scale: entry.scale, sqlChars: entry.sqlChars, result: entry.result,
      ns: median(summaries.map(r => r.ns)), bytes: median(summaries.map(r => r.bytes)),
      minNs: Math.min(...summaries.map(r => r.ns)), maxNs: Math.max(...summaries.map(r => r.ns)), forks: summaries };
  });
}

function paired(beforePrefix, afterPrefix, output) {
  const before = read(beforePrefix), after = read(afterPrefix);
  if (before.length !== after.length) throw new Error('Case count changed');
  const results = before.map((b, i) => {
    const a = after[i];
    if (b.name !== a.name || b.scale !== a.scale || b.result !== a.result || b.sqlChars !== a.sqlChars) throw new Error(`Mismatch: ${b.name}`);
    return { name: b.name, scale: b.scale, sqlChars: b.sqlChars, result: b.result,
      before: b, after: a, timeRatio: b.ns / a.ns, allocationReductionPercent: 100 * (b.bytes - a.bytes) / b.bytes };
  });
  fs.writeFileSync(path.join(dir, `${output}.json`), JSON.stringify(results, null, 2) + '\n');
  fs.writeFileSync(path.join(dir, `${output}.csv`), 'case,scale,sql_chars,before_ns,after_ns,time_ratio,before_bytes,after_bytes,allocation_reduction_percent\n'
    + results.map(r => [r.name,r.scale,r.sqlChars,r.before.ns,r.after.ns,r.timeRatio,r.before.bytes,r.after.bytes,r.allocationReductionPercent].join(',')).join('\n') + '\n');
  console.log(`${output}: ${results.length} cases, ${forks} forks each, 7 rounds`);
  for (const r of results.filter(r => r.scale >= 1024)) {
    console.log(`${r.name.padEnd(34)} ${String(r.scale).padStart(7)} ${r.timeRatio.toFixed(2).padStart(7)}x  ${r.before.bytes.toFixed(0).padStart(10)} -> ${r.after.bytes.toFixed(0).padStart(10)} B`);
  }
}

paired('before', 'after', 'comparison');
if (fs.existsSync(path.join(dir, `supplement-after-${forks}.csv`))) paired('supplement-before', 'supplement-after', 'supplement-comparison');
if (fs.existsSync(path.join(dir, `cursor-${forks}.csv`))) {
  const cursor = read('cursor');
  fs.writeFileSync(path.join(dir, 'cursor-summary.json'), JSON.stringify(cursor, null, 2) + '\n');
  fs.writeFileSync(path.join(dir, 'cursor-summary.csv'), 'case,scale,sql_chars,median_ns,median_bytes,min_fork_median_ns,max_fork_median_ns\n'
    + cursor.map(r => [r.name,r.scale,r.sqlChars,r.ns,r.bytes,r.minNs,r.maxNs].join(',')).join('\n') + '\n');
}
for (const name of fs.readdirSync(dir).filter(name => /\.(log|txt)$/.test(name))) {
  if (name.endsWith('.log') && !completedLogs.has(name)) continue;
  const file = path.join(dir, name);
  fs.writeFileSync(file, decode(file));
}
