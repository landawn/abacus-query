import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = path.dirname(fileURLToPath(import.meta.url));
const median = values => [...values].sort((a, b) => a - b)[Math.floor(values.length / 2)];
const percentChange = (current, previous) => previous === 0 ? (current === 0 ? 0 : null) : 100 * (current / previous - 1);
const variants = {};
for (const variant of ['original', 'previous', 'current']) {
    const cases = new Map();
    for (let fork = 1; fork <= 3; fork++) {
        const file = path.join(dir, `${variant}-${fork}.csv`);
        const lines = fs.readFileSync(file, 'utf8').trim().split(/\r?\n/);
        if (lines.shift() !== 'fork,case,scale,sql_chars,round,operations,ns_per_op,bytes_per_op,result') {
            throw new Error(`Unexpected CSV header: ${file}`);
        }
        const groups = new Map();
        for (const line of lines) {
            const [f, name, scale, chars, round, operations, ns, bytes, result] = line.split(',');
            if (+f !== fork || !Number.isFinite(+ns) || !Number.isFinite(+bytes) || +operations <= 0) {
                throw new Error(`Invalid measurement: ${file}: ${line}`);
            }
            const key = `${name}/${scale}`;
            if (!groups.has(key)) groups.set(key, []);
            groups.get(key).push({ ns: +ns, bytes: +bytes, round: +round, result, chars: +chars });
        }
        for (const [key, rows] of groups) {
            if (rows.length !== 7 || new Set(rows.map(r => r.round)).size !== 7 || new Set(rows.map(r => r.result)).size !== 1) {
                throw new Error(`Incomplete or unstable case: ${file}: ${key}`);
            }
            if (!cases.has(key)) cases.set(key, { chars: rows[0].chars, result: rows[0].result, forks: [] });
            const value = cases.get(key);
            if (value.result !== rows[0].result || value.chars !== rows[0].chars) throw new Error(`Fork mismatch: ${key}`);
            value.forks.push({ ns: median(rows.map(r => r.ns)), bytes: median(rows.map(r => r.bytes)) });
        }
    }
    variants[variant] = Object.fromEntries([...cases].map(([key, value]) => {
        if (value.forks.length !== 3) throw new Error(`Missing forks: ${variant}: ${key}`);
        return [key, { ...value, ns: median(value.forks.map(f => f.ns)), bytes: median(value.forks.map(f => f.bytes)) }];
    }));
}
const comparison = {};
for (const [key, current] of Object.entries(variants.current)) {
    const previous = variants.previous[key];
    if (!previous || current.result !== previous.result || current.chars !== previous.chars) throw new Error(`Variant mismatch: ${key}`);
    const original = variants.original[key];
    if (original && (original.result !== current.result || original.chars !== current.chars)) throw new Error(`Original mismatch: ${key}`);
    comparison[key] = {
        chars: current.chars, result: current.result,
        previous_ns: previous.ns, current_ns: current.ns, speedup: previous.ns / current.ns,
        previous_bytes: previous.bytes, current_bytes: current.bytes,
        allocation_change_percent: percentChange(current.bytes, previous.bytes),
        ...(original ? { original_ns: original.ns, original_bytes: original.bytes,
            allocation_vs_original_percent: percentChange(current.bytes, original.bytes) } : {})
    };
}
fs.writeFileSync(path.join(dir, 'summary.json'), JSON.stringify({ method: 'Median of seven rounds in each of three forks; median of the three fork medians.', variants, comparison }, null, 2) + '\n');
const header = 'case,scale,sql_chars,previous_ns,current_ns,speedup,original_bytes,previous_bytes,current_bytes,allocation_change_percent,allocation_vs_original_percent';
const csv = [header];
for (const [key, row] of Object.entries(comparison)) {
    const [name, scale] = key.split('/');
    csv.push([name, scale, row.chars, row.previous_ns, row.current_ns, row.speedup.toFixed(3), row.original_bytes ?? '',
        row.previous_bytes, row.current_bytes, row.allocation_change_percent?.toFixed(3) ?? '', row.allocation_vs_original_percent?.toFixed(3) ?? ''].join(','));
}
fs.writeFileSync(path.join(dir, 'summary.csv'), csv.join('\n') + '\n');
console.log(`Summarized ${Object.keys(comparison).length} before/after cases and ${Object.keys(variants.original).length} original allocation references.`);
