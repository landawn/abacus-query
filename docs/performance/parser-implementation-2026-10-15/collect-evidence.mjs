import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const dir = path.dirname(fileURLToPath(import.meta.url));
const audit = '.audit/parser-implementation-2026-10-15';
const decode = file => {
  const bytes = fs.readFileSync(file);
  return bytes.toString(bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf16le' : 'utf8')
    .replace(/^\uFEFF/, '').replace(/\r\n/g, '\n');
};
const sha = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const write = (name, value) => fs.writeFileSync(path.join(dir, name), JSON.stringify(value, null, 2) + '\n');
const sources = ['src/main/java/com/landawn/abacus/query/SqlParser.java', 'src/main/java/com/landawn/abacus/query/ParsedSql.java',
  'src/test/java/com/landawn/abacus/query/SqlParserTest.java', 'src/test/java/com/landawn/abacus/query/ParsedSqlTest.java', 'pom.xml'];
const hashes = Object.fromEntries(sources.map(file => [file, sha(file)]));
if (fs.existsSync(path.join(dir, 'source-hashes.json'))) {
  const previous = JSON.parse(fs.readFileSync(path.join(dir, 'source-hashes.json'), 'utf8'));
  for (const file of sources) if (previous[file] !== hashes[file]) throw new Error(`Validated source changed: ${file}`);
} else {
  write('source-hashes.json', hashes);
}

const corpora = [['tuning', 42000], ['classifier', 24000], ['edges', 35530]].map(([name, expected]) => {
  const before = `${audit}/${name}-before.tsv`, after = `${audit}/${name}-after.tsv`;
  const records = decode(after).trimEnd().split('\n').length;
  if (sha(before) !== sha(after) || records !== expected) throw new Error(`Compatibility failed for ${name}`);
  return { name, records, byteIdentical: true, sha256: sha(after) };
});
const xml = decode(`${audit}/test-reports/TEST-junit-platform-suite.xml`);
const suiteTag = xml.match(/<testsuite\b[^>]*>/)?.[0];
const attributes = Object.fromEntries([...suiteTag.matchAll(/(\w+)="([^"]*)"/g)].map(m => [m[1], m[2]]));
if (attributes.tests !== '6252' || attributes.failures !== '0' || attributes.errors !== '0' || attributes.skipped !== '0') {
  throw new Error(`Unexpected suite result ${JSON.stringify(attributes)}`);
}
if (!decode(`${audit}/edges-after.log`).includes('cursorChecked=true')) throw new Error('Cursor equivalence was not checked');
if (decode(`${audit}/javadoc.log`).trim()) throw new Error('Inspect Javadoc diagnostics');
const layout = decode(`${audit}/test-layout.log`).trim();
if (!layout.includes('6004 test methods in 88 files: 0 layout issues')) throw new Error('Test layout check failed');
write('validation.json', {
  baselineCommit: '33b655b87de09b33c4cad4a711956f0b93ff266a',
  productionSources: 82, testSources: 88, baselineTestInvocations: 6228,
  implementationTestInvocations: +attributes.tests, newRegressionMethods: 24,
  failures: 0, errors: 0, skipped: 0,
  corpora, totalCompatibilityRecords: corpora.reduce((n, c) => n + c.records, 0),
  cursorEquivalentToStatelessWalks: true, javadocDiagnostics: 0, layout,
  testRunner: 'JUnit Platform ConsoleLauncher 6.1.3, AbacusQueryTestSuite, assertions enabled',
  testLayoutMethod: 'javac AST fallback; installed Python launcher had no Python runtime',
  compileRelease: 21, runtime: 'Oracle JDK 25+37-LTS-3491'
});
for (const [source, target] of [['memory-after.log', 'memory-after.txt'], ['remask-after.log', 'remask-after.txt'],
    ['test-layout.log', 'test-layout.txt'], ['api-docs.log', 'api-docs.txt']]) {
  fs.writeFileSync(path.join(dir, target), decode(`${audit}/${source}`));
}
fs.copyFileSync('docs/performance/parser-review-2026-10-15/environment.json', path.join(dir, 'environment.json'));
console.log('Validated 6252 passing tests, 101530 byte-identical records, cursor equivalence, Javadoc and test layout.');
