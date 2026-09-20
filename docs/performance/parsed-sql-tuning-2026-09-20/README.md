# ParsedSql performance tuning — 2026-09-20

This records tuning against commit 965f6afef86d928fbff63589fb3d59edc322574e while retaining the current parser's binding rules. The pre-tuning snapshot is commit 417ab8a8a4c038b041b63592c99ee215ac7da686. No public parser contract was intentionally changed.

The tuned version has lower median latency in **all 40 cases** under the longer-sample protocol, across five fresh JVM forks per primary version. All 36 uncached cases also improve against the old source using the same abacus-common 8.0.0 dependency. This establishes the measured target; it does not establish superiority for every possible SQL string or execution environment.

| Workload | 965f6afe | Before tuning | Tuned | Less time vs 965f6afe |
|---|---:|---:|---:|---:|
| 2 positional bindings | 1.926 µs | 2.975 µs | 1.430 µs | 25.7% |
| 16 positional bindings | 11.170 µs | 14.943 µs | 5.869 µs | 47.5% |
| 128 positional bindings | 85.412 µs | 105.605 µs | 41.334 µs | 51.6% |
| 16 named bindings | 12.544 µs | 12.090 µs | 5.812 µs | 53.7% |
| 16 MyBatis bindings | 17.957 µs | 18.771 µs | 10.746 µs | 40.2% |
| JSON existence operator | 1.724 µs | 2.510 µs | 1.468 µs | 14.9% |
| Quoted text and comments | 1.942 µs | 2.932 µs | 1.698 µs | 12.5% |
| 256 positional array bindings † | 2.965 µs | 53.983 µs | 2.304 µs | 22.3% |
| 8,192 positional array bindings † | 84.055 µs | 1.749 ms | 63.874 µs | 24.0% |
| 8,192 MyBatis array bindings | 151.875 ms | 1.900 ms | 778.300 µs | 99.5% |
| 8,192 nested unary-expression groups | 1.896 ms | 2.535 ms | 856.425 µs | 54.8% |

† The old commit reports zero bindings for these positional arrays. Nine of the 40 cases have different old/new results: four positional-array sizes, four compact-concatenation sizes, and the SQL/JSON constructor. All **31 equivalent-result cases** also improve. Both the tuned and pre-tuning versions pass every benchmark correctness check.

**The original, shorter-sample protocol remains 39/40.** Its 256-element positional array is 4.259 µs tuned versus 2.919 µs old (+45.9%). With the longer warmup/sample protocol it is 2.304 µs versus 2.965 µs (−22.3%). The first protocol times only 32 parses per round for that case; the longer samples and warmup expose a different result. Both are retained rather than selecting or hiding individual rounds.

Ordinary positional queries take 26–52% less time than the exact old commit in the longer-sample run (16–46% less under the original protocol). Cache hits remain approximately 12–17 ns with zero allocation and unchanged cache code. Against the same-dependency control, the two positional cache medians are 0.16% and 1.53% slower; this is not evidence of a cache optimization or regression. Fork ranges are in the CSV summaries.

Allocation improves against the pre-tuning parser but does not uniformly beat the old implementation. Two positional bindings allocate 1,624 B (pre-tuning 1,872 B; old 1,456 B); sixteen allocate 5,680 B (6,128 B; 5,224 B). The 8,192-binding positional array drops from approximately 3.57 MB before tuning to 0.214 MB while retaining all binding positions. The MyBatis array drops from 2.75 MB to 2.16 MB; the old quadratic implementation allocates approximately 1.81 GB.

The two final protocols comprise **12,960 samples**. See [longer-sample results](final-steady/summary.csv), [original-protocol results](final-original/summary.csv), their per-fork summaries and raw CSVs. The complete production/test patch against the pre-tuning commit is [implementation.patch](implementation.patch).


## Implementation

- Tokenizer multi-character separators are indexed by their first character and matched longest-first. Impossible quote-crossing candidates are filtered when the immutable configuration is constructed.
- Quoted tokens are copied as ranges. Closing-quote searches inspect only the immediately preceding backslash run; doubled delimiters, bracket rules and unterminated strings are preserved. Disjoint backslash runs keep the scan linear.
- Hash-comment memo allocation is skipped when the original SQL has no hash character. Hash context checks cannot reach the absent memo in that case.
- The question-mark classifier defers operand checks until an actual question-mark token needs them. Keyword recognition dispatches by word length; quoted and punctuation-led operands bypass keyword comparisons.
- Named-marker storage and positional bookkeeping are allocated only when needed. Ordinary positional offsets use raw character positions only after classification proves every raw question mark binds. Otherwise the original source-alignment rules apply.
- Bracket groups retain one owned offset array instead of storing their token index once per binding. Source alignment merges ordinary and bracket positions directly.
- Plain comma-separated bracket bindings use a strict scanner. Operators, comments, quoted values, nested groups and malformed list syntax go through the shared classifier. Backslash-free tokens need one quote interpretation; ambiguous tokens retain both.
- MyBatis opener positions are filtered in their owned array. Quote-before-bracket checks inspect only the prefix, avoiding repeated full-array scans.

