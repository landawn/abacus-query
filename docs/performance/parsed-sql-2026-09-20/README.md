# ParsedSql before/after performance comparison

Measured locally on September 20, 2026. Production source and tests were not edited for this comparison.

The fixes add measurable cost to uncached ordinary positional queries and array expressions. Cached calls remain about 13 ns with zero measured allocation. Bare compact concatenation becomes substantially faster while correcting the missed binding. Both implementations show approximately linear scaling over the measured sizes.

**Status: performance tuning deferred at the user's request.** These measurements are the recorded starting point for that work. Linear scaling alone does not establish that the fixes preserved performance: ordinary uncached positional queries became 20-34% slower in these tests, and the 32,000-element array cases became 15-16% slower.

## Baseline provenance

The original `target/abacus-query-4.9.3.jar` and matching sources archive were built at 00:15:35/36 local time, before the latest three fixes. Before benchmarking, comparison of the sources archive against the current production tree found **only ParsedSql.java differed**, by 317 additions and 65 deletions. Another process ran Maven clean at approximately 00:47, deleting those artifacts and the extracted source.

The measured **before** version is therefore a **reconstructed behavioral baseline, not a recovered original artifact**. The six executable changes retained from that source comparison were reversed in an isolated source file: ordinary marker branches, bracket-local classification, lexical-neighbor operator recognition, the global JSON RHS keyword filter, the new shared classifier/helpers, and builder-allocation order. Current comments/Javadocs that do not affect bytecode were retained. The reconstruction reproduces the three reported failures.

The local reconstruction artifacts remain under the ignored `.audit/perf-compare/` directory: `reconstruct-before.py` (literal reversal specification), `reconstruct-before.cjs` (runnable Node replay), `before-src/com/landawn/abacus/query/ParsedSql.java`, and `reconstructed-before-manifest.json`. Those artifacts are not included in this durable results record; do not assume they will exist in a fresh checkout.

Current production source SHA-256: `eabbe8b439b39091cfab4081f3ff0b26f46febdc28bf7f97b0506e0904efe6dc`.

Reconstructed baseline source SHA-256: `8253277056001c2c8593ea2c93876512b85b098a2cf4c0bc802d62b04c9234e5`.

## Method

- Same Java 25 runtime, `javac --release 17 -proc:none` for both parser versions, and existing abacus-common 8.0.0 dependencies.
- Separate copies of the same production classes; only ParsedSql's executable implementation differs. No database or SQL execution is involved.
- Three fresh JVM forks per version, with identical flags: `-Xms512m -Xmx1024m -XX:+UseG1GC`. Run order: before1, current1, current2, before2, before3, current3. Processes ran sequentially.
- Each fork warms every case first: 10,000 parses per short-query case and 30 per stress size/case. Nine measured rounds follow, with deterministically shuffled case order.
- Unique SQL per uncached parse; input construction and cache clearing occur outside the timed interval. Small cases use batches of 1,000 parses (250 for sixteen bindings); stress batches use max(1, 16000 / size). JVM code and resource pools are warm.
- Timed work includes `ParsedSql.parse`, parameter-count inspection, and a small result-consumption step. SQL generation, startup, compilation, and cache clearing are excluded.
- Reported time is the median of the three per-fork medians. Fork ranges and all sizes are in `summary.csv`; the six raw fork CSVs retain counts, errors and medians.
- Allocation is measured with `ThreadMXBean.getThreadAllocatedBytes` for the benchmark thread. It is allocated bytes per operation, not retained heap or background-thread allocation.
- Cached calls reuse the same SQL string after warmup, with nine batches of 500,000 calls per fork. The expected count is asserted before timing.
- These are local comparative microbenchmarks, not JMH results or application throughput measurements. They do not provide a statistical confidence interval or tail-latency estimate.

## Ordinary queries

Times are microseconds per operation except the cache-hit row.

| Case | Before | Current | Observed time change | Allocated bytes before / current |
|---|---:|---:|---:|---:|
| No parameters | 1.491 us | 1.375 us | -7.8% | 1472 / 1472 |
| Two named parameters | 2.391 us | 2.154 us | -9.9% | 2312 / 2312 |
| Two positional parameters | 2.156 us | 2.890 us | +34.0% | 1672 / 1800 |
| Sixteen positional parameters | 11.570 us | 13.919 us | +20.3% | 6104 / 6376 |
| Cache hit, two positional parameters | 13.5 ns | 12.8 ns | approximately unchanged | 0 / 0 |

