import { spawn } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { resolve, join } from 'node:path';
import { fileURLToPath } from 'node:url';

// Prepared work directory contains baseline.jar, current.jar, harness/, and the two classpath text files.
const work = resolve(process.argv[2] ?? '.audit/parsed-sql-vs-965f6afe');
const java = process.argv[3] ?? 'java';
const output = resolve(process.argv[4] ?? fileURLToPath(new URL('.', import.meta.url)));
await mkdir(output, { recursive: true });
const dependencies = {
  baseline: (await readFile(join(work, 'baseline-classpath.txt'), 'utf8')).trim(),
  current: (await readFile(join(work, 'current-classpath.txt'), 'utf8')).trim()
};
const runs = [
  ['baseline', 1], ['current', 1], ['baseline_common8', 1],
  ['current', 2], ['baseline_common8', 2], ['baseline', 2],
  ['baseline_common8', 3], ['baseline', 3], ['current', 3],
  ['current', 4], ['baseline', 4], ['baseline', 5], ['current', 5]
];
const status = { started: new Date().toISOString(), completed: [] };
for (const [version, fork] of runs) {
  const library = version === 'current' ? 'current' : 'baseline';
  const deps = dependencies[version === 'baseline' ? 'baseline' : 'current'];
  const separator = process.platform === 'win32' ? ';' : ':';
  const classpath = [join(work, 'harness'), join(work, library + '.jar'), deps].join(separator);
  const started = new Date().toISOString();
  console.log(`Starting ${version} fork ${fork} at ${started}`);
  const result = await new Promise((accept, reject) => {
    const child = spawn(java, ['-Xms512m', '-Xmx1024m', '-XX:+UseG1GC', '-cp', classpath,
      'ParserBenchmark', version, String(fork)], { windowsHide: true });
    let stdout = '', stderr = '';
    child.stdout.on('data', data => { stdout += data.toString(); });
    child.stderr.on('data', data => { stderr += data.toString(); });
    child.on('error', reject);
    child.on('close', code => accept({ code, stdout, stderr }));
  });
  await writeFile(join(output, `${version}-${fork}.csv`), result.stdout);
  await writeFile(join(output, `${version}-${fork}.stderr.txt`), result.stderr);
  status.completed.push({ version, fork, started, finished: new Date().toISOString(), code: result.code });
  await writeFile(join(output, 'run-status.json'), JSON.stringify(status, null, 2) + '\n');
  if (result.code !== 0) throw new Error(`Failed: ${version} fork ${fork}: ${result.stderr}`);
}