These changes remove repeated work and temporary storage. They do not remove the geometric, SQL/JSON, pgJDBC escape or bracket ambiguity rules introduced after the old commit.

## Validation

All 81 production and 86 test source files compiled with JDK 25 and --release 17. The full JUnit suite passed: **5,278 tests, zero failures, zero skips**. Ten new test methods cover the optimized paths and their fallbacks. Both changed production files passed Javadoc doclint. The four edited Java files retain CRLF, and git diff --check passed.

The test runner reports JUnit failures directly because this repository configures Maven to ignore test failures. The Windows sandbox requires JUnit temporary directories inside the workspace and cleanup.mode.default=NEVER; temporary-directory assertions still ran. See full-tests.txt, compile-production.txt, compile-tests.txt and javadoc.txt.

TuningDifferential.java compares **42,000 deterministic probes** with the pre-tuning jar: 24,000 tokenizer cases across three configurations and 18,000 expression/parameter cases. Token text, scanner positions, case-sensitive/insensitive token search, parameter counts, names, rendered SQL, original positional offsets and rejection messages matched exactly. See differential-summary.json. This is behavioral regression evidence, not a claim that every existing parser limitation is resolved.

## Measurement protocol

Windows 10.0.26100; AMD Ryzen 7 PRO 7840U (16 logical processors); approximately 64 GB visible RAM; JDK 25+37. Every JVM uses -Xms512m -Xmx1024m -XX:+UseG1GC. Only one benchmark JVM runs at a time. No compilation or test suite runs alongside the final benchmarks.

Each protocol has five fresh JVM forks per primary version, plus three control forks running the old source with the current abacus-common 8.0.0 dependency. The exact old version uses its original abacus-common 7.9.3 dependency. All production classes are from the respective snapshot. manifest.json records source and jar hashes.

The harness exercises 40 cases: 36 uncached SQL shapes/sizes and four cache-hit cases. Uncached inputs vary their alias to prevent hits; generation and cache clearing are outside the timed region. Results consume count, rendered SQL length and name count. Each case has nine shuffled measured rounds. Summaries are medians of per-fork medians; fork ranges and thread-allocated bytes are retained. Correctness assertions apply to the tuned version; the summary also rejects any pre-tuning mismatch.

- **final-original/** preserves the previous benchmark protocol, including its short stress samples of 2–32 operations. This permits direct comparison with the earlier report.
- **final-steady/** adds four calibrated warmup batches per stress case and targets 10 ms per stress sample using a pilot measurement (actual durations vary as compilation and GC state change), capped at 8,192 operations. Calibration applies identically to each version. The slow old MyBatis cases keep the minimum operation count. Non-stress and cache-hit measurements use the original protocol.
- **first-pass/** retains the raw CSVs and summaries from the first full tuning pass, before the final offset, operand and memo optimizations. They are exploratory evidence; use the final subdirectories for the checked-in implementation.

The longer samples address noise observed in sub-0.1-ms stress rounds. Neither protocol is a guarantee about every SQL string, workload, machine, dependency or JVM. Cache-hit code is unchanged; nanosecond-scale differences should not be interpreted as a cache optimization. Rows marked equivalent_results=false identify old versions that miscount bindings; their timing is shown but they perform different work.

Initial diagnostic profiling used Java stack sampling, which is safepoint-biased. It pointed to separator matching, repeated operand checks and offset resolution. JFR could not start in the sandbox, so no JFR CPU or allocation attribution is claimed. Allocation figures come from ThreadMXBean in the benchmark.

## Reproduction

The prepared workspace uses .audit/parsed-sql-vs-965f6afe for the exact old and pre-tuning jars/classpath files, and .audit/parsed-sql-tuning for final.jar and the compiled harness. Snapshot preparation is described in ../parsed-sql-vs-965f6afe-2026-09-20/README.md. Compile the current production tree with --release 17 and abacus-common 8.0.0 into final.jar; compile ParserBenchmark.java into .audit/parsed-sql-tuning/harness.

Run (substitute the installed Java path):

    node docs/performance/parsed-sql-tuning-2026-09-20/run-benchmarks.mjs .audit/parsed-sql-tuning .audit/parsed-sql-vs-965f6afe docs/performance/parsed-sql-tuning-2026-09-20/final-original java original
    node docs/performance/parsed-sql-tuning-2026-09-20/run-benchmarks.mjs .audit/parsed-sql-tuning .audit/parsed-sql-vs-965f6afe docs/performance/parsed-sql-tuning-2026-09-20/final-steady java steady
    node docs/performance/parsed-sql-tuning-2026-09-20/summarize.mjs docs/performance/parsed-sql-tuning-2026-09-20/final-original
    node docs/performance/parsed-sql-tuning-2026-09-20/summarize.mjs docs/performance/parsed-sql-tuning-2026-09-20/final-steady

To repeat the differential check, run TuningDifferential.java once with the pre-tuning jar and once with final.jar (both with current dependencies), passing a different output filename each time; compare the output files byte-for-byte. It uses a fixed seed and writes one deterministic record per probe.
