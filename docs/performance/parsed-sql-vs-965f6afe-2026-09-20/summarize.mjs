import { readFile, writeFile } from 'node:fs/promises';
import { resolve, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const directory = resolve(process.argv[2] ?? fileURLToPath(new URL('.', import.meta.url)));
const median = values => {
  const sorted = [...values].sort((a, b) => a - b);
  return (sorted[Math.floor((sorted.length - 1) / 2)] + sorted[Math.floor(sorted.length / 2)]) / 2;
};
const versions = { baseline: 5, current: 5, baseline_common8: 3 };
const byCase = new Map();
const forkRows = [];
let totalSamples = 0;
for (const [version, forks] of Object.entries(versions)) {
  for (let fork = 1; fork <= forks; fork++) {
    const lines = (await readFile(join(directory, `${version}-${fork}.csv`), 'utf8')).trim().split(/\r?\n/);
    const header = lines.shift().split(',');
    const groups = new Map();
    for (const line of lines) {
      const row = Object.fromEntries(line.split(',').map((value, i) => [header[i], value]));
      if (row.version !== version || Number(row.fork) !== fork || Number(row.errors) !== 0) {
        throw new Error(`Unexpected raw result: ${line}`);
      }
      if (version === 'current' && (row.semantic_match !== 'true' || row.min_count !== row.expected || row.max_count !== row.expected)) {
        throw new Error(`Current correctness check failed: ${line}`);
      }
      const key = `${row.shape}/${row.size}`;
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key).push(row);
      totalSamples++;
    }
    if (groups.size !== 40) throw new Error(`Expected 40 cases: ${version}/${fork}`);
    for (const [key, rows] of groups) {
      if (rows.length !== 9 || new Set(rows.map(row => row.round)).size !== 9) throw new Error(`Missing rounds: ${key}`);
      const first = rows[0];
      const summary = { version, fork, shape: first.shape, size: Number(first.size),
        median_ns: median(rows.map(row => Number(row.ns_per_op))),
        median_bytes: median(rows.map(row => Number(row.bytes_per_op))),
        expected: Number(first.expected), min_count: Math.min(...rows.map(row => Number(row.min_count))),
        max_count: Math.max(...rows.map(row => Number(row.max_count))),
        semantic_match: rows.every(row => row.semantic_match === 'true') };
      forkRows.push(summary);
      if (!byCase.has(key)) byCase.set(key, {});
      const group = byCase.get(key);
      if (!group[version]) group[version] = [];
      group[version].push(summary);
    }
  }
}
const summaryRows = [];
for (const [key, versionsForCase] of byCase) {
  const baseline = versionsForCase.baseline, current = versionsForCase.current, control = versionsForCase.baseline_common8;
  const ns = rows => median(rows.map(row => row.median_ns));
  const bytes = rows => median(rows.map(row => row.median_bytes));
  const summary = { shape: baseline[0].shape, size: baseline[0].size,
    equivalent_results: [...baseline, ...current, ...control].every(row => row.semantic_match),
    baseline_ns: ns(baseline), current_ns: ns(current), time_change_pct: (ns(current) / ns(baseline) - 1) * 100,
    baseline_bytes: bytes(baseline), current_bytes: bytes(current),
    baseline_min_ns: Math.min(...baseline.map(row => row.median_ns)), baseline_max_ns: Math.max(...baseline.map(row => row.median_ns)),
    current_min_ns: Math.min(...current.map(row => row.median_ns)), current_max_ns: Math.max(...current.map(row => row.median_ns)),
    baseline_common8_ns: ns(control), baseline_common8_bytes: bytes(control),
    current_vs_common8_pct: (ns(current) / ns(control) - 1) * 100,
    baseline_common8_min_ns: Math.min(...control.map(row => row.median_ns)), baseline_common8_max_ns: Math.max(...control.map(row => row.median_ns)),
    baseline_count: baseline[0].min_count, current_count: current[0].min_count };
  summaryRows.push(summary);
}
summaryRows.sort((a, b) => a.shape.localeCompare(b.shape) || a.size - b.size);
const csv = rows => Object.keys(rows[0]).join(',') + '\n' + rows.map(row => Object.values(row)
  .map(value => typeof value === 'number' && !Number.isInteger(value) ? value.toFixed(3) : value).join(',')).join('\n') + '\n';
await writeFile(join(directory, 'fork-summary.csv'), csv(forkRows));
await writeFile(join(directory, 'summary.csv'), csv(summaryRows));
console.log(`Validated ${totalSamples} samples from 13 JVMs; generated ${summaryRows.length} case summaries.`);
for (const row of summaryRows.filter(row => row.size <= 128 || row.size === 8192)) console.log(JSON.stringify(row));
