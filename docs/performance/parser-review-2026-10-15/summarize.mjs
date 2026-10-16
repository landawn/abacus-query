import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import os from 'node:os';

const dir = path.dirname(new URL(import.meta.url).pathname.replace(/^\/(\w:)/, '$1'));
const median = values => {
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.floor(sorted.length / 2)];
};
const cases = new Map();
for (let fork = 1; fork <= 3; fork++) {
  const file = path.join(dir, `baseline-${fork}.csv`);
  const bytes = fs.readFileSync(file);
  const raw = bytes.toString(bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf16le' : 'utf8').replace(/^\uFEFF/, '');
  // Normalize only encoding/newlines; preserve every measured row.
  fs.writeFileSync(file, raw.replace(/\r\n/g, '\n'));
  for (const row of raw.trim().split(/\r?\n/).slice(1)) {
    const [reportedFork, name, scale, chars, round, operations, ns, allocated, result] = row.split(',');
    if (Number(reportedFork) !== fork) throw new Error('Mismatched fork');
    const key = `${name},${scale}`;
    if (!cases.has(key)) cases.set(key, { name, scale: +scale, chars: +chars, result, forks: {} });
    const entry = cases.get(key);
    if (entry.result !== result) throw new Error(`Result changed for ${key}`);
    (entry.forks[fork] ??= []).push({ round: +round, operations: +operations, ns: +ns, bytes: +allocated });
  }
}
const summary = [...cases.values()].map(entry => {
  const forks = Object.entries(entry.forks).map(([fork, rows]) => {
    if (rows.length !== 7) throw new Error(`Incomplete rounds for ${entry.name}`);
    return { fork: +fork, ns: median(rows.map(row => row.ns)), bytes: median(rows.map(row => row.bytes)) };
  });
  if (forks.length !== 3) throw new Error('Incomplete forks');
  return { name: entry.name, scale: entry.scale, sqlChars: entry.chars, result: entry.result,
    ns: median(forks.map(fork => fork.ns)), bytes: median(forks.map(fork => fork.bytes)),
    minNs: Math.min(...forks.map(fork => fork.ns)), maxNs: Math.max(...forks.map(fork => fork.ns)), forks };
});
fs.writeFileSync(path.join(dir, 'summary.json'), JSON.stringify(summary, null, 2) + '\n');
fs.writeFileSync(path.join(dir, 'summary.csv'), 'case,scale,sql_chars,median_ns_per_op,median_bytes_per_op,min_fork_median_ns,max_fork_median_ns,result\n'
  + summary.map(s => [s.name,s.scale,s.sqlChars,s.ns,s.bytes,s.minNs,s.maxNs,s.result].join(',')).join('\n') + '\n');
const sources = ['src/main/java/com/landawn/abacus/query/SqlParser.java', 'src/main/java/com/landawn/abacus/query/ParsedSql.java',
  'src/test/java/com/landawn/abacus/query/SqlParserTest.java', 'src/test/java/com/landawn/abacus/query/ParsedSqlTest.java', 'pom.xml'];
const sourceHashes = Object.fromEntries(sources.map(file => [file,
  crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex')]));
const hashFile = path.join(dir, 'source-hashes.json');
if (fs.existsSync(hashFile)) {
  const recordedHashes = JSON.parse(fs.readFileSync(hashFile, 'utf8'));
  for (const file of sources) {
    if (recordedHashes[file] !== sourceHashes[file]) throw new Error(`Reviewed source changed: ${file}`);
  }
} else {
  fs.writeFileSync(hashFile, JSON.stringify(sourceHashes, null, 2) + '\n');
}
const environmentFile = path.join(dir, 'environment.json');
if (!fs.existsSync(environmentFile)) {
  fs.writeFileSync(environmentFile, JSON.stringify({ platform: os.platform(), release: os.release(), architecture: os.arch(),
    processor: os.cpus()[0]?.model, logicalProcessors: os.cpus().length, memoryBytes: os.totalmem(),
    java: '25+37-LTS-3491', compileRelease: 21, dependency: 'abacus-common 8.2.0',
    benchmarkJvmFlags: ['-Xms256m', '-Xmx1024m', '-Xbatch'] }, null, 2) + '\n');
}
for (const folder of [dir, path.join(dir, 'initial-80ms-warmup')]) {
  for (const name of fs.readdirSync(folder).filter(name => /\.(log|txt)$/.test(name))) {
    const file = path.join(folder, name);
    const bytes = fs.readFileSync(file);
    if (bytes[0] === 0xff && bytes[1] === 0xfe) {
      fs.writeFileSync(file, bytes.toString('utf16le').replace(/^\uFEFF/, '').replace(/\r\n/g, '\n'));
    }
  }
}
console.log(`Validated ${summary.length} cases, 3 forks, 7 rounds: ${summary.length * 21} samples.`);
for (const s of summary) {
  if (/^(early_hash|walk_|multiline_|raw_bracket|quoted_literal|comment_|mode_|ordinary_parentheses|positional|unrecognized_ddl)/.test(s.name)) {
    console.log(`${s.name.padEnd(31)} ${String(s.scale).padStart(7)} ${s.ns.toFixed(1).padStart(13)} ns ${s.bytes.toFixed(0).padStart(12)} B`);
  }
}
