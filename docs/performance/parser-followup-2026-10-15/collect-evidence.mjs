import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const dir = path.dirname(fileURLToPath(import.meta.url));
const audit = '.audit/parser-followup-2026-10-15';
const previousAudit = '.audit/parser-implementation-2026-10-15';
const decode = file => {
    const bytes = fs.readFileSync(file);
    return bytes.toString(bytes[0] === 0xff && bytes[1] === 0xfe ? 'utf16le' : 'utf8')
        .replace(/^\uFEFF/, '').replace(/\r\n/g, '\n');
};
const sha = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const write = (name, value) => fs.writeFileSync(path.join(dir, name), JSON.stringify(value, null, 2) + '\n');
const sources = ['src/main/java/com/landawn/abacus/query/SqlParser.java', 'src/main/java/com/landawn/abacus/query/ParsedSql.java',
    'src/test/java/com/landawn/abacus/query/SqlParserTest.java', 'src/test/java/com/landawn/abacus/query/ParsedSqlTest.java', 'pom.xml',
    'docs/performance/parser-followup-2026-10-15/FollowupDifferential.java', 'docs/performance/parser-followup-2026-10-15/FollowupProbe.java'];
const hashes = Object.fromEntries(sources.map(file => [file, sha(file)]));
if (fs.existsSync(path.join(dir, 'source-hashes.json'))) {
    const previous = JSON.parse(fs.readFileSync(path.join(dir, 'source-hashes.json'), 'utf8'));
    for (const file of sources) if (previous[file] !== hashes[file]) throw new Error(`Validated source changed: ${file}`);
} else {
    write('source-hashes.json', hashes);
}

const corpora = [['tuning', 42000], ['classifier', 24000], ['edges', 35530], ['expanded', 28564]].map(([name, expected]) => {
    const before = name === 'expanded' ? `${audit}/expanded-baseline.tsv` : `${previousAudit}/${name}-before.tsv`;
    const current = `${audit}/${name}-current.tsv`;
    const records = decode(current).trimEnd().split('\n').length;
    if (sha(before) !== sha(current) || records !== expected) throw new Error(`Compatibility failed for ${name}`);
    return { name, records, byteIdentical: true, sha256: sha(current) };
});
const expandedOriginal = decode(`${audit}/expanded-baseline.tsv`).trimEnd().split('\n');
const expandedPrevious = decode(`${audit}/expanded-previous.tsv`).trimEnd().split('\n');
if (expandedOriginal.length !== expandedPrevious.length) throw new Error('Previous corpus record count differs');
const previousMismatches = expandedOriginal.filter((line, i) => line !== expandedPrevious[i]).length;
if (previousMismatches !== 240) throw new Error(`Unexpected previous regression count: ${previousMismatches}`);
const xml = decode(`${audit}/test-reports/TEST-junit-platform-suite.xml`);
const suiteTag = xml.match(/<testsuite\b[^>]*>/)?.[0];
const attributes = Object.fromEntries([...suiteTag.matchAll(/(\w+)="([^"]*)"/g)].map(m => [m[1], m[2]]));
if (attributes.tests !== '6261' || attributes.failures !== '0' || attributes.errors !== '0' || attributes.skipped !== '0') {
    throw new Error(`Unexpected suite result ${JSON.stringify(attributes)}`);
}
if (!decode(`${audit}/edges-current.log`).includes('cursorChecked=true')) throw new Error('Cursor equivalence was not checked');
if (decode(`${audit}/javadoc.log`).trim()) throw new Error('Inspect Javadoc diagnostics');
const layout = decode(`${audit}/test-layout.log`).trim();
if (!layout.includes('6013 test methods in 88 files: 0 layout issues')) throw new Error('Test layout check failed');
if (!decode(`${audit}/remask-current.log`).includes('PASS: no-hash remasking changes dollar-quote boundaries')) throw new Error('Remask safeguard check failed');
if (sha(`${audit}/staged-before.patch`) !== sha(`${audit}/staged-after.patch`)) throw new Error('The staged index changed');
write('validation.json', {
    baselineCommit: '33b655b87de09b33c4cad4a711956f0b93ff266a',
    productionSources: 82, testSources: 88,
    previousTestInvocations: 6252, currentTestInvocations: +attributes.tests, newRegressionMethods: 9,
    failures: 0, errors: 0, skipped: 0,
    corpora, totalCompatibilityRecords: corpora.reduce((n, c) => n + c.records, 0),
    expandedCorpusMismatchesPrevious: previousMismatches, expandedCorpusMismatchesCurrent: 0,
    cursorEquivalentToStatelessWalks: true, javadocDiagnostics: 0, layout, stagedIndexUnchanged: true,
    testRunner: 'JUnit Platform ConsoleLauncher 6.1.3, AbacusQueryTestSuite, assertions enabled; existing slow-test tag excluded',
    testLayoutMethod: 'javac AST fallback; installed Python launcher had no Python runtime',
    compileRelease: 21, runtime: 'Oracle JDK 25+37-LTS-3491'
});
for (const [source, target] of [['remask-current.log', 'remask-current.txt'], ['test-layout.log', 'test-layout.txt'], ['api-docs.log', 'api-docs.txt']]) {
    fs.writeFileSync(path.join(dir, target), decode(`${audit}/${source}`));
}
const suiteLog = decode(`${audit}/full-suite.log`);
const summaryStart = suiteLog.lastIndexOf('Test run finished');
if (summaryStart < 0) throw new Error('Missing JUnit summary');
const summaryLines = suiteLog.substring(summaryStart).split('\n');
const summaryEnd = summaryLines.findIndex(line => /\btests failed\b/.test(line));
if (summaryEnd < 0) throw new Error('Incomplete JUnit summary');
fs.writeFileSync(path.join(dir, 'test-summary.txt'), summaryLines.slice(0, summaryEnd + 1).join('\n') + '\n');
fs.copyFileSync('docs/performance/parser-review-2026-10-15/environment.json', path.join(dir, 'environment.json'));
for (const name of fs.readdirSync(dir).filter(name => /^(original|previous|current)-\d\.log$/.test(name))) {
    fs.writeFileSync(path.join(dir, name), decode(path.join(dir, name)));
}
console.log('Validated 6261 passing tests, 130094 byte-identical records, cursor equivalence, Javadoc, test layout and unchanged staged index.');