For the two-position query, before fork medians range from 2.090 to 2.175 us; current medians range from 2.758 to 2.897 us. For sixteen positions, the respective ranges are 11.060-13.508 us and 13.537-13.983 us. The ordinary positional overhead is visible across the measured forks.

## Stress inputs: 32,000 elements or nesting levels

Times are milliseconds per uncached parse. Rows marked with an asterisk change the correctness of the result, so their timing ratios compare different behavior.

| Shape | Before | Current | Observed time change | Before / current binding counts |
|---|---:|---:|---:|---:|
| Bare compact concatenation* | 7.533 ms | 3.599 ms | -52.2% | 31999 / 32000 |
| Array concatenation | 3.647 ms | 4.187 ms | +14.8% | 32000 / 32000 |
| Array addition | 6.260 ms | 7.259 ms | +15.9% | 32000 / 32000 |
| Repeated unary CAST* | 41.263 ms | 50.889 ms | +23.3% | 64000 / 32000 |
| Deep unary nesting* | 7.713 ms | 10.102 ms | +31.0% | 2 / 1 |

All current counts are correct. Neither version overflowed the stack at the measured nesting depths. The array cases allocate only about 120 extra bytes per 32,000-element parse in the median measurements. Bare concatenation adds about 509 KB and deep unary nesting adds about 518 KB, consistent with additional linear index storage.

## JSON correctness cases

- `SELECT payload ? NULL ...`: before reports one binding; current correctly reports zero. Median times are 1.329 us and 1.573 us, respectively.
- `SELECT payload ? format('%s', :key) ...`: before throws a mixed-style exception on every measured attempt; current succeeds with one binding. Exception timing is not a successful-parse baseline, so no speedup claim is made.

Every current-version measured row passed its expected parameter-count and zero-error checks. Baseline counts were consistent across all three forks. The six JVM runs all exited successfully.

## Recorded artifacts and reproduction

This directory preserves the [benchmark harness](ParserBenchmark.java), [summary with fork ranges and allocations](summary.csv), and all six per-fork result files: [before 1](before-1.csv), [before 2](before-2.csv), [before 3](before-3.csv), [current 1](current-1.csv), [current 2](current-2.csv), [current 3](current-3.csv). The CSV files record each fork's nine-round median, not individual round timings.

The original local execution scripts, dependency classpath, and copied class directories remain under `.audit/perf-compare/`. In that prepared directory, `compile.ps1`, `run-benchmarks.ps1`, and `summarize.ps1` compile, measure, and aggregate the comparison. `reconstruct-before.cjs` replays the saved reversals against the matching current source. For future tuning, capture and verify the source and dependencies for each candidate before rerunning; do not silently substitute a different baseline or compare timings collected under different settings.

## Deferred tuning work

1. Prioritize common uncached positional SQL: the two- and sixteen-binding cases add approximately 0.734 us and 2.349 us per parse, respectively. They also allocate about 128 and 272 extra bytes per parse.
2. Profile the shared `QuestionMarkClassifier` pass, token inspection, and temporary index allocation. These are candidate optimization targets inferred from the implementation; this benchmark did not isolate their individual CPU costs.
3. Examine array classification and the lazy parenthesis indexes used by typed unary geometry. Preserve approximately linear scaling and safe handling of 32,000 nested parentheses.
4. Preserve the correctness fixes: every compact binding and its offset, contextual SQL/JSON clauses, real JSON operator operands, typed unary geometric operators, and quoted or split MyBatis bindings. Incorrect baseline counts and exception paths must not become optimization targets.
5. Reuse identical inputs and JVM settings, report allocation as well as timing, and check ordinary queries, cached calls, and stress cases. Run the existing parser and composition regression tests after implementation changes. An optimization must retain correct counts, SQL rewriting, and binding order.

Tuning remains deferred. Resume with profiling of common uncached positional queries and use these measurements to evaluate improvements without weakening the regression coverage.
